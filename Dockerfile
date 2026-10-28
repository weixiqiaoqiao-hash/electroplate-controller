# 电镀控制系统 —— 前后端一体化镜像（前端构建产物打进 jar，单进程单端口 8080）
# 构建：docker build -t electroplate-controller:latest .
# 运行：docker run -d -p 8080:8080 -e MYSQL_HOST=192.168.2.43 -e MYSQL_PASSWORD=xxx electroplate-controller:latest

# ---------- 阶段 1：构建前端（Node 18 + Vite 5） ----------
FROM node:18-alpine AS frontend-build
WORKDIR /build/frontend
# 先只拷贝依赖清单，命中缓存后无需每次重新下载 node_modules
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/ ./
RUN npm run build

# ---------- 阶段 2：构建后端 jar（JDK 8 + Maven，前端产物并入 static） ----------
FROM maven:3.9-eclipse-temurin-8 AS backend-build
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
# 预热依赖缓存；失败不阻断构建（不影响后续 package）
RUN mvn -q -B -f backend/pom.xml -DskipTests dependency:go-offline || true
COPY backend backend
# 清空仓库里可能残留的旧构建产物，再放入本次前端产物
RUN rm -rf backend/src/main/resources/static
COPY --from=frontend-build /build/frontend/dist backend/src/main/resources/static
RUN mvn -q -B -f backend/pom.xml -DskipTests package

# ---------- 阶段 3：运行时（JRE 8，仅保留 jar） ----------
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"
COPY --from=backend-build /build/backend/target/electroplate-backend.jar /app/app.jar
RUN mkdir -p /app/logs
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar /app/app.jar"]
