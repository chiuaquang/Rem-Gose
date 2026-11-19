# Rem Gose — Ứng dụng chơi game Ren'Py trên Android

Chủ dự án: Anh Quảng
Tên ứng dụng: Rem Gose
Package đề xuất: com.remgose.rem

## Em đã kiểm tra thực tế (19/11/2026)

1. **Cách chính thức của Ren'Py:** Ren'Py không có sẵn 1 player chung.
   Mỗi game được đóng gói thành 1 APK riêng bằng RAPT (Ren'Py Android
   Packaging Tool), cần JDK 21 + Android SDK. Chạy Android 5.0+,
   OpenGL ES 3.0. Xuất được APK (cài trực tiếp) hoặc AAB (lên Google Play),
   tối đa 2GB. Nguồn: renpy.org/doc/html/android.html

2. **Cách các player đang làm (JoiPlay, RenPlay, Renpy Pocket):**
   - Người dùng nhập game từ file ZIP hoặc thư mục
   - App có thư viện game (library), ảnh bìa, nút Chơi
   - Bên trong bundle sẵn runtime Ren'Py — RenPlay hỗ trợ:
     8.5.2, 8.5.0, 8.4.1, 8.3.7, 8.3.4, 8.2.3, 8.1.3, 8.0.3
   - Game hợp lệ phải có thư mục `game/` (chứa .rpy / .rpa) và `renpy/`
   - Đây là hướng khó hơn, nhưng đúng với ý "ứng dụng CHƠI game Ren'Py"

## 2 hướng làm — cần Anh Quảng chọn

### Hướng A: Player đa năng (giống JoiPlay / RenPlay) — đúng ý ban đầu
- Mở app Rem Gose -> Nhập ZIP/thư mục game Ren'Py -> game vào thư viện -> bấm Chơi
- Ưu: chơi được nhiều game, đúng nhu cầu
- Nhược: phải tích hợp runtime Ren'Py 8.x, xử lý nhiều phiên bản Ren'Py,
  save game, giải nén, quyền truy cập file Android 11+. Thời gian làm V1 lâu hơn.

### Hướng B: 1 game = 1 APK tên Rem Gose
- Dùng Ren'Py Launcher + RAPT đóng gói 1 game cụ thể thành APK Rem Gose
- Ưu: làm nhanh, ra APK thật cài được ngay, ít lỗi
- Nhược: mỗi game là 1 app riêng, không có thư viện, không nhập game khác vào chơi được

### Hướng C (em đề xuất): Làm theo giai đoạn
- Giai đoạn 1: Dựng khung app Rem Gose (thư viện, nhập ZIP, nhận diện game Ren'Py,
  giải nén, quản lý save) + 1 game mẫu đóng APK để test luồng chơi
- Giai đoạn 2: Tích hợp runtime Ren'Py 8.x để chơi trực tiếp trong app như RenPlay

## Khung V1 — Hướng A/C
- Màn hình Thư viện: danh sách game đã nhập, bìa, tên, phiên bản Ren'Py, nút Chơi/Xoá
- Nhập game: chọn ZIP hoặc thư mục, kiểm tra `game/` + `.rpy/.rpa`, báo lỗi nếu không phải game Ren'Py
- Giải nén an toàn, chống Zip Slip, giới hạn dung lượng
- Quản lý save: mỗi game 1 thư mục save riêng, không lẫn nhau
- Cài đặt: thư mục game, xoá cache, thông tin phiên bản

## Cấu trúc thư mục dự án này
- `docs/` — kế hoạch chi tiết
- `renpy-launcher/game/` — game Ren'Py mẫu tên Rem Gose để test đóng APK bằng RAPT
- `android-player/` — khung app Android (Kotlin) cho player

## Bước tiếp theo
Anh Quảng chọn A, B hay C — em bắt đầu code V1 theo hướng đó luôn.
Nếu anh có sẵn 1 game Ren'Py mẫu (ZIP/thư mục), gửi em để em test đúng game đó.
