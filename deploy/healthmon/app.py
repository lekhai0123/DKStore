import os
import smtplib
import ssl
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from email.mime.text import MIMEText

CHECK_URL = os.environ.get("CHECK_URL", "http://app:10000/ping")
CHECK_INTERVAL_SECONDS = int(os.environ.get("CHECK_INTERVAL_SECONDS", "60"))
REQUEST_TIMEOUT_SECONDS = 10

MAIL_HOST = os.environ.get("MAIL_HOST", "smtp.gmail.com")
MAIL_PORT = int(os.environ.get("MAIL_PORT", "587"))
MAIL_USERNAME = os.environ.get("MAIL_USERNAME", "")
MAIL_PASSWORD = os.environ.get("MAIL_PASSWORD", "")
ALERT_EMAIL_TO = os.environ.get("ALERT_EMAIL_TO", "")


def check_up() -> bool:
    try:
        req = urllib.request.Request(CHECK_URL, headers={"User-Agent": "dkstore-healthmon"})
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT_SECONDS) as resp:
            return resp.status == 200
    except Exception as e:
        print(f"[healthmon] check failed: {e}", flush=True)
        return False


def send_mail(subject: str, body: str) -> None:
    if not (MAIL_USERNAME and MAIL_PASSWORD and ALERT_EMAIL_TO):
        print("[healthmon] mail not configured (MAIL_USERNAME/MAIL_PASSWORD/ALERT_EMAIL_TO), skip sending", flush=True)
        return
    msg = MIMEText(body, "plain", "utf-8")
    msg["Subject"] = subject
    msg["From"] = MAIL_USERNAME
    msg["To"] = ALERT_EMAIL_TO
    try:
        context = ssl.create_default_context()
        with smtplib.SMTP(MAIL_HOST, MAIL_PORT, timeout=20) as server:
            server.starttls(context=context)
            server.login(MAIL_USERNAME, MAIL_PASSWORD)
            server.sendmail(MAIL_USERNAME, [ALERT_EMAIL_TO], msg.as_string())
        print(f"[healthmon] alert email sent: {subject}", flush=True)
    except Exception as e:
        print(f"[healthmon] failed to send email: {e}", flush=True)


def main():
    print(f"[healthmon] watching {CHECK_URL} every {CHECK_INTERVAL_SECONDS}s", flush=True)
    last_status = None  # None = chua biet, tranh gui mail "phuc hoi" sai luc container moi start
    while True:
        is_up = check_up()
        now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

        if last_status is not None and last_status and not is_up:
            print("[healthmon] app went DOWN", flush=True)
            send_mail(
                "[DKStore] CANH BAO: app khong phan hoi",
                f"App khong phan hoi tai {CHECK_URL} luc {now}.\n"
                f"Server van dang song (email nay duoc gui tu chinh server), "
                f"nhieu kha nang app bi crash/treo. Docker se tu dong thu restart container.",
            )
        elif last_status is not None and not last_status and is_up:
            print("[healthmon] app RECOVERED", flush=True)
            send_mail(
                "[DKStore] App da hoat dong tro lai",
                f"App tai {CHECK_URL} da phan hoi binh thuong tro lai luc {now}.",
            )
        else:
            print(f"[healthmon] status: {'up' if is_up else 'down'}", flush=True)

        last_status = is_up
        time.sleep(CHECK_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
