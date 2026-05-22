# CentOS 部署说明

目标形态：

- Docker Compose 运行 MySQL 与后端。
- 宿主机 Nginx 监听 `8082`，托管前端静态文件并代理 `/api` 到后端。
- 业务数据存放在服务器 `/opt/reimvault/mysql`。
- 附件文件存放在服务器 `/opt/reimvault/files`。

生产目录：

```text
/opt/reimvault
├── backend/app.jar
├── docker-compose.yml
├── .env
├── files/
├── frontend/
└── mysql/
```

访问地址：

```text
http://<server-ip>:8082
```

默认初始管理员通过 `.env` 中的 `BOOTSTRAP_ADMIN_*` 创建；仅当数据库中不存在同名用户时生效。
