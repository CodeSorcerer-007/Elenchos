package com.example.elenchos.testing.runtime

import android.util.Log
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import java.util.UUID
import java.util.concurrent.TimeUnit

object LogcatMonitor {

    data class CapturedCrash(
        val exceptionType: String,
        val message: String,
        val stackTrace: String,
        val affectedClass: String,
        val timestamp: String
    )

    fun captureRecentCrashes(targetPackage: String): List<Issue> {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:E"))

            // Drain error stream in parallel to prevent deadlock if buffer fills
            val errorThread = Thread {
                try {
                    process.errorStream.bufferedReader().use { it.readText() }
                } catch (_: Exception) {}
            }
            errorThread.start()

            val lines = process.inputStream.bufferedReader().use { it.readLines() }

            val finished = process.waitFor(3, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
            }
            errorThread.join(500)

            parseLogcatLines(lines, targetPackage)
        } catch (e: Exception) {
            Log.w("LogcatMonitor", "Failed to capture logcat crashes: ${e.message}")
            emptyList()
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }

    internal fun parseLogcatLines(lines: List<String>, targetPackage: String): List<Issue> {
        val issues = mutableListOf<Issue>()
        val crashBlocks = mutableListOf<List<String>>()
        var currentBlock = mutableListOf<String>()
        var capturing = false

        for (line in lines) {
            val isCrashHeader = line.contains("FATAL EXCEPTION") ||
                    line.contains("AndroidRuntime: FATAL") ||
                    line.contains("ANR in ") ||
                    line.contains("Fatal signal ")

            if (isCrashHeader) {
                if (currentBlock.isNotEmpty()) {
                    crashBlocks.add(currentBlock)
                    currentBlock = mutableListOf()
                }
                capturing = true
            }

            if (capturing) {
                currentBlock.add(line)
                if (currentBlock.size >= 60) {
                    crashBlocks.add(currentBlock)
                    currentBlock = mutableListOf()
                    capturing = false
                }
            }
        }
        if (currentBlock.isNotEmpty()) {
            crashBlocks.add(currentBlock)
        }

        // Deduplicate blocks
        val deduplicated = mutableMapOf<String, MutableList<List<String>>>()
        for (block in crashBlocks) {
            val blockText = block.joinToString("\n")
            if (targetPackage.isNotBlank() && !blockText.contains(targetPackage)) {
                continue
            }
            // Use key derived from the core error line
            val key = identifyCrashKey(block)
            deduplicated.getOrPut(key) { mutableListOf() }.add(block)
        }

        for ((key, occurrences) in deduplicated) {
            val firstBlock = occurrences.first()
            val fullTrace = firstBlock.joinToString("\n")
            val isAnr = firstBlock.any { it.contains("ANR in ") }
            val isNative = firstBlock.any { it.contains("Fatal signal ") }

            val (category, severity, title, rootCause, fix) = when {
                isAnr -> {
                    val anrLine = firstBlock.firstOrNull { it.contains("ANR in ") } ?: "ANR detected"
                    val reasonLine = firstBlock.firstOrNull { it.contains("Reason:") }?.substringAfter("Reason:")?.trim()
                    val anrComponent = anrLine.substringAfter("ANR in ").substringBefore(" ").trim()
                    val titleText = if (!reasonLine.isNullOrBlank()) {
                        "ANR in $anrComponent: ${reasonLine.take(50)}"
                    } else {
                        "ANR: Application Not Responding in $anrComponent"
                    }
                    Tuple5(
                        IssueCategory.ANR,
                        IssueSeverity.P0,
                        titleText,
                        "Main thread blocked exceeding system input/broadcast threshold. $reasonLine",
                        "Move heavy I/O, database access, and CPU operations to background coroutines (Dispatchers.IO / Dispatchers.Default)."
                    )
                }
                isNative -> {
                    val sigLine = firstBlock.firstOrNull { it.contains("Fatal signal ") }?.trim() ?: "Fatal signal in native code"
                    val cleanSig = "Fatal signal " + sigLine.substringAfter("Fatal signal ").trim()
                    Tuple5(
                        IssueCategory.CRASH,
                        IssueSeverity.P0,
                        "Native Crash: ${cleanSig.take(65)}",
                        "Fatal native signal (SIGSEGV / SIGABRT) caused abnormal process termination.",
                        "Inspect native JNI library memory allocations, boundary checks, and null pointer dereferences using addr2line/ndk-stack."
                    )
                }
                else -> {
                    val (exClass, exMsg) = extractExceptionDetails(firstBlock)
                    val shortClass = exClass.substringAfterLast('.')
                    val titleText = if (exMsg.isNotBlank()) {
                        "Uncaught $shortClass: ${exMsg.take(55)}"
                    } else {
                        "Uncaught Exception: $shortClass"
                    }
                    Tuple5(
                        IssueCategory.CRASH,
                        IssueSeverity.P1,
                        titleText,
                        "Uncaught runtime exception ($exClass) in application process thread.",
                        "Review the top stack frame, guard nullable state or invalid parameters, and ensure safe lifecycle recreation."
                    )
                }
            }

            issues.add(
                Issue(
                    id = if (category == IssueCategory.ANR) "ANR-${UUID.randomUUID().toString().take(6).uppercase()}" else "CRASH-${UUID.randomUUID().toString().take(6).uppercase()}",
                    severity = severity,
                    confidence = IssueConfidence.CONFIRMED,
                    category = category,
                    title = title,
                    description = "Detected $title with ${occurrences.size} occurrence(s) in system logcat.",
                    affectedScreen = if (category == IssueCategory.ANR) "Main UI Thread" else "Runtime Process",
                    reproductionSteps = listOf(
                        "1. Launch target package $targetPackage.",
                        "2. Execute runtime UI interactions.",
                        "3. Process encounters $title."
                    ),
                    expectedBehavior = "Application process should maintain responsiveness and handle runtime states without terminating.",
                    actualBehavior = "Process terminated or stalled: $title",
                    stackTrace = fullTrace,
                    logs = firstBlock.take(15),
                    rootCauseHypothesis = rootCause,
                    hypothesisConfidence = IssueConfidence.CONFIRMED,
                    recommendedFix = fix,
                    occurrenceCount = occurrences.size
                )
            )
        }

        return issues
    }

    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun identifyCrashKey(block: List<String>): String {
        for (line in block) {
            if (line.contains("ANR in ")) return "ANR:" + line.substringAfter("ANR in ").substringBefore(" ")
            if (line.contains("Fatal signal ")) return "NATIVE:" + line.substringAfter("Fatal signal ").substringBefore(",")
        }
        val (exClass, _) = extractExceptionDetails(block)
        return exClass
    }

