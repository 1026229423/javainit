# IDAP 项目 - CLAUDE.md

## 项目概述

企业级后台管理系统，采用前后端分离架构：
- **前端**：Vue 2.6.14 + Genesis UI（df-前缀组件）
- **后端**：Java 8 + Spring Boot + MyBatis Plus + Dubbo

## 外网迁移开发硬约束（长期有效）

本项目的目标是在外网环境中逐步替代、隔离原有内网能力。以下规则适用于所有后续开发（含 AI Agent）：

1. **不得新增任何依赖**：不新增 Maven、npm 或其他构建/运行时依赖，也不升级或替换现有依赖版本；确有必要时，先说明用途、替代方案和影响，并等待项目负责人明确批准。
2. **不主动开发或扩展 Genesis 内网集成**：Java 侧 `com.orientsec.genesis` 相关父 POM、私有 Jar、BJCA/SSO、Dubbo 用户权限服务、LDAP 组织服务、ZooKeeper 注册中心及其配置，均只允许为保持现状而阅读、排错或隔离。任何新增功能、修改调用逻辑、接入或恢复内网联通，必须先征得项目负责人同意。
3. **前端完全禁止修改**：`idap-ui/` 是从其他团队直接拉取的代码，不得修改、格式化、生成文件、更新锁文件或调整配置；前端需求必须先由项目负责人明确授权。
4. **默认开发范围**：仅限现有 Java 业务代码中可脱离内网运行的部分；涉及数据库、认证、用户/组织信息等边界时，应先明确是否需要 Mock、替代实现或仅做接口隔离，不能自行假设。
5. **例外流程**：触及以上任一限制时，先向项目负责人说明拟修改的文件、目的、依赖/联通影响与可选方案；在获得明确批准前停止修改。

## 项目结构

```
idap-proj-parent/
├── idap-ui/                     # 前端项目（Vue 2.6.14 + Genesis UI）
│   ├── src/
│   │   ├── layout/              # 布局组件
│   │   ├── modules/             # 业务模块（微应用架构）
│   │   ├── shared/              # 共享资源
│   │   ├── router/              # 路由配置
│   │   ├── store/               # Vuex 状态管理
│   │   └── ...
├── idap-server/                 # 后端启动模块（Spring Boot 应用）
├── idap-service/                # 业务服务模块（Controller/Service/Mapper）
├── idap-common/                 # 公共模块（工具类/统一响应/常量）
├── idap-ddl/                    # 数据库建表语句模块
├── idap-genesis/                # Genesis 框架集成模块
├── idap-job/                    # 定时任务模块
└── autocode-gen/                # 代码生成器模块
```

# 前端项目规范（idap-ui）

## 技术栈

| 类型 | 技术 | 版本/说明 |
|---|---|---|
| 框架 | Vue | 2.6.14 |
| UI 库 | Genesis UI | df-前缀组件 |
| 路由 | Vue Router | 3.x |
| 状态管理 | Vuex | 3.x |
| HTTP | Axios | 0.26.x |
| 构建 | Vue CLI | 5.x |

## 目录结构

```
idap-ui/src/
├── layout/                      # 布局组件
│   └── Index.vue                # 顶部导航 + 内容区
├── modules/                     # 业务模块（微应用）
│   └── system/                  # 系统管理模块
│       ├── api/                 # API 接口调用
│       ├── constants/           # 常量/枚举/配置
│       ├── composables/         # 业务逻辑组合函数
│       ├── components/          # 模块私有组件
│       ├── types/               # 类型定义
│       └── views/               # 页面视图
├── shared/                      # 共享资源
│   ├── api/                     # 全局 API 封装（request.js）
│   ├── components/              # 公共组件（BaseTable、BaseSearch、BaseFormDialog）
│   ├── constants/               # 全局常量
│   ├── utils/                   # 工具函数
│   └── types/                   # 全局类型
├── router/                      # 路由配置
├── store/                       # Vuex 状态管理
├── assets/                      # 静态资源
├── styles/                      # 全局样式
├── App.vue
└── main.js
```

## Genesis UI 组件规范

所有组件使用 `df-` 前缀：

