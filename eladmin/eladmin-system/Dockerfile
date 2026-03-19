# 使用官方 OpenJDK 8 轻量镜像
FROM openjdk:8-jdk-slim

# 创建工作目录
WORKDIR /app

# 拷贝打包好的 Spring Boot jar
# 🔹 替换成你的实际模块路径和 jar 文件名
COPY target/eladmin-system-1.1.jar /app/app.jar

# 暴露端口（与 Spring Boot 配置一致）
EXPOSE 8000

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]