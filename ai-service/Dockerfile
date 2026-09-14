# syntax=docker/dockerfile:1
FROM python:3.12-slim
WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install deps in a separate layer so the source change doesn't bust the
# pip cache on every build.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# A non-root user for the runtime. The `python:3.12-slim` image ships with
# `wheel` (uid 999) by default; we keep that and never hand out a shell.
RUN groupadd --system flowops && useradd --system --gid flowops flowops \
    && chown -R flowops:flowops /app
USER flowops

EXPOSE 8100
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8100/health', timeout=2).read()" || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8100", "--workers", "1"]
