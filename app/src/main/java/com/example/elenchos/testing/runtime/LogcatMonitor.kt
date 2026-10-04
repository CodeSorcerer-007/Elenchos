package com.example.elenchos.testing.runtime

import android.util.Log
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import java.io.BufferedReader
import java.io.InputStreamReader
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
        val issues = mutableListOf<Issue>()
        var process: Process? = null
        try {
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

            val crashBlocks = mutableListOf<List<String>>()
            var currentBlock = mutableListOf<String>()
            var capturing = false

            for (line in lines) {
                if (line.contains("FATAL EXCEPTION") || line.contains("AndroidRuntime: FATAL")) {
                    if (currentBlock.isNotEmpty()) {
                        crashBlocks.add(currentBlock)
                        currentBlock = mutableListOf()
                    }
                    capturing = true
                }

                if (capturing) {
                    currentBlock.add(line)
                    // If empty line or process restart indicator, stop current block
                    if (line.contains("Process:") && !line.contains(targetPackage) && targetPackage.isNotBlank()) {
                        // Might be another process crash
                    }
                    if (currentBlock.size > 50) {
                        crashBlocks.add(currentBlock)
                        currentBlock = mutableListOf()
                        capturing = false
                    }
                }
            }
            if (currentBlock.isNotEmpty()) {
                crashBlocks.add(currentBlock)
            }

            // Deduplicate crashes by exception + affected line
            val deduplicated = mutableMapOf<String, MutableList<List<String>>>()
            for (block in crashBlocks) {
                val blockText = block.joinToString("\n")
                if (targetPackage.isNotBlank() && !blockText.contains(targetPackage)) {
                    continue
                }
                val key = block.firstOrNull { it.contains("Exception") || it.contains("Error") } ?: "UnknownCrash"
                deduplicated.getOrPut(key) { mutableListOf() }.add(block)
            }

            for ((key, occurrences) in deduplicated) {
                val firstBlock = occurrences.first()
                val fullTrace = firstBlock.joinToString("\n")
                val exceptionLine = key.trim()
                val exceptionName = exceptionLine.substringAfter("FATAL EXCEPTION:").substringAfter(":").trim()

                issues.add(
                    Issue(
                        id = "CRASH-${UUID.randomUUID().toString().take(6).uppercase()}",
                        severity = IssueSeverity.P1,
                        confidence = IssueConfidence.CONFIRMED,
                        category = IssueCategory.CRASH,
                        title = "Uncaught Exception: ${exceptionName.take(60)}",
                        description = "The target application crashed due to an uncaught exception during execution. Detected ${occurrences.size} occurrence(s) in system logcat.",
                        affectedScreen = "Runtime Process",
                        reproductionSteps = listOf(
                            "1. Launch target package $targetPackage.",
                            "2. Execute runtime UI interactions.",
                            "3. Application terminates with $exceptionName."
                        ),
                        expectedBehavior = "Application handles exceptions gracefully or guards nullable references without terminating.",
                        actualBehavior = "Process terminated with uncaught exception: $exceptionName",
                        stackTrace = fullTrace,
                        logs = firstBlock.take(15),
                        rootCauseHypothesis = "Uncaught runtime exception in thread. Check stack trace frames for unhandled null references or missing state checks.",
                        hypothesisConfidence = IssueConfidence.CONFIRMED,
                        recommendedFix = "Review the top stack frame, add null checks or try-catch bounds, and verify activity state preservation during lifecycle recreation.",
                        occurrenceCount = occurrences.size
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("LogcatMonitor", "Failed to capture logcat crashes: ${e.message}")
        } finally {
            try {
                process?.destroy()
            } catch (_: Exception) {}
        }
        return issues
    }

    fun clearLogcat() {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("logcat -c")
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
