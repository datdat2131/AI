# AstraxAI

Plugin trợ lý AI cho server **Minecraft Paper 1.21.1** (Java 21). Plugin độc lập, kết nối API AI qua HTTPS.

- `/ai <tin nhắn>`: trò chuyện với AI (tiếng Việt). `/ai clear`, `/ai help`, `/ai menu`.
- AI trả lời về server dựa trên `plugins/AstraxAI/knowledge.yml` (không bịa thông tin server).
- `/astraai status | diagnose | report | reload` (alias `/astraxai`): trợ lý cho admin. AI chỉ đọc số liệu (TPS, MSPT, RAM, người chơi, plugin, cảnh báo) và đưa ra đề xuất; AI **không** chạy lệnh và **không** đọc file tùy ý.
- Menu GUI: Chat với AI, Hướng dẫn server, Hỏi về Boss / Relic / Rank, Trợ lý Admin (chỉ admin).

## 1. Đưa project lên GitHub

1. Tạo repository mới trên GitHub (có thể để private).
2. Đẩy toàn bộ thư mục này lên nhánh `main`:
   ```bash
   git init
   git add .
   git commit -m "AstraxAI"
   git branch -M main
   git remote add origin https://github.com/<tên-bạn>/<tên-repo>.git
   git push -u origin main
   ```
   File `config.yml` trong repo có `api.key: ""` (để trống). **Không bao giờ** điền API Key vào repo; key chỉ điền trên server (bước 3).

## 2. Tải AstraxAI.jar từ GitHub Actions

1. Mỗi lần push lên `main`, GitHub tự build. Có thể build tay: vào tab **Actions** → **Build AstraxAI** → **Run workflow**.
2. Mở lần chạy mới nhất (dấu tick xanh) → kéo xuống mục **Artifacts** → bấm **AstraxAI** để tải về file `.zip`.
3. Giải nén, bên trong là `AstraxAI.jar`.

Build không cần API Key. Muốn build trên máy: `mvn clean package` → file nằm ở `target/AstraxAI.jar` (cần JDK 21 và Maven).

## 3. Cài plugin và cấu hình API Key

1. Chép `AstraxAI.jar` vào thư mục `plugins/` của server Paper 1.21.1 (Java 21), khởi động server. Plugin tự tạo `plugins/AstraxAI/config.yml` và `knowledge.yml`.
2. Mở `plugins/AstraxAI/config.yml`, điền API Key:
   ```yaml
   api:
     key: "DÁN_API_KEY_CỦA_BẠN_VÀO_ĐÂY"
   ```
   Hoặc để trống `api.key` và đặt biến môi trường `ASTRAXAI_API_KEY` cho tiến trình server.
3. Chạy `/astraai reload` (hoặc khởi động lại). Console sẽ ghi "API Key: đã cấu hình" (không bao giờ in ra key).
4. Sửa `plugins/AstraxAI/knowledge.yml` thành dữ liệu thật của server (nội dung hiện tại chỉ là ví dụ), rồi `/astraai reload`.

Mặc định dùng Messages API của Anthropic (`format: "anthropic"`, model `claude-haiku-5-5`). Có thể đổi `api.url`, `api.model`; với dịch vụ tương thích OpenAI đặt `format: "openai"`. `api.url` bắt buộc là HTTPS. Lưu ý việc gọi API tính phí theo lượng dùng; hãy đặt `cooldown-seconds` và `max-tokens` hợp lý.

## 4. Quyền (permissions)

| Quyền | Mặc định | Công dụng |
|---|---|---|
| `astraxai.chat` | mọi người | Dùng AI chat |
| `astraxai.gui` | mọi người | Mở menu GUI |
| `astraxai.admin` | OP | Admin AI: status, diagnose, report, nút Trợ lý Admin |
| `astraxai.reload` | OP | `/astraai reload` |

## 5. Bảo mật và hiệu năng

- API được gọi bất đồng bộ bằng `ExecutorService` riêng; main thread không bao giờ bị chặn. Tắt plugin sẽ shutdown executor.
- Cooldown theo người chơi, mỗi người chỉ có 1 yêu cầu đang chạy, giới hạn độ dài tin nhắn.
- Xử lý timeout, lỗi API, hết quota, API Key sai (người chơi chỉ thấy thông báo chung, chi tiết chỉ ở console, đã che key).
- Lịch sử chat lưu theo UUID, chỉ trong RAM, có giới hạn, có thể tắt (`memory.enabled`), tự xóa khi thoát (`memory.clear-on-quit`). Email, IP, mật khẩu, token, số thẻ bị che trước khi gửi/lưu.
- Câu trả lời của AI hiển thị dạng văn bản thuần (không thể chèn mã màu hay link click được).
- Nội dung `knowledge.yml` và số liệu admin được gửi tới dịch vụ AI: đừng ghi bí mật vào đó.
- `/astraai report` lưu báo cáo vào `plugins/AstraxAI/reports/`.

## 6. Menu Rank và thanh toán (shop.yml)

- File `plugins/AstraxAI/shop.yml` chứa bảng giá rank, tài khoản ngân hàng, link mã QR (`qr-url`) và link Discord (`discord-url`). Sửa xong dùng `/astraai reload`.
- Người chơi gõ `/ai` có chữ như rank, nạp, donate, bảng giá, bank... hoặc bấm nút "Hỏi về Rank" sẽ mở thẳng menu rank (không tốn lượt gọi API, dùng được cả khi chưa có API Key). Tắt bằng `intercept-chat: false`.
- Bấm vào một rank để xem số tiền, số tài khoản (bấm để sao chép) và nút mở mã QR. Việc gift rank do staff làm thủ công, plugin không cấp rank và không xác nhận giao dịch.
- Link QR/Discord chỉ chấp nhận `https://`.


## GitHub Actions build

Repository đã được sắp xếp dạng flat: các file `.java`, `plugin.yml`, `config.yml`,
`knowledge.yml` và `shop.yml` nằm ở thư mục gốc. Khai báo `package` trong Java
được giữ nguyên; Maven biên dịch các file Java ở thư mục gốc.

GitHub Actions tự build khi push hoặc mở pull request vào `main`, hoặc chạy thủ công
tại tab **Actions**. File JAR được tạo tại `target/AstraxAI.jar`.
