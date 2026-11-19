package com.remgose.rem.core

import org.junit.Assert.assertEquals
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
