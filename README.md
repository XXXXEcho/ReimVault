# ReimVault - 报销归档通

企业内部报销材料收集与归档系统。员工提交报销材料，管理员按分类、状态和时间筛选整理，批量导出 Excel 清单与附件压缩包，并归档记录。

> 当前版本不包含审批流、付款、OCR、财务系统对接或移动 App。

## 功能概览

- **员工**：创建报销记录、上传附件（付款凭证/订单截图/发票）、提交/撤回
- **专员/管理员**：审核驳回、管理费用科目、批量归档、导出 Excel 清单 + ZIP 附件包
- **用户管理**：管理员创建/编辑/删除用户

## 用户角色

| 角色 | 权限 |
|---|---|
| EMPLOYEE | 管理个人报销记录 |
| SPECIALIST | 审核报销、管理科目与批次（不可管理用户） |
| ADMIN | 全部权限，含用户管理 |

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| 数据库 | MySQL 8.0, Flyway 迁移（测试用 H2） |
| 前端 | Vue 3, TypeScript, Vite, Element Plus, Pinia |
| 部署 | Docker Compose, Nginx |

## 本地开发

### 前置条件

- JDK 21+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0

### 准备数据库

创建数据库和用户：

```sql
CREATE DATABASE reimbursement CHARACTER SET utf8mb4;
CREATE USER 'reimbursement'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON reimbursement.* TO 'reimbursement'@'localhost';
```

通过环境变量覆盖配置（推荐）：

```bash
export DB_URL=jdbc:mysql://localhost:3306/reimbursement?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
export DB_USERNAME=reimbursement
export DB_PASSWORD=your_password
```

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

带初始管理员启动（仅当数据库中不存在同名用户时生效）：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --app.bootstrap.admin.username=admin \
  --app.bootstrap.admin.password=your_admin_password \
  --app.bootstrap.admin.display-name=系统管理员 \
  --app.bootstrap.admin.department=财务部"
```

后端默认端口：`http://127.0.0.1:8080`

### 默认登录账号

| 角色 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `secret123` | 仅在启动参数包含 `app.bootstrap.admin.*` 且数据库中不存在同名用户时创建 |

员工账号无默认内置，请管理员登录后在"用户管理"中创建。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://127.0.0.1:5173`

开发服务器默认将 `/api` 代理到 `http://127.0.0.1:8080`。如后端端口不同：

```bash
VITE_API_TARGET=http://127.0.0.1:<port> npm run dev
```

## 使用流程

1. 管理员登录 → 在"用户管理"创建员工账号
2. 在"分类管理"维护报销用途分类
3. 员工登录 → 创建报销记录 → 上传附件 → 提交
4. 管理员在"报销管理"筛选记录、填写备注或驳回
5. 管理员在"批次管理"创建批次 → 加入已提交记录 → 导出 Excel 清单和附件 Zip → 归档

## 记录状态

| 状态 | 说明 |
|---|---|
| DRAFT | 草稿，可编辑/删除 |
| SUBMITTED | 已提交，可由管理员驳回或员工撤回 |
| ARCHIVED | 已归档 |

## 附件规则

- 付款凭证：必填
- 订单截图：选填
- 发票：选填

## 测试与构建

```bash
# 后端测试
cd backend && mvn test

# 前端测试
cd frontend && npm test

# 前端构建
cd frontend && npm run build   # 产物在 frontend/dist/
```

## 生产部署

### 架构

```
客户端 ──→ Nginx :8082 ──┬─→ /opt/reimvault/frontend/（静态文件）
                          └─→ /api/ ──→ Docker backend :18083 ──→ Docker MySQL :3306
```

### 目录结构

```
/opt/reimvault/
├── backend/app.jar
├── docker-compose.yml
├── .env                     # 不入库
├── files/                   # 上传附件
├── frontend/                # 构建产物
└── mysql/                   # MySQL 数据
```

### 端口分配

| 服务 | 容器端口 | 宿主机映射 |
|---|---|---|
| Nginx | — | `8082` |
| 后端 | `8080` | `127.0.0.1:18083` |
| MySQL | `3306` | 不暴露 |

### 环境变量（`.env`）

```bash
MYSQL_ROOT_PASSWORD=<root密码>
MYSQL_DATABASE=reimbursement
MYSQL_USER=reimbursement
MYSQL_PASSWORD=<数据库用户密码>
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_PASSWORD=<管理员密码>
BOOTSTRAP_ADMIN_DISPLAY_NAME=系统管理员
BOOTSTRAP_ADMIN_DEPARTMENT=财务部
```

### 部署步骤

1. 构建前端：`npm run build`，将 `dist/` 内容放到 `/opt/reimvault/frontend/`
2. 构建后端：`mvn -pl backend package -DskipTests`，将 jar 放到 `/opt/reimvault/backend/app.jar`
3. 将 `deploy/centos/docker-compose.yml` 复制到 `/opt/reimvault/`，创建 `.env` 文件
4. 将 `deploy/centos/reimvault-nginx.conf` 复制到 Nginx 配置目录（如 `/etc/nginx/conf.d/`）
5. 启动服务：

```bash
cd /opt/reimvault
docker compose up -d
sudo nginx -t && sudo systemctl reload nginx
```

6. 访问 `http://<server-ip>:8082`
