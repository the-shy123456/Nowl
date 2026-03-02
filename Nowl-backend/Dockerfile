# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制依赖文件进行缓存
COPY pom.xml .
COPY unimarket-security/pom.xml unimarket-security/
COPY unimarket-ai/pom.xml unimarket-ai/
COPY unimarket-core/pom.xml unimarket-core/
COPY unimarket-admin/pom.xml unimarket-admin/
COPY unimarket-search/pom.xml unimarket-search/
COPY unimarket-recommend/pom.xml unimarket-recommend/
COPY unimarket-web/pom.xml unimarket-web/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY . .
RUN mvn clean package -DskipTests -pl unimarket-web -am

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 复制jar包
COPY --from=builder /app/unimarket-web/target/*.jar app.jar

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "-Dfile.encoding=UTF-8", "app.jar"]