```html
<!-- 基础组件 -->
<df-button>, <df-icon>, <df-link>

<!-- 表单组件 -->
<df-input>, <df-select>, <df-checkbox>, <df-radio>, <df-form>,
<df-switch>, <df-slider>, <df-input-number>, <df-cascader>,
<df-date-picker>, <df-time-picker>, <df-upload>, <df-rate>

<!-- 数据展示 -->
<df-table>, <df-table-column>, <df-tag>, <df-badge>,
<df-tree>, <df-pagination>, <df-descriptions>

<!-- 导航组件 -->
<df-menu>, <df-submenu>, <df-menu-item>, <df-tabs>,
<df-tab-pane>, <df-breadcrumb>, <df-dropdown>

<!-- 反馈组件 -->
<df-message>, <df-notification>, <df-dialog>, <df-loading>,
<df-popover>, <df-tooltip>, <df-drawer>

<!-- 布局组件 -->
<df-container>, <df-header>, <df-main>, <df-aside>,
<df-row>, <df-col>, <df-card>, <df-divider>
```

**genesis-ui** 组件详细使用方法参考：`D:\claude_project\genesis-ui\genesis-ui-doc`

## 微应用模块规范

每个业务模块必须包含：

| 文件夹 | 用途 | 示例 |
|---|---|---|
| `views/` | 页面组件 | `UserList.vue`、`UserForm.vue` |
| `components/` | 模块私有组件 | `UserSelect.vue` |
| `composables/` | 业务逻辑 | `useUser.js` |
| `api/` | 后端接口 | `user.js` |
| `constants/` | 常量配置 | `user.js`（`USER_STATUS_OPTIONS`） |
| `types/` | 类型定义 | `user.js`（`UserItem`） |

### 命名规范

```javascript
// API 函数 - camelCase，动词开头
getUserList, createUser, updateUser, deleteUser

// Composable - use 前缀
useUser, useRole, useDept

// 组件 - PascalCase
UserList, RoleForm

// 常量 - 大写下划线
USER_STATUS, USER_STATUS_OPTIONS

// 类型 - PascalCase
UserItem, UserQueryParams
```

### Composable 模式

```javascript
// modules/system/composables/useUser.js
export function useUser() {
  let loading = false
  let userList = []
  let total = 0
  let queryParams = { page: 1, pageSize: 10 }

  const fetchList = async () => { /* ... */ }
  const saveUser = async (data) => { /* ... */ }
  const removeUser = async (ids) => { /* ... */ }

  return { loading, userList, total, queryParams, fetchList, saveUser, removeUser }
}
```

### API 封装模式

```javascript
// modules/system/api/user.js
import { request } from '@/shared/api/request'

export const getUserList = (params) => {
  return request('/user/list', params)
}
```

### 常量配置模式

```javascript
// modules/system/constants/user.js
export const USER_STATUS = {
  ENABLED: { value: 1, label: '启用', type: 'success' },
  DISABLED: { value: 0, label: '禁用', type: 'danger' }
}

export const USER_STATUS_OPTIONS = [
  { label: '全部', value: undefined },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]
```

### 页面设计规范

**整体风格**

- **简约企业级后台风格**：干净、专业、高效的金融/管理系统风格，避免深色主题
- **清晰高对比度配色**：使用较高对比度的配色方案，确保内容清晰易读，减少视觉疲劳
- **内容区域**：白色卡片式布局，以中等灰度背景衬托，层次分明
- **简洁原则**：减少视觉干扰，避免过多装饰，保持界面干净利落
- **清晰原则**：文字与背景对比度 WCAG AA 标准以上，组件边界清晰易识别

**布局规范**

- **布局结构**：使用 Genesis UI 组件
- **固定定位**：Header、Sider 固定，不随滚动条移动
- **侧边栏宽度**：展开 208px，折叠 64px（Ant Design 标准值）
- **内容区边距**：统一 16px～24px；表格/列表页使用 24px，表单页使用 16px
- **容器宽度**：内容尽量铺满全屏，宽度 100%，最小宽度 1080px；超大屏居中显示
- **滚动规则**：仅内容区滚动，导航栏永久固定

