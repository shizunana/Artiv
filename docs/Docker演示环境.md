# Artiv Docker 备用演示方案（非正式项）

> 最近核对：2026-09-07，依据提交 `1728985` 的源码、Compose、Dockerfile 和脚本；本轮未构建镜像或运行容器。

> **方案状态：仅作备用，不是当前正式部署或交付方案。** 当前推荐方式是按[《跨电脑部署文档》](部署文档.md)直接安装 MySQL 8、JDK 21、Maven 3.9+ 和 Node.js 20+。本仓库保留 Docker 配置是为了以后具备 Docker 环境时可以继续尝试；目前没有生成可交付镜像，也没有完成容器运行验证。

没有 Docker 环境的电脑无需安装 Docker 或 WSL，也不需要修改、删除这些备用文件。它们不会影响原生方式启动前后端或后续正常开发。

## 环境目标

这套 Compose 的设计目标是将来用于本机空白演示或换电脑后的离线运行，不连接当前开发数据库，也不读取 `backend/uploads`。以下内容描述的是预期行为，需在实际构建并完成回归后才能作为可交付能力使用。

- 页面地址：`http://localhost:8088`
- MySQL：只在 Docker 内部网络开放，不映射宿主机端口。
- 后端：只在 Docker 内部网络开放，由 Nginx 代理。
- 数据卷：`artiv-demo-mysql`、`artiv-demo-uploads`。
- 默认只绑定 `127.0.0.1`，不会直接暴露到局域网或公网。

首次在空数据卷初始化后仅存在三个账号，不包含作品、摊位、企划、应征、邀请、订单、消息、评价、举报、认证申请或后台审计记录。已有 MySQL 卷不会因重新构建镜像而自动执行初始化 SQL 或更新表结构。

| 账号 | 昵称 | 密码 | 初始权限 |
| --- | --- | --- | --- |
| `planner` | 东云绘名 | `123456` | 企划方，正常状态 |
| `artist` | 静名御折木 | `123456` | 已认证画师，正常状态 |
| `admin` | 管理员 | `123456` | 管理员，正常状态 |

三个账号初始余额仍均为 0，空白数据不能直接完成需扣款的交易演示。使用包含 `1728985` 或后续代码的镜像时，可先登录 `admin`，进入“用户管理 → 详情”，通过“发放资金”为演示账号追加模拟余额，或通过“余额调整”设置最终余额；两种操作都要求原因并写管理员审计。该入口只是模拟数据维护工具，不是充值闭环，不创建充值订单或资金流水。演示期间创建的数据只存在于上述 Docker 命名卷，不会写入原生开发 MySQL。

## 前置要求

只有决定试用该备用方案时才需要以下环境：

- Windows 已安装并启动 Docker Desktop。
- Docker 使用 Linux containers。
- PowerShell 可以执行当前仓库中的脚本；如执行策略限制，可使用 `powershell -ExecutionPolicy Bypass -File ...`。

如果 Docker Desktop 提示 WSL 不可用，请在“管理员 PowerShell”中执行：

```powershell
wsl --install
```

执行完成后重启 Windows，再启动 Docker Desktop。此操作属于 Windows 系统环境安装，不由演示脚本自动执行。

## 试用启动（待运行验证）

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Start
```

任何脚本动作（包括 `Status`）在 `.env.demo` 不存在时都会生成该文件，其中包含随机数据库密码和 JWT 密钥，受 Git 忽略。随机密钥不会修改三个应用账号的默认密码。

`Start` 使用 `up -d --build`，可能访问网络下载镜像和依赖；它只等待数据库健康条件，没有后端就绪探针，脚本打印“已启动”不等于接口和页面已通过检查。实际端口由 `.env.demo` 决定，脚本提示地址仍固定为 8088。

其他命令：

```powershell
# 查看容器状态
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Status

# 停止，但保留演示数据
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Stop

# 删除演示数据并恢复三个空白账号
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Reset
```

`Reset` 停止 `artiv-demo` Compose 项目并删除其数据库、上传文件两个命名卷，然后重新构建启动；默认要求输入 `RESET`，`-Force` 会跳过确认。它没有自动备份，删除后的数据不能由脚本恢复。卷名固定，多个仓库副本也会共用这些名字，不能假定副本之间互相隔离；操作前须确认卷归属并备份。当前配置没有挂载原生开发数据库或 `backend/uploads`。

## Docker 方式换电脑（备用）

当前推荐的换电脑方式是使用[《跨电脑部署文档》](部署文档.md)中的 Windows 原生安装流程。只有目标电脑已经具备可用 Docker 环境时，才考虑复制完整 Artiv 项目并执行 `Start`；镜像预期从当前源码重新构建，数据库从空白状态开始。

如果新电脑无法联网，可提前导出镜像：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Export
```

默认生成 `dist/docker/artiv-demo-images.tar`。将以下内容复制到新电脑：

1. Artiv 项目目录。
2. `dist/docker/artiv-demo-images.tar`。
3. 如需保持相同容器密码，可一并复制 `.env.demo`；不复制时新电脑会自动生成新的随机配置。

新电脑导入并启动：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\docker-demo.ps1 -Action Import
```

`Export` 会先构建，必须在网络和构建依赖可用的环境准备；`Import` 使用 `--no-build`。离线导入后不要改用会触发构建的 `Start` / `Reset`，除非依赖缓存也已完整准备。

镜像包只包含应用镜像和空白数据库初始化逻辑，不包含运行后产生的数据库卷或上传文件。复用已有数据库卷时必须保留与其一致的 `.env.demo` 数据库密码；重新随机生成配置不会同步修改卷内账号密码。
离线导出的镜像应在相同 CPU 架构的电脑上使用；如果新电脑架构不同，请复制源码后重新执行 `Start` 构建。

## 局域网演示

默认只允许本机访问。如果确实需要答辩局域网访问，将 `.env.demo` 中：

```text
ARTIV_DEMO_BIND=127.0.0.1
```

改为：

```text
ARTIV_DEMO_BIND=0.0.0.0
```

然后重新执行 `Start`，并通过 Windows 防火墙只允许可信局域网访问端口 8088。不要把当前演示环境直接暴露到公网。

## 源码核对后的待验证事项

- 当前镜像基于 MySQL 8.4、Java 21、Node 20 和 Nginx 1.27；原生 MySQL 8.0 的历史验证不能代替这组容器环境验证。
- 后端镜像构建明确跳过测试；构建成功也不代表自动化测试通过。
- Nginx 代理 `/api/`、`/uploads/` 和 `/ws`，但没有增加原图下载授权。公开上传目录、旧用户接口权限等问题仍见 [未解决问题](未解决问题.md)。
- 正式试用前须验证空卷初始化、接口就绪、上传持久化、WebSocket、重启恢复及可信局域网访问；本轮没有执行这些动作。
