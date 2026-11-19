# Tích hợp runtime Ren'Py — V1.1 (nghiên cứu từ RAPT + SDK 8.5.2 chính thức)

Nguồn sự thật: gói `renpy-8.5.2-rapt.zip` và `renpy-8.5.2-sdk.tar.bz2` tải từ
https://www.renpy.org/dl/8.5.2/ (mã nguồn mở MIT) — lưu ở `~/workspace/rem-gose/runtimes-src/`.

## Cơ chế chạy game của Ren'Py Android (bản chính thức)

1. `librenpython.so` (35MB arm64 / 31MB v7a / 38MB x86_64, trong
   `rapt/prototype/renpyandroid/src/main/jniLibs/`) chạy file `main.py` nằm ở
   `ANDROID_PRIVATE` (= filesDir của app).
2. Khi activity `org.renpy.android.PythonSDLActivity` khởi động, `preparePython()`:
   - Giải nén asset `private.mp3` (thực chất là tar.gz) vào filesDir — CHỈ khi
     string resource `private_version` khác với file `private.version` trên đĩa.
     **Thiếu string này = không giải nén gì cả.**
   - Set env: ANDROID_PRIVATE=filesDir, ANDROID_PUBLIC=getExternalFilesDir,
     ANDROID_OLD_PUBLIC, ANDROID_APK.
3. `main.py` chính là `renpy.py` của SDK. Các hàm `path_to_gamedir`,
   `path_to_saves`, `path_to_common` trong đó được ghi rõ là
   *"Functions to be customized by distributors"* — điểm tùy biến chính thức.
   - Mặc định: basedir = thư mục chứa main.py; gamedir = `<basedir>/game`.
   - Save trên Android mặc định: ANDROID_OLD_PUBLIC/game/saves →
     ANDROID_PRIVATE/saves → ANDROID_PUBLIC/saves (dùng chung mọi game — tệ).
   - commondir = `<renpy_base>/renpy/common`.
4. RAPT đóng gói: `private.mp3` = tar của (main.py + `renpy/` trừ common +
   `lib/`) — nhưng Rem Gose GIỮ cả `renpy/common` trong private cho đơn giản,
   để commondir chạy đúng mặc định.
   `lib/` cho Android = `lib/python3.12/` của SDK (theo `renpy/common/00build.rpy`:
   pattern `lib/**` → "windows linux mac android ios", các thư mục py3-* của
   desktop thuộc list riêng của từng hệ).

## Thiết kế Rem Gose V1.1

- Bundle sẵn runtime **8.5.2** trong APK (jniLibs arm64-v8a + armeabi-v7a;
  bỏ x86_64 để tiết kiệm 38MB).
- `assets/private.mp3`: tar.gz gồm:
  - `main.py` = renpy.py của SDK, tùy biến 2 hàm:
    - `path_to_gamedir`: nếu có file `remgose_game.txt` trong ANDROID_PRIVATE
      thì trả về dòng 1 (đường dẫn thư mục `game/` của game đang chọn).
    - `path_to_saves` (nhánh Android): trả về dòng 2 của file đó
      (`<gameRoot>/game/saves` — trùng layout GameDataManager đang quản lý).
  - `renpy/` đầy đủ của SDK (gồm common), bỏ `__pycache__`.
  - `lib/python3.12/` của SDK.
- `res/values/renpy_runtime.xml`: `<string name="private_version">` = md5 của
  private.mp3 (đổi mỗi khi bundle đổi để app tự giải nén lại).
- Mã Java copy nguyên trạng từ RAPT (MIT): `org.renpy.android`,
  `org.libsdl.app` (bỏ file .orig), `org.jnius`, `org.kamranzafar.jtar`.
  Deps: play asset-delivery 2.3.0 (chỉ để biên dịch — app không dùng asset pack),
  slf4j nếu có dùng, androidx.activity đã có.
- `GameRunnerActivity` (Kotlin) kế thừa `PythonSDLActivity`:
  - `onCreate`: TRƯỚC `super.onCreate`, ghi `filesDir/remgose_game.txt`
    (gamedir + savedir của game được chọn qua Intent extra).
  - Manifest: process `:game0`, configChanges theo template RAPT,
    theme fullscreen, exported=false.
- RuntimeCatalog: `inBundle` = {8.5.2}. Game 8.x → chạy bằng 8.5.2
  (Ren'Py tương thích tiến trong cùng major). Game 7.x: báo rõ cần runtime 7.x
  (V1.2 sẽ tải thêm runtime như RenDroid).

## V1.2+ (đã có hướng)
- Runtime tải thêm: mỗi runtime = 1 bộ (librenpython.so theo ABI + private.mp3)
  lưu ở `filesDir/runtimes/<ver>/`; cần fork PythonSDLActivity để
  unpackData trỏ vào thư mục riêng + nạp .so tương ứng (System.load theo đường
  dẫn) — phức tạp hơn, làm sau khi V1.1 chạy ổn.
- Nhiều slot chạy game (:game0..3) như RenDroid.
