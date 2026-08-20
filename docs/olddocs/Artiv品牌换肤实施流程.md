# 艺绘（Artiv）完整品牌迁移实施流程

> 制定日期：2026-08-14
> 当前状态：**已实施并完成构建验证。**
> 迁移原则：从内到外统一将当前项目中的“绘约 / HuiYue / huiyue”替换为“艺绘 / Artiv / artiv”；保留稳定 URL 结构与已有业务数据。

## 1. 迁移目标

项目由“绘约（HuiYue）”正式更名为“艺绘（Artiv）”。本轮不仅替换页面视觉，还同步迁移项目根目录、Java 包、Maven、Spring 配置、运行数据库、前端内部标识、SQL 和当前文档。

| 标识类别 | 旧值 | 新值 |
| --- | --- | --- |
| 中文品牌 | 绘约 | 艺绘 |
| 英文展示品牌 | HuiYue / Huiyue | Artiv |
| 技术标识 | huiyue | artiv |
| 根项目目录 | `G:\ProjectForGraduation\huiyue` | `G:\ProjectForGraduation\Artiv` |
| Java 根包 | `com.huiyue` | `com.artiv` |
| Spring 启动类 | `HuiYueApplication` | `ArtivApplication` |
| Maven 坐标 | `com.huiyue:huiyue` | `com.artiv:artiv` |
| Spring 应用名 | `huiyue` | `artiv` |
| MySQL 运行库 | `huiyue` | `artiv`（由原库完整复制） |
| 配置前缀 | `huiyue.*` | `artiv.*` |
| CSS / 本地存储标识 | `huiyue` | `artiv` |

## 2. 明确保留不变的稳定项

下列内容不含旧品牌，且已经被前后端、图片或演示流程依赖，迁移中必须保持不变：

- REST API 基础路径：`/api/v1/...`；
- Vue 页面路由：`/projects`、`/stalls`、`/orders`、`/works`、`/painter/...`、`/dashboard/...`；
- 上传访问 URL：`/uploads/...`；
- 数据库表名、字段名、业务 ID、用户账号、订单、消息、余额、企划、橱窗、作品和图片记录；
- 前后端端口：前端 `5173`、后端 `8080`；
- Git 历史、当前 `main` 分支、远程仓库 `https://github.com/shizunana/MyProject1.git`。

> 保留 `/api/v1`、页面路由与 `/uploads`，可以避免前端请求、已存图片路径和已有演示链接失效。它们不暴露 HuiYue 品牌。

## 3. 已具备的回退条件

完整原项目已复制到：`G:\ProjectForGraduation\oldproject`。

该备份包含 Git 元数据、后端源码、前端源码、`backend/uploads` 上传文件，以及当前未提交的 README 修改。迁移开始前还必须额外保留 MySQL 数据库 `huiyue`；该原库不会删除。

## 4. 正式迁移流程

### 阶段 0：冻结与记录

1. 停止前端 Vite、后端 Spring Boot 和可能占用项目目录的 IDE 进程。
2. 记录当前 Git 提交号、`git status`、前端构建结果和后端测试结果。
3. 使用 MySQL Workbench 或 `mysqldump` 对 `huiyue` 做完整导出；导出必须包含表结构、数据、索引、触发器、存储过程和事件。
4. 此阶段不写入新数据库、不移动原目录。

### 阶段 1：创建新的 Artiv 运行数据库

1. 创建 MySQL 数据库 `artiv`，字符集使用 `utf8mb4`，存储引擎使用 InnoDB。
2. 将 `huiyue` 的完整导出导入 `artiv`；不得手工重建表，以避免遗漏订单、消息或外键数据。
3. 对比 `huiyue` 与 `artiv` 的核心表行数：用户、企划、应征、橱窗、订单、订单阶段、订单事件、消息、作品、邀请和评价。
4. 抽查至少一个用户、橱窗、企划、订单、会话和上传图片路径记录。
5. 保留 `huiyue` 作为只读回退库；直到项目稳定并获得明确许可前，不删除原库。

### 阶段 2：迁移根目录与上传文件

1. 将项目根目录从 `G:\ProjectForGraduation\huiyue` 重命名为 `G:\ProjectForGraduation\Artiv`。
2. 确认 `backend/uploads` 随项目目录原样移动为 `G:\ProjectForGraduation\Artiv\backend\uploads`；不得清空或重新生成上传目录。
3. 使用新路径重新打开 IDE、终端和项目启动配置。
4. Git 仓库、分支和远程 URL 保持不变；目录改名不会改变提交历史。

### 阶段 3：后端技术标识迁移

1. 移动目录：
   - `backend/src/main/java/com/huiyue` → `backend/src/main/java/com/artiv`；
   - `backend/src/test/java/com/huiyue` → `backend/src/test/java/com/artiv`。
2. 替换全部 Java `package` 与 `import`：`com.huiyue` → `com.artiv`。
3. 启动类改名：`HuiYueApplication.java` / `HuiYueApplication` → `ArtivApplication.java` / `ArtivApplication`；同步修改 `@MapperScan` 字符串和任何启动配置引用。
4. 更新 `backend/pom.xml`：
   - `groupId`：`com.huiyue` → `com.artiv`；
   - `artifactId`：`huiyue` → `artiv`；
   - Maven `name`、`description` 更新为艺绘 / Artiv。
5. 更新 `application.yml`：
   - `spring.application.name`：`artiv`；
   - JDBC 数据库名：`huiyue` → `artiv`；
   - 配置根节点：`huiyue:` → `artiv:`；
   - JWT 密钥文本中的旧标识改为 `artiv`。
