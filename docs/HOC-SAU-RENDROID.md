# Học sâu RenDroid — từ chính file RenDroid_1.0.12_0_hr4h.apks anh gửi
# (đọc resources.arsc + classes.dex của base.apk, 19/11/2026)

File vẫn còn nguyên ở thư mục anh gửi: RenDroid_1.0.12_0_hr4h.apks (36MB).
Dưới đây là toàn bộ tính năng bóc ra được từ chuỗi giao diện + code của nó,
không phải đoán qua ảnh chụp.

## Thư viện (Library)
- Tìm kiếm game, xoá tìm kiếm, trạng thái "không có kết quả"
- Sắp xếp (SortType): tên / ngày / dung lượng, tăng & giảm dần
- Import game: được sửa TÊN game trước khi nhập, tên rỗng báo lỗi
- Các giai đoạn import hiện rõ: Waiting -> Preparing -> Detecting Version
  -> Validating -> Downloading runtime -> Extracting -> Finalizing
  -> huỷ import (hỏi xác nhận, xoá file đã tải), Retry Import khi lỗi
- Đổi tên game (Rename), xoá game phải đóng game đang chạy trước,
  hỏi: "Delete %s and all its game files and saves? This cannot be undone."

## Chi tiết game (GameDetailViewModel: clearSaves(), clearPersistent(), ...)
1. **Data Management** — Import Data / Export Data / Delete Data:
   sao lưu & khôi phục dữ liệu game (save + persistent) theo từng game
2. **File Management** — trình duyệt file đầy đủ trong thư mục game:
   tìm file, sắp xếp tên/ngày/size (6 kiểu), copy, paste N file,
   rename, xoá nhiều file đã chọn (hỏi xác nhận, không hoàn tác được),
   Import File, New Folder, Select All, mở file bằng app ngoài (Open with),
   chỉnh cỡ chữ hiển thị, lỗi riêng cho từng thao tác
3. **Mod Management** (ModManager / ModRepository / ModImportWorker):
   - Nhập mod ZIP (hoặc tải mod từ URL: mods/{gameName}?importUrl=...)
   - Xem trước khi nhập (preview): sửa tên mod, đếm file theo loại:
     ADD (thêm mới) / REPLACE (ghi đè) / COMPANION (file đi kèm bị dọn)
   - Mod nhập xong ở trạng thái TẮT (imported disabled), bật mới áp dụng
   - Xung đột: "X conflicts with files from other enabled mods.
     Disable those mods first." — có nút Enable Anyway sau khi cảnh báo
   - Gỡ mod: "Modified game files will be restored" — nó sao lưu file gốc
     khi mod ghi đè và khôi phục lại khi gỡ/tắt mod
   - Huỷ nhập mod giữa chừng (xoá file đã tải), Retry Import
4. **Runtime Version**:
   - 3 dòng: Auto-detected / Compatible / Manual override (override hiện tại)
   - Chọn runtime bằng danh sách hoặc gõ tay phiên bản (vd 8.5.2)
   - Game thiếu runtime: "Requires game runtime download", Download / Retry
5. **Clear Data**: Clear Saves (.save) / Clear Persistent (thành tích,
   cài đặt game — hỏi xác nhận riêng) / Clear Cache — thông báo
   "Saves cleared", "Persistent data cleared", lỗi "Failed to clear..."
6. **Danger Zone**: Reset to Fresh Install (xoá saves+persistent+cache),
   Delete Game

## Cài đặt (SettingsViewModel)
- Theme: Dark / Light / System · Language · Font Size
- **Runtime Management** (mục riêng trong Cài đặt):
  Installed Runtimes / Available Runtimes, dung lượng từng runtime,
  runtime đang được mấy game dùng (usage), trạng thái Unused,
  Download New Runtime (nhập phiên bản, có cả URL tải tuỳ chỉnh + nút reset URL),
  Clean Unused Runtimes (đếm số runtime thừa, hỏi xác nhận, báo đã dọn N),
  Delete Runtime — cảnh báo đặc biệt nếu runtime đang được N game dùng:
  "Deleting it will make these games unplayable. Are you sure?"
  Tap to install, Retry runtime download
- **About**: Version info & licenses, chạm nhiều lần vào phiên bản để mở
  Developer Mode ("%d more taps to enable developer mode")
- **Developer Options**: Debug mode & log export, chọn debug mode
  (Creative Preview / Troubleshooting), Log Viewer: xem system logs,
  copy all, Export Log File, Share Logs, cỡ chữ log, số dòng tối đa, word wrap
- Cách chạy: Legacy mode (Ren'Py 7.x) dùng nativeInit(),
  Modern mode (8.x) dùng nativeRunMain()

## Áp dụng vào Rem Gose (đợt cải thiện này)
- Đã thêm: GameDataManager (export/import/xoá data, clear saves/persistent/cache,
  thống kê dung lượng) + ModManager (preview ADD/REPLACE, backup file gốc,
  bật/tắt/gỡ mod khôi phục file, phát hiện xung đột)
- Đã làm lại màn Chi tiết game theo đúng 6 mục trên
- Cài đặt: thêm Runtime Management (Clean Unused, Download New Runtime),
  About chạm 7 lần mở Developer Mode, mục Log/Developer để sẵn khung
- Còn để đợt sau: trình duyệt file đầy đủ (copy/paste/rename nhiều file),
  tải mod từ URL, Log Viewer hoàn chỉnh
