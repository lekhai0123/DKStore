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
# Trong luc con down, nhac lai moi ngan nay phut thay vi chi gui 1 lan duy nhat luc
# vua phat hien - phong truong hop lan gui dau bi loi (SMTP hicc up...) thi van co
# co hoi gui lai o lan nhac tiep theo thay vi im lang toi khi app tu hoi phuc.
REMINDER_INTERVAL_SECONDS = int(os.environ.get("REMINDER_INTERVAL_SECONDS", str(30 * 60)))

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


def send_mail(subject: str, body: str) -> bool:
    if not (MAIL_USERNAME and MAIL_PASSWORD and ALERT_EMAIL_TO):
        print("[healthmon] mail not configured (MAIL_USERNAME/MAIL_PASSWORD/ALERT_EMAIL_TO), skip sending", flush=True)
        return False
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
        return True
    except Exception as e:
        print(f"[healthmon] failed to send email: {e}", flush=True)
        return False


def main():
    print(f"[healthmon] watching {CHECK_URL} every {CHECK_INTERVAL_SECONDS}s", flush=True)
    last_status = None  # None = chua biet, tranh gui mail "phuc hoi" sai luc container moi start
    down_since = None
    last_alert_at = None  # thoi diem (monotonic) gui mail thanh cong gan nhat trong dot down nay

    while True:
        is_up = check_up()
        now_wall = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        now_mono = time.monotonic()

        if is_up:
            if last_status is False:
                print("[healthmon] app RECOVERED", flush=True)
                since_msg = f" (bi down tu {down_since})" if down_since else ""
                send_mail(
                    "[DKStore] App da hoat dong tro lai",
                    f"App tai {CHECK_URL} da phan hoi binh thuong tro lai luc {now_wall}{since_msg}.",
                )
            else:
                print("[healthmon] status: up", flush=True)
            down_since = None
            last_alert_at = None
        else:
            should_alert = False
            if last_status is True or last_status is None:
                # lan dau phat hien down (hoac container healthmon vua khoi dong khi app da down san)
                print("[healthmon] app went DOWN", flush=True)
                down_since = now_wall
                should_alert = True
            else:
                should_alert = last_alert_at is None or (now_mono - last_alert_at) >= REMINDER_INTERVAL_SECONDS
                print(
                    f"[healthmon] still down (since {down_since}), "
                    f"{'sending reminder' if should_alert else 'skip, too soon'}",
                    flush=True,
                )

            if should_alert:
                since_msg = f" tu {down_since}" if down_since else ""
                sent = send_mail(
                    "[DKStore] CANH BAO: app khong phan hoi",
                    f"App khong phan hoi tai {CHECK_URL}{since_msg} (kiem tra luc {now_wall}).\n"
                    f"Server van dang song (email nay duoc gui tu chinh server), nhieu kha nang app "
                    f"bi crash/treo. Docker se tu dong thu restart container. Se nhac lai moi "
                    f"{REMINDER_INTERVAL_SECONDS // 60} phut neu van con down.",
                )
                if sent:
                    last_alert_at = now_mono

        last_status = is_up
        time.sleep(CHECK_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
