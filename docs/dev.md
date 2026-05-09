# 开发说明

## 容器开发环境

项目配置有容器开发环境（.devcontainer）

## 运行

`mvn spring-boot:run -pl app -Dspring.config.import=optional:file:.env`

## ORM 代码生成

本项目依赖 Mybatis-Plus 代码生成器(generator)，默认不覆盖旧代码，在新增功能和表结构字段改动时需用到代码生成

1. 当新增新表时，运行生成器，将生成对应的 Controller、Service、Mapper 相关代码

2. 当表结构发生变动时，由于默认不覆盖旧代码，所以需要删除原有的 entity 文件夹（或对应的实体类代码文件），重新生成即可，其他代码不受影响

3. 数据库中枚举统一使用 TINYINT 类型，生成代码前，应在 app\src\main\java\online\longlian\app\common\enumeration 中定义枚举类型，并使用 ModelEnum 注解声明对应的表和字段，以便代码生成器为枚举字段生成正确的类型声明 