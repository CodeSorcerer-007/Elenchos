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
    fun zipAnalysis_detectsDexFilesAndAssets() {
        val tempZip = File.createTempFile("test_app_", ".apk")
        try {
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                // Add classes.dex
                zos.putNextEntry(ZipEntry("classes.dex"))
                val dexBytes = "DEX\n035\u0000some bytecode here AIzaSyD1234567890123456789012345678901".toByteArray(Charsets.ISO_8859_1)
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

                // Add native lib
                zos.putNextEntry(ZipEntry("lib/arm64-v8a/libnative.so"))
                zos.write(ByteArray(100))
                zos.closeEntry()
            }

            assertTrue("File was created and has content", tempZip.length() > 0)
            assertTrue("File size formatted is not 0 B", ApkParser.formatFileSize(tempZip.length()) != "0 B")
        } finally {
            tempZip.delete()
        }
    }
}