**色彩体系（高对比度、易识别）**

- **主色调**：
  - 品牌蓝：`#1677FF`（主要按钮、链接、激活状态，`colorPrimary`）
  - 成功绿：`#00B578`（成功状态、正向指标）
  - 警告黄：`#FF7D00`（警告、待处理）
  - 危险红：`#F53F3F`（错误、高风险、紧急）
- **中性色（增强对比度）**：
  - 标题文字：`#000000`（纯黑，标题必须醒目）
  - 正文文字：`#1D2129`（近黑色，正文高可读性）
  - 次要文字：`#4E5969`（深灰色，次要信息清晰可见）
  - 辅助文字：`#86909C`（中灰色，辅助说明）
  - 禁用文字：`#C9CDD4`（浅灰色，禁用状态）
  - 分割线/边框：`#D1D5DB`（中等灰色边框，边界清晰）
  - 背景灰：`#E5E7EB`（中度灰色背景，与内容区对比明显）
  - 卡片白：`#FFFFFF`（纯白卡片，层次分明）

## 公共组件使用

### BaseTable

```vue
<base-table
  v-loading="loading"
  :data="tableData"
  :columns="columns"
  :total="total"
  :has-selection="true"
  :has-action-column="true"
  @page-change="handlePageChange"
>
  <template slot="status" slot-scope="{ row }">
    <df-tag :type="row.status === 1 ? 'success' : 'danger'">
      {{ row.status === 1 ? '启用' : '禁用' }}
    </df-tag>
  </template>
  <template slot="action" slot-scope="{ row }">
    <df-button type="text" @click="handleEdit(row)">编辑</df-button>
  </template>
</base-table>
```

**分页配置规范**：

- 每页条数选项：`[50, 100, 200]`
- 默认每页条数：`100`
- 默认页码：1

```javascript
// 分页参数默认值
queryParams: {
  page: 1,
  pageSize: 100 // 默认 100 条
}

// 重置时的默认值
handleReset() {
  this.queryParams = {
    page: 1,
    pageSize: 100, // 默认 100 条
    userName: '',
    mobile: ''
    // ...
  }
}
```

### BaseSearch

```vue
<base-search
  v-model="searchForm"
  :config="searchConfig"
  @search="handleSearch"
  @reset="handleReset"
/>
```

### BaseFormDialog

```vue
<base-form-dialog
  :visible.sync="dialogVisible"
  :title="dialogTitle"
  :form-data="formData"
  :fields="formFields"
  :rules="formRules"
  @submit="handleSubmit"
/>
```

### 详情/编辑弹窗样式规范

使用 `df-dialog` 和 `df-descriptions` 实现详情查看或编辑弹窗：

```vue
<df-dialog
  v-if="dialogVisible"
  :visible.sync="dialogVisible"
  :title="dialogTitle"
  width="800px"
  :show-close="true"
>
  <div class="detail-content">
    <df-descriptions :column="2" border>
      <df-descriptions-item label="用户名" :span="2">
        {{ formData.userName || '' }}
      </df-descriptions-item>
      <df-descriptions-item label="手机号">
        {{ formData.mobile || '' }}
      </df-descriptions-item>
      <df-descriptions-item label="邮箱">
        {{ formData.email || '' }}
      </df-descriptions-item>
      <df-descriptions-item label="状态">
        <df-tag :type="formData.status === 1 ? 'success' : 'danger'">
          {{ formData.status === 1 ? '启用' : '禁用' }}
        </df-tag>
      </df-descriptions-item>
      <df-descriptions-item label="备注" :span="2">
        {{ formData.remark || '' }}
      </df-descriptions-item>
    </df-descriptions>
  </div>
  <template slot="footer">
    <df-button @click="handleDialogClose">取消</df-button>
    <df-button type="primary" @click="handleEditSubmit">确定</df-button>
  </template>
</df-dialog>
```

```scss
<style lang="scss" scoped>
.detail-content {
  padding: 8px 0;

  ::v-deep .df-descriptions-item__label {
    background-color: #f5f7fa;
    font-weight: 500;
    color: #1d2129;
  }
}
</style>
```

**关键属性**：

