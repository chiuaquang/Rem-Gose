package com.remgose.rem.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CoreLogicTest {
    @Test fun parseVersion() {
        assertEquals(RenPyVersion(8, 3, 7), RenPyVersion.parse("Ren'Py 8.3.7"))
        assertEquals(RenPyVersion(7, 3, 5), RenPyVersion.parse("7.3.5.606"))
        assertNull(RenPyVersion.parse("khong co so"))
    }
    @Test fun autoAdaptExactAndNear() {
        assertEquals("8.3.7", RuntimeCatalog.autoAdapt(RenPyVersion(8, 3, 7))?.version.toString())
        // Game 8.3.0 -> runtime 8.3.7 (cung minor, cao nhat khong qua +1 minor)
        assertEquals("8.3.7", RuntimeCatalog.autoAdapt(RenPyVersion(8, 3, 0))?.version.toString())
        // Game 6.x khong co cung major -> null de nguoi dung chon tay
        assertNull(RuntimeCatalog.autoAdapt(RenPyVersion(6, 99, 0)))
    }
    @Test fun engineForUsesBundledForAll8x() {
        // Moi game 8.x chay bang engine bundle 8.5.2
        assertEquals("8.5.2", RuntimeCatalog.engineFor(RenPyVersion(8, 3, 7))?.version.toString())
        assertEquals("8.5.2", RuntimeCatalog.engineFor(RenPyVersion(8, 5, 2))?.version.toString())
        assertEquals("8.5.2", RuntimeCatalog.engineFor(RenPyVersion(8, 0, 3))?.version.toString())
        // Game 7.x chua co engine -> null; version null -> null
        assertNull(RuntimeCatalog.engineFor(RenPyVersion(7, 4, 11)))
        assertNull(RuntimeCatalog.engineFor(null))
        assertTrue(RuntimeCatalog.bundledSpec().inBundle)
    }
    @Test fun detectVersionFromVcVersionPy() {
        // Ren'Py moi (>= 8.2) ghi version trong renpy/vc_version.py dang version = '8.5.2.26010301'
        val root = Files.createTempDirectory("rgvc").toFile()
        val renpyDir = File(root, "renpy").apply { mkdirs() }
        val gameDir = File(root, "game").apply { mkdirs() }
        File(renpyDir, "vc_version.py").writeText(
            "branch = 'fix'\nnightly = False\nofficial = True\nversion = '8.3.7.25031701'\nversion_name = 'X'\n")
        assertEquals(RenPyVersion(8, 3, 7), GameDetector.detectVersion(renpyDir, gameDir))
        // Ban cu: version_tuple literal trong __init__.py
        File(renpyDir, "vc_version.py").delete()
        File(renpyDir, "__init__.py").writeText("version_tuple = (7, 4, 11, 0)\n")
        assertEquals(RenPyVersion(7, 4, 11), GameDetector.detectVersion(renpyDir, gameDir))
    }
    @Test fun runtimeStoreReadyNeedsSoAndMainPy() {
        val filesDir = Files.createTempDirectory("rgrt").toFile()
        assertFalse(RuntimeStore.isReady(filesDir, "8.3.7"))
        val so = RuntimeStore.soFile(filesDir, "8.3.7", RuntimeStore.deviceAbi())
        so.parentFile?.mkdirs(); so.writeBytes(byteArrayOf(1))
        assertFalse(RuntimeStore.isReady(filesDir, "8.3.7")) // chua co private/main.py
        val mainPy = File(RuntimeStore.privateDir(filesDir, "8.3.7"), "main.py")
        mainPy.parentFile?.mkdirs(); mainPy.writeText("# main")
        assertTrue(RuntimeStore.isReady(filesDir, "8.3.7"))
    }
    @Test fun detectRequiresGameDir() {
        val root = Files.createTempDirectory("rg").toFile()
        assertTrue(GameDetector.detect(root).isFailure)
        File(root, "game").mkdirs()
        File(root, "renpy").mkdirs()
        File(root, "renpy/__init__.py").writeText("version_tuple = (8, 2, 3, 0)\n")
        val d = GameDetector.detect(root).getOrThrow()
        assertEquals(RenPyVersion(8, 2, 3), d.renPyVersion)
        assertTrue(d.hasRenPyDir)
    }
    @Test fun wrapperIsIdempotent() {
        val root = Files.createTempDirectory("rgw").toFile()
        File(root, "main.py").writeText("print('hello')\n")
        assertTrue(WrapperGenerator.prepare(root))
        val first = File(root, "main.py").readText()
        assertTrue(first.startsWith(WrapperGenerator.WRAPPER_MARKER))
        assertTrue(WrapperGenerator.prepare(root))
        assertEquals(first, File(root, "main.py").readText())
        assertEquals("print('hello')\n", File(root, "_renpy_main.py").readText())
    }
}
