# Bootstrap 本地联调模板

## 目的

`HJO2OA-Bootstrap` 统一承载本地联调所需的基础设施模板、环境变量清单和推荐启动方式。当前模板覆盖：

- SQL Server 2017
- Redis
- RabbitMQ
- MinIO

## 文件

- `HJO2OA-Bootstrap/src/main/resources/application-local.yml`
- `HJO2OA-Bootstrap/.env.local.example`
- `HJO2OA-Bootstrap/docker-compose.local.yml`

## 环境变量

本地 profile 支持以下变量覆盖默认值：

| 变量 | 默认值 |
|------|--------|
| `HJO2OA_LOCAL_DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=hjo2oa_local;encrypt=true;trustServerCertificate=true` |
| `HJO2OA_LOCAL_DB_USERNAME` | `sa` |
| `HJO2OA_LOCAL_DB_PASSWORD` | `Hjo2oa@2026!` |
| `HJO2OA_LOCAL_REDIS_HOST` | `localhost` |
| `HJO2OA_LOCAL_REDIS_PORT` | `6379` |
| `HJO2OA_LOCAL_REDIS_PASSWORD` | 空 |
| `HJO2OA_LOCAL_RABBITMQ_HOST` | `localhost` |
| `HJO2OA_LOCAL_RABBITMQ_PORT` | `5672` |
| `HJO2OA_LOCAL_RABBITMQ_USERNAME` | `guest` |
| `HJO2OA_LOCAL_RABBITMQ_PASSWORD` | `guest` |
| `HJO2OA_LOCAL_MINIO_ENDPOINT` | `http://localhost:9000` |
| `HJO2OA_LOCAL_MINIO_ACCESS_KEY` | `minioadmin` |
| `HJO2OA_LOCAL_MINIO_SECRET_KEY` | `minioadmin` |
| `HJO2OA_LOCAL_MINIO_BUCKET` | `hjo2oa-local` |

## 启动基础设施

```powershell
docker compose --env-file HJO2OA-Bootstrap/.env.local.example -f HJO2OA-Bootstrap/docker-compose.local.yml up -d
```

本地模板默认开放以下端口：

- SQL Server: `1433`
- Redis: `6379`
- RabbitMQ AMQP: `5672`
- RabbitMQ 管理台: `15672`
- MinIO API: `9000`
- MinIO Console: `9001`

## 启动应用

```powershell
mvn -pl HJO2OA-Bootstrap -am spring-boot:run -Dspring-boot.run.profiles=local
```

或先打包再启动：

```powershell
mvn -pl HJO2OA-Bootstrap -am package
java -jar HJO2OA-Bootstrap/target/HJO2OA-Bootstrap-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

## 无外部依赖验证

如果当前机器没有启动 SQL Server、Redis、RabbitMQ 和 MinIO，仍可先跑 Bootstrap 烟雾测试验证主应用配置装配：

```powershell
mvn -q -pl HJO2OA-Bootstrap -am test
```
