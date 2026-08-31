import json
import os
import smtplib
import ssl
import urllib.error
import urllib.request
from datetime import datetime, timezone
from email.mime.multipart import MIMEMultipart
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

SOURCE_LABEL = "GitHub Actions (kiem tra tu ben ngoai, doc lap voi server)"


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


def render_html(is_down: bool, title: str, rows: list, note: str) -> str:
    color = "#dc2626" if is_down else "#16a34a"
    icon = "\U0001F534" if is_down else "\U0001F7E2"  # red/green circle
    rows_html = "".join(
        f'<tr>'
        f'<td style="padding:10px 12px;color:#6b7280;font-size:13px;white-space:nowrap;'
        f'border-bottom:1px solid #f3f4f6;">{key}</td>'
        f'<td style="padding:10px 12px;color:#111827;font-size:14px;font-weight:600;'
        f'border-bottom:1px solid #f3f4f6;">{value}</td>'
        f'</tr>'
        for key, value in rows
    )
    return f"""\
<!DOCTYPE html>
<html>
<body style="margin:0;padding:24px;background:#f3f4f6;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
    <tr>
      <td style="background:{color};padding:20px 24px;">
        <span style="font-size:18px;font-weight:700;color:#ffffff;">{icon} {title}</span>
      </td>
    </tr>
    <tr>
      <td style="padding:8px 12px 4px 12px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
          {rows_html}
        </table>
      </td>
    </tr>
    <tr>
      <td style="padding:16px 24px;border-top:1px solid #e5e7eb;">
        <span style="font-size:12px;color:#9ca3af;line-height:1.5;">{note}</span>
      </td>
    </tr>
  </table>
</body>
</html>"""


def render_text(title: str, rows: list, note: str) -> str:
    lines = [title, ""]
    lines += [f"{key}: {value}" for key, value in rows]
    lines += ["", note]
    return "\n".join(lines)


def send_alert(is_down: bool, title: str, rows: list, note: str) -> bool:
    if not (MAIL_USERNAME and MAIL_PASSWORD and ALERT_EMAIL_TO):
        print("mail not configured (secrets missing), skip sending")
        return False
    msg = MIMEMultipart("alternative")
    msg["Subject"] = title
    msg["From"] = MAIL_USERNAME
    msg["To"] = ALERT_EMAIL_TO
    msg.attach(MIMEText(render_text(title, rows, note), "plain", "utf-8"))
    msg.attach(MIMEText(render_html(is_down, title, rows, note), "html", "utf-8"))
    try:
        context = ssl.create_default_context()
        with smtplib.SMTP(MAIL_HOST, MAIL_PORT, timeout=20) as server:
            server.starttls(context=context)
            server.login(MAIL_USERNAME, MAIL_PASSWORD)
            server.sendmail(MAIL_USERNAME, [ALERT_EMAIL_TO], msg.as_string())
        print(f"alert email sent: {title}")
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
            send_alert(
                False,
                "DKStore - Server da hoat dong tro lai",
                [
                    ("Trang thai", "Da phan hoi binh thuong"),
                    ("Domain", CHECK_URL),
                    ("Down tu", down_since or "-"),
                    ("Phuc hoi luc", now_str),
                    ("Nguon kiem tra", SOURCE_LABEL),
                ],
                "Khong can lam gi them.",
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
            sent = send_alert(
                True,
                "DKStore - CANH BAO: server khong phan hoi",
                [
                    ("Trang thai", "Khong phan hoi"),
                    ("Domain", CHECK_URL),
                    ("Down tu", down_since or "-"),
                    ("Kiem tra luc", now_str),
                    ("Nguon kiem tra", SOURCE_LABEL),
                ],
                f"Se nhac lai moi {REMINDER_INTERVAL_MINUTES} phut neu van con down.",
            )
            if sent:
                last_alert_at = now_dt.isoformat()

    save_state("up" if is_up else "down", down_since, last_alert_at)


if __name__ == "__main__":
    main()
