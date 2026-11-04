# MADP Assistant Learning

医联智诊患者端智能助手学习项目。项目使用 FastAPI、LangChain、LangGraph 和千问，将自然语言请求转换为工具调用，再携带患者 JWT 调用现有 Java 医疗后端。

## 已实现能力

- 医生、号源、预约、支付、排队、诊疗记录、处方、检查报告和消息查询。
- 创建预约、取消预约和签到，执行前必须由患者确认。
- Redis 持久化多轮对话、待确认状态和七天 TTL。
- 患者与会话双重隔离、Redis 会话并发锁。
- 旧工具结果清理和长对话自动摘要。
- SSE 流式输出及 Vue 逐字平滑显示。
- request_id、HTTP 总耗时和 Java 接口耗时日志。
- Markdown 医学知识文档、Embedding 和 Milvus RAG 检索。
- 业务实时数据与医院通用规则组合回答，并展示参考文档。

## 环境要求

- Python 3.12
- Redis 8
- Milvus
- 可访问的 Java 医疗后端
- 千问 OpenAI 兼容接口

## 初始化

```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
Copy-Item .env.example .env
```

复制后编辑 `.env`，至少填写：

```text
JAVA_BACKEND_URL
REDIS_URL
LLM_MODEL
LLM_MODEL2
LLM_MODEL3
LLM_BASE_URL
LLM_API_KEY
MILVUS_URI
MILVUS_COLLECTION_NAME
EMBEDDING_DIMENSION
KNOWLEDGE_MIN_RELEVANCE_SCORE
```

不要提交真实 `.env`，项目已经通过 `.gitignore` 忽略该文件。

## 启动 Redis

示例 Docker 命令：

```powershell
docker run -d --name madp-redis -p 127.0.0.1:6379:6379 -v madp-redis-data:/data redis:8-alpine redis-server --appendonly yes
docker exec madp-redis redis-cli ping
```

看到 `PONG` 表示 Redis 可以使用。以后只需执行：

```powershell
docker start madp-redis
```

## 启动服务

先启动 Redis 和 Java 后端，再启动 Python Agent：

```powershell
python -m uvicorn app.main:app --reload
```

首次使用 RAG 或修改 `knowledge` 中的文档后执行：

```powershell
python -m app.scripts.ingest_knowledge
```

健康检查：

```text
GET http://127.0.0.1:8000/api/health
```

普通聊天接口：

```text
POST /api/chat
```

SSE 流式聊天接口：

```text
POST /api/chat/stream
```

## 质量检查

```powershell
ruff check app tests
pytest -q
```

当前验证基线：

```text
Ruff 全量检查通过
pytest：38 passed
前端生产构建通过
网页查询、人工确认、Redis 恢复和逐字流式输出通过
```

## 推荐阅读顺序

1. `app/main.py`
2. `app/api/routes/chat.py`
3. `app/services/chat_service.py`
4. `app/agent/factory.py`
5. `app/tools/`
6. `app/clients/java_backend.py`
7. `app/core/checkpointer.py`
8. `app/core/chat_lock.py`
9. `app/core/observability.py`
10. `09-RAG医学知识库.md`
11. `app/services/knowledge_documents.py`
12. `app/services/knowledge_ingestion.py`
13. `app/services/knowledge_search.py`

完整学习路径请从 `00-项目学习总路线.md` 开始。

项目收尾、回归和复盘进度统一记录在 `10-项目收尾进度记录.md`。
