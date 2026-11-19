package com.remgose.rem.core

/**
 * Danh muc runtime Rem Gose ho tro.
 * Hoc tu RenDroid: runtime tach theo phien ban, game map sang runtime gan nhat.
 * Ban hien tai bundle san runtime 8.5.2 (engine Ren'Py chinh thuc trong APK);
 * game 8.x chay bang engine nay nho Ren'Py tuong thich tien trong cung major.
 * Runtime tai them on-demand tu renpy.org se bo sung sau (V1.2).
 */
data class RuntimeSpec(
    val version: RenPyVersion,
    val moduleName: String,
    val inBundle: Boolean,
    val downloadUrl: String? = null,
)

object RuntimeCatalog {
    val specs: List<RuntimeSpec> = listOf(
        "7.3.5", "7.4.11", "7.5.3", "7.6.3", "7.7.3", "7.8.7",
        "8.0.3", "8.1.3", "8.2.3", "8.3.7", "8.4.1", "8.5.2", "8.6.0",
    ).map { v ->
        val ver = RenPyVersion.parse(v)!!
        RuntimeSpec(
            version = ver,
            moduleName = "runtime_" + v.replace('.', '_'),
            inBundle = v == BUNDLED_VERSION,
            downloadUrl = "https://www.renpy.org/dl/$v/",
        )
    }

    /**
     * Auto-adapt: chon runtime cho 1 game.
     * - Trung khop tuyet doi neu co.
     * - Cung major: runtime cao nhat nhung khong vuot qua ban game qua 1 minor
     *   (game cu chay tren runtime moi hon 1 chut thuong on; nhay qua xa de loi script).
     * - Khong co cung major: khong tu doan - tra null de UI bao nguoi dung chon tay.
     */
    fun autoAdapt(gameVersion: RenPyVersion?): RuntimeSpec? {
        if (gameVersion == null) return null
        specs.firstOrNull { it.version == gameVersion }?.let { return it }
        val sameMajor = specs.filter { it.version.major == gameVersion.major }
        if (sameMajor.isEmpty()) return null
        // Uu tien cung minor truoc (an toan nhat cho script game), roi moi toi minor +1
        val sameMinor = sameMajor.filter { it.version.minor == gameVersion.minor }
        if (sameMinor.isNotEmpty()) return sameMinor.maxByOrNull { it.version }
        val notTooNew = sameMajor.filter { it.version.minor <= gameVersion.minor + 1 }
        return (notTooNew.maxByOrNull { it.version } ?: sameMajor.minByOrNull { it.version })
    }

    fun byVersion(version: String?): RuntimeSpec? =
        specs.firstOrNull { it.version.toString() == version }

    /** Runtime duoc bundle san trong APK (engine that, chay duoc ngay). */
    const val BUNDLED_VERSION = "8.5.2"

    fun bundledSpec(): RuntimeSpec = byVersion(BUNDLED_VERSION)!!

    /**
     * Engine thuc te se chay 1 game:
     * - Trung runtime bundle -> chinh no.
     * - Cung major voi runtime bundle -> runtime bundle (Ren'Py 8.x tuong
     *   thich tien: game 8.0-8.6 chay tren engine 8.5.2).
     * - Khac major (game 7.x) -> chua co engine, tra null.
     */
    fun engineFor(gameVersion: RenPyVersion?): RuntimeSpec? {
        if (gameVersion == null) return null
        val bundled = bundledSpec()
        return if (gameVersion.major == bundled.version.major) bundled else null
    }
}
