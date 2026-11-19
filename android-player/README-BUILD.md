# Rem Gose — Android Player (Native, học theo RenDroid)

Phiên bản: 0.1.0 — khung V1 đã có logic lõi + UI, chưa tích hợp native runtime.

## Đã có trong V1 này
- Logo Rem Gose (avatar của Rem): icon launcher + roundIcon, header thư viện,
  màn hình thư viện trống, mục Giới thiệu trong Cài đặt
  (file gốc: ../logo/rem-gose-logo.webp, bản trong app: res/drawable/rem_gose_logo.webp)
- Cài đặt đầy đủ (DataStore): chủ đề/sáng-tối/dynamic color, ngôn ngữ
  System/EN/VI/ID/TH/SG/ZH, cỡ chữ, hướng màn hình, giữ sáng màn hình, rung,
  tự động map runtime, danh sách 13 runtime + trạng thái, xoá ZIP sau nhập,
  hỏi trước khi xoá, thống kê dung lượng game/runtime/save/cache + xoá cache,
  đặt lại cài đặt, Developer Mode (chạm 7 lần vào phiên bản), Log Viewer
- Thư viện game Compose (theme Midnight Visual Novel, xem docs/UI-PREVIEW)
- Room DB: games + runtimes (statusPhase/statusMessage như RenDroid)
- GameImportWorker: giải nén ZIP an toàn (chống Zip Slip) -> detect -> wrapper -> READY
- GameDetector: bắt buộc game/, đọc version từ renpy/__init__.py / script_version.txt, icon window_icon.png
- RuntimeCatalog: 13 runtime (7.3.5 -> 8.6.0), auto-adapt + override thủ công
- WrapperGenerator: vá searchpath renpy/common cho game 7.x (bài học từ RenDroid)
- GameRunnerActivity: chạy ở process riêng :game0, save riêng từng game
- Unit test cho logic lõi: app/src/test/.../CoreLogicTest.kt

## Chưa có (V1.1 — bước tích hợp runtime thật)
- Native runtime Ren'Py: cần lấy librenpython (8.x) / bộ SDL2+Python (7.x)
  từ bản build RAPT chính thức của Ren'Py và nối PythonSDLActivity vào
  GameRunnerActivity (đã để sẵn TODO đúng vị trí)
- Tải runtime on-demand từ renpy.org (RuntimeInstallWorker đã có khung)
- Nhập game từ thư mục (hiện mới có ZIP)

## Cách build APK
Môi trường build trên máy của Rem đã có: Temurin OpenJDK 21
(`~/tools/jdk`), Android SDK 35 + build-tools 35.0.0 (`~/tools/android-sdk`),
Gradle 8.10.2 (`~/tools/gradle`). Gradle chạy in-process (môi trường chặn
kết nối TCP giữa các tiến trình) và dùng kho thư viện tải sẵn ở `~/.m2`
(`mavenLocal()` đặt trước trong `settings.gradle.kts`).
Build thử đầu tiên (19/11/2026): đang tải thư viện bằng script
`~/tools/resolve_deps.py` rồi build offline.
Trên máy tính của Anh Quảng:
1. Cài Android Studio (kèm JDK 17/21 + Android SDK 35)
2. Mở thư mục android-player/ này trong Android Studio
3. Sync Gradle -> Run (máy ảo/điện thoại thật) hoặc Build > Build APK(s)
4. Chạy test logic: ./gradlew testDebugUnitTest

Yêu cầu máy chạy app: Android 7.0+ (minSdk 24), OpenGL ES 3.0.
