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

## 7. (Tuỳ chọn) Auto-deploy khi push code mới lên GitHub

Có sẵn 1 service `webhook` trong `docker-compose.yml`: GitHub gọi vào ngay khi push,
service này tự `git pull` + rebuild lại `app` — không cần tự tay SSH vào pull nữa.

**Đánh đổi cần biết trước khi bật:** service này cần quyền điều khiển Docker daemon
của server (`/var/run/docker.sock`) để tự rebuild — tương đương quyền root trên toàn
server. Được bảo vệ bằng chữ ký HMAC (secret riêng, GitHub và server dùng chung), chỉ
deploy khi push đúng nhánh `master`, nhưng nếu `WEBHOOK_SECRET` bị lộ thì ai cũng kích
được lệnh deploy. Nếu không cần deploy tức thì, cứ tiếp tục `git pull` thủ công như
bước 6, bỏ qua phần này cũng không sao — `webhook` không bật thì không ảnh hưởng gì
tới `app`/`db`/`cloudflared`.

**Giới hạn quan trọng:** webhook chỉ tự rebuild service `app`. Nếu bạn sửa
`docker-compose.yml`, `.env`, hoặc chính code trong `deploy/webhook/`, vẫn phải tự tay
chạy `docker compose up -d --build` (không kèm tên service) trên server một lần.

Các bước bật:

1. Trong `.env`, điền:
   - `HOST_REPO_PATH`: đường dẫn tuyệt đối tới thư mục chứa `docker-compose.yml` trên
     server (xem bằng lệnh `pwd` khi đang đứng trong thư mục đó, ví dụ
     `/data/uploads/khai/project/DKStore`). **Phải đúng tuyệt đối**, vì lệnh rebuild
     bên trong container `webhook` chạy qua Docker socket của server, cần biết đường
     dẫn thật trên server chứ không phải đường dẫn `/repo` bên trong container.
   - `WEBHOOK_SECRET`: sinh chuỗi ngẫu nhiên bằng `openssl rand -hex 32`, dán vào đây.

2. Thêm Public Hostname mới trong Cloudflare Tunnel (giống bước 2, cùng 1 tunnel):
   - Subdomain: vd `deploy`
   - Domain: `ltk.id.vn` (chọn từ dropdown)
   - Service: `HTTP` → `webhook:9000`

3. Khởi động service `webhook` (chỉ cần 1 lần, hoặc mỗi khi sửa code webhook):
   ```bash
   docker compose up -d --build
   ```

4. Trên GitHub: vào repo → **Settings → Webhooks → Add webhook**:
   - Payload URL: `https://deploy.ltk.id.vn/webhook`
   - Content type: `application/json`
   - Secret: đúng giá trị `WEBHOOK_SECRET` ở bước 1
   - Chọn "Just the push event"

5. Test: push 1 commit lên `master`, xem log:
   ```bash
   docker compose logs -f webhook
   ```
   Phải thấy `[deploy] git pull...` rồi `[deploy] docker compose up -d --build app...`.
   Nếu không thấy gì, vào tab **Recent Deliveries** của webhook trên GitHub xem response
   code (401 = sai secret, không thấy request nào = Cloudflare route chưa đúng).

## 8. (Tuỳ chọn) Giám sát sức khoẻ + cảnh báo email

Có 2 lớp giám sát độc lập, không phụ thuộc dịch vụ ngoài kiểu UptimeRobot — chỉ dùng
lại chính tài khoản Gmail đã cấu hình sẵn để gửi mail:

- **`healthmon`** (container mới trong `docker-compose.yml`, chạy ngay trên server):
  ping `http://app:10000/ping` mỗi 60 giây. Bắt được **app bị crash/treo trong khi
  server vẫn sống** — gửi mail khi app rớt và khi app hoạt động lại. Không cần tự viết
  lệnh restart vì `restart: unless-stopped` đã có sẵn cho container `app`.
  - **Giới hạn:** không bắt được trường hợp cả server bị tắt/mất mạng, vì lúc đó chính
    `healthmon` cũng "chết" theo, không gửi mail được.
- **GitHub Actions** (`.github/workflows/uptime-check.yml`, chạy trên máy GitHub, code
  tự viết 100%): ping domain public `https://dkstore.ltk.id.vn/ping` mỗi 5 phút (chu kỳ
  ngắn nhất GitHub Actions schedule hỗ trợ). Bắt được **cả server tắt hẳn/mất mạng**, vì
  chạy hoàn toàn độc lập, không nằm trên server của bạn.

### Bật `healthmon` (chạy trên server)

Chỉ cần điền `ALERT_EMAIL_TO` trong `.env` (email nhận cảnh báo), rồi:
```bash
docker compose up -d --build
docker compose logs -f healthmon   # xem "watching http://app:10000/ping every 60s"
```
Không cần thêm route Cloudflare gì — service này chỉ gọi nội bộ trong Docker network,
không cần ai từ ngoài gọi vào nó.

### Bật GitHub Actions uptime check

1. Tạo nhánh trạng thái riêng (chạy **1 lần duy nhất**, trên máy có sẵn code — làm ở
   máy bạn hoặc trên server đều được, miễn có `git`):
   ```bash
   git checkout --orphan uptime-state
   git rm -rf .
   echo '{"status":"up"}' > status.json
   git add status.json
   git commit -m "init uptime state"
   git push origin uptime-state
   git checkout master
   ```
