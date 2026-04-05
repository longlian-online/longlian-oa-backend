# 开发说明

## 容器开发环境

项目配置有容器开发环境（devops/Dockerfile.dev）

在项目根目录执行 `docker-compose -f  .\devops\docker-compose.dev.yml` 即可启动容器

## ORM 代码生成

本项目依赖 Mybatis-Plus 代码生成器(generator)，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成

1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码

2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响