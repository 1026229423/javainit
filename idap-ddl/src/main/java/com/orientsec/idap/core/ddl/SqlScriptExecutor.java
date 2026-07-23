package com.orientsec.idap.core.ddl;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 通用 SQL 增量脚本变更本执行器
 * 支持执行指定路径的增量脚本和批量插入
 *
 * 使用方式：
 * 1. 在 idap-ddl/src/main/resources/ddl/ 目录下创建 SQL 脚本文件
 * 2. 修改 main 方法中的 sqlFileName 参数
 * 3. 运行本类
 */
public class SqlScriptExecutor {

    private JdbcTemplate jdbcTemplate;

    private static final String DEFAULT_SQL_FILE_PATH = "ddl/V1.0.0__init_user_info.sql";

    public SqlScriptExecutor() {
        this("jdbc:mysql://10.46.41.138:3307/idap_dev?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&nullCatalogMeansCurrent=true",
                "root", "MySQL.123456");
    }

    public SqlScriptExecutor(String jdbcUrl, String username, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(10000);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public static void main(String[] args) {
        SqlScriptExecutor executor = new SqlScriptExecutor();

        // 指定要执行的 SQL 文件名（相对于 idap-ddl/src/main/resources/ddl/ 目录）
        String sqlFileName = args.length > 0 ? args[0] : DEFAULT_SQL_FILE_PATH;

        System.out.println("=== 开始执行 SQL 增量变更脚本 ===");
        System.out.println(">>> SQL 文件：" + sqlFileName);

        try {
            executor.executeSqlFile(sqlFileName);
            System.out.println("=== SQL 增量变更脚本执行完成 ===");
        } catch (Exception e) {
            System.err.println("=== SQL 脚本执行失败：" + e.getMessage() + " ===");
            e.printStackTrace();
        }
    }

    /**
     * 执行 SQL 脚本文件
     * @param sqlFilePath SQL 文件路径（相对于 classpath:ddl/ 目录）
     */
    public void executeSqlFile(String sqlFilePath) {
        try {
            // 兼容两种路径格式
            String resourcePath = sqlFilePath.startsWith("ddl/") ? sqlFilePath : "ddl/" + sqlFilePath;
            Resource resource = new ClassPathResource(resourcePath);

            if (!resource.exists()) {
                throw new IOException("SQL 文件不存在：" + resourcePath);
            }

            String sqlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            executeSqlContent(sqlContent);

        } catch (IOException e) {
            throw new RuntimeException("读取 SQL 文件失败：" + sqlFilePath, e);
        }
    }

    /**
     * 执行 SQL 内容（支持多条 SQL 语句，分号分隔）
     * @param sqlContent SQL 内容
     */
    public void executeSqlContent(String sqlContent) {
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            System.out.println(">>> SQL 内容为空，跳过执行");
            return;
        }

        // 移除注释
        sqlContent = removeComments(sqlContent);

        // 分割 SQL 语句（按分号分隔，但要注意处理字符串内的分号）
        String[] sqlStatements = sqlContent.split(";(?=(?:[^']*'[^']*')*[^']*$)");

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (String sql : sqlStatements) {
            String trimmedSql = sql.trim();
            if (trimmedSql.isEmpty()) {
                continue;
            }

            try {
                System.out.println(">>> 执行 SQL：" + truncateSql(trimmedSql, 80));
                jdbcTemplate.execute(trimmedSql);
                successCount++;
                System.out.println(">>> 执行成功");
            } catch (Exception e) {
                // 判断是否是重复对象错误，这类错误可以忽略
                String errorMsg = e.getMessage().toLowerCase();
                if (errorMsg.contains("already exists") ||
                        errorMsg.contains("duplicate") ||
                        errorMsg.contains("exists") ||
                        errorMsg.contains("已存在")) {
                    System.out.println(">>> 跳过（对象已存在）：" + e.getMessage());
                    skipCount++;
                } else {
                    System.err.println(">>> 执行失败：" + e.getMessage());
                    errorCount++;
                }
            }
        }

        System.out.println("========================================");
        System.out.println("执行统计：成功=" + successCount + "，跳过=" + skipCount + "，失败=" + errorCount);
        System.out.println("========================================");
    }

    /**
     * 批量插入数据（适用于大量数据插入场景）
     * @param sql 插入 SQL（不含 VALUES）
     * @param values 参数值列表，每个 Object[] 代表一条数据
     */
    public void batchInsert(String sql, List<Object[]> values) {
        if (values == null || values.isEmpty()) {
            System.out.println(">>> 没有数据需要插入");
            return;
        }

        System.out.println(">>> 开始批量插入 " + values.size() + " 条数据...");

        try {
            int[] results = jdbcTemplate.batchUpdate(sql, values);
            System.out.println(">>> 批量插入完成，成功 " + results.length + " 条");
        } catch (Exception e) {
            System.err.println(">>> 批量插入失败：" + e.getMessage());
            throw new RuntimeException("批量插入失败", e);
        }
    }

    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 是否存在
     */
    public boolean isTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println(">>> 检查表是否存在失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 检查列是否存在
     * @param tableName 表名
     * @param columnName 列名
     * @return 是否存在
     */
    public boolean isColumnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println(">>> 检查列是否存在失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 检查索引是否存在
     * @param tableName 表名
     * @param indexName 索引名
     * @return 是否存在
     */
    public boolean isIndexExists(String tableName, String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, indexName);
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println(">>> 检查索引是否存在失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 移除 SQL 中的注释
     */
    private String removeComments(String sql) {
        // 移除单行注释
        sql = sql.replaceAll("--[^\\n]*", "");
        // 移除多行注释
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        return sql;
    }

    /**
     * 截断 SQL 用于日志输出
     */
    private String truncateSql(String sql, int maxLength) {
        if (sql.length() <= maxLength) {
            return sql;
        }
        return sql.substring(0, maxLength) + "...";
    }
}
