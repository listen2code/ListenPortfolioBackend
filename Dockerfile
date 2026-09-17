# ===================================================================
# Portfolio 后端 Spring Boot 生产容器构建文件 (Dockerfile)
# ===================================================================
# 1. 基础镜像设计考量：
#    - 选用 eclipse-temurin:17-jre-alpine 作为极简运行时环境（体积仅约 140MB 左右）。
#    - 对比完整 JDK 镜像（400MB+），只包含 JRE 运行时，大幅减少生产镜像体积与攻击面。
#    - 基于 Alpine Linux，体积轻巧；由于使用纯 Java 字节码与 JDBC，无 glibc 强依赖。
# ===================================================================
FROM eclipse-temurin:17-jre-alpine

# 2. 挂载临时工作卷 /tmp：
#    - Spring Boot 内置的嵌入式 Tomcat 容器在运行期间默认使用 /tmp 作为 Servlet 临时工作目录。
#    - 挂载 Volume 能够避免频繁读写产生过多的容器可写层（Write Layer）差异，提高 I/O 吞吐并减小容器体积。
VOLUME /tmp

# 3. 复制编译打包产物：
#    - 宿主机先通过 Gradle 完成构建 (gradlew bootWar)，生成产物位于 target 目录下。
#    - 此处仅复制构建好的单一 War/Jar 包，避免在 Docker 镜像构建中下载庞大的依赖缓存。
COPY target/portfolio-0.0.1-SNAPSHOT.war app.war

# 4. 容器启动入口命令：
#    - 采用 "sh", "-c" 包装运行命令，使得传入的环境变量（如 $JAVA_OPTS）能够被 Shell 正确解析与展开。
#    - 启动参数支持外部动态注入：例如在 docker-compose 中指定的 -Xms128m -Xmx256m 等堆内存优化项。
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.war"]