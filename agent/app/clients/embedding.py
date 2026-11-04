"""调用文本 Embedding 模型，把文字转换成固定维度向量。"""

from langchain_openai import OpenAIEmbeddings
from openai import OpenAIError
from pydantic import SecretStr


class EmbeddingClientError(Exception):
    """调用 Embedding 模型时产生的可预期异常。"""


class EmbeddingClient:
    """统一封装文本向量化调用和返回维度校验。"""

    def __init__(
        self,
        *,
        model_name: str,
        base_url: str,
        api_key: SecretStr,
        dimensions: int,
        timeout_seconds: float,
    ) -> None:
        if dimensions <= 0:
            raise ValueError("Embedding 向量维度必须大于 0")

        self._dimensions = dimensions
        self._embeddings = OpenAIEmbeddings(
            model=model_name,
            api_key=api_key.get_secret_value(),
            base_url=base_url.rstrip("/"),
            dimensions=dimensions,
            timeout=timeout_seconds,
            max_retries=1,
            # 千问模型名称不在 tiktoken 内置列表中，由服务端处理文本长度。
            check_embedding_ctx_length=False,
            # 官方接口单次最多接收 20 条文本，后续文档入库可以批量调用。
            chunk_size=20,
        )

    async def embed_query(self, text: str) -> list[float]:
        """把一条患者查询转换为向量。"""

        normalized_text = text.strip()
        if not normalized_text:
            raise ValueError("待向量化的查询文本不能为空")

        try:
            vector = await self._embeddings.aembed_query(normalized_text)
        except OpenAIError as exc:
            raise EmbeddingClientError("文本向量化失败，请稍后重试") from exc

        self._validate_vector(vector)
        return vector

    async def embed_documents(self, texts: list[str]) -> list[list[float]]:
        """批量把知识文档片段转换为向量。"""

        if not texts:
            raise ValueError("待向量化的文档列表不能为空")

        normalized_texts = [text.strip() for text in texts]
        if any(not text for text in normalized_texts):
            raise ValueError("待向量化的文档片段不能为空")

        try:
            vectors = await self._embeddings.aembed_documents(normalized_texts)
        except OpenAIError as exc:
            raise EmbeddingClientError("知识文档向量化失败，请稍后重试") from exc

        if len(vectors) != len(normalized_texts):
            raise EmbeddingClientError("Embedding 返回的向量数量与文档数量不一致")

        for vector in vectors:
            self._validate_vector(vector)

        return vectors

    def _validate_vector(self, vector: list[float]) -> None:
        """确保模型返回维度能够写入当前 Milvus Collection。"""

        if len(vector) != self._dimensions:
            raise EmbeddingClientError(
                "Embedding 返回维度与 Milvus Collection 不一致"
            )

