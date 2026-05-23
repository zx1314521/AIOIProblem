# AIOIProblem 教学选题平台

AIOIProblem 是一个面向信息学竞赛内部教学的辅助选题平台。它可以根据题目文本、数据范围和算法标签预测题目难度，生成由浅入深的提示，管理题单，记录已通过题目，并根据学生当前做题情况推荐查漏补缺题目。

## 技术栈

- 后端：Java 21、Spring Boot、Spring Security、Spring Data JPA、Flyway、PostgreSQL
- 前端：Vue 3、Vite、TypeScript
- AI 评估：DeepSeek API 或本地 Codex CLI
- 数据库：PostgreSQL

## 项目结构

```text
AIOIProblem/
  backend/              Spring Boot 后端服务
  frontend/             Vue3 前端项目
  docker-compose.yml    本地 PostgreSQL 服务
  README.md             项目说明
```

## 本地启动

先进入项目根目录：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem
```

启动 PostgreSQL：

```powershell
docker compose up -d postgres
```

如果你在 `C:\Users\Zwzy` 直接运行 `docker compose up -d postgres`，Docker 会找不到 `docker-compose.yml`，就会出现：

```text
no configuration file provided: not found
```

## 启动后端

进入后端目录：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem\backend
```

运行测试：

```powershell
.\mvnw.cmd test
```

启动服务：

```powershell
.\mvnw.cmd spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

## 启动前端

进入前端目录：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem\frontend
```

安装依赖：

```powershell
npm.cmd install
```

启动开发服务：

```powershell
npm.cmd run dev
```

前端默认地址：

```text
http://localhost:5173
```

前端会把 `/api` 请求代理到后端 `http://localhost:8080`。

## 环境要求

需要安装：

- Docker Desktop
- JDK 21
- Maven，或可用的 Maven Wrapper
- Node.js 24 或兼容版本
- npm

当前 Windows PowerShell 可能会限制 `npm.ps1` 执行，建议使用：

```powershell
npm.cmd install
npm.cmd run dev
```

## AI 配置

后端配置文件：

```text
backend/src/main/resources/application.yml
```

常用环境变量：

- `AIOI_JWT_SECRET`：JWT 签名密钥
- `AIOI_AI_PROVIDER`：AI Provider，可选 `mock`、`deepseek`、`codex`
- `DEEPSEEK_API_KEY`：DeepSeek API Key
- `DEEPSEEK_BASE_URL`：DeepSeek 兼容接口地址
- `CODEX_CLI_COMMAND`：本地 Codex CLI 命令，默认 `codex`

默认 Provider 是 `mock`，也就是本地规则模型，不需要外部 API Key。

使用 DeepSeek：

```powershell
$env:AIOI_AI_PROVIDER="deepseek"
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
```

使用本地 Codex CLI：

```powershell
$env:AIOI_AI_PROVIDER="codex"
$env:CODEX_CLI_COMMAND="codex"
```

## 难度等级

系统固定使用以下难度标签：

- 入门
- 简单
- CSPJ中等
- CSPS提高
- NOIP困难
- 地狱NOI

## 主要功能

- 用户注册、登录、JWT 鉴权
- 粘贴题面文本进行 AI 分析
- 上传 `.txt` 或 `.md` 题面文件进行 AI 分析
- 批量上传多个 `title.txt` 文件，按队列逐题分析并保存到题库
- 批量任务支持查看进度、暂停、继续，服务重启后会自动恢复未完成任务
- 返回难度、置信度、算法标签和简短分析
- 分层提示：提示1、提示2、提示3，点击后展开
- 题目搜索、题目创建、标注已通过
- 新建题单、向题单加入题目、移出题目
- 根据已通过记录和薄弱标签生成 AI 推荐题
- 页面内 AI 设置：可切换本地 Codex、DeepSeek API、本地规则模型

## 批量导入题目

批量任务入口在前端左侧导航的“批量任务”。

文件命名规则：

```text
题目标题.txt
```

系统会把文件名去掉 `.txt` 后作为题目标题，把文件内容作为题面。上传后后端会创建一个批量任务，并按单线程队列逐题分析，避免 3000 多个题同时调用 AI。

暂停说明：

- 点击“暂停”后，不会强制中断正在分析的当前题。
- 当前题完成后，队列会停在后续等待项。
- 点击“继续”后，从等待项继续处理。

## 常用验证命令

前端测试：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem\frontend
npm.cmd run test
```

前端构建：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem\frontend
npm.cmd run build
```

后端测试：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem\backend
.\mvnw.cmd test
```

## 常见问题

### Docker 提示 no configuration file provided

原因：当前目录不是项目根目录。

解决：

```powershell
cd C:\Users\Zwzy\Documents\AIOIProblem
docker compose up -d postgres
```

### PowerShell 无法运行 npm

原因：PowerShell 执行策略拦截了 `npm.ps1`。

解决：使用 `npm.cmd`。

```powershell
npm.cmd install
npm.cmd run dev
```

### 后端提示找不到 Java 或 Maven

需要安装 JDK 21 和 Maven，并确保 `java`、`mvn` 能在 PowerShell 中直接运行。
