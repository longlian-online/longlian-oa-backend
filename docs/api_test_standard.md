# API 测试规范

## 一、测试类组织结构

### 1.1 包结构

```
src/test/java/online/longlian/app/api/
├── BaseApiTest.java                    # 测试基类（不允许直接修改）
├── util/                               # 测试工具类
│   └── DatabaseCleanupUtil.java
└── {模块名}/
    └── {功能名}ApiTest.java           # 按模块/功能分组
```

### 1.2 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 测试类 | `{功能}ApiTest` | `AdminSessionApiTest`, `ScheduledTaskApiTest` |
| 测试方法 | `should{场景}{预期结果}` | `shouldLoginSuccessfully`, `shouldFailWithWrongPassword` |
| 测试数据构造方法 | `create{实体}` | `createAdmin()`, `createOrganization()` |

### 1.3 测试文件位置

- 管理端接口测试：`src/test/java/online/longlian/app/api/admin/`
- 用户端接口测试：`src/test/java/online/longlian/app/api/app/`
- 组织端接口测试：`src/test/java/online/longlian/app/api/orgadmin/`

---

## 二、测试方法命名规范

### 2.1 命名模式

测试方法必须使用 `should{场景}{预期结果}` 格式：

```
shouldLoginSuccessfully                    # 成功场景
shouldFailWithWrongPassword                # 失败场景-具体原因
shouldFailCreateAdminWithoutAuth           # 失败场景-缺少认证
shouldChangeOrgStatusToDisabled            # 状态转换
shouldListOrganizationsWithNameFilter      # 条件查询
```

### 2.2 方法数量要求

每个 API 测试类必须覆盖以下几类测试：

| 测试类型 | 说明 | 最少数量 |
|---------|------|---------|
| 成功路径 | 正常参数调用成功 | 1+ |
| 认证失败 | 未认证/Token无效/Token过期 | 1+ |
| 参数校验失败 | 空值、长度超限、格式错误 | 1+ |
| 业务规则失败 | 权限不足、操作对象不存在等 | 1+ |
| 边界条件 | 分页边界、空数据、极端值 | 1+ |

---

## 三、测试数据准备

### 3.1 测试隔离原则

**每个测试方法必须独立**，通过 `@BeforeEach` 自动清空数据库保证隔离。禁止在测试之间共享状态。

### 3.2 测试数据构造

使用 `BaseApiTest` 提供的辅助方法：

```java
// 创建管理员（角色：root / ADMIN / SUPER_ADMIN）
createAdmin(long id, String username, String password, String role)

// 创建用户端用户
createTestUser(long userId, String username, String password, String email)

// 创建组织
createOrganization(long orgId, String name)

// 创建组织成员
createOrganizationMember(long id, long orgId, long userId, String orgRole)

// 一站式创建用户+组织+成员
createUserWithOrganization(long userId, String username, String password,
                           String email, long orgMemberId, long orgId, String orgRole)
```

### 3.3 数据 ID 分配

为避免测试间冲突，采用固定 ID 分配策略：

| 测试类 | ID 范围 |
|--------|--------|
| AdminSessionApiTest | 1-10 |
| AdminManagementApiTest | 10-100 |
| AdminOrganizationApiTest | 100-1000 |
| ScheduledTaskApiTest | 1000+ |

单个测试方法内创建多条数据时，使用自增偏移：

```java
// 良好示例
createAdmin(1L, "admin", "123456", "SUPER_ADMIN");
createAdmin(2L, "admin2", "123456", "SUPER_ADMIN");  // ID 偏移

// 禁止示例 - 可能与 BeforeEach 清空前的数据冲突
createAdmin(1L, "admin", "123456", "SUPER_ADMIN");
createAdmin(1L, "another", "123456", "SUPER_ADMIN");  // ID 重复
```

### 3.4 密码规范

测试用户密码统一使用 `123456`，除非测试密码相关场景（如密码长度校验）。

---

## 四、认证与请求

### 4.1 认证方法

| 方法 | 用途 |
|------|------|
| `loginAs(username, password)` | 用户端登录 |
| `adminLoginAs(username, password)` | 管理端登录 |

```java
// 管理端示例
String token = adminLoginAs("admin", "123456");
authRequest(token).get("/admin/admins/");

// 用户端示例
String token = loginAs("user", "123456");
authRequest(token).get("/app/session/");
```

### 4.2 请求规范

```java
// 无认证请求
request().post("/admin/session");

// 带认证请求
authRequest(token)
    .body(Map.of("key", "value"))
    .post("/endpoint");
```

### 4.3 请求体构造

使用 `Map.of()` 创建简洁的请求体：

```java
// 推荐
.body(Map.of("username", "admin", "password", "123456"))

// 禁止 - 过于冗长
.body(new HashMap<String, String>() {{
    put("username", "admin");
    put("password", "123456");
}})
```

---

## 五、断言规范

### 5.1 断言结构

```java
response
    .then()
    .statusCode(200)
    .body("code", equalTo(0))
    .body("msg", equalTo("操作成功"))
    .body("data.token", notNullValue())
    .body("data.list.size()", greaterThanOrEqualTo(0));
```

### 5.2 常用断言模式

| 场景 | 断言 |
|------|------|
| 成功响应 | `statusCode(200)`, `code", equalTo(0)` |
| 失败响应 | `code", equalTo(ResultCode.XXX.getCode())` |
| 未认证 | `statusCode(401)` |
| 参数错误 | `statusCode(400)` 或 `code", equalTo(ResultCode.PARAM_ERROR.getCode())` |
| 数据存在 | `body("data", notNullValue())` |
| 列表非空 | `body("data.list", hasSize(greaterThan(0)))` |
| 分页 | `body("data.total", greaterThanOrEqualTo(n))` |

