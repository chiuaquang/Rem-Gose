# Kế hoạch V1 - Rem Gose

## Mục tiêu V1
App Android tên Rem Gose, icon riêng, mở lên có thư viện game,
nhập được 1 game Ren'Py từ ZIP, nhận diện đúng/sai, và có nút Chơi.

## Việc cần làm theo thứ tự
1. Khung Android Kotlin: 1 Activity, màn Thư viện + màn Chi tiết game
2. Chọn file ZIP / thư mục bằng Storage Access Framework (Android 11+ không dùng quyền cũ)
3. Kiểm tra game: phải có thư mục game/, có file .rpy hoặc .rpa, đọc phiên bản nếu có
4. Giải nén vào thư mục riêng của app, chống Zip Slip
5. Lưu danh sách game (Room DB): tên, đường dẫn, bìa, ngày nhập, dung lượng
6. Màn Chơi: giai đoạn 1 mở bằng runtime đóng gói sẵn / giai đoạn 2 tích hợp Ren'Py 8.x
7. Test trên máy thật của Anh Quảng, build APK debug để cài thử
8. Build APK release ký bằng keystore riêng (giữ keystore cẩn thận, mất là không update được)

## Rủi ro đã biết
- Game Ren'Py cũ (7.x) và mới (8.x) khác runtime, player phải bundle nhiều runtime hoặc chỉ hỗ trợ 8.x ở V1
- Một số game dùng Python/custom code sẽ lỗi trên Android dù đóng gói chính thức cũng lỗi tương tự
- Game >2GB không đóng APK trực tiếp được theo giới hạn của Ren'Py/RAPT
