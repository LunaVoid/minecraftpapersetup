FROM python:3.12-slim
WORKDIR /app
RUN pip install --no-cache-dir "discord.py>=2,<3"
COPY bot.py test_bot.py ./
RUN python test_bot.py
# Match the itzg/minecraft-server uid so files in the shared volume are owned the same.
USER 1000:1000
EXPOSE 8080
CMD ["python", "bot.py"]
