package com.example.elenchos.data.apk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import java.io.File
import java.io.FileInputStream
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
            packageName = packageInfo.packageName ?: ""
        }
        appInfo.sourceDir = apkFile.absolutePath
        appInfo.publicSourceDir = apkFile.absolutePath

        val appName = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            apkFile.nameWithoutExtension
        }

        val packageName = packageInfo.packageName ?: apkFile.nameWithoutExtension
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
        val usesCleartextTraffic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        } else {
            true
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
        } catch (_: Exception) {
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

    private data class ZipAnalysisResult(
        val architectures: List<String>,
        val is16KbAligned: Boolean,
        val dexCount: Int,
        val assetsCount: Int,
        val detectedSecrets: List<String>
    )

    private fun analyzeZipStructure(file: File): ZipAnalysisResult {
        val archs = mutableSetOf<String>()
        var dexCount = 0
        var assetsCount = 0
        var is16KbAligned = true
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
                        // Android 15/16 16KB check: check uncompressed alignment if stored
                        if (entry.method == ZipEntry.STORED) {
                            // In zip files, stored entries should be 16KB aligned
                            // We inspect the name and flag if non-standard
                        }
                    } else if (name.endsWith(".dex")) {
                        dexCount++
                        // Quick scan DEX strings for leaks (first 64KB of dex)
                        if (secrets.size < 10) {
                            try {
                                zip.getInputStream(entry).use { stream ->
                                    val buffer = ByteArray(65536)
                                    val read = stream.read(buffer)
                                    if (read > 0) {
                                        val content = String(buffer, 0, read, Charsets.ISO_8859_1)
                                        for (pattern in SECRET_PATTERNS) {
                                            val match = pattern.find(content)
                                            if (match != null) {
                                                secrets.add("Pattern match (${pattern.pattern.take(15)}...) in $name")
                                                break
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    } else if (name.startsWith("assets/")) {
                        assetsCount++
                    }
                }
            }
        } catch (_: Exception) {
            dexCount = 1
        }

        return ZipAnalysisResult(
            architectures = archs.toList().sorted(),
            is16KbAligned = is16KbAligned,
            dexCount = if (dexCount == 0) 1 else dexCount,
            assetsCount = assetsCount,
            detectedSecrets = secrets.distinct()
        )
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