- `:column="2"`：两列布局，单列时使用 `:column="1"`
- `border`：显示边框
- `:span="2"`：跨两列显示
- 标签背景色通过 `::v-deep .df-descriptions-item__label` 设置

## 请求封装

```javascript
import { request, get, upload, download } from '@/shared/api/request'

// POST（默认）
await request('/user/list', { page: 1, pageSize: 10 })

// GET
await get('/user/detail', { id: 1 })

// 上传
await upload('/user/import', formData)

// 下载
await download('/user/export', params, '文件.xlsx')
```

拦截器：

- 请求：自动注入 `Authorization: Bearer xxx`
- 响应：统一错误处理，401 跳转登录

## 开发规范

1. **必须使用 `df-` 前缀的 Genesis UI 组件**
2. **禁止在页面中直接调用 API，必须通过 composable**
3. **常量配置与业务逻辑分离**
4. **表格操作列超过 3 个按钮时使用下拉菜单收纳**
5. **`df-button` 默认不需要 `icon` 属性**——按钮内文字需居中显示，避免使用 `icon` 属性导致文字偏移

模块开发顺序：

1. `api/*.js`——定义接口
2. `constants/*.js`——定义常量
3. `composables/useXxx.js`——封装逻辑
4. `views/XxxList.vue`——实现页面
5. 更新 `router/index.js`——添加路由

# 后端项目规范

## 技术栈

| 类型 | 技术 | 版本/说明 |
|---|---|---|
| 语言 | Java | 8 |
| 框架 | Spring Boot | 2.x |
| ORM | MyBatis Plus | 3.5.12 |
| RPC | Dubbo | 2.7.8 |
| 数据库 | MySQL | 8.0.11 |
| 连接池 | Druid | 1.1.20 |
| 工具库 | Hutool | 5.8.40 |

## 模块结构

`idap-ui` 是独立前端工程，不属于后端模块。后端模块结构如下：

```text
idap-proj-parent/
├── idap-server/      # Spring Boot 启动模块
├── idap-service/     # 业务服务（Controller/Service/Mapper）
├── idap-common/      # 公共模块（工具/响应/常量）
├── idap-ddl/         # 数据库建表语句
├── idap-genesis/     # Genesis 框架集成
├── idap-job/         # 定时任务
└── idap-test/        # 测试模块
```

## 统一响应格式

### Result 统一响应

```java
package com.orientsec.idap.common.model;

public class Result<T> {
  private int code;
  private String message;
  private T data;

  // getter/setter
}
```

### ResultGenerator 生成工具

```java
// 成功响应（无数据）
ResultGenerator.genSuccessResult();

// 成功响应（带数据）
ResultGenerator.genSuccessResult(data);

// 失败响应
ResultGenerator.genFailResult(e);
```

### 响应码定义

```java
// ResultCode.java
SUCCESS(200),
FAIL(400),
UNAUTHORIZED(401),
NOT_FOUND(404),
INTERNAL_SERVER_ERROR(500);
```

## Controller 规范

```java
package com.orientsec.idap.core.controller;

@RestController
@RequestMapping("/idap/v1/{module}")
public class XxxController {

  @Autowired
  private XxxService xxxService;

  /**
   * 查询列表
   */
  @GetMapping("/list")
  public Result list(XxxQueryDTO dto) {
    return ResultGenerator.genSuccessResult(xxxService.list(dto));
  }

  /**
   * 查询详情
   */
  @GetMapping("/detail")
  public Result detail(@RequestParam String id) {
    return ResultGenerator.genSuccessResult(xxxService.getById(id));
  }

  /**
   * 新增
   */
  @PostMapping("/create")
  public Result create(@RequestBody XxxDTO dto) {
    xxxService.create(dto);
    return ResultGenerator.genSuccessResult();
  }

  /**
   * 更新
   */
  @PostMapping("/update")
  public Result update(@RequestBody XxxDTO dto) {
    xxxService.update(dto);
    return ResultGenerator.genSuccessResult();
  }

  /**
   * 删除
   */
  @PostMapping("/delete")
  public Result delete(@RequestParam String id) {
    xxxService.delete(id);
    return ResultGenerator.genSuccessResult();
  }
}
```

