# 基础镜像：Maven + JDK21
FROM maven:3.9-eclipse-temurin-21

# 设置工作目录
WORKDIR /app

# 暴露SpringBoot默认端口
EXPOSE 8080

# 复制 Maven 配置（使用阿里云镜像）
COPY settings.xml /usr/share/maven/conf/settings.xml

# 安装常用工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    wget \
    vim \
    && rm -rf /var/lib/apt/lists/*

# 保持容器运行，不自动启动应用
CMD ["sleep", "infinity"]
