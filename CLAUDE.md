# DKStore — Ghi chú dự án cho Claude Code

## Tổng quan
DKStore là ứng dụng bán hàng trực tuyến (e-commerce) viết bằng Spring Boot, kiến trúc
server-rendered MVC với Thymeleaf — một stack duy nhất phục vụ cả trang người dùng và
trang quản trị (admin). Bản deploy cũ tại `https://dkstore.ltk.id.vn/` chạy trên
Render; dự án đang chuyển sang tự host trên server riêng của chủ dự án qua
**Docker Compose + Cloudflare Tunnel** (xem [DEPLOY.md](DEPLOY.md)).

## Tech stack
- Java 17, Spring Boot 3.3.5 (`spring-boot-starter-parent`), packaging `war`.
- Spring MVC + Thymeleaf (không phải SPA/REST API, trừ vài endpoint `@ResponseBody`
  nhỏ lẻ dùng cho giỏ hàng/chi tiết hóa đơn qua AJAX).
- Spring Data JPA + Hibernate, DB PostgreSQL (Render). `ddl-auto=update` →
  **không có migration script (Flyway/Liquibase)**, schema tự đồng bộ theo entity khi
  app khởi động.
- Spring Security: form login, mã hoá `BCrypt`, 2 role ADMIN/USER (bảng
  Role/UserRole quan hệ nhiều-nhiều qua `UserRole`).
- Cloudinary để lưu ảnh upload (dù có thư mục `static/uploads`, ảnh thực tế không lưu
  local mà upload lên Cloudinary).
- Spring Mail (Gmail SMTP) cho email xác thực tài khoản / quên mật khẩu.
- Maven wrapper (`mvnw` / `mvnw.cmd`). Không có build tool frontend riêng — JS/CSS
  thuần và thư viện bên thứ ba nằm sẵn trong `static/fe/plugins`.
- Không dùng Lombok — mọi entity viết tay constructor/getter/setter.

## Cấu trúc thư mục chính
- `src/main/java/com/dkstore/`
  - `SecurityConfig.java`, `ServletInitializer.java`, `DkStoreApplication.java`: cấu
    hình gốc của app.
  - `config/`: `DataSeeder` (seed role + tài khoản mặc định lúc khởi động),
    `CloudinaryConfig`.
  - `controllers/`: controller phần người dùng (`UserControllerMain`,
    `HomeController`) và `controllers/admin/*` cho phần quản trị;
    `controllers/health/HealthController` expose `GET /ping`.
  - `models/`: JPA entity, đặt tên theo domain tiếng Việt (xem bảng thuật ngữ).
  - `repository/`: interface `JpaRepository`; có cả JPQL (`@Query`) lẫn native SQL
    cho query lọc/tìm kiếm phức tạp (ví dụ `ProductRepository`).
  - `services/`: theo cặp interface + impl, hậu tố impl là **`ServiceImple`**
    (không phải "Impl") — giữ đúng chính tả này khi thêm service mới.
- `src/main/resources/templates/`: Thymeleaf, tách `admin/` và `user/` (mỗi bên có
  `layout/` chứa fragment dùng chung).
- `src/main/resources/static/`: `assets/` và `fe/` là theme HTML tĩnh (Bootstrap,
  jQuery, slick...), không qua bundler nào.
- `application.properties`: file cấu hình duy nhất (không có
  `application-dev/prod.properties`). Toàn bộ secret (`DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD`, `CLOUDINARY_*`, `MAIL_USERNAME`, `MAIL_PASSWORD`) đọc từ biến môi
  trường **không có default** — bắt buộc phải set, không còn hardcode giá trị thật
  trong file này nữa (đã sửa vì repo GitHub public). `app.base-url` (mặc định
  `http://localhost:10000`) dùng để build link xác thực email/reset password, set qua
  `APP_BASE_URL`.
- `docker-compose.yml`, `.env.example`, `.dockerignore`, `DEPLOY.md`: bộ file phục vụ
  tự deploy — `docker-compose.yml` chạy 3 service (`db` = Postgres, `app` = build từ
  `Dockerfile` có sẵn, `cloudflared` = Cloudflare Tunnel, không cần mở port trên
  router/firewall). `.env` (thật, không commit) tạo từ `.env.example` trên server.

## Thuật ngữ tiếng Việt trong domain model
- `GioHang` = giỏ hàng (Cart), `ChiTietGioHang` = dòng chi tiết giỏ hàng.
- `HoaDon` = hóa đơn (Order/Invoice), `ChiTietHoaDon` = dòng chi tiết hóa đơn.
- `ThanhToan` = thanh toán (Payment), `PhuongThucThanhToan` = enum phương thức
  thanh toán (COD/BANK...).
