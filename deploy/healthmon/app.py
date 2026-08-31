import os
import smtplib
import ssl
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from email.mime.multipart import MIMEMultipart
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

SOURCE_LABEL = "Giam sat noi bo tren server (healthmon)"


def check_up() -> bool:
    try:
        req = urllib.request.Request(CHECK_URL, headers={"User-Agent": "dkstore-healthmon"})
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT_SECONDS) as resp:
            return resp.status == 200
    except Exception as e:
        print(f"[healthmon] check failed: {e}", flush=True)
        return False


def render_html(is_down: bool, title: str, rows: list, note: str) -> str:
    color = "#dc2626" if is_down else "#16a34a"
    icon = "\U0001F534" if is_down else "\U0001F7E2"
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
        print("[healthmon] mail not configured (MAIL_USERNAME/MAIL_PASSWORD/ALERT_EMAIL_TO), skip sending", flush=True)
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
        print(f"[healthmon] alert email sent: {title}", flush=True)
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
                send_alert(
                    False,
                    "DKStore - App da hoat dong tro lai",
                    [
                        ("Trang thai", "Da phan hoi binh thuong"),
                        ("Endpoint", CHECK_URL),
                        ("Down tu", down_since or "-"),
                        ("Phuc hoi luc", now_wall),
                        ("Nguon kiem tra", SOURCE_LABEL),
                    ],
                    "Khong can lam gi them.",
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
                sent = send_alert(
                    True,
                    "DKStore - CANH BAO: app khong phan hoi",
                    [
                        ("Trang thai", "Khong phan hoi (server van song)"),
                        ("Endpoint", CHECK_URL),
                        ("Down tu", down_since or "-"),
                        ("Kiem tra luc", now_wall),
                        ("Nguon kiem tra", SOURCE_LABEL),
                    ],
                    f"Docker se tu dong thu restart container app. Se nhac lai moi "
                    f"{REMINDER_INTERVAL_SECONDS // 60} phut neu van con down.",
                )
                if sent:
                    last_alert_at = now_mono

        last_status = is_up
        time.sleep(CHECK_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
