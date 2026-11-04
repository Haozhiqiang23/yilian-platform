# 项目要求 Python >=3.12,<3.13
FROM python:3.12-slim

# Python 容器运行配置
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

# 创建非 root 用户
RUN addgroup --system appgroup \
    && adduser --system --ingroup appgroup appuser

# 只复制安装项目所需的文件，不复制 .env、.venv 和测试缓存
COPY pyproject.toml README.md ./
COPY app ./app
COPY knowledge ./knowledge

# pyproject.toml 中已经声明全部生产依赖
RUN pip install -i https://pypi.tuna.tsinghua.edu.cn/simple .

# 将程序目录交给非 root 用户
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

# 容器必须监听 0.0.0.0，外部服务才能访问
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