2. Trên GitHub: vào repo → **Settings → Secrets and variables → Actions → New repository
   secret**, tạo 3 secret:
   - `MAIL_USERNAME`: email Gmail đang dùng để gửi mail (giống `MAIL_USERNAME` trong `.env`)
   - `MAIL_PASSWORD`: mật khẩu ứng dụng Gmail (giống `MAIL_PASSWORD` trong `.env`)
   - `ALERT_EMAIL_TO`: email nhận cảnh báo
3. Vào **Settings → Actions → General → Workflow permissions**, chọn **"Read and write
   permissions"** rồi Save — bắt buộc để workflow tự commit cập nhật trạng thái vào
   nhánh `uptime-state` được.
4. Vào tab **Actions** trên GitHub, chọn workflow "Uptime Check" → **Run workflow** để
   test thử ngay (không cần đợi 5 phút). Nếu server đang sống, log sẽ ghi
   `status unchanged: up`, không gửi mail (đúng vì chưa có gì thay đổi) — muốn test gửi
   mail thật thì tạm sửa `status.json` trên nhánh `uptime-state` thành
   `{"status":"down"}` rồi chạy lại, sẽ thấy mail "đã hoạt động trở lại" gửi tới.

**Giới hạn thật đã gặp:** lịch `schedule` mỗi 5 phút của GitHub Actions **không đáng
tin cậy** — thực tế quan sát được các lần chạy tự động cách nhau **2-6.5 tiếng** thay
vì 5 phút (GitHub tự giãn lịch, đặc biệt với repo public/free), nên dễ bỏ lỡ sự cố
ngắn hạn. Không sửa được bằng code vì đây là hành vi phía hạ tầng GitHub.

### 8b. Khắc phục lịch chạy không đều — dùng cron ngoài chỉ để "bấm nút"

Thay vì tin vào lịch `schedule` nội bộ của Actions, dùng 1 dịch vụ cron ngoài miễn phí
(vd [cron-job.org](https://cron-job.org)) gọi thẳng vào GitHub API để kích hoạt
workflow đúng mỗi 5 phút — dịch vụ ngoài chỉ đóng vai trò "đến giờ thì bấm nút", toàn
bộ logic check + gửi mail vẫn 100% là code bạn tự viết, không phải "giám sát ngoài"
theo nghĩa hộp đen.

1. Tạo **fine-grained personal access token** (KHÔNG dùng classic token có quyền quá
   rộng): GitHub → avatar góc phải → **Settings → Developer settings → Personal access
   tokens → Fine-grained tokens → Generate new token**.
   - Repository access: chọn **Only select repositories** → `DKStore` (chỉ đúng repo
     này, không cấp quyền các repo khác).
   - Permissions: chỉ tick **Actions: Read and write** (không tick gì thêm) — token bị
     lộ cũng chỉ có thể trigger workflow, không push được code hay đọc secret.
   - Generate, copy token lại (chỉ hiện 1 lần).
2. Đăng ký tài khoản free trên cron-job.org (hoặc dịch vụ tương đương), tạo 1 cronjob:
   - URL: `https://api.github.com/repos/lekhai0123/DKStore/actions/workflows/uptime-check.yml/dispatches`
   - Method: `POST`
   - Headers:
     ```
     Authorization: Bearer <token vừa tạo>
     Accept: application/vnd.github+json
     Content-Type: application/json
     ```
   - Body: `{"ref":"master"}`
   - Lịch chạy: mỗi 5 phút.
3. Test: lưu cronjob xong bấm "Run now" (hoặc tương đương) trên cron-job.org, rồi vào
   tab Actions của repo xem có run mới xuất hiện đúng lúc đó không.

**Lưu ý bảo mật riêng cho token này:** chỉ dán token vào ô cấu hình của cron-job.org,
không commit vào bất kỳ file nào trong repo. Nếu nghi ngờ lộ, vào lại Settings →
Developer settings → Personal access tokens để revoke ngay.

## Lưu ý bảo mật quan trọng

- Repo GitHub này đang **public** và trước đây từng commit thẳng mật khẩu DB Render,
  Cloudinary API secret, mật khẩu ứng dụng Gmail vào `application.properties`. Các
  giá trị đó **đã bị lộ trong lịch sử git** dù bản mới nhất đã bỏ đi. Vì vẫn giữ
  nguyên tài khoản Cloudinary/Gmail, bạn nên **đổi (rotate) Cloudinary API secret và
  mật khẩu ứng dụng Gmail** trên dashboard tương ứng, rồi cập nhật lại vào `.env` trên
  server — không liên quan gì tới code, làm khi nào tiện.
- Ngoài ra GitHub secret scanning từng phát hiện 1 **Google Maps API key**
  (`AIzaSyCC72...`, alert mở từ 6/12/2024) từng bị commit vào `user/shop.html` ở một
  bản cũ. Key này **không còn được dùng trong code hiện tại** (trang đã đổi sang
  Leaflet + OpenStreetMap, không cần API key), nên không cần sửa code — nhưng key vẫn
  còn đọc được trong lịch sử git, nên vào Google Cloud Console **xoá/thu hồi hẳn key
  đó** để tránh bị người khác lợi dụng gọi API tính phí vào tài khoản Google Cloud
  của bạn.
- Nếu muốn dữ liệu Postgres không mất khi chạy `docker compose down`, không thêm
  `-v` vào lệnh đó (`-v` sẽ xoá luôn volume `pgdata`).
