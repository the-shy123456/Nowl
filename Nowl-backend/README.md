# Nowl Backend

后端采用 Spring Boot 多模块结构，入口模块为 `unimarket-web`。

## 编译

```bash
mvn -q -DskipTests compile
```

## 运行

1. 配置环境变量（参考仓库根目录 `.env.example`）。
2. 启动 `unimarket-web` 模块主类。

## 说明

当前 Java 包名和模块名保留 `unimarket` 前缀，以避免大规模重命名带来的兼容性风险；
对外项目名统一为 `Nowl`。
