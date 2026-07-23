package com.orientsec.idap.core.controller;

import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import com.orientsec.idap.common.utils.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 制度问答控制器
 *
 * @author generaton
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/idap/v1/audit/institution-qa")
@Slf4j
public class InstitutionQaController {

    /**
     * 搜索制度问答
     */
    @PostMapping("/search")
    public Result search(@RequestParam Map<String, Object> params) {
        LogHelper.log(log, "进入制度问答搜索，参数：", params);
        try {
            String keyword = (String) params.get("keyword");
            Map<String, Object> result = mockSearchData(keyword);
            LogHelper.log(log, "制度问答搜索成功：", result);
            return ResultGenerator.genSuccessResult(result);
        } catch (Exception e) {
            LogHelper.error(log, e, "制度问答搜索失败：", e.getMessage());
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 获取关联制度文件
     */
    @GetMapping("/related-regulations")
    public Result getRelatedRegulations(@RequestParam String keyword) {
        LogHelper.log(log, "进入获取关联制度文件, keyword: ", keyword);
        try {
            List<Map<String, String>> regulations = mockRegulations(keyword);
            LogHelper.log(log, "获取关联制度文件成功, 数量：", regulations.size());
            return ResultGenerator.genSuccessResult(regulations);
        } catch (Exception e) {
            LogHelper.error(log, e, "获取关联制度文件失败：", e.getMessage());
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 获取相似案例
     */
    @GetMapping("/similar-cases")
    public Result getSimilarCases(@RequestParam String keyword) {
        LogHelper.log(log, "进入获取相似案例, keyword: ", keyword);
        try {
            List<Map<String, Object>> cases = mockSimilarCases(keyword);
            LogHelper.log(log, "获取相似案例成功, 数量：", cases.size());
            return ResultGenerator.genSuccessResult(cases);
        } catch (Exception e) {
            LogHelper.error(log, e, "获取相似案例失败：", e.getMessage());
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 获取推荐查询
     */
    @GetMapping("/recommended-queries")
    public Result getRecommendedQueries() {
        LogHelper.log(log, "进入获取推荐查询");
        try {
            List<String> queries = mockRecommendedQueries();
            LogHelper.log(log, "获取推荐查询成功, 数量：", queries.size());
            return ResultGenerator.genSuccessResult(queries);
        } catch (Exception e) {
            LogHelper.error(log, e, "获取推荐查询失败：", e.getMessage());
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 模拟搜索数据
     */
    private Map<String, Object> mockSearchData(String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("regulations", mockRegulations(keyword));
        result.put("cases", mockSimilarCases(keyword));
        return result;
    }

    /**
     * 模拟制度文件数据
     */
    private List<Map<String, String>> mockRegulations(String keyword) {
        List<Map<String, String>> regulations = new ArrayList<>();

        Map<String, String> reg1 = new HashMap<>();
        reg1.put("title", "关于进一步加强证券公司反洗钱客户身份识别工作的通知");
        reg1.put("type", "外部");
        reg1.put("date", "2024-01-10");
        regulations.add(reg1);

        Map<String, String> reg2 = new HashMap<>();
        reg2.put("title", "公司客户资金安全管理办法（2024 年修订）");
        reg2.put("type", "内部");
        reg2.put("date", "2024-03-01");
        regulations.add(reg2);

        Map<String, String> reg3 = new HashMap<>();
        reg3.put("title", "金融机构大额交易和可疑交易报告管理办法");
        reg3.put("type", "外部");
        reg3.put("date", "2023-12-08");
        regulations.add(reg3);

        Map<String, String> reg4 = new HashMap<>();
        reg4.put("title", "公司反洗钱工作管理办法");
        reg4.put("type", "内部");
        reg4.put("date", "2023-06-15");
        regulations.add(reg4);

        Map<String, String> reg5 = new HashMap<>();
        reg5.put("title", "证券公司客户资产管理业务规范");
        reg5.put("type", "外部");
        reg5.put("date", "2023-09-20");
        regulations.add(reg5);

        return regulations;
    }

    /**
     * 模拟相似案例数据
     */
    private List<Map<String, Object>> mockSimilarCases(String keyword) {
        List<Map<String, Object>> cases = new ArrayList<>();

        Map<String, Object> case1 = new HashMap<>();
        case1.put("title", "某证券公司客户资金管理违规案");
        case1.put("organization", "XX 证券有限责任公司");
        case1.put("authority", "中国证监会 XX 监管局");
        case1.put("date", "2024-02-28");
        case1.put("similarity", 92);
        case1.put("facts", "该公司在 2023 年经营过程中，存在以下违规行为：一是客户资金三方存管制度执行不到位，擅自划转客户资金用于归还公司");
        cases.add(case1);

        return cases;
    }

    /**
     * 模拟推荐查询数据
     */
    private List<String> mockRecommendedQueries() {
        List<String> queries = new ArrayList<>();
        queries.add("客户资金存管");
        queries.add("反洗钱义务");
        queries.add("大额交易报告");
        queries.add("可疑交易识别");
        queries.add("客户身份识别");
        return queries;
    }
}
