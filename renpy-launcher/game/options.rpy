## Rem Gose - Cấu hình

define config.name = _("Rem Gose")
define config.version = "1.0.0"
define build.name = "RemGose"

define config.save_directory = "RemGose-1.0"

define config.window_title = "Rem Gose"

init python:
    # Android: package đề xuất com.remgose.rem
    # Build bằng Ren'Py Launcher > Android > Configure + Build
    # Cần: JDK 21, Android SDK (cài qua Launcher), RAPT
    build.android_permissions = ["INTERNET"]
