from langgraph.checkpoint.redis.aio import AsyncRedisSaver

# 负责共享这个资源

# 保存当前应用正在使用的 Redis Checkpointer。
# FastAPI 启动时赋值，Agent 创建时读取。
_checkpointer: AsyncRedisSaver | None = None


def set_checkpointer(checkpointer: AsyncRedisSaver) -> None:
    """保存已经初始化完成的 Redis Checkpointer。"""

    # 表示我们要修改模块最外层的 _checkpointer，而不是创建一个同名的局部变量。
    global _checkpointer
    _checkpointer = checkpointer


def get_checkpointer() -> AsyncRedisSaver:
    """获取当前应用的 Redis Checkpointer。"""
    if _checkpointer is None:
        raise RuntimeError("Redis Checkpointer 尚未初始化")

    return _checkpointer

def clear_checkpointer() -> None:
    """清除已经关闭的 Redis Checkpointer。"""
    global _checkpointer
    _checkpointer = None
