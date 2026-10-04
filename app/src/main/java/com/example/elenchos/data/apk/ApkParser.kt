package com.example.elenchos.data.apk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.util.Log
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkParser {

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.CAMERA",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.BODY_SENSORS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.RECEIVE_MMS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_MEDIA_LOCATION",
        "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.WRITE_SETTINGS"
    )

    private val SECRET_PATTERNS = listOf(
        Regex("(?i)AIza[0-9A-Za-z\\-_]{35}"), // Google API Key
        Regex("(?i)AKIA[0-9A-Z]{16}"),         // AWS Access Key ID
        Regex("(?i)ghp_[0-9a-zA-Z]{36}"),     // GitHub Personal Access Token
        Regex("-----BEGIN (RSA |EC )?PRIVATE KEY-----"),
        Regex("(?i)https?://(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2):[0-9]+") // Localhost debug endpoints
    )

    fun parseApk(context: Context, apkFile: File): APKArtifact {
        val sha256 = calculateSha256(apkFile)
        val fileSizeBytes = apkFile.length()
        val formattedSize = formatFileSize(fileSizeBytes)

        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_SIGNING_CERTIFICATES

        val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: throw IllegalArgumentException("Could not parse APK archive info: ${apkFile.name}")

        val appInfo: ApplicationInfo = packageInfo.applicationInfo ?: ApplicationInfo().apply {
            packageName = packageInfo.packageName
        }
        appInfo.sourceDir = apkFile.absolutePath
        appInfo.publicSourceDir = apkFile.absolutePath

        val appName = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.d("ApkParser", "Could not extract application label, falling back to file name: ${e.message}")
            apkFile.nameWithoutExtension
        }

        val packageName = packageInfo.packageName.ifEmpty { apkFile.nameWithoutExtension }
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appInfo.minSdkVersion
        } else {
            21
        }
        val targetSdk = appInfo.targetSdkVersion

        // Security flags
        val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val allowsBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0

        val manifestExplicitCleartext = inspectManifestCleartext(apkFile)
        val usesCleartextTraffic = if (manifestExplicitCleartext != null) {
            manifestExplicitCleartext
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdk < 28) {
                // Prior to Android 9 (API 28), cleartext was allowed by default
                (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0 || true
            } else {
                (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
            }
        } else {
            targetSdk < 28
        }

        // Permissions
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val dangerousPermissions = permissions.filter { DANGEROUS_PERMISSIONS.contains(it) }

        // Components
        val activities = packageInfo.activities?.map {
            ComponentInfo(
                name = it.name,
                isExported = it.exported,
                permission = it.permission
            )
        } ?: emptyList()

        val services = packageInfo.services?.map {
            ComponentInfo(
                name = it.name,
                isExported = it.exported,
                permission = it.permission
            )
        } ?: emptyList()

        val receivers = packageInfo.receivers?.map {
            ComponentInfo(
                name = it.name,
                isExported = it.exported,
                permission = it.permission
            )
        } ?: emptyList()

        val providers = packageInfo.providers?.map {
            ComponentInfo(
                name = it.name,
                isExported = it.exported,
                permission = it.readPermission ?: it.writePermission
            )
        } ?: emptyList()

        // Certificate Fingerprint
        val certFingerprint = extractCertificateFingerprint(packageInfo)

        // Check if currently installed
        val isInstalled = try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            Log.d("ApkParser", "Package $packageName is not currently installed: ${e.message}")
            false
        }

        // Deep ZIP analysis: Native libraries, DEX count, assets, and secrets
        val zipAnalysis = analyzeZipStructure(apkFile)

        return APKArtifact(
            id = UUID.randomUUID().toString(),
            fileName = apkFile.name,
            filePath = apkFile.absolutePath,
            sha256 = sha256,
            fileSizeBytes = fileSizeBytes,
            formattedSize = formattedSize,
            appName = if (appName.isNotBlank()) appName else packageName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            compileSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) appInfo.compileSdkVersion else 35,
            importedTimestamp = System.currentTimeMillis(),
            isInstalledOnDevice = isInstalled,
            isDebuggable = isDebuggable,
            allowsBackup = allowsBackup,
            usesCleartextTraffic = usesCleartextTraffic,
            permissions = permissions,
            dangerousPermissions = dangerousPermissions,
            activities = activities,
            services = services,
            receivers = receivers,
            providers = providers,
            nativeArchitectures = zipAnalysis.architectures,
            is16KbPageAligned = zipAnalysis.is16KbAligned,
            certificateFingerprint = certFingerprint,
            dexCount = zipAnalysis.dexCount,
            totalAssetsCount = zipAnalysis.assetsCount,
            detectedSecrets = zipAnalysis.detectedSecrets
        )
    }

    internal data class ZipAnalysisResult(
        val architectures: List<String>,
        val is16KbAligned: Boolean,
        val dexCount: Int,
        val assetsCount: Int,
        val detectedSecrets: List<String>
    )

    internal fun analyzeZipStructure(file: File): ZipAnalysisResult {
        val archs = mutableSetOf<String>()
        var dexCount = 0
        var assetsCount = 0
        val secrets = mutableListOf<String>()

        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry: ZipEntry = entries.nextElement()
                    val name = entry.name

                    if (name.startsWith("lib/")) {
                        val parts = name.split("/")
                        if (parts.size >= 2) {
                            archs.add(parts[1])
                        }
                    } else if (name.endsWith(".dex")) {
                        dexCount++
                        // Stream scan DEX strings across chunks with overlap (up to 2MB per DEX)
                        if (secrets.size < 15) {
                            try {
                                zip.getInputStream(entry).use { stream ->
                                    val buffer = ByteArray(131072) // 128KB chunk
                                    var bytesRead: Int
                                    var totalBytesScanned = 0L
                                    val maxScanBytes = 2 * 1024 * 1024L
                                    var carryOver = ""

                                    while (stream.read(buffer).also { bytesRead = it } != -1 &&
                                        totalBytesScanned < maxScanBytes &&
                                        secrets.size < 15
                                    ) {
                                        totalBytesScanned += bytesRead
                                        val chunkStr = carryOver + String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
                                        for (pattern in SECRET_PATTERNS) {
                                            val match = pattern.find(chunkStr)
                                            if (match != null) {
                                                val matchTag = "Pattern match (${pattern.pattern.take(15)}...) in $name"
                                                if (!secrets.contains(matchTag)) {
                                                    secrets.add(matchTag)
                                                }
                                                break
                                            }
                                        }
                                        carryOver = chunkStr.takeLast(256)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("ApkParser", "Failed scanning $name for secrets: ${e.message}")
                            }
                        }
                    } else if (name.startsWith("assets/")) {
                        assetsCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ApkParser", "Error reading APK zip structure: ${e.message}")
            dexCount = 1
        }

        val is16KbAligned = verify16KbAlignment(file)

        return ZipAnalysisResult(
            architectures = archs.toList().sorted(),
            is16KbAligned = is16KbAligned,
            dexCount = if (dexCount == 0) 1 else dexCount,
            assetsCount = assetsCount,
            detectedSecrets = secrets.distinct()
        )
    }

    internal fun verify16KbAlignment(file: File): Boolean {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val length = raf.length()
                if (length < 22) return true

                // Locate End of Central Directory (EOCD)
                var eocdOffset = -1L
                val searchLimit = (length - 65557L).coerceAtLeast(0L)
                var p = length - 22
                while (p >= searchLimit) {
                    raf.seek(p)
                    // Signature 0x06054b50 in little endian order
                    if (raf.readInt() == 0x504b0506) {
                        eocdOffset = p
                        break
                    }
                    p--
                }
                if (eocdOffset == -1L) return true

                raf.seek(eocdOffset + 10)
                val totalEntries = readShortLittleEndian(raf)
                val cdSize = readIntLittleEndian(raf)
                val cdOffset = readIntLittleEndian(raf).toLong() and 0xFFFFFFFFL

                if (cdOffset + cdSize > length) return true

                var hasNativeLibs = false
                var currentPos = cdOffset

                for (i in 0 until totalEntries) {
                    raf.seek(currentPos)
                    val sig = raf.readInt()
                    if (sig != 0x504b0102) break // Central Directory record signature 0x02014b50

                    raf.seek(currentPos + 10)
                    val compressionMethod = readShortLittleEndian(raf)
                    raf.seek(currentPos + 28)
                    val nameLen = readShortLittleEndian(raf)
                    val extraLen = readShortLittleEndian(raf)
                    val commentLen = readShortLittleEndian(raf)
                    raf.seek(currentPos + 42)
                    val localHeaderOffset = readIntLittleEndian(raf).toLong() and 0xFFFFFFFFL

                    val nameBytes = ByteArray(nameLen)
                    raf.seek(currentPos + 46)
                    raf.readFully(nameBytes)
                    val entryName = String(nameBytes, Charsets.UTF_8)

                    currentPos += 46 + nameLen + extraLen + commentLen

                    if (entryName.startsWith("lib/") && entryName.endsWith(".so")) {
                        hasNativeLibs = true

                        // Requirement 1: Native libraries must be STORED uncompressed for direct 16KB mmap
                        if (compressionMethod != 0) {
                            return false
                        }

                        // Requirement 2: Local header data offset must be 16KB-aligned (0x4000)
                        raf.seek(localHeaderOffset)
                        val localSig = raf.readInt()
                        if (localSig != 0x504b0304) continue

                        raf.seek(localHeaderOffset + 26)
                        val localNameLen = readShortLittleEndian(raf)
                        val localExtraLen = readShortLittleEndian(raf)
                        val dataOffset = localHeaderOffset + 30 + localNameLen + localExtraLen

                        if (dataOffset % 16384L != 0L) {
                            return false
                        }

                        // Requirement 3: ELF PT_LOAD segment alignment check for 64-bit binaries
                        if (raf.length() >= dataOffset + 64) {
                            raf.seek(dataOffset)
                            val elfMagic = ByteArray(4)
                            raf.readFully(elfMagic)
                            if (elfMagic[0] == 0x7f.toByte() && elfMagic[1] == 'E'.code.toByte() &&
                                elfMagic[2] == 'L'.code.toByte() && elfMagic[3] == 'F'.code.toByte()) {
                                val eiClass = raf.readByte().toInt()
                                if (eiClass == 2) { // 64-bit ELF
                                    raf.seek(dataOffset + 32)
                                    val phOff = readLongLittleEndian(raf)
                                    raf.seek(dataOffset + 54)
                                    val phEntSize = readShortLittleEndian(raf)
                                    val phNum = readShortLittleEndian(raf)

                                    if (phEntSize >= 56 && phNum in 1..64) {
                                        for (phIdx in 0 until phNum) {
                                            val entryPos = dataOffset + phOff + (phIdx * phEntSize)
                                            if (entryPos + 56 <= raf.length()) {
                                                raf.seek(entryPos)
                                                val pType = readIntLittleEndian(raf)
                                                if (pType == 1) { // PT_LOAD segment
                                                    raf.seek(entryPos + 48)
                                                    val pAlign = readLongLittleEndian(raf)
                                                    if (pAlign in 1..16383) {
                                                        // Sub-16KB alignment indicates legacy 4KB compilation
                                                        return false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            Log.w("ApkParser", "Error verifying 16KB alignment: ${e.message}", e)
            true
        }
    }

    internal fun inspectManifestCleartext(apkFile: File): Boolean? {
        return try {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: return null
                zip.getInputStream(entry).use { stream ->
                    val bytes = stream.readBytes()
                    parseAxmlCleartextTraffic(bytes)
                }
            }
        } catch (e: Exception) {
            Log.w("ApkParser", "Failed to inspect AndroidManifest.xml cleartext attribute: ${e.message}")
            null
        }
    }

    internal fun parseAxmlCleartextTraffic(bytes: ByteArray): Boolean? {
        if (bytes.size < 36) return null
        val magic = getIntLittleEndian(bytes, 0)
        if (magic != 0x00080003 && magic != 0x00080001) return null

        val stringChunkType = getIntLittleEndian(bytes, 8)
        if (stringChunkType != 0x001C0001) return null

        val stringCount = getIntLittleEndian(bytes, 16)
        val flags = getIntLittleEndian(bytes, 24)
        val isUtf8 = (flags and (1 shl 8)) != 0
        val stringsStart = 8 + getIntLittleEndian(bytes, 28)

        var cleartextAttrIndex = -1
        for (i in 0 until stringCount) {
            val offsetInOffsets = 36 + (i * 4)
            if (offsetInOffsets + 4 > bytes.size) break
            val strOffset = stringsStart + getIntLittleEndian(bytes, offsetInOffsets)
            if (strOffset < 0 || strOffset >= bytes.size) continue

            val str = if (isUtf8) {
                var lenOffset = strOffset
                if (lenOffset >= bytes.size) continue
                var len = bytes[lenOffset].toInt() and 0xFF
                if (len and 0x80 != 0) lenOffset += 2 else lenOffset += 1
                if (lenOffset >= bytes.size) continue
                var byteLen = bytes[lenOffset].toInt() and 0xFF
                if (byteLen and 0x80 != 0) lenOffset += 2 else lenOffset += 1
                if (lenOffset + byteLen <= bytes.size && byteLen > 0) {
                    String(bytes, lenOffset, byteLen, Charsets.UTF_8)
                } else ""
            } else {
                var lenOffset = strOffset
                if (lenOffset + 1 >= bytes.size) continue
                var charCount = getShortLittleEndian(bytes, lenOffset)
                if (charCount and 0x8000 != 0) lenOffset += 4 else lenOffset += 2
                val byteLen = charCount * 2
                if (lenOffset + byteLen <= bytes.size && byteLen > 0) {
                    String(bytes, lenOffset, byteLen, Charsets.UTF_16LE)
                } else ""
            }

            if (str == "usesCleartextTraffic") {
                cleartextAttrIndex = i
                break
            }
        }

        if (cleartextAttrIndex == -1) return null

        var offset = 8 + getIntLittleEndian(bytes, 12)
        while (offset + 8 <= bytes.size) {
            val chunkType = getIntLittleEndian(bytes, offset)
            val chunkSize = getIntLittleEndian(bytes, offset + 4)
            if (chunkSize <= 0 || offset + chunkSize > bytes.size) break

            if (chunkType == 0x00100102) { // START_ELEMENT
                val attrStart = getShortLittleEndian(bytes, offset + 20)
                val attrSize = getShortLittleEndian(bytes, offset + 22)
                val attrCount = getShortLittleEndian(bytes, offset + 24)

                var attrOffset = offset + attrStart
                for (a in 0 until attrCount) {
                    if (attrOffset + 20 > offset + chunkSize) break
                    val nameIdx = getIntLittleEndian(bytes, attrOffset + 4)
                    val typedValData = getIntLittleEndian(bytes, attrOffset + 16)

                    if (nameIdx == cleartextAttrIndex) {
                        return typedValData != 0
                    }
                    attrOffset += attrSize
                }
            }
            offset += chunkSize
        }
        return null
    }

    private fun readShortLittleEndian(raf: RandomAccessFile): Int {
        val b1 = raf.read()
        val b2 = raf.read()
        if (b1 or b2 < 0) return 0
        return (b2 shl 8) or b1
    }

    private fun readIntLittleEndian(raf: RandomAccessFile): Int {
        val b1 = raf.read()
        val b2 = raf.read()
        val b3 = raf.read()
        val b4 = raf.read()
        if (b1 or b2 or b3 or b4 < 0) return 0
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }

    private fun readLongLittleEndian(raf: RandomAccessFile): Long {
        val low = readIntLittleEndian(raf).toLong() and 0xFFFFFFFFL
        val high = readIntLittleEndian(raf).toLong() and 0xFFFFFFFFL
        return (high shl 32) or low
    }

    private fun getShortLittleEndian(bytes: ByteArray, offset: Int): Int {
        if (offset + 1 >= bytes.size) return 0
        val b1 = bytes[offset].toInt() and 0xFF
        val b2 = bytes[offset + 1].toInt() and 0xFF
        return (b2 shl 8) or b1
    }

    private fun getIntLittleEndian(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        val b1 = bytes[offset].toInt() and 0xFF
        val b2 = bytes[offset + 1].toInt() and 0xFF
        val b3 = bytes[offset + 2].toInt() and 0xFF
        val b4 = bytes[offset + 3].toInt() and 0xFF
        return (b4 shl 24) or (b3 shl 16) or (b2 shl 8) or b1
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w("ApkParser", "Failed to compute SHA-256 for ${file.name}: ${e.message}")
            "SHA256-UNAVAILABLE"
        }
    }

    private fun extractCertificateFingerprint(packageInfo: PackageInfo): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures != null && signatures.isNotEmpty()) {
                val cert = signatures[0].toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val fingerprint = md.digest(cert)
                fingerprint.joinToString(":") { "%02X".format(it) }
            } else {
                "UNSIGNED_OR_DEBUG_KEY"
            }
        } catch (e: Exception) {
            Log.w("ApkParser", "Failed to extract certificate fingerprint: ${e.message}")
            "NOT_OBTAINABLE"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
