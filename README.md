<div align="center">
 <img align="center" src="https://raw.githubusercontent.com/Cn-Asukai/longlian-oa-backend/main/docs/static/logo.jpg" height="96" width="96" alt="logo"/>
 <h1 align="center">
  LongLian-OA
 </h1>
</div>

# Project Structure
```
app
├── .mvn                      # Maven 配置目录
├── src
│   ├── main
│   │   ├── java
│   │   │   └── online.longlian.app
│   │   │       ├── common                # 通用核心代码
│   │   │       │   ├── aspect            # AOP 切面
│   │   │       │   ├── constants         # 全局常量定义
│   │   │       │   ├── enumeration       # 枚举类
│   │   │       │   ├── exception         # 自定义异常
│   │   │       │   ├── filter            # 过滤器
│   │   │       │   ├── handler           # 处理器
│   │   │       │   ├── result            # 统一响应封装
│   │   │       │   ├── util              # 工具类
│   │   │       │   ├── security          # 安全相关（权限、认证、授权）
│   │   │       │   └── config            # 配置类
│   │   │       ├── controller            # 控制器层
│   │   │       ├── mapper                # 数据访问层（DAO）
│   │   │       ├── pojo                  # 数据对象层
│   │   │       │   ├── bo                # 业务对象（封装复杂业务逻辑）
│   │   │       │   ├── dto               # 数据传输对象（封装前端请求/服务间调用）
│   │   │       │   ├── entity            # 实体对象
│   │   │       │   └── vo                # 视图对象（封装后端响应给前端的数据）
│   │   │       ├── service               # 业务逻辑层
│   │   │       │   ├── user              # 用户相关服务
│   │   │       │   ├── notify            # 通知相关服务
│   │   │       │   └── resource          # 资源相关服务
├── generator                             # 代码生成器模块
└── pom.xml                               # Maven 依赖配置

# Quick Start

## 代码生成

本项目依赖 Mybatis-Plus 代码生成器，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成
1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码
2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响