- `SanPhamTonKho` = tồn kho theo size (stock per size).
- `HinhAnhSanPham` = ảnh sản phẩm (có cờ `isMain` đánh dấu ảnh đại diện).
- `Brand` = thương hiệu, dùng luôn làm category để lọc sản phẩm.

Khi thêm field/entity liên quan các domain này, giữ nguyên naming tiếng Việt hiện có,
không dịch nửa chừng sang tiếng Anh.

## Luồng chính
- **Auth & phân quyền** (`SecurityConfig`): `/admin/**` cần authority `ADMIN`,
  `/user/**` cần `USER` hoặc `ADMIN`, còn lại `permitAll`. Login/logout custom
  redirect theo role qua `AuthenticationSuccessHandler`. **CSRF đang bị tắt**
  (`csrf.disable()`).
- **Đăng ký & xác thực email** (`AuthenticationServiceImple`): user tạo với
  `enabled=false`, gửi `ConfirmationToken` qua email; login sẽ redirect
  `/account-not-confirmed` nếu tài khoản chưa được xác thực.
- **Giỏ hàng → thanh toán → hóa đơn** (`UserControllerMain`): add-giohang → giohang
  → checkout → confirm-thanhtoan → hoadon; có kiểm tra/trừ/hoàn tồn kho qua
  `SanPhamTonKhoService.updateTonKho`.
- **Quản trị sản phẩm** (`admin/ProductController`): thêm sản phẩm kèm nhiều ảnh
  (upload Cloudinary) và nhiều dòng tồn kho theo size cùng lúc (nhận qua param CSV
  `sizeList`/`soluongList` từ form, không phải danh sách object chuẩn).
- **Seed dữ liệu** (`DataSeeder`, `CommandLineRunner`): tạo role USER/ADMIN và 2 tài
  khoản mặc định (`user`/`123456`, `admin`/`123456`) mỗi lần app start nếu chưa có.

## Quy ước code hiện tại (giữ nguyên khi sửa/thêm mới)
- Controller dùng field injection `@Autowired` (không dùng constructor injection).
  Method trả về String tên view hoặc `redirect:...`. **Tên view KHÔNG được có `/` ở
  đầu** (vd `"admin/product/index"`, không phải `"/admin/product/index"`) — dấu `/`
  đầu làm Thymeleaf 3.1.2 (bản đang dùng) không resolve được template, ném
  `TemplateInputException` → lỗi 500 khi request thật (đã phát hiện qua
  `/admin/giohang` bị lỗi lúc deploy, rồi rà soát và sửa hết 13 chỗ tương tự trong
  `UserController`/`ProductController`/`HoaDonController`/`GioHangController`). Nếu
  thêm view mới, luôn viết không có `/` ở đầu. `redirect:/...` thì vẫn giữ nguyên `/`
  vì đó là URL thật, không phải tên template.
- Service: interface riêng + impl riêng (`XxxService` / `XxxServiceImple`). Method
  create/update/delete thường trả `Boolean`, bắt exception rồi `e.printStackTrace()`
  và trả `false` — không có custom exception hay `@ControllerAdvice` global handler.
- Không có DTO layer: entity JPA được bind thẳng qua `@ModelAttribute` từ form.
- Indent bằng tab, không có Checkstyle/Spotless — không có lint/formatter tự động.
- Ít comment/Javadoc; nếu cần comment, viết ngắn gọn bằng tiếng Việt như code hiện tại.

## Build / Test / Run
- Chạy dev: `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`) → mặc
  định port `10000` (`server.port`).
- Build: `./mvnw clean package` → WAR tại `target/DKStore-0.0.1-SNAPSHOT.war`
  (Dockerfile build theo cách này với `-DskipTests`).
- Test: `./mvnw test`. Hiện chỉ có đúng 1 test
  (`DkStoreApplicationTests.contextLoads`), test này load full Spring context nên
  **cần kết nối được Postgres** theo cấu hình trong `application.properties` (không
  có H2/profile test riêng, không mock DB). Nếu thiếu mạng/DB thật, `mvnw test` và
  `spring-boot:run` đều fail khi khởi tạo context. Vì secret không còn default, phải
  set đủ biến môi trường (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CLOUDINARY_*`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`) trước khi chạy — xem `.env.example`.
- Không có CI (không có `.github/workflows`) — verify thủ công bằng các lệnh Maven ở
  trên sau khi sửa code.
- Máy chạy Claude Code hiện tại (session này) **không có Java/Maven/Docker** cài sẵn
  — không tự chạy được `mvnw compile/test` hay `docker compose config` để verify, chỉ
  review code thủ công. Nếu môi trường sau này có các toolchain này thì nên chạy thật
  để verify thay vì chỉ đọc code.