### 路径规范

- 统一前缀：`/idap/v1/`
- 模块名：小写，如 `/idap/v1/user`
- 方法：RESTful 风格

## Service 规范

### Service 接口

```java
package com.orientsec.idap.core.service;

public interface XxxService {
  List<XxxDTO> list(XxxQueryDTO dto);
  XxxDTO getById(String id);
  void create(XxxDTO dto);
  void update(XxxDTO dto);
  void delete(String id);
}
```

### Service 实现

```java
package com.orientsec.idap.core.service.impl;

@Service
public class XxxServiceImpl implements XxxService {

  @Autowired
  private XxxMapper xxxMapper;

  @Override
  public List<Xxx> list(XxxQueryDTO dto) {
    LambdaQueryWrapper<Xxx> param = new LambdaQueryWrapper<>();
    param.eq(dto.getStatus() != null, Xxx::getStatus, dto.getStatus())
        .like(StringUtils.isNotBlank(dto.getKeyword()), Xxx::getName, dto.getKeyword());
    return xxxMapper.selectList(param);
  }

  @Override
  public Xxx getById(String id) {
    return xxxMapper.selectById(id);
  }

  @Override
  public void create(Xxx dto) {
    dto.setId(IdUtil.fastSimpleUUID());
    dto.setCreateTime(new Date());
    dto.setCreateUser(UserContext.getUsername());
    xxxMapper.insert(dto);
  }

  @Override
  public void update(Xxx dto) {
    xxxMapper.updateById(dto);
  }

  @Override
  public void delete(String id) {
    xxxMapper.deleteById(id);
  }
}
```

## Entity 规范（MyBatis Plus）

```java
package com.orientsec.idap.core.model;

@TableName("idap_xxx")
public class Xxx implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId("id")
  private String id;

  @TableField("xxx_name")
  private String xxxName;

  @TableField("create_time")
  private Date createTime;

  @TableField("create_user")
  private String createUser;

  // getter/setter
}
```

### 注解说明

- `@TableName("table_name")`：表名映射
- `@TableId("id")`：主键字段
- `@TableField("column_name")`：字段映射
- `@TableLogic`：逻辑删除字段

## Mapper 规范

```java
package com.orientsec.idap.core.mapper;

public interface XxxMapper extends BaseMapper<Xxx> {
  // 自定义方法
  List<XxxDTO> selectCustomList(XxxQueryDTO dto);
}
```

### 继承 BaseMapper

- `selectList()`：查询列表
- `selectById()`：根据 ID 查询
- `insert()`：插入
- `updateById()`：根据 ID 更新
- `deleteById()`：根据 ID 删除

所有 Mapper 继承 `com.baomidou.mybatisplus.core.mapper.BaseMapper`。

### LambdaQueryWrapper 查询

使用 `LambdaQueryWrapper` 进行类型安全的条件查询：

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

// 基础查询
LambdaQueryWrapper<IdapTestUser> param = new LambdaQueryWrapper<>();
param.eq(IdapTestUser::getId, "1");
List<IdapTestUser> list = idapTestUserMapper.selectList(param);

// 多条件查询
LambdaQueryWrapper<IdapTestUser> query = new LambdaQueryWrapper<>();
query.eq(IdapTestUser::getStatus, 1)
    .like(IdapTestUser::getUserName, "张")
    .ge(IdapTestUser::getAge, 18)
    .orderByDesc(IdapTestUser::getCreateTime);
List<IdapTestUser> users = idapTestUserMapper.selectList(query);

// 查询单个
LambdaQueryWrapper<IdapTestUser> oneQuery = new LambdaQueryWrapper<>();
oneQuery.eq(IdapTestUser::getUserId, "U001");
IdapTestUser user = idapTestUserMapper.selectOne(oneQuery);

// 查询数量
Long count = idapTestUserMapper.selectCount(query);

