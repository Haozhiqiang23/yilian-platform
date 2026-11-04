"""RAG 知识文档在读取和切分阶段使用的数据结构。"""

from pydantic import BaseModel, Field


class KnowledgeChunk(BaseModel):
    """从一份 Markdown 文档中切分出来的一个知识片段。"""

    id: str = Field(min_length=1, max_length=64)
    document_name: str = Field(min_length=1, max_length=256)
    category: str = Field(min_length=1, max_length=64)
    content: str = Field(min_length=1, max_length=65535)
    source: str = Field(default="", max_length=2048)
    chunk_index: int = Field(ge=0)


class KnowledgeSearchResult(BaseModel):
    """Milvus 相似度检索返回给上层的一条知识结果。"""

    id: str
    document_name: str
    category: str
    content: str
    source: str = ""
    chunk_index: int
    score: float
