package com.example.elenchos.data.apk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkParserTest {

    @Test
    fun formatFileSize_formatsBytesCorrectly() {
        assertEquals("0 B", ApkParser.formatFileSize(0))
        assertEquals("0 B", ApkParser.formatFileSize(-10))
        assertEquals("512 B", ApkParser.formatFileSize(512))
        assertEquals("2.0 KB", ApkParser.formatFileSize(2048))
        assertEquals("15.0 MB", ApkParser.formatFileSize(15 * 1024 * 1024))
        assertEquals("2.50 GB", ApkParser.formatFileSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun zipAnalysis_detectsDexFilesAssetsAndSecrets() {
        val tempZip = File.createTempFile("test_app_", ".apk")
        try {
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                // Add classes.dex with a secret pattern (AIzaSy...)
                zos.putNextEntry(ZipEntry("classes.dex"))
                val dexBytes = "DEX\n035\u0000some bytecode here AIzaSyD12345678901234567890123456789012".toByteArray(Charsets.ISO_8859_1)
                zos.write(dexBytes)
                zos.closeEntry()

                // Add classes2.dex
                zos.putNextEntry(ZipEntry("classes2.dex"))
                zos.write("DEX\n035\u0000more classes".toByteArray(Charsets.ISO_8859_1))
                zos.closeEntry()

                // Add asset
                zos.putNextEntry(ZipEntry("assets/logo.png"))
                zos.write(byteArrayOf(1, 2, 3, 4))
                zos.closeEntry()

                // Add native lib (default ZipOutputStream will deflate it)
                zos.putNextEntry(ZipEntry("lib/arm64-v8a/libnative.so"))
                zos.write(ByteArray(100))
                zos.closeEntry()
            }

            val analysis = ApkParser.analyzeZipStructure(tempZip)
            assertEquals(2, analysis.dexCount)
            assertEquals(1, analysis.assetsCount)
            assertTrue("arm64-v8a architecture detected", analysis.architectures.contains("arm64-v8a"))
            assertTrue("Detected secrets should contain Google API key match", analysis.detectedSecrets.any { it.contains("AIza") })

            // Because the .so is compressed (DEFLATED), 16KB alignment verification must fail
            val isAligned = ApkParser.verify16KbAlignment(tempZip)
            org.junit.Assert.assertFalse("Compressed .so must not be 16KB aligned", isAligned)
        } finally {
            tempZip.delete()
        }
    }

    @Test
    fun verify16KbAlignment_returnsTrue_whenNoNativeLibs() {
        val tempZip = File.createTempFile("test_pure_java_", ".apk")
        try {
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write("DEX\n035\u0000dummy dex".toByteArray(Charsets.ISO_8859_1))
                zos.closeEntry()
            }

            val isAligned = ApkParser.verify16KbAlignment(tempZip)
            assertTrue("APKs without native libraries are considered aligned", isAligned)
        } finally {
            tempZip.delete()
        }
    }

    @Test
    fun parseAxmlCleartextTraffic_returnsNullOnInvalidAxml() {
        val invalidBytes = ByteArray(20) { 0 }
        val result = ApkParser.parseAxmlCleartextTraffic(invalidBytes)
        org.junit.Assert.assertNull("Invalid AXML header should return null", result)
    }
}
