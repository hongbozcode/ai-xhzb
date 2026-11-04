# 星海智伴 Java + Python AI 迁移实现文档

> 编制日期：2026-11-03。性质：待实施的工程设计与验收依据，不表示功能已经完成。
> 本文可脱离原讨论直接使用。本次交付仅新增本文，不修改业务代码，不执行 Git 暂存、提交或推送。

## 1. 项目目标与确定范围

将现有 Spring AI 能力迁出 Java，建立独立、可扩展的 Python Agent 服务。对用户保持前端 → Java → Python → Java → 前端的入口链路；Python 调用业务工具时回调 Java。

首期必须交付：

1. 迁移流式聊天，完成历史会话、新建、切换、删除与多轮上下文。
2. 完成知识库上传保存、手动触发异步解析、状态查询、重试、重新解析、禁用、删除。
3. 实现本地 BGE-M3 + Milvus 的 RAG，回答关联可核验的原文、PDF 页码和文档版本。
4. 通过受控工具接入已有护理项目、护理等级、护理计划查询，授权和业务数据访问统一交给 Java。
5. 将健康评估的两阶段模型调用迁至 Python；Java 保留既有业务规则、校验与报告入库。
6. 迁移验证完成后移除 Java Spring AI 依赖、配置、导入及模型调用。
7. 形成三个可演示亮点：可靠异步入库、RAG 溯源、受控工具调用。

本期不包含：微信小程序（尚未开发）、家属及老人归属业务建设、服务预约、护理执行记录业务、财务功能、自然语言 SQL、多 Agent Handoff、护理计划自动草拟/审核、OCR、独立 RAG 评测平台、完整分布式追踪平台。保留扩展边界，不注册不存在的业务工具。

## 2. 当前工程基线与实施入口

### 2.1 仓库位置与保护规则

```text
D:/Users/ZHB/Desktop/AICoding/xhzb-project/
├─ xhzb-service/             Java 后端，本文所在根目录
├─ xhzb-ui/                  Vue 管理端
└─ xhzb-agent/               拟新增的独立 Python 项目，当前尚未创建
```

检查时 Java 分支为 `dev02-spring-ai`，已有用户未提交/已暂存的健康评估、KnowledgeBase 修改和 `.agents/` 内容。开始实施前重新检查分支、工作区及各目录的 AGENTS.md；沿用用户已有修改，不重置、覆盖或擅自提交。不因本文存在而推定相邻仓库的文件写入权限已获得。

项目现有技术：Java 17、Spring Boot 3.5.0、Spring AI BOM 1.1.8、MyBatis/MyBatis-Plus、MySQL、Redis、阿里云 OSS。版本取自编制时源码，实施前检查是否变化。

### 2.2 需要阅读和修改的源码

以下路径相对 `xhzb-service/`，前端路径从其父目录访问：

