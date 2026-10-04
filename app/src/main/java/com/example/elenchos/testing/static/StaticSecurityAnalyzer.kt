package com.example.elenchos.testing.static

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import java.util.UUID

object StaticSecurityAnalyzer {

    fun analyze(apk: APKArtifact): List<Issue> {
        val issues = mutableListOf<Issue>()

        // 1. Debuggable build
        if (apk.isDebuggable) {
            issues.add(
                Issue(
                    id = "SEC-DBG-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P0,
                    confidence = IssueConfidence.CONFIRMED,
                    category = IssueCategory.SECURITY,
                    title = "Application is packaged with android:debuggable='true'",
                    description = "The target APK has debugging enabled in its AndroidManifest. This allows attackers or third-party tools to attach debuggers via ADB (jdb), dump process memory, inspect internal SQLite databases, and execute arbitrary code in the application's user ID.",
                    affectedScreen = "AndroidManifest.xml (<application>)",
                    reproductionSteps = listOf(
                        "1. Inspect AndroidManifest.xml application attributes.",
                        "2. Observe android:debuggable is set to 'true'.",
                        "3. Verify adb jdwp can locate and attach to the target process."
                    ),
                    expectedBehavior = "Production and testing release APKs must have android:debuggable set to false.",
                    actualBehavior = "android:debuggable is true.",
                    rootCauseHypothesis = "The APK was built using a debug build variant or debuggable was explicitly hardcoded in the manifest.",
                    hypothesisConfidence = IssueConfidence.CONFIRMED,
                    recommendedFix = "Ensure `android:debuggable='false'` in production manifests, or compile using the release build type: `./gradlew assembleRelease`."
                )
            )
        }

        // 2. Cleartext traffic permitted
        if (apk.usesCleartextTraffic) {
            issues.add(
                Issue(
                    id = "SEC-NET-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P2,
                    confidence = IssueConfidence.CONFIRMED,
                    category = IssueCategory.SECURITY,
                    title = "Cleartext HTTP traffic permitted (android:usesCleartextTraffic='true')",
                    description = "The application allows unencrypted HTTP network communication. Traffic is vulnerable to interception, eavesdropping, and Man-in-the-Middle (MitM) packet manipulation on public Wi-Fi networks.",
                    affectedScreen = "AndroidManifest.xml (<application>)",
                    reproductionSteps = listOf(
                        "1. Inspect AndroidManifest.xml application tag.",
                        "2. Check android:usesCleartextTraffic attribute value.",
                        "3. Observe network socket requests allow plain http:// schemes."
                    ),
                    expectedBehavior = "Cleartext traffic should be disabled and restricted strictly to TLS/HTTPS endpoints.",
                    actualBehavior = "android:usesCleartextTraffic is true.",
                    rootCauseHypothesis = "Developers often toggle cleartext traffic during local backend API development and forget to disable it for release.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Set `android:usesCleartextTraffic=\"false\"` and configure a `<network-security-config>` file specifying pinned domains if local staging is required."
                )
            )
        }

        // 3. Insecure exported components without permissions
        val exportedActivities = apk.activities.filter { it.isExported && it.permission == null }
        if (exportedActivities.size > 2) {
            issues.add(
                Issue(
                    id = "SEC-EXP-ACT-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P1,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.SECURITY,
                    title = "${exportedActivities.size} Activities exported without permission protection",
                    description = "Multiple Activity components are exported (`android:exported=\"true\"`) without requiring calling permissions. Any third-party application on the device can launch these internal screens directly, bypassing authentication or onboarding gates.",
                    affectedScreen = exportedActivities.firstOrNull()?.name ?: "Activities",
                    reproductionSteps = listOf(
                        "1. Run `adb shell am start -n ${apk.packageName}/${exportedActivities.firstOrNull()?.name}`.",
                        "2. Observe that the activity opens without authentication verification.",
                        "3. Sensitive workflows may be invoked externally."
                    ),
                    expectedBehavior = "Only main launcher or explicitly intended deep-link activities should be exported. Internal screens should set android:exported=\"false\".",
                    actualBehavior = "${exportedActivities.size} activities are publicly accessible: ${exportedActivities.take(3).joinToString { it.name.substringAfterLast('.') }}...",
                    rootCauseHypothesis = "Activities declaring <intent-filter> require explicit android:exported on Android 12+, and developers frequently set exported='true' indiscriminately.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Set `android:exported=\"false\"` for internal screens. If an activity must be opened by other apps, protect it with a custom signature-level permission."
                )
            )
        }

        val exportedServices = apk.services.filter { it.isExported && it.permission == null }
        if (exportedServices.isNotEmpty()) {
            issues.add(
                Issue(
                    id = "SEC-EXP-SRV-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P1,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.SECURITY,
                    title = "Exported Service without permission guard: ${exportedServices.first().name.substringAfterLast('.')}",
                    description = "The service is exported to all applications on the operating system without a protecting permission. Untrusted apps can bind to this service, send malicious IPC messages, or drain device battery.",
                    affectedScreen = exportedServices.first().name,
                    reproductionSteps = listOf(
                        "1. Query package services via PackageManager.",
                        "2. Identify exported service ${exportedServices.first().name}.",
                        "3. Issue bindService from an external app without permission."
                    ),
                    expectedBehavior = "Background services should not be exported unless designed for public system integration (e.g. Accessibility or MediaBrowserService).",
                    actualBehavior = "Service is exported without permission requirements.",
                    rootCauseHypothesis = "Missing permission attribute or accidental exported declaration in AndroidManifest.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Add `android:exported=\"false\"` to the `<service>` declaration, or guard it with `android:permission=\"...\"`."
                )
            )
        }

        val exportedReceivers = apk.receivers.filter { it.isExported && it.permission == null }
        if (exportedReceivers.isNotEmpty()) {
            issues.add(
                Issue(
                    id = "SEC-EXP-RCV-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P2,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.SECURITY,
                    title = "Exported BroadcastReceiver vulnerable to spoofed broadcasts",
                    description = "The BroadcastReceiver is exported without permission restrictions. Any application on the phone can send broadcast intents to trigger actions or pass malformed Parcelables.",
                    affectedScreen = exportedReceivers.first().name,
                    reproductionSteps = listOf(
                        "1. Find receiver ${exportedReceivers.first().name}.",
                        "2. Send intent using `am broadcast -a <action>`.",
                        "3. Receiver executes logic from untrusted sender."
                    ),
                    expectedBehavior = "BroadcastReceivers should be internal (`exported=\"false\"`) or specify signature permissions.",
                    actualBehavior = "Receiver is exported with no permission.",
                    rootCauseHypothesis = "BroadcastReceiver declared with intent-filter without explicitly disabling exported status.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Set `android:exported=\"false\"` or use LocalBroadcastManager / Flow events for internal app messaging."
                )
            )
        }

        // 4. Backup enabled
        if (apk.allowsBackup) {
            issues.add(
                Issue(
                    id = "SEC-BCK-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P3,
                    confidence = IssueConfidence.CONFIRMED,
                    category = IssueCategory.SECURITY,
                    title = "Application data backup is enabled (android:allowBackup='true')",
                    description = "Application private files (databases, SharedPreferences, cache) may be extracted via `adb backup` on developer-unlocked devices unless restricted by data extraction rules.",
                    affectedScreen = "AndroidManifest.xml (<application>)",
                    reproductionSteps = listOf(
                        "1. Check android:allowBackup attribute.",
                        "2. Execute `adb backup -f backup.ab -noapk ${apk.packageName}`.",
                        "3. Sensitive application files can be extracted into an archive."
                    ),
                    expectedBehavior = "Sensitive apps should configure explicit data extraction rules or set allowBackup to false.",
                    actualBehavior = "android:allowBackup is true without explicit exclusion rules.",
                    rootCauseHypothesis = "Default Android template setting is allowBackup='true'.",
                    hypothesisConfidence = IssueConfidence.CONFIRMED,
                    recommendedFix = "Add a custom `<data-extraction-rules>` resource excluding tokens, databases, and keys, or set `android:allowBackup=\"false\"`."
                )
            )
        }

        // 5. Special / dangerous permissions audit
        if (apk.permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
            issues.add(
                Issue(
                    id = "SEC-PERM-OVR-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P2,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.PERMISSION,
                    title = "Dangerous overlay permission requested (SYSTEM_ALERT_WINDOW)",
                    description = "The application requests SYSTEM_ALERT_WINDOW (Draw over other apps). This permission is heavily scrutinized on Google Play and can be abused for tapjacking attacks.",
                    affectedScreen = "AndroidManifest.xml (<uses-permission>)",
                    reproductionSteps = listOf(
                        "1. Inspect requested permissions.",
                        "2. Observe android.permission.SYSTEM_ALERT_WINDOW.",
                        "3. App can draw floating windows over arbitrary system screens."
                    ),
                    expectedBehavior = "Overlay permissions should only be requested by designated system utility tools or video PIP players.",
                    actualBehavior = "SYSTEM_ALERT_WINDOW is requested.",
                    rootCauseHypothesis = "Third-party SDK or legacy chat-head overlay implementation requested unnecessary window permissions.",
                    hypothesisConfidence = IssueConfidence.MEDIUM,
                    recommendedFix = "Remove `SYSTEM_ALERT_WINDOW` unless strictly required for core user-facing functionality."
                )
            )
        }

        if (apk.permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            issues.add(
                Issue(
                    id = "SEC-PERM-INS-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P2,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.PERMISSION,
                    title = "Self-update permission declared (REQUEST_INSTALL_PACKAGES)",
                    description = "The application requests permission to install unknown packages. Google Play policies prohibit side-loading APK updates directly from apps distributed in the store.",
                    affectedScreen = "AndroidManifest.xml (<uses-permission>)",
                    reproductionSteps = listOf(
                        "1. Check uses-permission list in manifest.",
                        "2. REQUEST_INSTALL_PACKAGES is present.",
                        "3. Submitting to Google Play may trigger rejection."
                    ),
                    expectedBehavior = "App updates should be managed by the Google Play In-App Updates API.",
                    actualBehavior = "REQUEST_INSTALL_PACKAGES is present in manifest.",
                    rootCauseHypothesis = "Legacy in-house auto-updater code was bundled in the APK.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Remove `REQUEST_INSTALL_PACKAGES` and adopt the Play Core In-App Updates library."
                )
            )
        }

        // 6. Native architectures and 64-bit / 16KB Page alignment audit
        if (apk.nativeArchitectures.isNotEmpty()) {
            val has64Bit = apk.nativeArchitectures.any { it.contains("64") }
            if (!has64Bit) {
                issues.add(
                    Issue(
                        id = "CMP-64BIT-${UUID.randomUUID().toString().take(6).uppercase()}",
                        severity = IssueSeverity.P1,
                        confidence = IssueConfidence.CONFIRMED,
                        category = IssueCategory.COMPATIBILITY,
                        title = "APK lacks 64-bit native libraries (armeabi-v7a only)",
                        description = "The APK contains 32-bit native libraries (${apk.nativeArchitectures.joinToString()}) but no 64-bit architectures (`arm64-v8a` or `x86_64`). Modern Android devices (Pixel 7+, Galaxy S24, Android 15/16) are 64-bit only and will immediately crash or refuse installation.",
                        affectedScreen = "lib/ architectures",
                        reproductionSteps = listOf(
                            "1. Inspect APK ZIP /lib folder.",
                            "2. Observe absence of arm64-v8a directory.",
                            "3. Attempt installation on 64-bit-only Android device."
                        ),
                        expectedBehavior = "All applications with native C/C++ code must include arm64-v8a libraries.",
                        actualBehavior = "Only 32-bit architectures detected.",
                        rootCauseHypothesis = "ndk abiFilters was restricted to armeabi-v7a in build.gradle.",
                        hypothesisConfidence = IssueConfidence.CONFIRMED,
                        recommendedFix = "Add `ndk { abiFilters += listOf(\"arm64-v8a\", \"x86_64\") }` in app build.gradle.kts."
                    )
                )
            }
        }

        // 7. Detected hardcoded secrets
        if (apk.detectedSecrets.isNotEmpty()) {
            issues.add(
                Issue(
                    id = "SEC-LEAK-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P1,
                    confidence = IssueConfidence.HIGH,
                    category = IssueCategory.SECURITY,
                    title = "Potential hardcoded credentials/endpoints in compiled DEX: ${apk.detectedSecrets.first()}",
                    description = "Static scanning of DEX string pools revealed suspicious patterns resembling API keys, auth tokens, or private localhost staging endpoints: ${apk.detectedSecrets.joinToString("; ")}.",
                    affectedScreen = "classes.dex string table",
                    reproductionSteps = listOf(
                        "1. Decompile or extract DEX string pools.",
                        "2. Search for sensitive credential regular expressions.",
                        "3. Observe unencrypted string literal in binary."
                    ),
                    expectedBehavior = "API secrets must never be embedded in client-side APK binaries; use backend proxying or Android Keystore.",
                    actualBehavior = "Pattern matches found in compiled bytecode: ${apk.detectedSecrets.take(2).joinToString()}",
                    rootCauseHypothesis = "Keys or debug server URLs were hardcoded in Kotlin/Java constants instead of environment variables or backend tokens.",
                    hypothesisConfidence = IssueConfidence.HIGH,
                    recommendedFix = "Migrate sensitive API access to an authenticated backend service, or store keys in Google Cloud Secret Manager / EncryptedSharedPreferences."
                )
            )
        }

        // 8. Performance: Resource bloat
        if (apk.fileSizeBytes > 60 * 1024 * 1024) {
            issues.add(
                Issue(
                    id = "PERF-SIZE-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = IssueSeverity.P4,
                    confidence = IssueConfidence.CONFIRMED,
                    category = IssueCategory.PERFORMANCE,
                    title = "High APK package size (${apk.formattedSize})",
                    description = "The total APK size exceeds 60MB. Large downloads increase install abandonment rates, memory footprint during installation, and storage pressure on lower-tier devices.",
                    affectedScreen = "APK File Size",
                    reproductionSteps = listOf(
                        "1. Check APK total file size: ${apk.formattedSize}.",
                        "2. Inspect DEX count: ${apk.dexCount} multidex files.",
                        "3. Inspect total assets count: ${apk.totalAssetsCount} items."
                    ),
                    expectedBehavior = "Target optimized download size using Android App Bundles (.aab), R8 code shrinking, and WebP image compression.",
                    actualBehavior = "APK size is ${apk.formattedSize}.",
                    rootCauseHypothesis = "Uncompressed assets, unminified dependencies, or bundling multiple native ABIs in a single universal APK.",
                    hypothesisConfidence = IssueConfidence.CONFIRMED,
                    recommendedFix = "Enable R8 shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`), convert PNGs to WebP/AVIF, and distribute via Android App Bundles."
                )
            )
        }

        return issues
    }
}