// 删除
idapTestUserMapper.delete(query);
```

### 常用条件方法

| 方法 | 说明 | 示例 |
|---|---|---|
| `eq` | 等于 | `eq(IdapTestUser::getStatus, 1)` |
| `ne` | 不等于 | `ne(IdapTestUser::getStatus, 0)` |
| `gt` | 大于 | `gt(IdapTestUser::getAge, 18)` |
| `ge` | 大于等于 | `ge(IdapTestUser::getAge, 18)` |
| `lt` | 小于 | `lt(IdapTestUser::getAge, 60)` |
| `le` | 小于等于 | `le(IdapTestUser::getAge, 60)` |
| `like` | 模糊查询 | `like(IdapTestUser::getUserName, "张")` |
| `in` | IN 查询 | `in(IdapTestUser::getStatus, Arrays.asList(1, 2))` |
| `isNull` | 为空 | `isNull(IdapTestUser::getRemark)` |
| `isNotNull` | 不为空 | `isNotNull(IdapTestUser::getEmail)` |
| `orderByAsc` | 升序 | `orderByAsc(IdapTestUser::getCreateTime)` |
| `orderByDesc` | 降序 | `orderByDesc(IdapTestUser::getCreateTime)` |

## DTO/VO 规范

### Query DTO（查询参数）

```java
public class XxxQueryDTO {
  private String keyword;       // 关键词
  private Integer status;       // 状态
  private Date startTime;       // 开始时间
  private Date endTime;         // 结束时间
  private Integer page = 1;     // 页码
  private Integer pageSize = 10;// 每页条数
  // getter/setter
}
```

### Form DTO（表单数据）

```java
public class XxxDTO {
  private String id;
  private String name;
  private Integer status;
  private String remark;
  // getter/setter
}
```

### VO（视图对象）

```java
public class XxxVO {
  private String id;
  private String name;
  private String statusLabel; // 状态文本
  private Date createTime;
  // getter/setter
}
```

## idap-ddl 模块规范

`idap-ddl` 模块用于保存数据库建表语句。

### 目录结构

```text
idap-ddl/src/main/
└── resources/
    └── ddl/
        ├── V1.0.0__init.sql
        └── V1.0.1__add_user_table.sql
