# 报销材料管理系统

## 本地启动

### 后端

默认连接 MySQL：`jdbc:mysql://localhost:3306/reimbursement`。

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -pl backend spring-boot:run
```

首次本地验收可显式创建初始管理员：

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -pl backend spring-boot:run -Dspring-boot.run.arguments="--app.bootstrap.admin.username=admin --app.bootstrap.admin.password=secret123 --app.bootstrap.admin.display-name=系统管理员 --app.bootstrap.admin.department=财务部"
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认把 `/api` 代理到 `http://127.0.0.1:8080`。如后端改端口，可设置 `VITE_API_TARGET=http://127.0.0.1:<port>`。

## 数据库

默认使用 MySQL。测试使用 H2。

## 文件存储

默认目录：`storage/reimbursements`。

## 不包含

第一期不包含审批流、付款、OCR、财务系统对接、企业微信/钉钉登录、移动 App。
