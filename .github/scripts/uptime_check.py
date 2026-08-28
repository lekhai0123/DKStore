import json
import os
import smtplib
import ssl
import urllib.error
import urllib.request
from datetime import datetime, timezone
from email.mime.text import MIMEText

CHECK_URL = os.environ.get("CHECK_URL", "")
STATE_FILE = os.environ.get("STATE_FILE", "status.json")
MAIL_HOST = os.environ.get("MAIL_HOST", "smtp.gmail.com")
MAIL_PORT = int(os.environ.get("MAIL_PORT", "587"))
MAIL_USERNAME = os.environ.get("MAIL_USERNAME", "")
MAIL_PASSWORD = os.environ.get("MAIL_PASSWORD", "")
ALERT_EMAIL_TO = os.environ.get("ALERT_EMAIL_TO", "")


def check_up() -> bool:
    try:
        req = urllib.request.Request(CHECK_URL, headers={"User-Agent": "dkstore-uptime-check"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status == 200
    except Exception as e:
        print(f"check failed: {e}")
        return False


def load_previous_status() -> str:
    # File nay hay bi ghi de bang cac editor/shell tren Windows (PowerShell mac dinh
    # UTF-16, Notepad hay them BOM...) nen bat rong moi loi doc/parse/encoding, coi
    # nhu "chua ro trang thai" thay vi lam crash ca workflow. save_status() ben duoi
    # luon ghi lai dung UTF-8 khong BOM nen file se tu "lanh" o lan chay ke tiep.
    try:
        with open(STATE_FILE, "rb") as f:
            raw = f.read()
        if raw[:2] in (b"\xff\xfe", b"\xfe\xff"):
            text = raw.decode("utf-16")
        else:
            text = raw.decode("utf-8-sig")
        return json.loads(text).get("status", "up")
    except Exception as e:
        print(f"could not read previous status ({e}), assuming 'up'")
        return "up"


def save_status(status: str) -> None:
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump({"status": status, "checked_at": datetime.now(timezone.utc).isoformat()}, f)


def send_mail(subject: str, body: str) -> None:
    if not (MAIL_USERNAME and MAIL_PASSWORD and ALERT_EMAIL_TO):
        print("mail not configured (secrets missing), skip sending")
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
        print(f"alert email sent: {subject}")
    except Exception as e:
        # Khong de loi gui mail lam fail ca workflow (state van phai duoc luu binh thuong)
        print(f"failed to send email: {e}")


def main():
    if not CHECK_URL:
        print("CHECK_URL not set, aborting")
        return

    is_up = check_up()
    previous = load_previous_status()
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    if is_up and previous == "down":
        print("recovered")
        send_mail(
            "[DKStore] Server da hoat dong tro lai",
            f"Domain {CHECK_URL} da phan hoi binh thuong tro lai luc {now} (kiem tra tu GitHub Actions).",
        )
    elif not is_up and previous == "up":
        print("went down")
        send_mail(
            "[DKStore] CANH BAO: server khong phan hoi",
            f"Domain {CHECK_URL} khong phan hoi luc {now} (kiem tra tu GitHub Actions, "
            f"doc lap voi server). Kiem tra server/mang/dien ngay.",
        )
    else:
        print(f"status unchanged: {'up' if is_up else 'down'}")

    save_status("up" if is_up else "down")


if __name__ == "__main__":
    main()