```

### 命名规范

```text
V{版本号}--{描述}.sql
```

- 版本号：`V1.0.0`、`V1.0.1`、`V1.1.0`
- 描述：小写字母加下划线，如 `add_user_table`

### 建表规范

```sql
CREATE TABLE IF NOT EXISTS `idap_user` (
  `id` VARCHAR(32) NOT NULL COMMENT '主键 ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 字段规范

- `id`：`VARCHAR(32)`，主键 UUID
- `create_time`：`DATETIME`，创建时间
- `create_user`：`VARCHAR(50)`，创建人
- `update_time`：`DATETIME`，更新时间
- `update_user`：`VARCHAR(50)`，更新人
- `deleted`：`TINYINT`，逻辑删除标记

### 命名规范

- 表名：`idap_{模块}_{业务}`，如 `idap_sys_user`
- 字段：小写字母加下划线，如 `user_name`
- 索引：`idx_{字段}`（普通索引）、`uk_{字段}`（唯一索引）

## 代码生成

使用 `autocode-gen` 模块生成基础代码：

1. 在 `idap-ddl` 中编写建表语句
2. 运行代码生成器
3. 生成的代码位于 `idap-service` 模块
4. 手动补充业务逻辑

## 公共工具类

### idap-common 模块

```java
// 常量
com.orientsec.idap.common.constants.YesNoEnum // 是否枚举

// 工具
com.orientsec.idap.common.utils.Utils         // 通用工具
com.orientsec.idap.common.utils.LogHelper     // 日志助手

// 模型
com.orientsec.idap.common.model.Result         // 统一响应
com.orientsec.idap.common.model.ResultCode     // 响应码
com.orientsec.idap.common.model.ResultGenerator// 响应生成器
```

## 配置规范

### application.yml

```yaml
server:
  port: 8090

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/idap?useUnicode=true&characterEncoding=UTF-8
    username: root
    password: root

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.orientsec.idap.core.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 开发流程

### 1. 数据库设计

在 `idap-ddl/src/main/resources/ddl/` 中创建建表脚本：

```sql
CREATE TABLE `idap_sys_user` (...) COMMENT='用户表';
```

### 2. 生成代码

运行代码生成器，生成：

- Entity：`Xxx.java`
- Mapper：`XxxMapper.java`
- Service：`XxxService.java`、`XxxServiceImpl.java`
- Controller：`XxxController.java`

### 3. 补充业务

在 `idap-service` 模块中：

- 补充 DTO/VO 类
- 实现业务逻辑
- 添加权限控制

### 4. 前端对接

`idap-ui` 是独立的 Vue 前端项目。在 `idap-ui` 中：

- 创建 API 接口
- 实现页面组件
- 联调测试

## 开发协作规范

### 前后端协同开发

1. **每次修改功能时，必须同步检查前后端**，保持接口一致
2. **Mock 数据由后端提供**，前端通过调用后端 API 获取模拟数据
3. **接口变更需同步**，后端接口变更时应及时更新前端 API 调用

### 联调流程

1. 后端先提供接口文档（路径、参数、响应格式）
2. 前端根据文档调用接口
3. 发现问题及时沟通调整
4. 接口稳定后不再随意变更

## 关键规则

1. **禁止在日志或响应中暴露密钥**等敏感信息
2. **日志使用 LogHelper 打印**
3. **列表接口必须返回 `total` 总数**
4. **方法必须有日志记录**（进入和退出）
5. **每个方法只能有一个 return 语句**
6. **每个方法最多 4 个参数**
7. **遵循现有模式**——先查看类似文件
8. **高内聚低耦合**——模块化开发，职责单一
9. **最小改动原则**——只修改必要代码，保持现有结构稳定
10. **重用现有接口和常量**——优先复用已有功能，统一常量定义
11. **零低级错误**——杜绝语法错误、变量未定义、空指针、除零等
12. **有效日志**——方法进入和返回前必须打印有效日志，包含关键上下文信息

### 日志规范

```java
// 正确示例
public Result list(XxxQueryDTO dto) {
  LogHelper.log(log, "进入用户列表查询，参数：", dto);
  Result result;
  try {
    List<XxxDTO> list = xxxService.list(dto);
    LogHelper.log(log, "用户列表查询成功，数量：", list.size());
    result = ResultGenerator.genSuccessResult(list);
  } catch (Exception e) {
    LogHelper.error(log, e, "用户列表查询失败：", e.getMessage());
    result = ResultGenerator.genFailResult(e);
  }
  return result;
}

// 错误示例：多个 return
public Result getData() {
  if (condition) {
    return success(); // 错误
  }
  return fail(); // 错误
}
```

### 参数过多时的处理

```java
// 错误：参数超过 4 个
public void createUser(
    String username,
    String password,
    String email,
    Integer age,
    String phone,
    String address) {
  // ...
}

// 正确：使用 DTO 封装
public void createUser(CreateUserDTO dto) {
  // ...
}
```

## 注意事项

1. **所有 API 响应必须使用 `Result` 封装**
2. **Controller 中不写业务逻辑，只调用 Service**
3. **数据库操作必须使用 MyBatis Plus，禁止手写 SQL**
   - 使用 `LambdaQueryWrapper` 进行条件查询
   - 使用 `BaseMapper` 提供的方法进行 CRUD 操作
4. **建表语句必须放在 `idap-ddl` 模块**
5. **实体类必须实现 `Serializable` 接口**
6. **所有字段必须有 `COMMENT` 注释**
7. **必须使用逻辑删除，禁止物理删除**
8. **Service 中优先使用 `LambdaQueryWrapper`，保证类型安全**

## 重要开发规范

### Vue 2.6.14 兼容性

**禁止使用 `setup()` 语法**——项目使用 Vue 2.6.14，不支持 Composition API，必须使用 Options API：

```javascript
// 错误：Vue 2.6 不支持 setup()
export default {
  setup() {
    const { loading, fetchList } = useUserInfo()
    return { loading, fetchList }
  }
}

// 正确：使用 data() 和 methods
export default {
  data() {
    return {
      loading: false,
      userList: []
    }
  },
  mounted() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      getUserList(this.queryParams).then(res => {
        this.userList = res.data?.records || []
      })
    }
  }
}
```

### LogHelper 正确用法

配合 `@Slf4j` 注解使用，先调用 `LogHelper.log()` 记录参数：

```java
@RestController
@Slf4j
public class XxxController {

