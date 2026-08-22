# 基础镜像：GraalVM Community JDK25
FROM ghcr.io/graalvm/graalvm-community:25

# 设置工作目录
WORKDIR /app

# 暴露SpringBoot默认端口
EXPOSE 8080

# 复制 Maven 配置（使用阿里云镜像）
COPY settings.xml /root/.m2/settings.xml

# 安装常用工具
# 开发容器不锁包版本：锁死后 base image 更新会直接导致构建失败，维护成本大于收益
# skipcq: DOK-W1005
RUN microdnf install -y git curl wget vim tar gzip && microdnf clean all

# 保持容器运行，不自动启动应用
CMD ["sleep", "infinity"]
