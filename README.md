# WeCom Customer Service

企业微信 / 微信客服 AI 助手，基于原项目
[xliking/wechat_ai](https://github.com/xliking/wechat_ai) 二次开发。

本项目面向企业客服场景，重点提供：

- 企业资料与企业知识库
- 普通消息自动 RAG 问答
- 多轮上下文对话
- 人工客服接管与后台实时聊天
- 国内大模型接入
- 管理后台、数据隔离和安全加固

> 本项目是独立二开项目，不代表原项目作者，也不代表腾讯 / 企业微信官方。

## 功能

### 企业知识库

- 后台手动录入企业介绍、产品、FAQ、售后政策等知识
- 上传 PDF、DOCX、XLS、XLSX、CSV、TXT、MD、HTML
- 自动解析、切片、建立索引
- 知识状态：待处理、处理中、已完成、失败
- 支持编辑、删除、重建索引
- 支持检索测试和 AI 回答测试

### 自动 RAG

- 客户发送普通消息时自动检索企业知识
- 不需要客户输入“知识库”等特殊指令
- 企业资料也会作为结构化知识参与检索
- 检索结果结合最近聊天历史交给大模型
- 资料不足时明确提示，不编造价格、规格、售后承诺

未配置 Embedding Key 时使用关键词/字符匹配兜底；配置 SiliconFlow
Embedding Key 并重建索引后启用向量检索。

### 人工接管

- 客户发送“转人工”进入人工模式
- 后台“客服窗口”实时接收消息
- 人工回复通过微信客服 API 发送
- 支持结束人工、恢复 AI
- Redis 人工状态超时后自动尝试切回 AI

当前微信客服 `service_state` 接口需要相应接口权限。若返回 `48002`，
微信侧会话状态可能无法由程序切换，需要在微信客服工作台手动结束会话。

### 安全

- 管理后台登录鉴权
- 管理 API 未登录返回 401
- WebSocket 握手鉴权
- AI / MCP API Key 使用 AES-GCM 加密保存
- 管理端 API Key 只显示掩码
- 上传文件类型、大小限制
- XML 解析禁用外部实体
- Flyway 管理数据库结构

## 技术栈

- Java 21
- Spring Boot 3.5
- Spring Data JPA + H2
- Redis
- Flyway
- Thymeleaf + Bootstrap
- Docker / Docker Compose
- DeepSeek OpenAI-compatible API
- SiliconFlow（可选：Embedding、图片、语音）

## 快速开始

### 1. 准备环境

- Docker 24+
- Docker Compose v2
- 一个可访问的 HTTPS 域名
- 企业微信客服账号和回调配置

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env`，至少修改：

```dotenv
ADMIN_PASSWORD=你的后台密码
APP_ENCRYPTION_KEY=至少32位随机字符串
DB_PASSWORD=数据库密码
REDIS_PASSWORD=Redis密码

WECHAT_CORP_ID=企业ID
WECHAT_SECRET=微信客服Secret
WECHAT_TOKEN=回调Token
WECHAT_AES_KEY=回调EncodingAESKey

AI_API_KEY=你的大模型Key
DEFAULT_AI_BASE_URL=https://api.deepseek.com/chat/completions
DEFAULT_AI_MODEL=deepseek-chat
```

不要提交真实的 `.env` 文件。

### 3. 启动

```bash
docker compose up -d
```

访问：

- 管理后台：`https://你的域名/admin/login`
- 企业资料：`https://你的域名/admin/enterprise`
- 企业知识库：`https://你的域名/admin/knowledge`
- 客服窗口：`https://你的域名/admin/chat`

### 4. 微信客服回调

在微信客服后台的“开发配置”中设置：

```text
https://你的域名/wechat/callback
```

填入与 `.env` 一致的 Token、EncodingAESKey、企业ID 和 Secret。

> 注意：企业微信“联合版”和微信客服“独立版”不能同时接管同一个客服账号。
> 如果接口返回 `95011 already use in wecom`，请只保留一套 API 管理方式，
> 并在对应后台停用另一套接入。

## 知识库使用

1. 打开 `/admin/knowledge`
2. 点击“新增知识”或上传文件
3. 选择分类：企业介绍、产品、FAQ、售后、其他
4. 等待状态变为“已完成”
5. 使用“检索测试”检查命中片段
6. 使用“生成回答”检查大模型回答

支持的格式：

```text
.pdf .docx .xls .xlsx .csv .txt .md .html .htm
```

文件上传后保存在 `uploads/`，数据库和索引保存在 `data/`。

## 人工客服使用

客户发送：

```text
转人工
```

后台打开：

```text
https://你的域名/admin/chat
```

在左侧待接入列表选择客户，在底部输入框回复。

结束人工：

- 客户发送“结束人工服务”，或
- 后台客服窗口点击“结束服务”

如果微信侧返回 `48002`，本地人工状态仍会结束，但微信侧会话可能仍处于人工状态；
此时需要在企业微信客服工作台手动结束会话。

## 配置说明

关键变量：

| 变量 | 说明 |
|---|---|
| `ADMIN_PASSWORD` | 管理后台密码 |
| `APP_ENCRYPTION_KEY` | API Key 加密密钥，设置后不要更换 |
| `WECHAT_CORP_ID` | 企业 ID |
| `WECHAT_SECRET` | 微信客服 Secret |
| `WECHAT_TOKEN` | 回调 Token |
| `WECHAT_AES_KEY` | 回调 EncodingAESKey |
| `WECHAT_OPEN_KFID` | 客服账号 ID，可留空由程序自动获取 |
| `AI_API_KEY` | 默认大模型 Key |
| `DEFAULT_AI_BASE_URL` | OpenAI-compatible 接口 |
| `DEFAULT_AI_MODEL` | 模型名称 |
| `RAG_API_KEY` | 可选，SiliconFlow Embedding Key |
| `RAG_ENABLED` | 是否启用 RAG |
| `DB_PASSWORD` | H2 数据库密码 |
| `REDIS_PASSWORD` | Redis 密码 |

完整变量见 [`.env.example`](.env.example)。

## Docker 镜像

镜像由 GitHub Actions 构建并推送：

- Docker Hub：`dockorae/wecom-customer-service`
- GHCR：`ghcr.io/minimaxflora/wecom-customer-service`

工作流文件：`.github/workflows/docker-publish.yml`

触发条件：

- push 到 `main`
- push `v*` 标签
- 手动 `workflow_dispatch`

需要在 GitHub 仓库中配置：

- `DOCKERHUB_USERNAME`：`dockorae`
- `DOCKERHUB_TOKEN`：Docker Hub Access Token

GHCR 使用仓库自带的 `GITHUB_TOKEN`，工作流已声明 `packages: write`。

本地构建：

```bash
docker build -t wecom-customer-service:local .
```

## 测试

```bash
mvn test
mvn -DskipTests package
```

手工回归建议：

1. 登录后台，确认未登录 API 返回 401
2. 企业资料页保存名称、行业、简介
3. 上传套餐/产品文档，等待 READY
4. 检索测试命中知识片段
5. 客户发“你们是做什么的”“套餐多少钱”
6. 客户发“转人工”，后台客服窗口回复
7. 后台结束服务，客户再发消息确认 AI 恢复

## 目录结构

```text
src/main/java/...       Java 源码
src/main/resources/
  db/migration/         Flyway 数据库迁移
  templates/            Thymeleaf 后台页面
Dockerfile             多阶段镜像构建
docker-compose.yml     应用 + Redis
.env.example           环境变量模板
.github/workflows/     CI / 镜像发布
```

## 数据与升级

- `data/`：H2 数据库文件
- `uploads/`：企业知识库原始文件
- 升级前先备份 `data/` 和 `uploads/`
- 数据库结构由 Flyway 迁移，不要手工修改表结构

## License

本项目基于 MIT License 二次开发。

原始项目版权与许可声明保留在 [`LICENSE`](LICENSE) 和 [`NOTICE`](NOTICE) 中。
使用、修改、分发时请保留原始版权声明和 MIT 许可文本。
