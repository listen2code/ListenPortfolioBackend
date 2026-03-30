# 运行 SpotBugs 分析
./mvnw spotbugs:check

# 生成完整报告
./mvnw spotbugs:spotbugs

# 查看报告
start target/spotbugs.html