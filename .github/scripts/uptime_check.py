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
# Trong luc con down, nhac lai moi ngan nay phut (khong phai gui lien tuc moi 5 phut)
# - phong truong hop lan gui mail dau tien (luc vua chuyen sang down) bi loi/that lac,
# van co co hoi gui lai o lan nhac tiep theo thay vi im lang mai mai.
REMINDER_INTERVAL_MINUTES = int(os.environ.get("REMINDER_INTERVAL_MINUTES", "30"))


def check_up() -> bool:
    try:
        req = urllib.request.Request(CHECK_URL, headers={"User-Agent": "dkstore-uptime-check"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status == 200
    except Exception as e:
        print(f"check failed: {e}")
        return False


def load_state() -> dict:
    # File nay hay bi ghi de bang cac editor/shell tren Windows (PowerShell mac dinh
    # UTF-16, Notepad hay them BOM...) nen bat rong moi loi doc/parse/encoding, coi
    # nhu "chua ro trang thai" thay vi lam crash ca workflow. save_state() ben duoi
    # luon ghi lai dung UTF-8 khong BOM nen file se tu "lanh" o lan chay ke tiep.
    try:
        with open(STATE_FILE, "rb") as f:
            raw = f.read()
        if raw[:2] in (b"\xff\xfe", b"\xfe\xff"):
            text = raw.decode("utf-16")
        else:
            text = raw.decode("utf-8-sig")
        data = json.loads(text)
        return {
            "status": data.get("status", "up"),
            "down_since": data.get("down_since"),
            "last_alert_at": data.get("last_alert_at"),
        }
    except Exception as e:
        print(f"could not read previous state ({e}), assuming 'up'")
        return {"status": "up", "down_since": None, "last_alert_at": None}


def save_state(status: str, down_since, last_alert_at) -> None:
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "status": status,
                "checked_at": datetime.now(timezone.utc).isoformat(),
                "down_since": down_since,
                "last_alert_at": last_alert_at,
            },
            f,
        )


def send_mail(subject: str, body: str) -> bool:
    if not (MAIL_USERNAME and MAIL_PASSWORD and ALERT_EMAIL_TO):
        print("mail not configured (secrets missing), skip sending")
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
        print(f"alert email sent: {subject}")
        return True
    except Exception as e:
        # Khong de loi gui mail lam fail ca workflow (state van phai duoc luu binh thuong)
        print(f"failed to send email: {e}")
        return False


def main():
    if not CHECK_URL:
        print("CHECK_URL not set, aborting")
        return

    is_up = check_up()
    state = load_state()
    previous_status = state["status"]
    now_dt = datetime.now(timezone.utc)
    now_str = now_dt.strftime("%Y-%m-%d %H:%M:%S UTC")

    down_since = state["down_since"]
    last_alert_at = state["last_alert_at"]

    if is_up:
        if previous_status == "down":
            print("recovered")
            since_msg = f" (bi down tu {down_since})" if down_since else ""
            send_mail(
                "[DKStore] Server da hoat dong tro lai",
                f"Domain {CHECK_URL} da phan hoi binh thuong tro lai luc {now_str}{since_msg} "
                f"(kiem tra tu GitHub Actions).",
            )
        else:
            print("status unchanged: up")
        down_since = None
        last_alert_at = None
    else:
        should_alert = False
        if previous_status == "up":
            print("went down")
            down_since = now_str
            should_alert = True
        else:
            # Van dang down tu truoc - chi nhac lai neu da qua REMINDER_INTERVAL_MINUTES
            # ke tu lan gui mail gan nhat (hoac chua tung gui duoc lan nao).
            if last_alert_at is None:
                should_alert = True
            else:
                try:
                    elapsed_min = (now_dt - datetime.fromisoformat(last_alert_at)).total_seconds() / 60
                except ValueError:
                    elapsed_min = REMINDER_INTERVAL_MINUTES  # gia tri cu bi hong, cu nhac lai cho chac
                should_alert = elapsed_min >= REMINDER_INTERVAL_MINUTES
            print(f"still down (since {down_since}), {'sending reminder' if should_alert else 'skip, too soon'}")

        if should_alert:
            since_msg = f" tu {down_since}" if down_since else ""
            sent = send_mail(
                "[DKStore] CANH BAO: server khong phan hoi",
                f"Domain {CHECK_URL} khong phan hoi{since_msg} (kiem tra luc {now_str} tu GitHub Actions, "
                f"doc lap voi server). Kiem tra server/mang/dien ngay. Se nhac lai moi "
                f"{REMINDER_INTERVAL_MINUTES} phut neu van con down.",
            )
            if sent:
                last_alert_at = now_dt.isoformat()

    save_state("up" if is_up else "down", down_since, last_alert_at)


if __name__ == "__main__":
    main()
