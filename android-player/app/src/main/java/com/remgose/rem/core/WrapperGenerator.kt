package com.remgose.rem.core

import java.io.File

/**
 * Wrapper main.py - bai hoc quan trong nhat tu RenDroid.
 * Game Ren'Py 7.x tren Android hay loi khong tim thay renpy/common vi
 * searchpath cua engine chi tro vao thu muc game. Wrapper nay:
 * 1. Doi main.py goc cua game thanh _renpy_main.py (chi lam 1 lan)
 * 2. Ghi main.py moi: cai import hook va searchpath cho renpy/common,
 *    roi execfile vao main goc.
 * Chi ap dung khi game co main.py o root (game dong goi kieu cu).
 * Game khong co main.py thi bo qua - engine tu nap game/ truc tiep.
 */
object WrapperGenerator {
    const val WRAPPER_MARKER = "# Rem Gose wrapper"

    fun prepare(gameRoot: File): Boolean {
        val mainPy = File(gameRoot, "main.py")
        if (!mainPy.isFile) return false
        if (mainPy.readText().startsWith(WRAPPER_MARKER)) return true
        val original = File(gameRoot, "_renpy_main.py")
        if (!mainPy.renameTo(original)) return false
        mainPy.writeText(wrapperSource())
        return true
    }

    fun wrapperSource(): String = """
        |$WRAPPER_MARKER
        |# Rem Gose wrapper - Ren'Py Android searchpath fix for renpy/common
        |import os, sys
        |_rg_game_dir = os.path.dirname(os.path.abspath(__file__))
        |_rg_common = os.path.join(_rg_game_dir, "renpy", "common")
        |if "ANDROID_PRIVATE" in os.environ and os.path.isdir(_rg_common):
        |    class _RemGoseSearchPathHook(object):
        |        "Import hook: patches renpy.loader so renpy/common is scanned."
        |        def find_module(self, name, path=None):
        |            return self if name == "renpy.loader" else None
        |        def load_module(self, name):
        |            import importlib, sys as _s
        |            _s.meta_path.remove(self)
        |            mod = importlib.import_module(name)
        |            try:
        |                import renpy
        |                renpy.config.commondir = _rg_common
        |                if _rg_common not in renpy.config.searchpath:
        |                    renpy.config.searchpath.append(_rg_common)
        |            except Exception:
        |                pass
        |            return mod
        |    sys.meta_path.insert(0, _RemGoseSearchPathHook())
        |exec(compile(open(os.path.join(_rg_game_dir, "_renpy_main.py"), "rb").read(),
        |             "_renpy_main.py", "exec"), {"__file__": os.path.join(_rg_game_dir, "_renpy_main.py")})
        |""".trimMargin()
}
