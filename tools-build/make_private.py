#!/usr/bin/env python3
"""
Dung assets/private.mp3 cho runtime Ren'Py bundle san trong Rem Gose.

Cau truc private (theo RAPT chinh thuc, doc docs/TICH-HOP-RUNTIME-V1.1.md):
  main.py          <- SDK renpy.py, tuy bien path_to_gamedir/path_to_saves
  renpy/           <- SDK renpy/ (gom ca common)
  lib/python3.12/  <- SDK lib/python3.12/

main.py tuy bien: doc file remgose_game.txt (Java ghi vao filesDir truoc khi chay):
  dong 1 = duong dan thu muc game/ cua game dang chon
  dong 2 = duong dan thu muc save rieng cua game do

Chay: python3 make_private.py <sdk_dir> <version>
VD:   python3 make_private.py ~/workspace/rem-gose/runtimes-src/renpy-8.5.2-sdk 8.5.2
"""
import hashlib
import os
import shutil
import sys
import tarfile

PATCH_GAMEDIR = '''
    # Rem Gose: the Java side writes remgose_game.txt next to main.py,
    # line 1 is the selected game's "game" directory.
    try:
        _rg = os.path.join(os.path.dirname(os.path.abspath(__file__)), "remgose_game.txt")
        with open(_rg, "r") as _f:
            _lines = _f.read().splitlines()
        if _lines and os.path.isdir(_lines[0]):
            return _lines[0]
    except Exception:
        pass

    # A list of candidate game directory names.'''

PATCH_SAVES = '''    # Android.
    if renpy.android:
        # Rem Gose: per-game save directory (line 2 of remgose_game.txt).
        try:
            _rg = os.path.join(os.path.dirname(os.path.abspath(__file__)), "remgose_game.txt")
            with open(_rg, "r") as _f:
                _lines = _f.read().splitlines()
            if len(_lines) > 1 and _lines[1]:
                os.makedirs(_lines[1], exist_ok=True)
                return _lines[1]
        except Exception:
            pass

        paths = ['''


def main():
    sdk = os.path.expanduser(sys.argv[1])
    version = sys.argv[2]
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    work = os.path.join(root, "build-private", version)
    assets_dir = os.path.join(root, "android-player", "app", "src", "main", "assets")
    res_dir = os.path.join(root, "android-player", "app", "src", "main", "res", "values")

    if os.path.exists(work):
        shutil.rmtree(work)
    os.makedirs(work)

    # 1) main.py tuy bien
    with open(os.path.join(sdk, "renpy.py"), "r", encoding="utf-8") as f:
        main_py = f.read()
    assert "    # A list of candidate game directory names." in main_py
    main_py = main_py.replace(
        "    # A list of candidate game directory names.", PATCH_GAMEDIR, 1)
    assert "    # Android.\n    if renpy.android:\n        paths = [" in main_py
    main_py = main_py.replace(
        "    # Android.\n    if renpy.android:\n        paths = [", PATCH_SAVES, 1)
    with open(os.path.join(work, "main.py"), "w", encoding="utf-8") as f:
        f.write(main_py)
    print("main.py patched OK")

    # 2) renpy/ va lib/python3.12/
    def ignore_pycache(dirpath, names):
        return [n for n in names if n == "__pycache__"]

    shutil.copytree(os.path.join(sdk, "renpy"), os.path.join(work, "renpy"),
                    ignore=ignore_pycache)
    shutil.copytree(os.path.join(sdk, "lib", "python3.12"),
                    os.path.join(work, "lib", "python3.12"),
                    ignore=ignore_pycache)
    total = sum(os.path.getsize(os.path.join(dp, fn))
                for dp, _, fns in os.walk(work) for fn in fns)
    print(f"private dir: {total / 1e6:.1f} MB")

    # 3) tar.gz (GNU format nhu RAPT make_tar) -> assets/private.mp3
    os.makedirs(assets_dir, exist_ok=True)
    mp3_path = os.path.join(assets_dir, "private.mp3")
    with tarfile.open(mp3_path, "w:gz", format=tarfile.GNU_FORMAT) as tf:
        for name in sorted(os.listdir(work)):
            tf.add(os.path.join(work, name), arcname=name)
    with open(mp3_path, "rb") as f:
        md5 = hashlib.md5(f.read()).hexdigest()
    print(f"private.mp3: {os.path.getsize(mp3_path) / 1e6:.1f} MB, md5={md5}")

    # 4) string resource private_version (thieu string nay app se khong giai nen!)
    os.makedirs(res_dir, exist_ok=True)
    with open(os.path.join(res_dir, "renpy_runtime.xml"), "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<resources>\n'
                f'    <string name="private_version">{md5}</string>\n'
                '</resources>\n')
    print("renpy_runtime.xml written")


if __name__ == "__main__":
    main()
