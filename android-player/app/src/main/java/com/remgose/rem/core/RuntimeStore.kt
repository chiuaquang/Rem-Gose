package com.remgose.rem.core

import android.os.Build
import java.io.File

/**
 * Noi luu runtime TAI RIENG tung ban tu renpy.org (filesDir/runtimes/<version>/):
 *   lib/<abi>/librenpython.so  - thu vien native cua dung ban do (tu goi RAPT)
 *   private/                   - main.py (da va) + renpy/ + lib/pythonXX/ (tu SDK)
 * Engine se nap .so nay + dung private nay thay cho ban bundle san 8.5.2.
 */
object RuntimeStore {
    fun runtimeDir(filesDir: File, version: String): File = File(filesDir, "runtimes/$version")
    fun privateDir(filesDir: File, version: String): File = File(runtimeDir(filesDir, version), "private")
    fun soFile(filesDir: File, version: String, abi: String): File =
        File(runtimeDir(filesDir, version), "lib/$abi/librenpython.so")

    /** ABI chinh cua may (arm64-v8a tren hau het dien thoai hien nay). */
    fun deviceAbi(): String =
        runCatching { Build.SUPPORTED_ABIS.firstOrNull() }.getOrNull() ?: "arm64-v8a"

    /** Runtime da tai du ca .so lan private bundle thi moi coi la san sang. */
    fun isReady(filesDir: File, version: String): Boolean =
        soFile(filesDir, version, deviceAbi()).isFile &&
            File(privateDir(filesDir, version), "main.py").isFile

    fun sizeBytes(filesDir: File, version: String): Long =
        GameDataManager.sizeOf(runtimeDir(filesDir, version))
}