6. 将 Java 中所有 `@Value("${huiyue...}")` 或等价配置读取改为 `artiv...`。
7. 保留端口、数据库账号密码、`uploads` 目录值与 `/uploads` URL 前缀不变。

### 阶段 4：前端标识与视觉迁移

1. 替换全站可见名称、Logo 文字、浏览器标题、Favicon、页脚版权、登录注册提示、空状态和系统提示：绘约/HuiYue → 艺绘/Artiv。
2. 更新 `frontend/package.json`、`package-lock.json`、`index.html` 中的项目名、描述和页面元信息。
3. CSS 变量和选择器中的 `huiyue` 统一改为 `artiv`，例如 `--huiyue-primary` → `--artiv-primary`；同步替换所有引用。
4. 前端本地存储、Session Storage、Cookie 或 WebSocket 客户端标识中若存在 `huiyue`，统一改为 `artiv`；该变动会使浏览器现有登录状态失效一次，属于预期结果。
5. 保持 `/api/v1`、Vue 路由及 `/uploads` 图片 URL 完全不变。

### 阶段 5：数据库脚本与当前文档迁移

1. 更新 `backend/sql/init.sql` 与当前迁移脚本中的数据库名、注释和品牌文字，使其可初始化 `artiv`。
2. 更新 README、docs 及当前生效的项目说明中的品牌、技术标识和根目录路径。
3. `docs/olddocs/superpowers/`、`docs/superpowers/` 为历史执行归档：保留内容真实性，不做机械改名；其不参与运行和对外演示。原项目根目录 `.superpowers/` 已迁入 `docs/olddocs/superpowers/`，并加入 Git 忽略。
4. 检查测试数据、默认素材与截图是否带有肉眼可见的旧品牌文字；只替换项目自带的测试素材，不改用户业务图片。

### 阶段 6：构建、运行与数据回归

1. 后端执行 `mvn.cmd test`，确认包名迁移、配置读取和数据库连接均正常。
2. 前端执行 `npm.cmd run build`，确认 CSS 变量和模块引用无遗漏。
3. 使用 `artiv` 数据库启动系统，完成以下回归：登录、注册、上传图片、作品、橱窗、企划、应征、私信、系统通知、橱窗订单、企划订单、订单详情。
4. 检查上传图片可通过原 URL `/uploads/...` 加载，确认新项目实际使用的是迁移后的 `backend/uploads`。
5. 在前端可见页面、README 和当前 docs 中搜索，确认不再出现“绘约 / HuiYue / huiyue”。
6. 在源码与 SQL 中搜索 `huiyue`，只允许出现在 Git 历史、`oldproject` 和已明确保留的历史归档中。

### 阶段 7：提交与回退

1. 运行 `git diff --check` 后创建独立本地 Git 提交，例如：`refactor: migrate HuiYue branding to Artiv`。
2. 是否推送远程仓库由项目负责人单独确认；默认只提交本地。
3. 若出现构建、数据或图片问题：
   - 代码可通过此次独立提交回退；
   - 原项目文件可从 `G:\ProjectForGraduation\oldproject` 恢复；
   - 原数据库 `huiyue` 保持完整，可重新复制到 `artiv`。

## 5. 预期影响与处理方式

| 影响 | 原因 | 处理方式 |
| --- | --- | --- |
| 需要重新登录 | JWT 密钥与前端存储 key 改名 | 清除旧本地存储或直接重新登录 |
| IDE 启动配置失效 | 根目录、启动类和包路径变化 | 用新根目录重新导入项目并选择 `ArtivApplication` |
| 新 Jar 文件名变化 | Maven artifactId 改为 `artiv` | 重新构建并使用新产物 |
| 历史 SQL 运行库变化 | SQL 改用 `artiv` | 旧库保留，旧脚本仅用于 Artiv 初始化 |
| 已有图片不可丢失 | 数据库存的是 `/uploads/...` 相对路径 | 原样迁移 `backend/uploads`，保持 URL 不变 |

## 6. 最终验收标准

- 页面、素材、README、当前 docs、源码与 SQL 中的现行标识统一为艺绘（Artiv/artiv）；
- 项目根目录为 `G:\ProjectForGraduation\Artiv`；
- 后端包名为 `com.artiv`，启动类和 Maven 坐标为 Artiv；
- 运行数据库为 `artiv`，原 `huiyue` 库完整保留且不被新项目写入；
- `/api/v1`、Vue 路由与 `/uploads` URL 不变；
- 前后端构建通过，核心业务回归通过，已有图片、消息、订单和余额完整保留；
- 迁移作为独立 Git 提交，能够通过 Git、文件备份和原数据库三层回退。

## 7. 实施结果（2026-08-14）

- 已将主项目迁移到 `G:\ProjectForGraduation\Artiv`，完整备份保留在 `G:\ProjectForGraduation\oldproject`；
- 已由原运行库完整复制生成 `artiv` 数据库，11 张表的记录数逐表核对一致，原数据库保持不变；
- Java 包名已迁移到 `com.artiv`，启动类为 `ArtivApplication`，Maven 坐标与 Spring 应用名均已统一为 `artiv`；
- 前端包名、页面品牌、浏览器标题、CSS 变量和登录凭证存储键均已迁移到 Artiv；
- `/api/v1`、Vue 页面路由、`/uploads` 地址、端口以及业务表结构保持不变；
- 后端执行 `mvn.cmd clean test`：47 项测试通过、0 失败；前端执行 `npm.cmd run build`：构建通过；
- 本次仅创建本地 Git 提交，提交说明为 `项目升级优化`，不推送远程仓库。