| 入口 | 当前行为与迁移目标 |
|---|---|
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/controller/ChatController.java` | `/ai/chat` 接收 prompt，返回 `Flux<String>` 普通文本；改为 Java 到 Python 的事件流代理 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/config/SpringAIConfig.java` | 普通聊天、健康评估两个 ChatClient Bean；迁移后删除 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/service/impl/HealthAssessmentServiceImpl.java` | 保存表单、两阶段模型分析、保存报告；拆分模型分析与短事务 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/SystemConstant.java` | 现有评估提示词；提取至 Python 场景包并版本化 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/domain/vo/ElderLevelResultVO.java`、`HealthReportResultVO.java` | 结构化评估结果的兼容基线 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/controller/KnowledgeBaseController.java` | 已有上传和 CRUD 模板；补异步接口、上传权限与服务端校验 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/service/impl/KnowledgeBaseServiceImpl.java` | 新增时下载文件，Python 调用留空，remark 拟存切片 ID；需调整生命周期 |
| `xhzb-nursing-platform/src/main/java/com/xhzb/nursing/domain/KnowledgeBase.java`、`service/IKnowledgeBaseService.java`、`mapper/KnowledgeBaseMapper.java` | 增加明确 DTO、状态及持久化能力 |
| `xhzb-nursing-platform/src/main/resources/mapper/nursing/KnowledgeBaseMapper.xml` | 读写映射随数据库迁移更新 |
| `xhzb-framework/src/main/java/com/xhzb/framework/config/SecurityConfig.java` | 当前 `/ai/chat` 在 permitAll 中；业务聊天必须改为 Java 鉴权 |
| `../xhzb-ui/src/views/nursing/zhixun/index.vue`、`../xhzb-ui/src/api/nursing/ai.js` | 已引用历史接口，但聊天后端未实现；重写事件解析和会话 ID 管理 |
| `../xhzb-ui/src/views/nursing/knowledgeBase/index.vue`、`../xhzb-ui/src/api/nursing/knowledgeBase.js` | 补解析按钮、阶段、失败信息和离页恢复展示 |
| `../xhzb-ui/src/views/nursing/healthAssessment/`、`../xhzb-ui/src/api/nursing/healthAssessment.js` | 保持评估业务入口和报告查看兼容，补失败反馈 |
| `pom.xml`、`xhzb-nursing-platform/pom.xml`、`xhzb-admin/src/main/resources/application*.yml` | 最后清理 Spring AI，新增 Python 客户端配置 |
| `sql/xhzb.sql` | 现有表结构参考；另交付增量迁移，不能导入整库覆盖用户数据 |

现有前端通过 POST 拼接 URL 查询参数发送 prompt，直接拼接原始响应；新协议使用 JSON 请求体和标准 SSE 分帧。现有临时时间戳会话 ID、按最新历史猜测会话等逻辑应移除。

KnowledgeBase 上传页面文字提示 10MB，实际前端检查 2MB，后端只检查非空。实施时统一为配置驱动的限制，建议初始 10MiB，并同步网关、Java multipart、前端、Python下载及解析限制。普通文本 PDF 为首期支持类型，检查扩展名、内容类型和文件签名。

已有 `nursing_project` 是项目目录；`nursing_project_plan` 是计划项目关系；SQL 中 `nursing_task` 虽有执行字段，但业务实现未开发。`reservation` 是参观/探访预约，不可当作护理服务预约。不得声称已能查询实际执行次数、剩余额度或财务状态。

## 3. 技术选型与服务边界

### 3.1 确定的技术选择

| 能力 | 实现方向 |
|---|---|
| Python HTTP | FastAPI，异步请求与生命周期管理 |
| Agent | LangChain `create_agent`，LangGraph 用于明确的场景流程 |
| 生成模型 | `langchain-openai` 接入现有 OpenAI 兼容服务；聊天与评估分别配置 |
| Embedding | 本地 BGE-M3，首期稠密向量 1024 维 |
| 向量存储 | Milvus，首期独立服务部署，版本与 SDK 联调后锁定 |
| 持久化 | 继续 MySQL，不引入 PostgreSQL；Redis 现有能力可复用，但不作为唯一任务记录 |
| 文件 | 复用 Java OSS 管理；Python 不持有 OSS 管理密钥 |
| 后台执行 | Python 独立 Worker + MySQL 持久化任务；Java 持久化命令与派发器 |
| 流式输出 | Python SSE → Java 流代理 → 前端 fetch 流解析 |

BGE-M3 是检索模型，不用于生成聊天或健康评估回答。现有开发配置写有 DashScope OpenAI 兼容地址及 `qwen3.7-max`，仅作为迁移配置基线，不代表已验证服务可用。密钥不复制进文档或示例。

目前模型文件、推理环境、Milvus 等尚未准备；硬件、地址、模型路径是部署参数，不阻塞代码架构设计。Python 建议 3.12，实际依赖版本需联调后写入锁文件；不得把 Skill 样例版本直接当作已验证生产组合。

### 3.2 权限与数据所有权

Java 负责登录、Token 验证、角色权限、数据范围、会话归属、文档可见性、业务规则、业务事务和审计。Python 不实现入站 JWT 验签、登录、RBAC 或独立的业务授权系统，按 Java 提供的可信结果执行。

Python API 无应用层入站鉴权，因此部署必须绑定回环地址，或使用仅 Java/Worker 可达的受限网络；不能公开 Python 端口供前端直连。知识源、工具目标均来自管理员固定配置，禁止模型或用户提供任意出站 URL。

| 数据 | 权威拥有者 |
|---|---|
| 业务数据、知识启停/删除状态、活动索引版本 | Java MySQL |
| 会话、消息、用户归属、公开对话执行状态 | Java MySQL |
| 任务提交意图、派发重试、发布/删除决策 | Java MySQL |
| 入库执行任务、租约、阶段、切片、构建清单 | Python MySQL |
| 向量与检索字段 | Milvus，不能取代 Java 的可见性决策 |
| 原始文档 | OSS，由 Java 管理 |

两个服务可复用同一 MySQL 实例，建议独立数据库/账户。Python 不直接读写护理业务表，Java 不直接更新 Python 任务表；通过版本化 HTTP 契约交互。

## 4. Python 框架目录与扩展方式

```text
xhzb-agent/
├─ pyproject.toml / uv.lock / .env.example
├─ README.md / Dockerfile / compose.yaml
├─ migrations/
├─ src/ai_backend/
│  ├─ main.py
│  ├─ bootstrap.py
│  ├─ core/           # config、context、errors、logging、limits
│  ├─ transport/http/ # chat、knowledge、assessments、events、health
│  ├─ models/         # contracts、factory、adapters/openai_chat、bge_m3
│  ├─ biz/            # BusinessGateway、adapters/java_http
│  ├─ agent/          # AgentRunner、工具执行约束、LangChain adapter
│  ├─ rag/            # contracts、ingest、retrieval、adapters/milvus、mysql
│  ├─ workflows/      # contracts、LangGraph adapter
│  ├─ scenarios/xhzb/ # consultation、health_assessment、nursing/tools
│  └─ workers/        # knowledge_ingest、lease、cleanup
└─ tests/             # unit、integration、contract
```

| 模块 | 职责与抽象 | 首期默认实现 | 扩展点与未来适配 |
|---|---|---|---|
| Core | Context、AppError、配置、预算、日志 | 配置校验、JSON日志、trace/run/request ID | 配置中心、监控；不依赖上层业务 |
| Models | ChatModelFactory、Embedder | langchain-openai、本地 BGE-M3 | 替换生成模型或远程 Embedding；输出协议不变 |
| Biz | BusinessGateway.call | 固定 Java HTTP 路由、错误映射、超时 | 第三方系统；不把养老业务方法塞进通用接口 |
| Agent | AgentRunner、ToolSpec/ToolHandler | create_agent，预注册工具 | 场景路由、子代理；不另套无限推理循环 |
| RAG | KnowledgeBuilder、KnowledgeSearch、JobRepository | PDF、MySQL、Milvus | 新解析器、混合检索、reranker、OCR |
| Workflows | WorkflowRunner | 健康评估固定流程 | 持久化恢复、人审；首期不开发可视化 DSL |
| Transport | DTO、应用事件、HTTP错误 | FastAPI/SSE | 新传输方式不改核心 |

依赖规则：transport → 应用端口；bootstrap 负责装配实现；场景包把 Biz/RAG 装为工具；Agent 核心不直接 import 养老业务客户端；Core 不依赖上层。框架消息、State、Command 在适配层转换，禁止泄漏 SDK 类型到 Java 契约。

参考 `.agents/skills/ai-backend-foundry/SKILL.md` 及其 architecture、biz、rag、agent、langchain-langgraph 文档。参考代码不作为运行依赖；不复制其中演示认证、演示凭据、关键词检索替身并声称已接通生产能力。未知 provider、缺模型、向量维度不匹配等情况启动失败，不静默回退假数据。

## 5. 共享契约与基础运行规则

- 对外 Java 保留 AjaxResult/TableDataInfo；Python 使用 `{ok,data,error,trace_id}` 统一信封，Java 显式转换。HTTP 状态必须反映真实结果。
- 跨服务 ID 使用字符串，尤其 Java Long 对前端统一序列化为字符串，避免精度损失。
- 通用错误：INVALID_ARGUMENT、FORBIDDEN、NOT_FOUND、CONFLICT、RATE_LIMITED、UPSTREAM_UNAVAILABLE、TIMEOUT、INTERNAL。展示受控错误，不透传原始下游正文、密钥或异常堆栈。
- request_id 表示客户端幂等请求，run_id 表示对话执行，task_id 表示入库任务，trace_id 仅用于关联；这些 ID 本身不是用户身份凭证。
- 用户凭据只放请求级 Context/HTTP header；不放 prompt、模型参数、共享可变全局变量、MySQL消息、任务payload或 checkpoint。
- 只读调用允许按总预算有限重试；401/403、参数错误不重试。存在副作用的操作必须有幂等键，超时结果未知先查询，不直接重放。
- 首期交付轻量结构化日志和必要指标即可，不强制增加完整观测平台。日志记录阶段、结果码、耗时、计数，不默认记录提示词、健康原文、完整工具结果。

## 6. 聊天、会话与流式迁移

### 6.1 Java API

| 接口 | 约定 |
|---|---|
| `POST /ai/chat` | JSON `{requestId,chatId?,prompt}`，SSE；不再用 URL 传 prompt |
| `GET /ai/history` | 当前用户会话列表，支持分页；前后端同时适配明确 DTO |
| `GET /ai/history/{chatId}` | 校验归属后返回消息及引用、状态 |
| `DELETE /ai/history/{chatId}` | 校验归属；有运行中请求时先取消/作废结果 |

Java 在调用 Python 前创建/校验会话、落用户消息及执行记录，生成权威 chatId；通过首个 accepted 事件返回。不得由前端时间戳直接决定会话归属，不按“最近一条历史”猜测 ID。

建议 Java 表：`ai_chat_session`（owner_user_id、标题、active_run_id、revision、deleted_at），`ai_chat_message`（session_id、role、content、status、citations_json、request_id、run_id），`ai_chat_run`（状态、错误码、耗时）。同一会话最多一个活动 run，使用数据库原子更新/唯一约束保证，不能只依赖前端按钮或单机锁。

每轮 Java 从 MySQL 加载有预算的历史/摘要，Python 接收后执行当前轮。首期不实现持久化 LangGraph checkpoint，不承诺进程崩溃后恢复到某次工具调用；聊天消息持久化不等于执行状态持久化。摘要仍属对话数据，不能提升为系统指令。

### 6.2 SSE v1

Python 入口建议 `POST /v1/chat/runs`。应用事件转换为标准 SSE，统一字段 `schema_version,event_id,run_id,chat_id,type,data`。

| 事件 | data 核心字段 |
|---|---|
| accepted | chatId、runId、requestId |
| text_delta | delta |
| tool_started | toolCallId、toolName、用户可读说明 |
| tool_finished | toolCallId、status、受控错误码；不向前端暴露完整业务结果 |
| citation | citationId、knowledgeId、documentVersion、buildId、页码、标题、受控摘录 |
| error | code、message、retryable |
| done | status=completed/failed/cancelled |

每个正常存活连接恰有一个 done；业务错误发送 error 后 done(failed)。断网/进程崩溃可能没有终止帧，前端必须显示中断，通过历史状态恢复展示。流开始前用 HTTP 状态报错，开始后不能再更改 HTTP 状态。Java 不把流一次性读完，正确传播取消，配置心跳、代理不缓冲及合理超时。

前端用 TextDecoder 流式解码，缓存跨网络块的 SSE 帧，支持 CRLF、多个事件和 UTF-8 跨块，不把一次 read 当成完整事件。不自动重发可能已执行的请求；重复 requestId 返回已有执行状态/历史结果，而不是再次运行 Agent。显示文本要消毒 Markdown/HTML，避免现有 v-html 直接渲染不可信内容。

Java 保存完成/失败/中断状态及可恢复的部分回答。若前端断开，取消在线只读推理并释放资源；独立的知识入库任务不随页面断开取消。

## 7. 知识库两步交互与接口

### 7.1 用户流程

```text
上传 PDF → 保存知识记录（未解析）
  → 列表点击“解析” → 立即返回任务ID
  → 排队 / 处理中 → 成功或失败
  → 用户可以离开页面，后台继续，返回后重新查询状态