## Bug đã phát hiện & sửa khi deploy lên DB rỗng (2026-08-25/26)
DB Render cũ đã tích luỹ dữ liệu nhiều năm nên các bug "empty state" dưới đây chưa
từng lộ ra; khi chuyển sang Postgres tự host (rỗng hoàn toàn) mới hiện hình. Nếu sau
này tạo thêm trang danh sách/phân trang mới, tránh lặp lại 2 lỗi này:
- **Tên view Thymeleaf không được có `/` ở đầu** — xem mục quy ước code bên dưới.
- **Phân trang kiểu `#numbers.sequence(1, totalPage)`**: khi `totalPage == 0` (bảng
  rỗng), hàm này trả về `[1, 0]` (2 phần tử, không rỗng/không lỗi như tưởng), nên
  luôn phải bọc `th:if="${totalPage != null && totalPage > 0}"` trên cùng thẻ
  `<li>` (5/6 template đã có sẵn, `user/shop.html` từng thiếu → đã thêm). Đồng thời
  tất cả service `getAll(pageNo)`/`search(keyword, pageNo)` dùng
  `PageRequest.of(pageNo - 1, size)` đã đổi thành
  `PageRequest.of(Math.max(pageNo - 1, 0), size)` để không crash
  (`IllegalArgumentException: Page index must not be less than zero!`) nếu có
  `pageNo=0` lọt qua (bấm link "0" sinh ra ở trên, hoặc gõ tay URL).
- Chưa rà hết các trường hợp "empty state" khác ngoài phân trang (vd `findById(id)`
  cho id không tồn tại ở vài chỗ vẫn dùng `.get()`/`orElseThrow` không bắt riêng) —
  chưa gặp lỗi thật nên chưa sửa, cứ theo dõi log khi test thêm.

## ⚠️ Rủi ro đã biết (không tự sửa nếu task không yêu cầu, nhưng nên nhắc user)
- Repo GitHub là **public** và **lịch sử git** vẫn còn commit cũ chứa plaintext mật
  khẩu DB Render, Cloudinary API secret, mật khẩu ứng dụng Gmail (đã xoá khỏi
  `application.properties` ở bản hiện tại nhưng vẫn đọc được nếu xem git log). Vì vẫn
  giữ nguyên tài khoản Cloudinary/Gmail, nên khuyến nghị user rotate 2 secret này —
  đã ghi trong `DEPLOY.md`, chưa tự làm vì cần thao tác trên dashboard bên ngoài.
- CSRF bị tắt toàn cục trong `SecurityConfig` — chưa sửa, ngoài phạm vi các task deploy
  đã làm.
- `DataSeeder` luôn seed 2 tài khoản mặc định mật khẩu `123456` — `DEPLOY.md` đã nhắc
  đổi ngay sau khi deploy lần đầu, nhưng bản thân code chưa có cơ chế bắt buộc đổi.

## File quan trọng theo khu vực (đọc trước khi sửa)
- Auth/phân quyền: `SecurityConfig.java`, `CustomUserDetailService`,
  `CustomUserDetails`, `AuthenticationServiceImple`.
- Sản phẩm & tồn kho: `controllers/admin/ProductController`, `ProductServiceImple`,
  `SanPhamTonKhoServiceImple`, `HinhAnhSanPhamServiceImple`.
- Giỏ hàng/thanh toán/hóa đơn: `UserControllerMain`, `GioHangServiceImple`,
  `ThanhToanServiceImple`, `HoaDonServiceImple`.
- Upload ảnh: `StorageService` / `FileSystemStorageService` (tên class gây hiểu lầm —
  thực chất upload lên Cloudinary chứ không lưu filesystem local; giữ nguyên tên trừ
  khi được yêu cầu đổi), `CloudinaryConfig`.
- Cấu hình: `application.properties`, `pom.xml`, `Dockerfile`.
- Deploy: `docker-compose.yml`, `.env.example`, `.dockerignore`, `DEPLOY.md`.

## Nguyên tắc làm việc cho các task sau
- Đọc code hiện có trước khi sửa; tuân theo convention ở trên thay vì áp convention
  mới.
- Chỉ thay đổi ở mức tối thiểu, an toàn, đúng phạm vi task — không refactor phần
  không liên quan (kể cả khi thấy `e.printStackTrace()`, thiếu DTO, thiếu
  constructor injection... đều là style hiện tại, không tự "dọn dẹp").
- Không đoán khi có thể tìm câu trả lời trong project (đọc entity/repository/service
  liên quan trước khi đoán field hay behavior).
- Sau khi sửa, chạy `./mvnw test` và/hoặc `./mvnw clean package` nếu phù hợp với thay
  đổi; nếu không có DB/mạng để chạy test thật, nói rõ giới hạn này thay vì báo cáo
  thành công.