    internal fun extractExceptionDetails(block: List<String>): Pair<String, String> {
        for (line in block) {
            val content = extractMessageContent(line).trim()
            if (content.startsWith("FATAL EXCEPTION:") || content.startsWith("Process:") || content.startsWith("PID:")) {
                continue
            }
            if (content.contains("Exception") || content.contains("Error")) {
                val clean = content.substringAfter("Caused by: ").trim()
                return if (clean.contains(":")) {
                    val exClass = clean.substringBefore(":").trim()
                    val exMsg = clean.substringAfter(":").trim()
                    Pair(exClass, exMsg)
                } else {
                    Pair(clean, "")
                }
            }
        }
        return Pair("UnknownException", "")
    }

    private fun extractMessageContent(line: String): String {
        // Strip logcat timestamp and tag if present (e.g. "10-04 22:30:15.123 1234 1234 E AndroidRuntime: msg")
        return when {
            line.contains("): ") -> line.substringAfter("): ")
            line.contains("AndroidRuntime: ") -> line.substringAfter("AndroidRuntime: ")
            line.contains("ActivityManager: ") -> line.substringAfter("ActivityManager: ")
            line.contains("libc: ") -> line.substringAfter("libc: ")
            else -> line
        }
    }

    fun clearLogcat() {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
            }
        } catch (e: Exception) {
            Log.w("LogcatMonitor", "Failed to clear logcat: ${e.message}")
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
    }
}