### 5.3 断言禁止项

```java
// 禁止 - 过于宽松的断言
response.then().statusCode(200);  // 不验证 body

// 禁止 - 只验证存在而不验证值
body("data", notNullValue())  // 需进一步验证内容

// 禁止 - 硬编码 magic number 而不用 ResultCode
body("code", equalTo(0))  // 应使用 ResultCode.XXX.getCode()
```

---

## 六、测试场景覆盖

### 6.1 必测场景清单

#### 认证与会话

```
[ ] 正常登录成功
[ ] 用户名错误登录失败
[ ] 密码错误登录失败
[ ] 用户不存在登录失败
[ ] 空用户名/空密码
[ ] 登出成功
[ ] 登出后 Token 失效
[ ] 无 Token 访问受保护资源
[ ] 无效/过期 Token 访问受保护资源
```

#### 管理员管理

```
[ ] 创建管理员成功（root 操作）
[ ] 普通管理员无权创建
[ ] 用户名已存在创建失败
[ ] 用户名长度校验（<3, >32）
[ ] 密码长度校验（<6, >64）
[ ] 删除管理员成功（root 操作）
[ ] 普通管理员无权删除
[ ] 不能删除 root 管理员
[ ] 不能删除自己
[ ] 删除不存在的管理员
[ ] 分页查询管理员
[ ] 分页边界值（pageNum=0/-1, pageSize=0/-1）
```

#### 组织管理

```
[ ] 分页查询组织列表
[ ] 按组织名称筛选
[ ] 生成创建组织邀请码
[ ] 普通管理员无权生成邀请码（如果需要权限）
[ ] 修改组织状态（启用/禁用）
[ ] 修改不存在的组织状态
[ ] 无效状态值
```

#### 定时任务

```
[ ] 获取任务列表
[ ] 手动触发任务
[ ] 触发不存在的任务
[ ] 无认证触发任务失败
[ ] 传入 executeTime 触发
[ ] 空任务列表
```

### 6.2 安全性测试

```
[ ] SQL 注入尝试（orgName, username 等字符串参数）
[ ] Token 篡改
[ ] 非 JSON 请求体
[ ] 畸形 JSON
```

---

## 七、测试类模板

```java
package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class XxxApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    @Test
    void shouldDoSomethingSuccessfully() {
        createAdmin(1L, "admin", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("admin", "123456");

        Response response = authRequest(token)
                .body(Map.of("key", "value"))
                .post("/admin/xxx/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("操作成功"))
                .body("data", notNullValue());
    }

    // ========== 认证失败 ==========

    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .body(Map.of("key", "value"))
                .post("/admin/xxx/");

        response
                .then()
                .statusCode(401);
    }

    // ========== 业务规则失败 ==========

    @Test
    void shouldFailWithUnauthorizedOperation() {
        createAdmin(1L, "normal_admin", "123456", "ADMIN");
        String token = adminLoginAs("normal_admin", "123456");

        Response response = authRequest(token)
                .body(Map.of("key", "value"))
                .post("/admin/xxx/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    // ========== 参数校验 ==========

    @Test
    void shouldFailWithInvalidParams() {
        createAdmin(1L, "admin", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("admin", "123456");

        Response response = authRequest(token)
                .body(Map.of("key", "short"))  // 触发校验失败
                .post("/admin/xxx/");

        response
                .then()
                .statusCode(400);
    }
}
```

---

## 八、常见错误避免

### 8.1 数据泄露

```java
// 禁止 - 共享状态
private static String sharedToken;  // 类变量会被其他测试污染

// 推荐 - 每个测试独立创建
void shouldDoSomething() {
    createAdmin(1L, "admin", "123456", "SUPER_ADMIN");
    String token = adminLoginAs("admin", "123456");
    // 使用 token
}
```

### 8.2 断言不足

```java
// 禁止 - 只验证状态码
response.then().statusCode(200);

// 推荐 - 验证响应体
response
    .then()
    .statusCode(200)
    .body("code", equalTo(0))
    .body("data", notNullValue());
```

### 8.3 魔法数字

```java
// 禁止
body("code", equalTo(3));  // 3 是什么含义？

// 推荐
body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
```

### 8.4 测试依赖顺序

```java
// 禁止 - 测试之间有依赖
@Test
void test1() { /* 创建数据 */ }
@Test
void test2() { /* 依赖 test1 创建的数据 */ }

// 推荐 - 每个测试独立准备数据
@Test
void shouldCreateXxx() { createXxx(); /* 验证 */ }
@Test
void shouldDeleteXxx() { createXxx(); /* 再创建一个用于删除 */ }
```

---

## 九、调试与维护

### 9.1 测试失败时

1. 检查 `BaseApiTest` 是否正确初始化（`@BeforeAll` initSchema）
2. 确认测试数据库配置（`@ActiveProfiles("test")`）
3. 查看 `DatabaseCleanupUtil` 是否正确清空表
4. 检查 ID 分配是否冲突

### 9.2 添加新测试

1. 在对应模块目录下创建/找到 `{功能}ApiTest.java`
2. 使用 `should{场景}{预期结果}` 命名
3. 使用 `createAdmin()` 准备测试数据
4. 使用 `adminLoginAs()` 获取认证 Token
5. 使用 `authRequest(token)` 发送请求
6. 使用 `response.then().body(...)` 断言

---

## 十、相关文档

- [测试完善计划](./api_test_todo.md)
- [项目规范](./standard.md)
- [单元测试](./unit_test.md)