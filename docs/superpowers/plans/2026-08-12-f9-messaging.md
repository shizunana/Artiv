# F9 沟通与消息模块历史实施计划（归档）

> 归档说明（2026-08-13）：本计划对应的功能已合并至 `main`。其中分支名、Superpowers 子技能要求和任务复选框均为当时的历史上下文，不应作为当前开发指令；当前规则与进度以 `../../沟通与消息设计.md`、`../../接口文档.md`、`../../项目进度.md` 为准。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付订单开发前置的站内通知、应征私聊、会话消息中心和 WebSocket 实时推送能力。

**Architecture:** 消息先由 REST 业务服务完成鉴权与落库，再由 WebSocket 向在线用户推送事件；数据库是消息可靠来源，前端在重连后重新拉取会话与未读数。订单聊天仅预留 `type=2`，本次不接入订单业务。

**Tech Stack:** Spring Boot 3.3、MyBatis-Plus、JWT、Spring WebSocket、Vue 3、Pinia、Element Plus、MySQL 8。

## Global Constraints

- 代码根目录为 `G:\ProjectForGraduation\huiyue`，开发分支为 `feature/f9-messaging`。
- 接口前缀固定为 `/api/v1`，返回结构固定为 `{ code, message, data }`。
- 发送者身份必须从 JWT `UserContext` 获取，前端不得传入 `fromId`。
- 私信和通知必须先落库再推送；订单状态机、订单聊天、议价均不属于本次范围。
- 所有新增后端行为遵循先写失败测试、确认失败、最小实现、确认通过的 TDD 周期。

---

### Task 1: 消息数据模型与服务测试基础

**Files:**
- Modify: `backend/sql/init.sql`
- Modify: `backend/src/main/java/com/huiyue/entity/Message.java`
- Create: `backend/src/main/java/com/huiyue/dto/PrivateMessageRequest.java`
- Create: `backend/src/test/java/com/huiyue/service/MessageServiceTest.java`

**Interfaces:**
- Produces: `Message.projectId`, `Message.conversationKey`, `Message.subtype`；`PrivateMessageRequest(toId, projectId, content)`。

- [ ] 写一个失败测试，断言双方私信使用排序后的 `conversationKey`，并且发送者由当前用户决定。
- [ ] 运行 `mvn -Dtest=MessageServiceTest test`，确认因服务接口缺失而失败。
- [ ] 最小化补齐消息字段、请求 DTO 与服务接口。
- [ ] 重跑目标测试，确认通过。
- [ ] 提交：`feat: add message conversation foundation`。

### Task 2: 私信、会话和已读 REST 接口

**Files:**
- Modify: `backend/src/main/java/com/huiyue/controller/MessageController.java`
- Create: `backend/src/main/java/com/huiyue/service/MessageService.java`
- Create: `backend/src/main/java/com/huiyue/service/impl/MessageServiceImpl.java`
- Modify: `backend/src/main/java/com/huiyue/mapper/MessageMapper.java`
- Modify: `backend/src/test/java/com/huiyue/service/MessageServiceTest.java`

**Interfaces:**
- Produces: `POST /messages/private`、`GET /messages/conversation`、`GET /messages/conversations`、`GET /messages/unread-count`。

- [ ] 写失败测试：普通用户能发送私信；空内容、给自己发送、非法项目关联被拒绝；只允许双方读取会话。
- [ ] 运行目标测试确认失败。
- [ ] 实现消息服务与控制器；会话查询按创建时间升序分页，会话列表按最后消息降序。
- [ ] 重跑目标测试与 `mvn test`。
- [ ] 提交：`feat: add private message conversations`。

### Task 3: 应征通知

**Files:**
- Modify: `backend/src/main/java/com/huiyue/service/impl/ApplicationServiceImpl.java`
- Modify: `backend/src/test/java/com/huiyue/service/ApplicationServiceTest.java`
- Modify: `backend/src/main/java/com/huiyue/service/MessageService.java`

**Interfaces:**
- Consumes: `MessageService.createNotification(toUserId, subtype, projectId, content)`。
- Produces: 画师应征成功后给企划方的 type=1 应征通知。

- [ ] 写失败测试，断言应征落库后为企划方创建 `APPLICATION_CREATED` 通知。
- [ ] 运行测试确认失败。
- [ ] 在应征服务中通过消息服务创建通知。
- [ ] 重跑全部后端测试。
- [ ] 提交：`feat: notify employer about new applications`。

### Task 4: WebSocket 鉴权和推送

**Files:**
- Modify: `backend/src/main/java/com/huiyue/websocket/MessageWebSocketHandler.java`
- Modify: `backend/src/main/java/com/huiyue/config/WebSocketConfig.java`
- Modify: `backend/src/main/java/com/huiyue/service/impl/MessageServiceImpl.java`
- Create: `backend/src/test/java/com/huiyue/websocket/MessageWebSocketHandlerTest.java`

**Interfaces:**
- Produces: `/ws?token=<jwt>` 鉴权连接和 `NEW_MESSAGE` / `APPLICATION` / `SYSTEM` 事件推送。

- [ ] 写失败测试，断言无效 token 被拒绝、同用户多连接都接收推送。
- [ ] 运行测试确认失败。
- [ ] 用 `JwtUtil` 解析 token，使用 `Map<Long, Set<WebSocketSession>>` 管理连接；消息服务落库后推送。
- [ ] 重跑 WebSocket 测试和全部后端测试。
- [ ] 提交：`feat: push messaging events over websocket`。

### Task 5: 前端会话消息中心与入口

**Files:**
- Modify: `frontend/src/api/message.js`
- Modify: `frontend/src/stores/message.js`
- Create: `frontend/src/utils/ws.js`
- Modify: `frontend/src/views/messages/MessageList.vue`
- Modify: `frontend/src/views/dashboard/Applications.vue`
- Modify: `frontend/src/views/painter/MyApplications.vue`
- Modify: `frontend/src/components/layout/AppHeader.vue`

**Interfaces:**
- Consumes: Task 2 REST 接口和 Task 4 WebSocket 事件。
- Produces: 会话列表、聊天面板、未读徽标、应征页面的沟通跳转。

- [ ] 写前端行为测试或在现有工具链缺少测试运行器时，先记录该限制并以 `npm run build` 作为构建验证。
- [ ] 实现 API、Pinia 消息状态、重连 WebSocket 客户端和会话式消息页面。
- [ ] 在两类应征页面增加“沟通”入口，路由参数带 `withUserId` 与 `projectId`。
- [ ] 运行 `npm.cmd run build`，确认构建通过。
- [ ] 提交：`feat: add realtime conversation experience`。

### Task 6: F9 端到端验收与文档

**Files:**
- Modify: `docs/接口文档.md`
- Modify: `docs/项目进度.md`
- Modify: `docs/测试.md`
- Modify: `docs/未解决问题.md`

- [ ] 用 `shizuna / zaza1414` 登录，向任意已认证画师发送消息。
- [ ] 用画师账户回复，确认双方会话存在、消息实时抵达、未读状态可更新。
- [ ] 运行 `mvn test` 和 `npm.cmd run build`。
- [ ] 将接口状态、测试过程与遗留的订单聊天边界写入文档。
- [ ] 提交：`docs: record f9 messaging verification`。
