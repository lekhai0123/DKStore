# Deploy DKStore lên server riêng (Docker Compose + Cloudflare Tunnel)

Hướng dẫn này dành cho server chạy qua SSH, không có IDE, không có quyền mở port
trên router. Toàn bộ traffic vào app đi qua Cloudflare Tunnel (kết nối outbound từ
server ra Cloudflare) nên **không cần mở port nào** trên router/firewall.

Kiến trúc: 3 container chạy cùng `docker compose`:
- `db`: PostgreSQL 16, dữ liệu lưu trong volume `pgdata` (không mất khi restart).
- `app`: build từ `Dockerfile` có sẵn trong repo (Spring Boot, port nội bộ 10000).
- `cloudflared`: tunnel outbound tới Cloudflare, route domain về `app`.

## 1. Chuẩn bị server (chạy 1 lần)

Cài Docker + Docker Compose plugin (Ubuntu/Debian):

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # rồi logout/login lại để áp dụng
docker compose version          # kiểm tra đã có plugin compose
```

## 2. Tạo Cloudflare Tunnel (làm trên dashboard, không cần SSH)

1. Vào **Cloudflare Zero Trust dashboard** → **Networks → Tunnels → Create a tunnel**.
2. Chọn loại **Cloudflared**, đặt tên (vd `dkstore`), Cloudflare sẽ cho một **Tunnel
   Token** — copy lại, sẽ dán vào file `.env` ở bước 4 (không gửi token này cho ai).
3. Ở bước **Public Hostname**, thêm:
   - Domain: domain của bạn (vd `dkstore.yourdomain.com`)
   - Service: `HTTP` → `app:10000` (đúng tên service `app` trong `docker-compose.yml`
     và port nội bộ Spring Boot đang chạy, khớp với `server.port` mặc định 10000).
4. Lưu lại. Không cần cấu hình gì thêm ở phía DNS thủ công — Cloudflare tự tạo bản
   ghi CNAME khi bạn thêm Public Hostname.

## 3. Lấy code về server

```bash
git clone https://github.com/lekhai0123/DKStore.git
cd DKStore
```

(Những lần sau chỉ cần `git pull` trong thư mục này rồi lặp lại bước 5.)

## 4. Tạo file `.env`

```bash
cp .env.example .env
nano .env   # điền giá trị thật
```

Điền:
- `POSTGRES_PASSWORD` / `DB_PASSWORD`: **đặt cùng một mật khẩu mạnh** cho cả hai biến
  (một bên là mật khẩu Postgres tạo container, một bên là mật khẩu app dùng để kết
  nối — phải khớp nhau). `POSTGRES_DB`/`POSTGRES_USER` nên khớp `DB_URL`/`DB_USERNAME`
  (mặc định để sẵn `dkstore`/`dkstore`, có thể giữ nguyên).
- `CLOUDINARY_*`: lấy từ Cloudinary dashboard (tài khoản hiện tại đang dùng).
- `MAIL_USERNAME` / `MAIL_PASSWORD`: Gmail + mật khẩu ứng dụng (app password) hiện
  tại đang dùng để gửi mail xác thực.
- `APP_BASE_URL`: domain thật, có `https://`, ví dụ `https://dkstore.yourdomain.com`
  (dùng để sinh link xác thực email / reset mật khẩu — **bắt buộc phải đúng domain**,
  nếu để sai thì link trong email gửi cho user sẽ trỏ sai chỗ).
- `TUNNEL_TOKEN`: token lấy ở bước 2.

File `.env` đã nằm trong `.gitignore`, không bị commit lên git.

## 5. Chạy

```bash
docker compose up -d --build
```

Lần đầu chạy: Hibernate (`ddl-auto=update`) sẽ tự tạo toàn bộ bảng trên DB Postgres
rỗng, và `DataSeeder` tự tạo 2 role (USER/ADMIN) + 2 tài khoản mặc định
(`user`/`123456`, `admin`/`123456`) — **đổi mật khẩu 2 tài khoản này ngay sau khi
đăng nhập lần đầu**, vì đây là mật khẩu mặc định ai đọc code cũng biết.

Kiểm tra:

```bash
docker compose logs -f app        # xem log khởi động, Ctrl+C để thoát xem log
docker compose ps                 # cả 3 container phải Up (db phải "healthy")
curl -s http://localhost:10000/ping   # test trực tiếp trong server, kỳ vọng trả "ok"
```

Sau đó mở `https://<domain-của-bạn>/` từ trình duyệt để kiểm tra qua Cloudflare Tunnel.

## 6. Cập nhật code sau này

```bash
git pull
docker compose up -d --build   # chỉ rebuild lại image app, dữ liệu DB trong volume không mất
```

## Lưu ý bảo mật quan trọng

- Repo GitHub này đang **public** và trước đây từng commit thẳng mật khẩu DB Render,
  Cloudinary API secret, mật khẩu ứng dụng Gmail vào `application.properties`. Các
  giá trị đó **đã bị lộ trong lịch sử git** dù bản mới nhất đã bỏ đi. Vì vẫn giữ
  nguyên tài khoản Cloudinary/Gmail, bạn nên **đổi (rotate) Cloudinary API secret và
  mật khẩu ứng dụng Gmail** trên dashboard tương ứng, rồi cập nhật lại vào `.env` trên
  server — không liên quan gì tới code, làm khi nào tiện.
- Nếu muốn dữ liệu Postgres không mất khi chạy `docker compose down`, không thêm
  `-v` vào lệnh đó (`-v` sẽ xoá luôn volume `pgdata`).
