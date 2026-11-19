# Học hỏi logic 2 app Ren'Py Player — để làm Rem Gose

Phân tích tĩnh (không chạy app, không copy code) ngày 19/11/2026
File 1: RenDroid_1.0.12_0_hr4h.apks (36MB) — anh gửi trực tiếp
File 2: renpy HHG_1.2.1.apks (173MB) — link Google Drive anh gửi

---

## 1. RenDroid 1.0.12 — org.renlauncher.rendroid

### Cấu trúc .apks (9 file)
- base.apk — 8.8MB: app chính (thư viện, nhập game, quản lý runtime)
- split_config.arm64_v8a / xxhdpi — cấu hình máy
- split_runtime_7_3_5.apk (3.3MB) + .config.arm64 (18MB native)
- split_runtime_8_2_3.apk (17KB) + .config.arm64 (27MB native)
- split_runtime_8_3_7.apk (17KB) + .config.arm64 (27MB native)

=> Runtime là **module tách riêng (Play Feature Delivery / split APK)**,
chỉ cài runtime nào cần, không phình base app.

### Logic chạy — suy ra từ code đã biên dịch (classes.dex + manifest)
1. **Nhập game** bằng GameImportWorker (chạy nền bằng WorkManager):
   - Nhận source_uri (file ZIP / thư mục người dùng chọn)
   - Kiểm tra bắt buộc: phải có thư mục `game/` — thiếu là báo
     "Missing 'game/' directory in game root"
   - Cảnh báo mềm: thiếu `renpy/` vẫn có thể chạy nếu game thuần Python
   - Lấy icon từ `game/gui/window_icon.png`, tên/phiên bản game từ game
2. **Nhận diện phiên bản Ren'Py của game:**
   - Đọc `renpy/__init__.py` ("Detected version from renpy/__init__.py")
   - Và `game/script_version.txt`
3. **Chọn runtime tự động (auto-adapt):**
   - Bảng runtime hỗ trợ trong code: 7.3.5, 7.4.11, 7.5.3, 7.6.3, 7.7.3,
     7.8.7, 8.0.3, 8.1.3, 8.2.3, 8.3.7, 8.4.1, 8.5.2, 8.6.0
   - Game phiên bản nào thì map sang runtime gần nhất tương thích
     ("Auto-adapted runtime"), người dùng đổi tay được bằng
     `runtime_version_override` trong DB
4. **Cài runtime** bằng RuntimeInstallWorker:
   - Runtime có sẵn dạng split module, hoặc tải từ nguồn chính thức
     `https://www.renpy.org/dl/` ("downloadRuntime")
   - Trạng thái cài đặt lưu theo phase/message để UI báo tiến độ/lỗi
     ("Could not activate runtime. Reinstall the runtime")
5. **Chuẩn bị game trước khi chạy (wrapper trick — quan trọng nhất):**
   - Đổi `main.py` của game thành `_renpy_main.py`
   - Ghi 1 file `main.py` mới (RenDroid wrapper):
     - Patch `renpy.loader.scandirfiles` bằng import hook để cộng thêm
       thư mục `renpy/common` vào searchpath
     - Set `renpy.config.commondir` đúng thư mục đó, khi có biến
       môi trường `ANDROID_PRIVATE`
     - Lý do: trên Android, game 7.x hay lỗi không tìm thấy renpy/common
     - Cuối cùng `execfile(_renpy_main.py)` để chạy game gốc
6. **Chạy game — mỗi game 1 process riêng:**
   - GameRunnerActivity bọc PythonSDLActivity (org.renpy.android)
   - Có 4 slot: GameRunnerSlot0..3, chạy ở process `:game0`..`:game3`
   - 1 process chỉ thuộc 1 game: "A game already owns this process",
     "Close the running game before changing its runtime or starting it again"
   - Phiên chạy quản lý qua RunnerSessionProvider, uri `rendroid://session/`
7. **Dữ liệu (Room DB — rendroid_database):**
   - Bảng `games`: name (PK), display_name, creation/deletion_timestamp,
     finalizers, source_uri, path, renpy_version, version, icon,
     runtime_version_override, status_phase, status_message
   - Bảng `runtimes`: version (PK), creation/deletion_timestamp,
     status_phase, status_message
   - FileProvider chia sẻ thư mục `games/` và `sdks/`
