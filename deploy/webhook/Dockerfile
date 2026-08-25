FROM python:3.12-alpine
RUN apk add --no-cache git docker-cli docker-cli-compose
RUN git config --global --add safe.directory /repo
WORKDIR /app
COPY app.py .
EXPOSE 9000
CMD ["python", "app.py"]
