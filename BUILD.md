# Build AstraxAI

Repository dùng cấu trúc flat: các file `.java`, `pom.xml`, `plugin.yml`,
`config.yml` và `knowledge.yml` nằm ở thư mục gốc.

Yêu cầu JDK 21 và Maven.

```bash
mvn --batch-mode --no-transfer-progress clean package
```

JAR đầu ra: `target/AstraxAI.jar`
