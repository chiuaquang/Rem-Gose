# HỌC HẾT RENDROID — Checklist tính năng đầy đủ → Rem Gose

Yêu cầu của Anh Quảng (19/11/2026):
- Học **toàn bộ** RenDroid, gồm cả phần Cài đặt của nó.
- Khi phát hành, ứng dụng phải hoạt động **100%, không lỗi** — bấm vào cái gì cũng phải chạy bình thường.
- Ngôn ngữ: English, Vietnamese, Indonesian, Thailand, Singapore, Chinese (+ System Default).

Cách học: phân tích tĩnh file .apks (56.431 chuỗi trong dex + resources.arsc + manifest).
RenDroid là app thương mại đóng mã nguồn — file .apks chỉ chứa code đã biên dịch, **không có
source gốc bên trong** và không được sao chép code của họ. Ta học **tính năng/hành vi/cấu trúc**
rồi tự viết lại bằng code của Rem Gose; phần engine dùng Ren'Py mã nguồn mở (MIT) chính thức.

## 1. Thư viện game
- [x] Tìm kiếm game + xoá từ khoá + trạng thái không có kết quả
- [x] Sắp xếp: tên / ngày thêm / dung lượng (tăng + giảm)
- [x] Thẻ game: icon (hoặc chữ cái đầu khi thiếu icon), tên, nút Chơi
- [x] FAB "+ Import", nhập từ ZIP (đặt lại tên khi nhập, tên trống = báo lỗi)
- [x] Các bước nhập: Waiting → Preparing → Detecting Version → Validating →
      Downloading runtime → Extracting → Finalizing; Huỷ nhập (hỏi + xoá file đã tải); Thử lại
- [ ] Nhập từ thư mục (folder) — bổ sung trước phát hành
- [x] Đổi tên game; xoá game phải đóng game đang chạy trước + hỏi xác nhận

## 2. Chi tiết game
- [x] Data Management: Import Data / Export Data / Delete Data
- [x] File Management: dung lượng đã dùng, duyệt file (mở rộng đầy đủ trước phát hành:
      copy/paste/rename/xoá nhiều file/tạo thư mục/mở bằng app ngoài)
- [x] Mod Management: preview ADD/REPLACE, nhập từ ZIP, mod nhập xong ở trạng thái TẮT,
      phát hiện xung đột, gỡ mod khôi phục file gốc
- [ ] Mod nhập từ URL — bổ sung trước phát hành
- [x] Runtime Version: "Compatible: X (detected Y)", override thủ công, tải/thử lại runtime
- [x] Clear Data: Clear Saves / Clear Persistent (cảnh báo mất settings + achievements) / Clear Cache
- [x] Danger Zone: Reset to Fresh Install / Delete Game

## 3. Cài đặt (đúng ảnh Anh Quảng gửi 19/11/2026)
### Appearance
- [x] Theme: System Default / Dark / Light (hộp chọn radio)
- [x] Dynamic Color: dùng màu hình nền hệ thống (Android 12+)
- [x] Language: System Default / English / Vietnamese / Indonesian / Thailand / Singapore / Chinese
- [x] Prefer landscape: mở game nằm ngang; tắt = theo xoay hệ thống
- [x] Cỡ chữ (Font Size) — RenDroid có, Rem Gose có slider 85–130%
### Management
- [x] Runtime Management: runtime đã cài / có sẵn, dung lượng, số game đang dùng,
      Clean Unused Runtimes, Download New Runtime (gõ phiên bản)
- [x] Storage Management: dung lượng từng khu vực + dọn cache
### About
- [x] About: thông tin phiên bản & licenses; chạm 7 lần vào phiên bản → Developer Mode
      (có đếm ngược "còn N lần chạm nữa")
- [x] Advanced Settings: Debug mode (Off / Troubleshooting / Creative Preview),
      Log Viewer (xem log), Export Log File (chia sẻ file log)

## 4. Chạy game
- [x] Tiến trình riêng cho game (:game0), tối đa nhiều slot như RenDroid (:game0–3) — V1.1
- [x] Wrapper main.py: patch loader để đọc renpy/common (tự viết theo ý tưởng)
- [x] Legacy 7.x chạy nativeInit() / Modern 8.x chạy nativeRunMain()
- [x] Giữ màn hình sáng, rung, chế độ immersive khi chơi

## 5. TIÊU CHUẨN PHÁT HÀNH (Anh Quảng yêu cầu)
1. Mọi nút/mục bấm vào đều hoạt động thật — không nút chết, không mục "sắp có".
2. Mọi thao tác xoá đều hỏi xác nhận; lỗi đều có thông báo dễ hiểu, không crash.
3. Test trên máy thật: nhập game ZIP + thư mục, chơi thử game 7.x và 8.x,
   save/load, mod bật/tắt, xoá dữ liệu, đổi mọi tuỳ chọn trong Cài đặt.
4. Chỉ phát hành khi checklist trên đạt 100%.
