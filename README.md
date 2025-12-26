<div align="center">
 <img align="center" src="https://raw.githubusercontent.com/Cn-Asukai/longlian-oa-backend/main/docs/static/logo.jpg" height="96" width="96" alt="logo"/>
 <h1 align="center">
  LongLian-OA
 </h1>
</div>

# Project Structure

app
├── .mvn
├── src
│   └── main
│       └── java
│           └── online.longlian.app
│               ├── common                  //通用代码
│               ├── config                  //配置文件
│               ├── controller              //业务
│               │   ├── admin
│               │   └── home
│               ├── mapper                  
│               │   ├── xml
│               │   └── UserMapper
│               ├── pojo                    //实体
│               │   ├── dto
│               │   ├── entity
│               │   └── vo
│               ├── service                 //服务层
│               │   ├── impl
│               │   └── IUserService
│               └── AppApplication
└─generator                                  // 代码生成器
```

# Quick Start

## 代码生成

本项目依赖 Mybatis-Plus 代码生成器，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成
1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码
2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响