```

新增时不得下载文件、解析或调用生成模型。文件上传仍需等待上传本身完成；要求离页继续的是已提交的后台解析任务，不承诺浏览器关闭后未完成上传仍继续。

| Java 接口 | 行为 |
|---|---|
| `POST /nursing/knowledgeBase/upload` | 上传受控 PDF，返回 uploadId、文件描述；可兼容现有 url 字段 |
| `POST /nursing/knowledgeBase` | 绑定当前用户有权使用的上传记录，保存元数据，返回 id、parseStatus |
| `PUT /nursing/knowledgeBase` | 修改元数据或提交新的文件版本，不隐式同步解析 |
| `POST /nursing/knowledgeBase/{id}/parse` | 首次解析、失败重试、重新解析统一入口；202+taskId |
| `GET /nursing/knowledgeBase/{id}/parse-status` | 返回阶段、状态、错误、更新时间、当前活动版本 |
| `GET /nursing/knowledgeBase/list` | 增加解析摘要，未解析/运行/失败可以筛选 |
| `GET /nursing/knowledgeBase/{id}` | 详情含文件版本和索引状态 |
| `DELETE /nursing/knowledgeBase/{ids}` | 立即逻辑隐藏，202返回清理任务结果；批量逐项可追踪 |
| `GET /nursing/knowledgeBase/{id}/source?version=...&page=...` | Java授权后返回受控预览入口/文件流 |

同一当前构建重复点击返回已有 taskId；失败重试创建新执行记录并关联原任务。重新解析生成新的 buildId，即使原文件未变化也与旧活动构建分离。

运行中的同一文档不允许再启动并行构建；更换文件使旧构建作废，可等待取消后再构建。标题/标签/优先级只更新元数据，不必重新向量化；启停只影响可见性。替换 PDF 创建新 sourceVersion，旧活动版本可继续使用直至新版本成功，页面明确提示“新版本待解析/失败，当前仍使用旧版”。

前端增加未解析、排队、处理中、成功、失败及清理中；显示实际处理阶段，不虚构百分比。仅对运行任务轮询，初始建议3秒，可退避；组件卸载停止轮询，回页重新查数据库状态。

### 7.2 模板方法调整

- `insertKnowledgeBase()`：只落元数据、文件版本和未解析状态；删除同步 OSS download、解析占位及 remark 写切片 ID 的逻辑。
- `updateKnowledgeBase()`：使用专用 DTO 和字段白名单，区分文件更新、元数据和启停；客户端不可修改 owner、active_build_id、task_id、parse_status。
- 单条/批量删除统一走一个生命周期服务，避免现有 `deleteKnowledgeBaseById()` 直接 removeById 绕过清理。
- 不复用 remark 存 docIdList；新增明确的版本/任务关联，remark 保留备注。兼容旧 remark 中空值、null或历史JSON，但不能把未知历史数据当成已完成索引。
- 上传、解析、状态、删除、预览均由 Java 做权限检查；文件地址不能由客户端随意指定为任意 OSS 对象。未绑定上传记录按TTL清理，替换文件按引用/版本清理。

## 8. 数据模型与迁移要求

以下是逻辑字段和约束，不是可直接执行的最终 DDL。实施时输出显式、可回滚的增量迁移，校验现有字段类型、索引、MySQL版本和时区。

### 8.1 Java 管理的数据

| 表 | 关键字段与约束 |
|---|---|
| `knowledge_base` 扩展 | source_version、active_build_id、active_source_version、current_task_id、parse_status、error_code、error_message、revision、deleted_at；原status仍表示启停 |
| `knowledge_file_version` | knowledge_id、source_version、受控对象key、content_hash、字节数、原文件名、创建人；唯一(knowledge_id,source_version)；保留活动旧文件供引用 |
| `knowledge_build_request` | task_id、knowledge_id、source_version、build_id、pipeline_version、requested_by、期望revision、公开状态、agent_job_id、错误及时间 |
| `ai_command_outbox` | command_id、幂等键、命令类型、受控payload、状态、重试次数、next_attempt_at、派发租约、错误；同事务持久化业务意图与待派发命令 |
| `ai_tool_audit` | run_id、tool_call_id、tool_name、真实operator、必要资源ID、结果码、耗时；不保存Token/完整业务正文 |
| `ai_health_analysis_run` | assessment_id、request_id、input_revision/hash、状态、模型/提示词版本、错误；支持阻止旧结果覆盖新表单 |

所有对用户可见的“解析成功”由 Java 活动版本发布决定。Java 的任务状态是执行任务的投影，不能凭请求已派发就标成功。

### 8.2 Python 管理的数据

| 表 | 关键字段与约束 |
|---|---|
| `ai_ingest_job` | job_id、request_id唯一、knowledge_id、source_version、build_id、pipeline_signature、state、stage、attempt、lease_owner、lease_until、fencing_token、error、时间 |
| `ai_index_build` | build_id、source hash、模型版本/维度、切片参数、pipeline版本、collection、预期/实际切片数、构建状态 |
| `ai_document_chunk` | chunk_id、build_id、chunk序号、页码范围、偏移、文本/受控存储引用、hash；唯一(build_id,chunk_no) |
| `ai_cleanup_job` | 操作ID、知识/构建范围、进度、租约、错误与重试 |

Milvus 字段至少含 chunk_id、knowledge_id、source_version、build_id、pipeline_signature、page_start/page_end、content_hash、dense_vector。切片 ID 对同一构建确定性生成；检索引用必须能够反查原文和文件版本。

## 9. 核心亮点一：可靠异步入库

### 9.1 提交与可靠派发

1. Java 在一个短事务中锁定知识记录，确认未删除/无冲突任务，创建 build request，写 outbox，并更新 current_task_id。
2. 事务提交即可以返回202。该响应表示提交意图已持久化；Python暂不可达时显示排队/待派发，不丢请求。
3. Java后台派发器领取outbox，调用 Python `POST /v1/knowledge/jobs`。只传知识ID、sourceVersion、buildId、pipelineVersion和requestId等受控描述，不接受客户端任意URL。
4. Python校验数据结构及注册配置，MySQL提交任务后返回202；同一requestId重复提交返回原jobId，不能重复执行同一命令。
5. Java记录agent_job_id。响应丢失或记录前崩溃时按相同requestId重发，或查询状态，最终收敛。

### 9.2 后台执行与文件获取

Worker原子领取任务，定期续租。首期支持MySQL8时可选事务行锁/跳过已锁记录，或用版本条件更新；具体SQL要按目标版本核对。租约、心跳和单次模型超时协调配置，耗时解析/Embedding不能阻塞心跳。

流程：校验源版本 → 从Java受控源接口流式取PDF → 内容hash校验 → 解析 → 切片 → BGE-M3 → Milvus幂等upsert → 核验构建清单 → 标记构建完成。

后台任务不保存用户登录Token。文件读取采用Python服务配置的出站机器凭据，由Java内部端点验证并约束到已登记任务、文档版本和操作；该凭据不能用于普通护理业务接口。Python入站仍不做鉴权。机器凭据放环境变量/密钥配置，不放任务payload；取文档地址由固定Java base URL + ID组成。

### 9.3 重试、租约与发布

- Python执行状态：QUEUED → RUNNING → SUCCEEDED / FAILED / CANCELLED；stage区分DOWNLOAD、PARSE、EMBED、INDEX、VERIFY。Python SUCCEEDED只代表构建可发布，不代表Java已经激活。
- Java公开状态：NOT_PARSED → QUEUED → PROCESSING → PUBLISHING → SUCCEEDED，另有FAILED、CANCELLED；删除单独生命周期处理。
- Worker失联后可重新领取，fencing_token递增；旧Worker不得更新新租约的任务状态、清理或发布他人构建。
- 多实例可能重复执行，采用至少一次调度+幂等写入，不宣称跨MySQL/OSS/Milvus的exactly-once事务。
- 阶段重试读取持久化清单；重算也写同一构建的确定性chunk_id。支持暂时故障有限退避，参数/文件不支持等错误直接失败。
- Java后台对账器查询Python任务，不依赖浏览器。收到构建成功后，在短事务中校验knowledge未删除、task仍为当前、sourceVersion和pipeline符合期望，然后原子切换active_build_id并标公开成功。
- 构建ID一旦创建不可变；重建另建buildId。新构建发布前旧活动版本不动。发布事务回执丢失时先查active_build_id，不因超时立即删除新向量。
- 旧向量发布后异步清理；旧引用需要的文件/文本按保留策略留存，文档已禁用/删除时预览仍拒绝访问。

### 9.4 删除与补偿

先由Java提交删除标记、使文档不可见、使当前构建失效并记录清理命令，再后台取消/收敛运行任务、清理Milvus与索引元数据、最后清理不再引用的OSS文件。保留墓碑与清理任务直到完成，物理删除记录不能抹掉重试依据。

删除完成需要所有有效执行尝试已收敛，或晚到写入已被二次清扫；不能只发一次cancel就宣告清理完毕。旧任务即使写出残留向量，也因Java活动清单不匹配而不可检索。旧版本清理只删除指定buildId，不能误删新版本；整个知识删除才清理其全部构建。批量删除返回逐项状态，不能把部分失败包装成全部完成。

## 10. 核心亮点二：RAG 溯源

### 10.1 检索与可见性

1. Java确认用户会话、可检索知识范围，Python拿到可信检索描述。
2. 使用与建库相同的BGE-M3配置对问题向量化，Milvus按结构化知识范围/构建范围过滤并召回有限候选。
3. 生成前将候选(knowledgeId,buildId,sourceVersion)交给Java批量复核启用、删除、活动版本及用户可见性；Java决定授权，Python只剔除未获准的候选。
4. 只有通过复核的片段进入模型、citation事件和用户输出。候选不足可按预算扩大召回，不放宽授权条件。
5. 根据有效片段生成回答，输出来源引用。无依据时说明资料不足，不能虚构引用或把通用回答标成机构规范。

禁止仅在生成回答后隐藏引用。不能以Milvus删除是否完成作为禁用生效依据。以Java可见性检查的时点为授权快照，禁用提交后的新检查拒绝；已向用户发送的历史内容无法撤回，引用打开时重新校验。

首期dense检索，不默认添加稀疏检索、BM25或reranker；保留统一Search端口。topK、长度、重叠、阈值根据实际PDF调优，不把任意距离分数当概率。模型维度、文本归一化、模型权重版本、切片参数共同构成pipeline_signature，变更后建新索引，不混用旧向量。

### 10.2 引用与原文定位

引用DTO至少含citationId、knowledgeId、sourceVersion、buildId、chunkId、title、pageStart/pageEnd、excerpt及Java受控预览路径。citationId在run内唯一，引用清单由实际检索命中生成，模型只能使用已有ID；不存在的ID不展示为真实来源。

Python保存物理PDF页码（从1开始），与印刷页码不同需在界面说明。跨页切片保留范围，引用保持原文依据。首期无法可靠提取文本的扫描PDF标记需要OCR，不伪造解析成功。

Java把回答及引用快照存入消息，重进历史仍可看到来源。点击来源走Java校验并定位对应版本页码，不能永远打开最新文件冒充旧引用；文件已清理时明确显示原版本不可用，不切换到新版本替代。引文片段做长度限制、HTML转义，文档内容作为不可信资料不能改变系统指令或权限。

## 11. 核心亮点三：受控工具调用

### 11.1 首期注册工具

| 工具 | 业务参数 | 固定Java路由 | Java操作权限 |
|---|---|---|---|
| search_nursing_projects | name、分页 | `GET /nursing/project/list` | nursing:project:list |
| get_nursing_project | project_id | `GET /nursing/project/{id}` | nursing:project:query |
| list_nursing_levels | name、分页 | `GET /nursing/nursingLevel/list` | nursing:nursingLevel:list |
| get_nursing_level | level_id | `GET /nursing/nursingLevel/{id}` | nursing:nursingLevel:query |
| list_nursing_plans | name、分页 | `GET /nursing/nursingPlan/list` | nursing:nursingPlan:list |
| get_nursing_plan | plan_id | `GET /nursing/nursingPlan/{id}` | nursing:nursingPlan:query |

实施时按实际DTO字段映射分页/名称参数，结果限定行数、字段和字节数。已有 `/all` 入口没有显式方法级权限时，不用它绕开表中受保护路由；必要时补齐Java授权后再开放。

### 11.2 身份与执行流程

```text
前端Bearer Token → Java验证身份与聊天权限
 → Python收到问题及独立运行上下文
 → 模型请求 get_nursing_plan(plan_id=12)
 → Python预注册handler调用固定Java接口，透传当前用户Bearer
 → Java重新验证Token、操作权限和适用数据范围
 → 结果返回Agent → 组织回答；拒绝时如实反馈