8. **Tính năng phụ:** ModImportWorker (nhập mod cho game), hỗ trợ finalizers
   (bước xử lý sau khi nhập), log `rendroid_log.txt`
9. **Native runtime:**
   - 7.3.5: nhiều .so tách rời — libSDL2, libSDL2_image/mixer/ttf/gfx,
     libpython2.7, libpymodules (vì Ren'Py 7 dùng Python 2.7 + SDL2)
     + assets `runtimes/7.3.5-private.zip` (770 file Python 2.7)
   - 8.2.3 / 8.3.7: 1 file duy nhất `librenpython_8_x_x.so` ~27MB
     (Ren'Py 8 gộp Python 3 + Ren'Py vào 1 native lib)
10. **Tech stack:** Kotlin, Jetpack Compose Material 3, Hilt, Room,
    WorkManager, DataStore, SplitCompat. Quyền: INTERNET,
    READ/WRITE_EXTERNAL_STORAGE (legacy), RECORD_AUDIO, VIBRATE,
    WAKE_LOCK, FOREGROUND_SERVICE. Có quảng cáo AdMob.

---

## 2. renpy HHG 1.2.1 — com.gamingforever.renpy

### Cấu trúc .apks (4 file, tổng 173MB — nặng hơn RenDroid gần 5 lần)
- base.apk — 172MB: chứa TẤT CẢ trong 1 cục, không tách runtime
- split_config.arm64_v8a — 36MB: chỉ 1 file native `librenpython.so`
- split_config.vi / xxhdpi — ngôn ngữ/màn hình

### Logic hoàn toàn khác RenDroid: player viết BẰNG CHÍNH Ren'Py
1. **Bản thân HHG là 1 game Ren'Py 8.4** đóng bằng RAPT chuẩn:
   - `assets/x-android.json`: name "renpy HHG", package com.gamingforever.renpy,
     orientation sensorLandscape, version 1.0
   - Activity chính là `org.renpy.android.PythonSDLActivity` gốc của Ren'Py
   - Python 3.12 (`assets/extracted/lib/python3.12/*.pyc`),
     Ren'Py common nằm ở `assets/x-renpy/x-common/` — mọi file bị thêm
     tiền tố `x-`, và trong code có hàm `remove_x_prefix()` để đổi tên lại
     khi giải nén (mẹo lách quét file / đóng gói)
2. **Thư viện game = 1 thư mục ngoài bộ nhớ:**
   - Người dùng chép thư mục project vào: `Renpy HHG -> Games`
     (đường dẫn gốc là biến môi trường `ANDROID_PUBLIC`)
   - Cấu trúc bắt buộc: `Games/<TênGame>/game/`
   - Màn hình chính (viết bằng Ren'Py screen) liệt kê các thư mục trong
     Games: "Supports all projects that can run on RenPy 8.4 SDK"
3. **Chọn game & khởi động lại vào game đó:**
   - Bấm START: ghi tên game đã chọn vào `Saves/cache/saved_path.txt`,
     ghi cờ `Saves/cache/sh.txt` = "1"
   - Sau đó gọi `renpy.reload_script()` + `renpy.full_restart()` +
     `renpy.jump("start")` — tức là engine tự khởi động lại và lần chạy
     sau nạp project đã chọn thay vì nạp launcher
     (phần chuyển project nằm ở bootstrap lúc engine khởi động,
     đọc saved_path.txt — em xác nhận được cơ chế ghi cờ + restart,
     chưa bóc hết phần bootstrap native)
4. **Quản lý file viết ngay trong Ren'Py (pyjnius):**
   - Dùng `jnius.autoclass` gọi Android API từ Python: DocumentFile (SAF),
     ContentResolver, PythonSDLActivity.mActivity, Intent
   - Có file manager 2 panel: chọn thư mục bằng SAF, copy sang thư mục
     Games với màn hình tiến độ (MB/%, chạy thread riêng)
   - Giải nén ZIP: bung thẳng vào `games/`
   - Giải nén APK: bung vào `games/<tên apk>/game`, chỉ lấy `assets/game`
     hoặc `assets/x-game`, bỏ tiền tố `assets/` và `x-`
5. **Tính năng phụ:** đổi font cho game (bundle sẵn ~83MB font: default,
   vietnamese, japanese, korean, taiwan, hindi, thai, arabic... chép đè
   thành font.ttf cho game đã chọn), cấu hình dịch lưu ở
   `Saves/cache/save.json` (target_lang, translation_mode, font_name)
6. **Nhược điểm rõ ràng:**
   - Chỉ 1 runtime Ren'Py 8.4 duy nhất — game 7.x hoặc game 8.x khác
     phiên bản dễ lỗi, không tự map runtime như RenDroid
   - Phình to 173MB vì font + nhiều bản copy của chính launcher
     (assets có cả `game/`, `game2/`, `game12/` gần giống hệt nhau)
   - Logic nằm rải trong file .rpy viết vội (nhiều code chết, tiếng Trung
     trong comment) — khó bảo trì, khó mở rộng

### So sánh nhanh 2 app
| | RenDroid 1.0.12 | renpy HHG 1.2.1 |
|---|---|---|
| Kiểu app | App Android native (Kotlin/Compose) | Game Ren'Py làm launcher |
| Dung lượng gốc | 8.8MB + runtime khi cần | 172MB trọn gói |
| Runtime | 13 phiên bản (7.3.5 -> 8.6.0), tự map + override | Chỉ 8.4 |
| Cài runtime | Split module / tải từ renpy.org | Có sẵn, không thay được |
| Chạy game | Process riêng :game0-:game3 (tối đa 4) | Restart chính app vào game đã chọn |
| Nhập game | Worker nền, kiểm tra game/ + renpy/ | File manager SAF + unzip ZIP/APK trong app |
| Quản lý dữ liệu | Room DB (games + runtimes) | Thư mục Games + file txt/json trong Saves/cache |
| Độ sạch/bảo trì | Cao | Thấp |

**Kết luận:** RenDroid là kiến trúc đúng để học lâu dài.
HHG chứng minh 1 đường tắt thông minh: nếu muốn ra bản thử nhanh,
có thể viết launcher bằng chính Ren'Py như HHG — không cần biết Kotlin —
nhưng sẽ kẹt ở 1 runtime 8.4 và app rất nặng.

---

## 3. Bài học áp dụng cho Rem Gose

### Nên học theo
- Tách runtime theo phiên bản, đừng nhét hết vào 1 cục — RenDroid làm split,
  app chính chỉ 8.8MB
- Tự nhận diện renpy_version từ renpy/__init__.py + script_version.txt,
  tự map runtime gần nhất, cho người dùng override khi game lỗi
- Wrapper main.py patch searchpath/commondir — sửa được lỗi kinh điển của game 7.x trên Android
- Mỗi game chạy process riêng, đóng game này không crash thư viện
- Import/cài runtime/xoá game đều chạy nền bằng Worker, có status_phase/message lưu DB để UI báo rõ
- DB đơn giản 2 bảng games/runtimes như trên là đủ cho V1

### Không nên copy nguyên
- Không copy code/assets của RenDroid/HHG — chỉ học kiến trúc, tự viết lại.
  Runtime Ren'Py thì lấy từ nguồn chính thức renpy.org (giấy phép mở)
- V1 của Rem Gose chỉ cần 2-3 runtime trước (8.3.7, 8.2.3, 7.3.5 như file mẫu)
  thay vì 13 runtime như RenDroid
- Bỏ quảng cáo, bỏ mod system ở V1 cho nhẹ
- Bắt đầu với 1 slot chạy game thay vì 4 slot

### Kiến trúc V1 đề xuất cho Rem Gose (cập nhật sau khi học 2 app)
Library (Compose) -> Import Worker (kiểm tra game/ + nhận diện version)
-> Runtime Manager (map version -> runtime, cài/tải runtime)
-> Prepare (wrapper main.py) -> Runner (process riêng, PythonSDLActivity)
