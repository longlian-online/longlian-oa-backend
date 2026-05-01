# AGENTS.md

## 项目概述

- **类型**: Spring Boot 3.3.5 REST API 后端
- **Java**: 21
- **数据库**: MySQL + MyBatis Plus ORM
- **认证**: JWT (jjwt 0.11.5)
- **构建**: Maven 多模块 (app, generator)

## 包结构

``` 
app/src/main/java/online/longlian/app/
├── controller/
│   ├── admin/         # 管理端接口
│   ├── app/           # 用户端接口
│   ├── orgadmin/      # 组织管理端接口
│   └── common/        # 通用接口（文件上传等）
├── service/           # 服务层代码
│   ├── user/          # 用户、组织、角色、权限服务
│   ├── notify/       # 通知服务
│   └── resource/     # 文件存储服务
├── mapper/           # MyBatis 数据访问层
├── pojo/
│   ├── entity/       # 数据库实体 (User, Project, Task 等)
│   ├── dto/           # 请求数据传输对象
│   ├── vo/            # 响应视图对象
│   └── bo/            # 业务对象
└── common/
    ├── util/          # 工具类 (JwtUtil, Redis, Mail)
    ├── constants/    # 常量定义
    └── exception/    # 自定义异常
```

## 代码规范

1. 接口保持 RESTFul 风格：
    1. 正确的路由：post /app/session/
    2. 错误的路由：post /app/user/login
2. 实现代码前应该查看是否有能够复用的代码，如列表请求和响应 PageRequestDTO 和 PageResultVO 对象
3. 涉及到数据库实体类修改，不应该直接更改 entity，而是在 app/src/main/resources/manifest/migrate 中找到当前版本的迁移 sql，编写 sql 后等待用户执行后，使用 generator 包生成实体
4. 请求体必须用 DTO 对象接收(app/src/main/java/online/longlian/app/pojo/dto), 响应体必须使用 VO 对象返回(app/src/main/java/online/longlian/app/pojo/vo)
5. 完全信任 Controller 预处理后的入参，不做重复非空判断、不做分页参数兜底默认值、不做业务无关的参数防御性编码。
6. 当一个方法有多个入参，根据入参类型，定义一个或多个独立的入参类(如 service 层应该使用 BO 类)进行传参，以便拓展