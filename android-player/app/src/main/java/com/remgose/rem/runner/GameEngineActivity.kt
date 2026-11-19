package com.remgose.rem.runner

import org.renpy.android.PythonSDLActivity

/**
 * Activity chay game that: ke thua PythonSDLActivity chinh thuc cua Ren'Py
 * (ma nguon mo, lay tu goi RAPT 8.5.2 o renpy.org).
 *
 * Khi mo, no tu giai nen assets/private.mp3 (engine Python + renpy package)
 * vao filesDir, set ANDROID_PRIVATE, roi chay main.py o do. main.py da duoc
 * Rem Gose tuy bien (tools-build/make_private.py) de doc remgose_game.txt
 * ma GameRunnerActivity ghi san - nho vay engine nap dung game nguoi dung chon
 * va luu save vao thu muc rieng cua tung game.
 */
class GameEngineActivity : PythonSDLActivity()