```

首期在线工具沿用用户Token透传。Python不验签，不查询角色；Token只用于固定Java目标的出站请求，不传模型/其他服务，不持久化。后续可替换Java短期票据，无需新增Python鉴权。

Java可返回当前用户获准的工具名称作为工具装配输入，模型只看到可用工具；这不是执行授权的替代。每次Java接口仍做最终检查，权限撤销应在下一次调用生效。Token过期返回明确错误，不改用超级管理员服务凭据重试。

模型只填写业务参数，不能填写身份、角色、任意URL、权限或confirmed=true。Python工具执行器验证注册项、参数schema和执行预算；Java用户身份来自真实认证上下文，绝不采信模型/前端传来的user_id、role。通用范围参数由Java受控生成，不能让模型扩大。

### 11.3 审计与失败

Java记录真实操作者、runId、toolCallId、工具名、必要资源ID、结果/拒绝码和耗时；包括被授权过滤器拒绝的请求。同步审计上下文只用于关联，不能用于身份认证。Python产生可展示的tool_started/tool_finished事件，不输出完整个人资料。

现有@PreAuthorize仅控制操作权限，不自动解决资源归属。首期工具访问已有护理目录/计划；未来接入老人资料前，在Java补齐机构/老人/家属数据范围并做越权测试。健康评估独立业务接口也需明确权限，不因本期工具只读而遗漏保护。

首期不注册创建预约、修改计划、支付或删除业务工具。未来写工具必须建立Java真实确认记录、绑定参数版本、数据库幂等约束及未知结果查询机制；单独JSON confirmed=true或“模型认为用户同意”均不构成授权。本期不为此提前建设审批平台。

## 12. 健康评估完整迁移

保留Java入口 `POST /nursing/healthAssessment/assessmentData` 和现有返回assessmentId的主要契约，`GET /nursing/healthAssessment/report/{id}`继续由Java提供。首期不把健康评估改成自由聊天或后台入库任务。

流程：

1. Java验证用户、表单和评估归属；短事务保存输入及分析run、input_revision，记录进行中状态。
2. 沿用Java OSS下载和PDF取文本，严格关闭InputStream、限制大小；将必要评估字段及报告文本发Python `POST /v1/assessments/health`，不发送无关身份证号等字段。
3. Python按已有SystemConstant语义分别做能力分析和体检分析，结构化输出并严格校验；不依赖正则从任意Markdown拼凑JSON作为唯一校验。
4. Java二次校验结果；短事务确认run仍有效、表单revision未变、评估未取消，再保存完整报告、应用原有规则并标记完成。
5. 任一阶段失败记录run失败和受控错误，不把evaluation_progress标为完成，不保存半份最终报告。旧run晚到结果不得覆盖更新/已取消的评估。

Python兼容结果字段：能力阶段`preLevel/finalLevel/reason`；体检阶段`healthScore/riskLevel/abnormalData/systemScore/summarize`。异常项字段和八大系统字段以现有VO为准，检查必需字段、枚举、有限数值和合理范围（健康分0~100），缺失时失败或明确缺失，不能补造事实。

现有Java规则原样保持：健康分≥60建议入住；≥90三级、≥80二级、≥70一级、≥60特级、否则不建议入住；报告初始入住状态为未入住。这是迁移兼容要求，不代表本文对规则作医学有效性背书。任何规则改变需另行业务确认。

现有evaluation_progress=0进行中、1完成、2取消保持兼容，失败详情放独立run；不要未经前端同步就在旧字段塞入新枚举。长模型调用不包在数据库事务内，因此输入保存与最终报告事务边界改变，需覆盖重试、取消和旧结果竞争测试。首期保持等待结果的交互，客户端/Java/Python设置一致的总截止时间；重复requestId不得重复创建报告。

健康报告原文及结果不自动写入公共知识库，不作为普通聊天历史供其他用户检索。

## 13. Python 内部接口契约

| 接口 | 说明 |
|---|---|
| `POST /v1/chat/runs` | 在线Agent运行，输入可信历史/工具能力与独立Context，输出SSE v1 |
| `POST /v1/assessments/health` | 两阶段结构化分析，普通JSON结果 |
| `POST /v1/knowledge/jobs` | 幂等提交构建，202返回jobId/requestId/state |
| `GET /v1/knowledge/jobs/{jobId}` | 查询执行阶段、错误、build manifest，不返回原始密钥 |
| `GET /v1/knowledge/jobs/by-request/{requestId}` | 提交响应丢失时核验 |
| `POST /v1/knowledge/jobs/{jobId}/cancel` | 请求取消，返回已请求状态；不等同清理完成 |
| `POST /v1/knowledge/cleanup-jobs` | 按知识/指定build范围幂等提交清理，返回任务ID |
| `GET /v1/knowledge/cleanup-jobs/{jobId}` | 清理状态 |
| `GET /health/live`、`GET /health/ready` | 存活及必要驱动就绪；不公开配置秘密 |

Java补固定的内部源读取、候选可见性复核等端点，用户Token与后台机器凭据分别约束；普通RAG复核以当前用户为准，后台取源只允许任务所绑定版本。输入输出定义OpenAPI/JSON Schema和契约测试。API字段命名在实施时统一映射，本文示意中的snake_case/camelCase不能直接混用。

## 14. 配置、资源与运行

- 交付Java开发配置样例与Python `.env.example`，仅占位值。关键项：AGENT_BASE_URL、JAVA_BASE_URL、MYSQL连接、MILVUS_URI/collection、CHAT/ASSESSMENT模型及地址、模型密钥引用、BGE模型路径/版本/device、并发、超时、租约、重试、文件限制、pipeline版本。
- 本地BGE模型通过Embedder适配器加载；CPU/GPU与精度可配置。API和Worker共享受控推理容量；采用单独Embedding进程或明确单模型进程策略，不能让多个Uvicorn worker无意重复加载耗尽内存。
- 提供独立API、Worker、Java派发/对账器的启动说明。MySQL/Milvus版本、连接可用性、1024维和collection schema启动校验。模型下载不发生在每个请求中。
- 批处理Embedding、阻塞PDF解析移到合适执行池；控制文件数、文本/页数、任务并发、模型步数、工具次数、返回大小和队列积压。关闭时停止领取新任务、处理租约并释放客户端。
- 提供环境准备、依赖安装、迁移、初始化Milvus、启动、停止、故障恢复及备份说明。部署参数尚未提供时明确待配置，不编造生产地址或声称真实模型已验证。
- compose作为可复现本地启动方案，不强制Redis消息队列、Kafka、Kubernetes或其他新增基础设施。

## 15. 实施拆分与交付顺序

| 阶段 | 工作 | 完成条件 |
|---|---|---|
| P0 契约与基线 | 保护现有修改，核对源码/Skill，锁定接口、迁移与错误规范 | 接口/schema及迁移草案可评审 |
| P1 Python骨架 | 分层、配置、驱动、HTTP、错误/日志、MySQL与Milvus连接、本地BGE、langchain-openai | 真实驱动可启动、错误配置拒绝，无演示假成功 |
| P2 聊天迁移 | Java客户端、鉴权、MySQL会话、SSE代理、Vue适配、取消/幂等 | 多轮、历史隔离和流错误通过 |
| P3 可靠入库 | Java两步交互、增量迁移、outbox、Python任务/Worker、版本发布、重试删除补偿 | 离页/重启继续，重复不重复索引 |
| P4 RAG溯源 | 有效版本过滤、Java授权复核、回答引用、页码预览、历史引用 | 能追到实际版本，禁用/删除不可新检索 |
| P5 受控工具 | 6个真实查询工具、Token上下文、Java授权、受限输出、拒绝审计 | 正常查询与越权/过期/注入测试通过 |
| P6 健康评估 | 两阶段结构化分析、短事务、run/revision、前端兼容 | 报告及旧规则兼容，失败不标完成 |
| P7 清理交付 | 移除Spring AI、依赖扫描、构建、端到端故障演示、操作文档 | 三个亮点和迁移全部有证据 |

数据库变更先部署兼容字段，再部署使用方；删除旧字段/配置放最后。Spring AI不能在健康评估或新KnowledgeBase仍有import时先删除。需要同时检查直接依赖、BOM、Java import、chatClient引用、application配置；HTTP流代理如仍需Reactor等依赖应显式保留，不能误删非AI所需能力。

完成实现后交付：Python项目及锁文件、Java/前端改动、SQL增量/回滚、OpenAPI/契约、配置样例、部署说明、验收证据与限制。是否提交/推送由后续明确指令决定；本次文档任务不提交。

## 16. 验收矩阵

下表均为待执行标准，不能当成已通过记录；单元替身测试和真实驱动集成验证分开报告。

| 流程 | 必须验证的结果 |
|---|---|
| 上传/新增 | 保存仅产生文件与未解析记录，不触发解析；非法/超限/非所属上传被Java拒绝 |
| 离页/回页 | 点击解析收到202后离页，后台继续，回页状态来自服务端 |
| 重启恢复 | Worker中途退出、租约过期后恢复，任务最终收敛，无重复活动切片 |
| 派发故障 | Python不可达、响应丢失、Java派发后崩溃，outbox及requestId确保可恢复 |
| 并发 | 双击、多Java/Worker实例、旧租约Worker晚到，只产生一个有效构建结果 |
| 发布 | Milvus写入失败保留旧活动版本；构建完成但Java未发布时新向量不可进入回答 |
| 重建 | 相同文件重新解析切换build；换模型/切片配置需新pipeline，旧清理不删新向量 |
| 删除 | 解析中删除、批量部分失败、OSS/Milvus失败可重试，旧任务不能复活文档 |
| RAG | 已知问题命中正确片段；无资料不编造；引用ID真实、页码/版本正确 |
| 可见性 | 禁用/删除后新检查拒绝，残留向量不进模型；预览重新授权 |
| 历史引用 | 新PDF发布后旧消息仍指向旧版本；旧源不可用明确提示 |
| SSE | UTF-8跨块、半帧、多帧、错误、心跳、断网、取消，不串消息、不误标完成 |
| 会话 | 跨用户ID猜测、并发run、重复requestId、运行中删除均受控 |
| 工具 | 正确选择查询工具；未知工具、非法参数、任意URL拒绝；Java403如实反馈 |
| 身份 | Token不进入模型/日志/任务；过期不提升权限；模型伪造管理员无效 |
| 审计 | 成功、拒绝、超时均能按run/toolCall定位真实操作者与结果 |
| 健康评估 | 两阶段VO兼容、0~100和枚举校验；失败/取消/旧revision不保存为完成；重试不重复报告 |
| 无Spring AI | 依赖树/import扫描无残留，Java构建及既有相关业务回归通过 |

建议演示三条可复现链路：

1. 上传保存 → 手动解析 → 离页 → 杀Worker → 重启恢复 → 页面成功 → 检查切片无重复。
2. 提问 → 回答引用 → 打开准确版本页码 → 禁用知识 → 再问不再使用该文档。
3. 有权限用户查询护理计划成功 → 无权限用户同问被拒绝 → 检查Java审计，Python没有独立鉴权系统。

至少准备一个可复制的文本PDF、可用生成模型、本地BGE-M3、MySQL、Milvus和两个权限不同的测试账号；使用合成/脱敏资料。统计真实任务次数、耗时、失败和恢复结果，不编造吞吐量、准确率或提升百分比。

## 17. 官方资料与实施前核对

- [LangChain 工具与运行上下文](https://docs.langchain.com/oss/python/langchain/tools)
- [LangChain 人工审核能力（本期仅作为未来扩展参考）](https://docs.langchain.com/oss/python/langchain/human-in-the-loop)
- [LangGraph 中断与幂等约束](https://docs.langchain.com/oss/python/langgraph/interrupts)
- [BGE-M3 官方模型说明](https://github.com/FlagOpen/FlagEmbedding/tree/master/research/BGE_M3)
- [Milvus 与 LangChain 集成](https://milvus.io/docs/basic_usage_langchain.md)

框架、SDK、CLI的具体语法/配置/版本兼容问题，遵循项目要求优先用Context7核对官方文档，缺结果再查官方网页。本文给出工程契约，不保证任意未来版本的SDK调用无变化。实现时将真实版本写入锁文件，并验证模型调用、流事件、Milvus过滤与删除、MySQL租约SQL和取消传播。

尚待部署时填写：模型文件路径与CPU/GPU环境、生成模型服务可用配置、MySQL/Milvus地址和版本、后台受限凭据、存储/引用保留周期及资源预算。这些参数可配置，不需要重新选择已确定的技术架构。
