# All disc tools live here, installed at build time (ADR 0006); the Minecraft container runs none of them.
FROM denoland/deno:bin-2.9.8 AS deno

FROM python:3.12-slim
COPY --from=deno /deno /usr/local/bin/deno
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
# yt-dlp[default] includes yt-dlp-ejs (YouTube's JS challenges, run with Deno). The bot also
# `pip install -U --user`s it at startup, since YouTube breaks old versions.
RUN pip install --no-cache-dir "discord.py>=2,<3" "yt-dlp[default]"
# Same uid as itzg/minecraft-server so files in the shared folder are owned the same.
RUN useradd -u 1000 -m app
WORKDIR /app
COPY bot.py discs.py test_bot.py pack-base.zip ./
USER app
RUN python test_bot.py
EXPOSE 8080
CMD ["python", "bot.py"]