  @Autowired
  private XxxService xxxService;

  @GetMapping("/list")
  public Result list(XxxQueryDTO dto) {
    // 先调用 LogHelper.log 记录参数
    LogHelper.log(log, "进入用户列表查询，参数：", dto);

    try {
      List<XxxDTO> list = xxxService.list(dto);
      LogHelper.log(log, "查询成功，数量：", list.size());
      return ResultGenerator.genSuccessResult(list);
    } catch (Exception e) {
      LogHelper.error(log, e, "查询失败：", e.getMessage());
      return ResultGenerator.genFailResult(e);
    }
  }
}
```

### 代码生成器使用说明

**执行类**：`com.orientsec.idap.autocode.run.IdapAutoCodeApp`

**使用步骤**：

1. **修改配置**——编辑 `IdapAutoCodeApp.java`，指定要生成代码的表名：

```java
public class IdapAutoCodeApp {
  public static void main(String[] args) {
    List<String> table = new ArrayList<>();
    table.add("idap_user_info"); // 要生成代码的表名
    AutoCodeBase.doStart("idap", table, args);
  }
}
```

2. **运行生成器**——执行 Maven 命令：

```bash
mvn compile exec:java -Dexec.mainClass="com.orientsec.idap.autocode.run.IdapAutoCodeApp" -pl autocode-gen
```

3. **生成文件位置**：

   - Entity：`idap-service/src/main/java/com/orientsec/idap/core/model/`
   - Mapper：`idap-service/src/main/java/com/orientsec/idap/core/mapper/`
   - Service：`idap-service/src/main/java/com/orientsec/idap/core/service/`
   - Controller：`idap-service/src/main/java/com/orientsec/idap/core/controller/`
   - Mapper XML：`idap-service/src/main/resources/com/orientsec/idap/core/mapper/ext/`

4. **补充业务逻辑**——生成的 Controller 只有空壳，需要手动补充 CRUD 方法。

**完整开发流程**：

1. 在 `idap-ddl/src/main/resources/ddl/` 创建建表 SQL，例如 `V1.0.0__init_user_info.sql`
2. 运行 Java 脚本 `SqlScriptExecutor.java` 创建数据库表（可选）
3. 修改 `IdapAutoCodeApp.java`，配置要生成代码的表名
4. 运行代码生成器生成基础代码
5. 补充 Controller 业务逻辑
6. 在前端项目 `idap-ui` 开发对应页面：
   - `api/userInfo.js`——API 接口
   - `constants/userInfo.js`——常量配置
   - `composables/useUserInfo.js`——业务逻辑（仅用于逻辑复用，页面仍使用 Options API）
   - `views/UserInfoList.vue`——页面组件

## 常见问题排查

### 1. df-icon 组件报错 “Missing required prop: iconClass”

**问题**：`df-icon` 组件必须使用 `icon-class` 属性，不支持 `name` 属性。

**错误示例**：

```html
<df-icon name="arrow-down"></df-icon>
```

**正确示例**：

```html
<df-icon icon-class="arrow-down"></df-icon>
```

### 2. BaseSearch 组件报错 “Cannot read properties of undefined (reading '$options')”

**问题**：`BaseSearch` 组件内部使用了 `<df-form-item>`，必须包裹在 `<df-form>` 中才能获取 form 上下文。

**解决方案**：在 `BaseSearch.vue` 模板中添加 `<df-form>` 包裹所有表单项。

### 3. 405 Method Not Allowed 错误

**问题**：前端 API 调用方法与后端 Controller 不匹配。

**解决方案**：

- 后端使用 `@GetMapping` 时，前端使用 `get()` 方法
- 后端使用 `@PostMapping` 时，前端使用 `request()` 方法（POST）

```javascript
// 后端 @GetMapping("/list")
export const getUserList = (params) => {
  return get('/user/list', params) // 使用 get()
}

// 后端 @PostMapping("/create")
export const createUser = (data) => {
  return request('/user/create', data) // 使用 request()
}
```
