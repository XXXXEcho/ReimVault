# 报销归档通 / ReimVault

企业级报销材料管理平台。员工提交报销材料，管理员按分类、状态和时间筛选整理，批量导出 Excel 清单与附件压缩包，并归档记录。

> 当前版本不包含审批流、付款、OCR、财务系统对接、企业微信/钉钉登录或移动 App。

## 默认登录账号

系统默认不会硬编码创建账号。首次本地启动时，使用下方“带初始管理员启动”命令会自动创建管理员账号：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `secret123` | 仅在启动参数包含 `app.bootstrap.admin.*` 且数据库中不存在同名用户时创建 |

员工账号没有默认内置账号。请使用管理员登录后，在“用户管理”中创建员工账号。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus
- 后端：Spring Boot 3、Spring Security、Spring Data JPA、Flyway
- 数据库：MySQL；测试使用 H2
- 文件存储：本地文件系统

## 本地启动

### 1. 准备数据库

默认后端连接：

```text
jdbc:mysql://localhost:3306/reimbursement
username: reimbursement
password: reimbursement
```

需要先创建数据库和用户，或按需修改 `backend/src/main/resources/application.yml`。

### 2. 启动后端

普通启动：

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -pl backend spring-boot:run
```

带初始管理员启动：

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -pl backend spring-boot:run -Dspring-boot.run.arguments="--app.bootstrap.admin.username=admin --app.bootstrap.admin.password=secret123 --app.bootstrap.admin.display-name=系统管理员 --app.bootstrap.admin.department=财务部"
```

后端默认端口：`http://127.0.0.1:8080`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://127.0.0.1:5173`。如果该端口被占用，Vite 会自动使用下一个可用端口。

前端开发服务器默认把 `/api` 代理到 `http://127.0.0.1:8080`。如后端端口不同：

```bash
VITE_API_TARGET=http://127.0.0.1:<port> npm run dev
```

## 使用流程

1. 管理员登录。
2. 在“用户管理”创建员工账号。
3. 在“分类管理”维护报销用途分类。
4. 员工登录并创建报销记录。
5. 员工上传支付凭证；订单截图和发票可选。
6. 员工提交记录。
7. 管理员在“报销管理”筛选记录、填写备注。
8. 管理员在“批次管理”创建批次、加入已提交记录。
9. 管理员导出 Excel 清单和附件 Zip。
10. 管理员归档批次。

## 材料规则

- 支付凭证：必填
- 订单截图：选填
- 发票：选填

## 记录状态

- `DRAFT`：草稿
- `SUBMITTED`：已提交
- `ARCHIVED`：已归档

## 文件存储

默认目录：

```text
storage/reimbursements
```

该目录用于保存上传附件，不应提交到 Git。

## 验证命令

后端测试：

```bash
JAVA_HOME="D:\software\jdk-21" "/d/software/apache-maven-3.9.11/bin/mvn.cmd" -q -pl backend test
```

前端测试：

```bash
npm test --prefix "F:/Code/报销/frontend"
```

前端构建：

```bash
npm run build --prefix "F:/Code/报销/frontend"
```

## 当前边界

第一期聚焦“材料收集、整理、导出、归档”。审批、支付、影像识别和外部系统集成后续再做。
