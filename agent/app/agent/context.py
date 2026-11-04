from dataclasses import dataclass


#@dataclass 让 Python 自动生成初始化方法
@dataclass(frozen=True)  #frozen=True  防止被修改
class AgentContext:
    """只供程序和工具使用，不发送给大模型。"""

    access_token: str


