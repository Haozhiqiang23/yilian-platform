"""读取并切分本地 Markdown 医学知识文档。"""

import re
from hashlib import sha256
from pathlib import Path

from app.schemas.knowledge import KnowledgeChunk

# 文档文件名与业务分类集中维护，避免在循环中堆放判断分支。
DOCUMENT_CATEGORIES = {
    "预约挂号说明.md": "appointment",
    "取消预约与退款说明.md": "refund",
    "就诊与签到流程.md": "visit",
    "检查注意事项.md": "examination",
    "常见问题.md": "faq",
}


class KnowledgeDocumentError(Exception):
    """读取或切分知识文档时产生的可预期异常。"""


class KnowledgeDocumentService:
    """把 knowledge 目录中的 Markdown 转换为统一知识分块。"""

    def __init__(
        self,
        *,
        knowledge_dir: Path,
        chunk_size: int = 800,
        chunk_overlap: int = 100,
    ) -> None:
        if chunk_size <= 0:
            raise ValueError("chunk_size 必须大于 0")

        if chunk_overlap < 0 or chunk_overlap >= chunk_size:
            raise ValueError("chunk_overlap 必须大于等于 0 且小于 chunk_size")

        self._knowledge_dir = knowledge_dir
        self._chunk_size = chunk_size
        self._chunk_overlap = chunk_overlap

    def load_chunks(self) -> list[KnowledgeChunk]:
        """读取目录中所有已登记文档，并返回切分后的知识片段。"""

        if not self._knowledge_dir.is_dir():
            raise KnowledgeDocumentError("知识文档目录不存在")

        chunks: list[KnowledgeChunk] = []

        # sorted 保证每次读取顺序一致，便于测试和重复入库。
        for path in sorted(self._knowledge_dir.glob("*.md")):
            category = DOCUMENT_CATEGORIES.get(path.name)
            if category is None:
                continue

            chunks.extend(self._load_document(path, category=category))

        if not chunks:
            raise KnowledgeDocumentError("知识文档目录中没有可读取的文档")

        return chunks

    def _load_document(
        self,
        path: Path,
        *,
        category: str,
    ) -> list[KnowledgeChunk]:
        """读取单个 Markdown，并按照二级标题组织分块。"""

        try:
            text = path.read_text(encoding="utf-8")
        except OSError as exc:
            raise KnowledgeDocumentError(f"读取知识文档失败：{path.name}") from exc

        source = self._extract_sources(text)
        sections = self._split_markdown_sections(text)
        chunks: list[KnowledgeChunk] = []

        for section_title, section_content in sections:
            # 标题跟随正文进入分块，检索结果脱离原文后仍能理解上下文。
            section_text = f"{section_title}\n\n{section_content}".strip()

            for content in self._split_long_text(section_text):
                chunk_index = len(chunks)
                chunk_id = self._create_chunk_id(
                    document_name=path.name,
                    chunk_index=chunk_index,
                    content=content,
                )
                chunks.append(
                    KnowledgeChunk(
                        id=chunk_id,
                        document_name=path.name,
                        category=category,
                        content=content,
                        source=source,
                        chunk_index=chunk_index,
                    )
                )

        return chunks

    @staticmethod
    def _extract_sources(text: str) -> str:
        """提取文档中的公开网页地址，作为回答来源元数据。"""

        urls = re.findall(r"https?://[^\s)]+", text)
        return " | ".join(dict.fromkeys(urls))[:2048]

    @staticmethod
    def _split_markdown_sections(text: str) -> list[tuple[str, str]]:
        """按 Markdown 二级标题切分，并排除只用于元数据的参考资料段。"""

        sections: list[tuple[str, str]] = []
        current_title = "文档说明"
        current_lines: list[str] = []

        for line in text.splitlines():
            if line.startswith("# "):
                current_title = line.removeprefix("# ").strip()
                continue

            if line.startswith("## "):
                if current_lines:
                    content = "\n".join(current_lines).strip()
                    if content:
                        sections.append((current_title, content))

                current_title = line.removeprefix("## ").strip()
                current_lines = []

                if current_title == "参考资料":
                    break
                continue

            current_lines.append(line)

        if current_lines:
            content = "\n".join(current_lines).strip()
            if content:
                sections.append((current_title, content))

        return sections

    def _split_long_text(self, text: str) -> list[str]:
        """把过长章节继续按字符长度切分，并保留少量上下文重叠。"""

        if len(text) <= self._chunk_size:
            return [text]

        chunks: list[str] = []
        start = 0

        while start < len(text):
            end = min(start + self._chunk_size, len(text))

            # 优先在换行处截断，避免从一句话中间切开。
            if end < len(text):
                boundary = text.rfind("\n", start + self._chunk_size // 2, end)
                if boundary > start:
                    end = boundary

            content = text[start:end].strip()
            if content:
                chunks.append(content)

            if end >= len(text):
                break

            start = end - self._chunk_overlap

        return chunks

    @staticmethod
    def _create_chunk_id(
        *,
        document_name: str,
        chunk_index: int,
        content: str,
    ) -> str:
        """根据文档、顺序和内容生成稳定的 64 位分块编号。"""

        raw_value = f"{document_name}:{chunk_index}:{content}"
        return sha256(raw_value.encode("utf-8")).hexdigest()

