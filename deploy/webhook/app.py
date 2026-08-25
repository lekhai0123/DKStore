import hashlib
import hmac
import json
import os
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

SECRET = os.environ.get("WEBHOOK_SECRET", "")
DEPLOY_REF = os.environ.get("DEPLOY_REF", "refs/heads/master")
REPO_DIR = "/repo"
HOST_REPO_PATH = os.environ.get("HOST_REPO_PATH", "")
COMPOSE_FILE = f"{HOST_REPO_PATH}/docker-compose.yml"


def verify_signature(body: bytes, signature_header: str) -> bool:
    if not SECRET or not signature_header or not signature_header.startswith("sha256="):
        return False
    expected = hmac.new(SECRET.encode(), body, hashlib.sha256).hexdigest()
    received = signature_header.split("=", 1)[1]
    return hmac.compare_digest(expected, received)


def deploy():
    print("[deploy] git pull...", flush=True)
    subprocess.run(["git", "-C", REPO_DIR, "pull"], check=False)
    print("[deploy] docker compose up -d --build app...", flush=True)
    subprocess.run(
        [
            "docker", "compose",
            "--project-directory", HOST_REPO_PATH,
            "-f", COMPOSE_FILE,
            "up", "-d", "--build", "app",
        ],
        check=False,
    )
    print("[deploy] done", flush=True)


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/webhook":
            self.send_response(404)
            self.end_headers()
            return

        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        signature = self.headers.get("X-Hub-Signature-256", "")

        if not verify_signature(body, signature):
            print("[webhook] invalid signature, ignored", flush=True)
            self.send_response(401)
            self.end_headers()
            self.wfile.write(b"invalid signature")
            return

        try:
            payload = json.loads(body or b"{}")
        except json.JSONDecodeError:
            payload = {}

        if payload.get("ref") != DEPLOY_REF:
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ignored: not target branch")
            return

        self.send_response(202)
        self.end_headers()
        self.wfile.write(b"deploy triggered")
        threading.Thread(target=deploy, daemon=True).start()

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"webhook ok")

    def log_message(self, format, *args):
        print("[http] " + (format % args), flush=True)


if __name__ == "__main__":
    if not SECRET:
        print("WARNING: WEBHOOK_SECRET is empty, all requests will be rejected", flush=True)
    if not HOST_REPO_PATH:
        print("WARNING: HOST_REPO_PATH is empty, deploy will fail", flush=True)
    server = ThreadingHTTPServer(("0.0.0.0", 9000), Handler)
    print("Webhook listener started on :9000", flush=True)
    server.serve_forever()
