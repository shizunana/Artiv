# 绘约（HuiYue）

绘约是一个面向企划方与自由画师的美术约稿交易平台（毕业设计项目），基于 **Vue 3 + Spring Boot + MySQL** 前后端分离开发。

核心业务闭环：**发布需求 → 画师应征 / 定向邀请 → 选定下单 → 沟通修改 → 分阶段验收 → 互相评价**

功能模块：用户与认证、画师入驻认证、个人主页与作品集、内容服务板、企划发布、约稿大厅、应征与邀请、订单与模拟托管、沟通与消息、评价体系、后台管理（共 11 个模块）。

## 技术栈

- 前端：Vue 3、Vite、Vue Router、Pinia、Axios、Element Plus
- 后端：Spring Boot、MyBatis-Plus、MySQL、JWT、WebSocket
- 数据库：MySQL

## 目录结构

```text
huiyue/
├── docs/          # 文档（开发计划书、ER 图、接口文档、演示脚本）
├── frontend/      # 前端（Vue 3 + Vite；公开页 + 企划方/画师/管理三工作台）
└── backend/       # 后端（Spring Boot）
```

## 启动方式

### 1. 数据库

1. 启动本地 MySQL 服务。
2. 执行 [`backend/sql/init.sql`](backend/sql/init.sql) 完成建库、建表并导入演示数据。
3. 按需修改 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) 中的数据库连接信息。

### 2. 后端（Spring Boot）

```bash
cd backend
mvn spring-boot:run
```

后端默认端口：`8080`。

### 3. 前端（Vue 3 + Vite）

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`，开发环境通过 Vite 代理将 `/api`、`/ws` 转发到后端。

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

## 文档

- [项目开发说明书](../项目开发说明书.md)
- [项目开发计划书](docs/项目开发计划书.md)
- [项目进度](docs/项目进度.md)
- [未解决问题](docs/未解决问题.md)
- [测试记录](docs/测试.md)
- [数据库设计（ER 图）](docs/ER图.png)
- [接口文档](docs/接口文档.md)
- [演示脚本](docs/演示脚本.md)
