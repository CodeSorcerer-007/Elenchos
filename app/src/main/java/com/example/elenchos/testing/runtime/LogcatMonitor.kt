package com.example.elenchos.testing.runtime

import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

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
        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:E"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = reader.readLines()
            process.waitFor()

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
        } catch (_: Exception) {
            // Logcat permissions or sandbox restrictions handled gracefully
        }
        return issues
    }

    fun clearLogcat() {
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (_: Exception) {}
    }
}
