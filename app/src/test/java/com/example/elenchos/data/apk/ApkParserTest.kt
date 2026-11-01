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

    private fun writeIntLE(bos: java.io.ByteArrayOutputStream, v: Int) {
        bos.write(v and 0xFF)
        bos.write((v shr 8) and 0xFF)
        bos.write((v shr 16) and 0xFF)
        bos.write((v shr 24) and 0xFF)
    }

    private fun writeShortLE(bos: java.io.ByteArrayOutputStream, v: Int) {
        bos.write(v and 0xFF)
        bos.write((v shr 8) and 0xFF)
    }

    private fun buildAxmlWithCleartext(isUtf8: Boolean, cleartextEnabled: Boolean, includeCleartextString: Boolean = true): ByteArray {
        val stringPoolData = java.io.ByteArrayOutputStream()
        val strings = if (includeCleartextString) {
            listOf("application", "usesCleartextTraffic")
        } else {
            listOf("application", "someOtherAttribute")
        }

        val offsets = mutableListOf<Int>()
        for (str in strings) {
            offsets.add(stringPoolData.size())
            if (isUtf8) {
                val bytes = str.toByteArray(Charsets.UTF_8)
                stringPoolData.write(bytes.size)
                stringPoolData.write(bytes.size)
                stringPoolData.write(bytes)
                stringPoolData.write(0)
            } else {
                writeShortLE(stringPoolData, str.length)
                for (ch in str) {
                    writeShortLE(stringPoolData, ch.code)
                }
                writeShortLE(stringPoolData, 0)
            }
        }

        val stringCount = strings.size
        val offsetsBytesSize = stringCount * 4
        val stringsStartOffset = 28 + offsetsBytesSize
        val stringChunkSize = stringsStartOffset + stringPoolData.size()

        val stringChunk = java.io.ByteArrayOutputStream()
        writeIntLE(stringChunk, 0x001C0001) // String pool type
        writeIntLE(stringChunk, stringChunkSize)
        writeIntLE(stringChunk, stringCount)
        writeIntLE(stringChunk, 0) // style count
        writeIntLE(stringChunk, if (isUtf8) (1 shl 8) else 0) // flags
        writeIntLE(stringChunk, stringsStartOffset)
        writeIntLE(stringChunk, 0) // stylesStart
        for (off in offsets) {
            writeIntLE(stringChunk, off)
        }
        stringChunk.write(stringPoolData.toByteArray())

        // Build Start Element with attribute
        val elementChunk = java.io.ByteArrayOutputStream()
        writeIntLE(elementChunk, 0x00100102) // START_ELEMENT
        val attrSize = 20
        val attrCount = 1
        val elementHeaderSize = 36
        val totalElementChunkSize = elementHeaderSize + (attrCount * attrSize)
        writeIntLE(elementChunk, totalElementChunkSize)
        writeIntLE(elementChunk, 1) // line
        writeIntLE(elementChunk, -1) // comment
        writeIntLE(elementChunk, -1) // ns
        writeIntLE(elementChunk, 0) // name = "application"
        writeShortLE(elementChunk, elementHeaderSize) // attrStart
        writeShortLE(elementChunk, attrSize) // attrSize
        writeShortLE(elementChunk, attrCount) // attrCount
        writeShortLE(elementChunk, 0) // idIndex
        writeShortLE(elementChunk, 0) // classIndex
        writeShortLE(elementChunk, 0) // styleIndex

        // Attribute: ns(-1), name(index 1 = "usesCleartextTraffic"), rawVal(-1), size(8), res(0), type(3), data(1 or 0)
        writeIntLE(elementChunk, -1) // ns
        writeIntLE(elementChunk, 1) // name index
        writeIntLE(elementChunk, -1) // rawValue
        writeShortLE(elementChunk, 8) // size
        elementChunk.write(0) // res0
        elementChunk.write(3) // type
        writeIntLE(elementChunk, if (cleartextEnabled) 1 else 0) // typedValData

        // Assemble root AXML
        val axml = java.io.ByteArrayOutputStream()
        val totalFileSize = 8 + stringChunk.size() + elementChunk.size()
        writeIntLE(axml, 0x00080003) // AXML magic
        writeIntLE(axml, totalFileSize)
        axml.write(stringChunk.toByteArray())
        axml.write(elementChunk.toByteArray())

        return axml.toByteArray()
    }

    @Test
    fun parseAxmlCleartextTraffic_detectsUtf8CleartextTrue() {
        val bytes = buildAxmlWithCleartext(isUtf8 = true, cleartextEnabled = true)
        val result = ApkParser.parseAxmlCleartextTraffic(bytes)
        org.junit.Assert.assertEquals(true, result)
    }

    @Test
    fun parseAxmlCleartextTraffic_detectsUtf8CleartextFalse() {
        val bytes = buildAxmlWithCleartext(isUtf8 = true, cleartextEnabled = false)
        val result = ApkParser.parseAxmlCleartextTraffic(bytes)
        org.junit.Assert.assertEquals(false, result)
    }

    @Test
    fun parseAxmlCleartextTraffic_detectsUtf16CleartextTrue() {
        val bytes = buildAxmlWithCleartext(isUtf8 = false, cleartextEnabled = true)
        val result = ApkParser.parseAxmlCleartextTraffic(bytes)
        org.junit.Assert.assertEquals(true, result)
    }

    @Test
    fun parseAxmlCleartextTraffic_detectsUtf16CleartextFalse() {
        val bytes = buildAxmlWithCleartext(isUtf8 = false, cleartextEnabled = false)
        val result = ApkParser.parseAxmlCleartextTraffic(bytes)
        org.junit.Assert.assertEquals(false, result)
    }

    @Test
    fun parseAxmlCleartextTraffic_returnsNullWhenAttributeMissing() {
        val bytes = buildAxmlWithCleartext(isUtf8 = true, cleartextEnabled = true, includeCleartextString = false)
        val result = ApkParser.parseAxmlCleartextTraffic(bytes)
        org.junit.Assert.assertNull(result)
    }

    @Test
    fun parseAxmlCleartextTraffic_handlesMalformedTruncatedSafely() {
        val bytes = buildAxmlWithCleartext(isUtf8 = true, cleartextEnabled = true)
        val truncated = bytes.copyOf(bytes.size / 2)
        val result = ApkParser.parseAxmlCleartextTraffic(truncated)
        org.junit.Assert.assertNull(result)
    }
}
