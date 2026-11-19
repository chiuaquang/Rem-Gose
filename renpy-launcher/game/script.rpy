# Rem Gose - Game Ren'Py mẫu để test đóng gói Android
# File này chạy được ngay trong Ren'Py Launcher

define e = Character("Rem Gose", color="#7aa2ff")

label start:
    scene bg room
    show eileen happy

    e "Xin chào Anh Quảng! Đây là Rem Gose."
    e "Ứng dụng chơi game Ren'Py trên Android đang được dựng từ bản mẫu này."
    e "Nếu anh thấy dòng này trên điện thoại, nghĩa là APK đã build thành công."

    menu:
        "Test lưu game":
            e "Anh bấm Save trong menu để test lưu/tải game nhé."
        "Kết thúc":
            e "Hẹn gặp lại Anh Quảng!"

    return
