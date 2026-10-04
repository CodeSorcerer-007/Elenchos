package com.example.elenchos.testing.runtime

import com.example.elenchos.domain.model.IssueCategory
import com.example.elenchos.domain.model.IssueConfidence
import com.example.elenchos.domain.model.IssueSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatMonitorTest {

    private val targetPackage = "com.sample.targetapp"

    @Test
    fun parseLogcatLines_withUncaughtNullPointerException_extractsProperExceptionClassAndMessage() {
        val lines = listOf(
            "10-04 22:15:30.123  1000  1000 E AndroidRuntime: FATAL EXCEPTION: main",
            "10-04 22:15:30.123  1000  1000 E AndroidRuntime: Process: $targetPackage, PID: 12345",
            "10-04 22:15:30.124  1000  1000 E AndroidRuntime: java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference",
            "10-04 22:15:30.125  1000  1000 E AndroidRuntime: \tat com.sample.targetapp.MainActivity.onCreate(MainActivity.kt:42)",
            "10-04 22:15:30.126  1000  1000 E AndroidRuntime: \tat android.app.Activity.performCreate(Activity.java:8000)"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)

        assertEquals("Should parse exactly one crash issue", 1, issues.size)
        val issue = issues.first()
        assertEquals(IssueCategory.CRASH, issue.category)
        assertEquals(IssueSeverity.P1, issue.severity)
        assertEquals(IssueConfidence.CONFIRMED, issue.confidence)

        // CRITICAL: Ensure title does not claim the thread name "main" is the exception!
        assertFalse("Title must not be 'Uncaught Exception: main'", issue.title.contains("Uncaught Exception: main"))
        assertTrue("Title should contain NullPointerException", issue.title.contains("NullPointerException"))
        assertTrue("Title should contain exception message", issue.title.contains("Attempt to invoke virtual method"))
        assertTrue("Stack trace should preserve crash stack", issue.stackTrace?.contains("MainActivity.onCreate") == true)
    }

    @Test
    fun parseLogcatLines_withAnrBlock_extractsP0AnrIssue() {
        val lines = listOf(
            "10-04 22:16:00.000  1000  1000 E ActivityManager: ANR in $targetPackage (com.sample.targetapp/.MainActivity)",
            "10-04 22:16:00.000  1000  1000 E ActivityManager: PID: 12345",
            "10-04 22:16:00.001  1000  1000 E ActivityManager: Reason: Input dispatching timed out (Application does not have a focused window)",
            "10-04 22:16:00.002  1000  1000 E ActivityManager: Load: 4.5 / 2.3 / 1.0",
            "10-04 22:16:00.003  1000  1000 E ActivityManager: CPU usage from 0ms to 5000ms later:"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)

        assertEquals("Should capture ANR", 1, issues.size)
        val anrIssue = issues.first()
        assertEquals(IssueCategory.ANR, anrIssue.category)
        assertEquals(IssueSeverity.P0, anrIssue.severity)
        assertEquals("Main UI Thread", anrIssue.affectedScreen)
        assertTrue("Title should mention ANR", anrIssue.title.contains("ANR"))
        assertTrue("Root cause should mention input timeout", anrIssue.rootCauseHypothesis.contains("threshold"))
    }

    @Test
    fun parseLogcatLines_withNativeCrash_extractsP0NativeCrashIssue() {
        val lines = listOf(
            "10-04 22:17:00.000  1000  1000 F libc    : Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0 in tid 12345 (sample.targetapp)",
            "10-04 22:17:00.001  1000  1000 F DEBUG   : *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***",
            "10-04 22:17:00.002  1000  1000 F DEBUG   : Build fingerprint: 'google/pixel8/pixel8:15/AP1A...'",
            "10-04 22:17:00.003  1000  1000 F DEBUG   : pid: 12345, tid: 12345, name: $targetPackage  >>> $targetPackage <<<",
            "10-04 22:17:00.004  1000  1000 F DEBUG   :     #00 pc 0000000000012345  /data/app/$targetPackage/lib/arm64/libnative.so"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)

        assertEquals("Should capture native crash", 1, issues.size)
        val nativeIssue = issues.first()
        assertEquals(IssueCategory.CRASH, nativeIssue.category)
        assertEquals(IssueSeverity.P0, nativeIssue.severity)
        assertTrue("Title should mention Native Crash", nativeIssue.title.contains("Native Crash"))
        assertTrue("Title should mention SIGSEGV", nativeIssue.title.contains("SIGSEGV"))
    }

    @Test
    fun parseLogcatLines_withOutOfMemoryError_extractsErrorDetails() {
        val lines = listOf(
            "10-04 22:18:00.000  1000  1000 E AndroidRuntime: FATAL EXCEPTION: main",
            "10-04 22:18:00.000  1000  1000 E AndroidRuntime: Process: $targetPackage, PID: 12345",
            "10-04 22:18:00.001  1000  1000 E AndroidRuntime: java.lang.OutOfMemoryError: Failed to allocate a 52428816 byte allocation with 16777216 free bytes and 32MB until OOM",
            "10-04 22:18:00.002  1000  1000 E AndroidRuntime: \tat dalvik.system.VMRuntime.newNonMovableArray(Native Method)"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)

        assertEquals(1, issues.size)
        val oomIssue = issues.first()
        assertTrue(oomIssue.title.contains("OutOfMemoryError"))
        assertTrue(oomIssue.title.contains("Failed to allocate"))
    }

    @Test
    fun parseLogcatLines_filtersOutOtherPackages() {
        val lines = listOf(
            "10-04 22:19:00.000  1000  1000 E AndroidRuntime: FATAL EXCEPTION: main",
            "10-04 22:19:00.000  1000  1000 E AndroidRuntime: Process: com.other.unrelated.app, PID: 9999",
            "10-04 22:19:00.001  1000  1000 E AndroidRuntime: java.lang.RuntimeException: Unrelated crash",
            "10-04 22:19:00.002  1000  1000 E AndroidRuntime: \tat com.other.unrelated.app.MainActivity.onCreate(MainActivity.kt:10)"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)
        assertTrue("Crashes from unrelated packages must be ignored", issues.isEmpty())
    }

    @Test
    fun parseLogcatLines_cleanLogcat_returnsEmpty() {
        val lines = listOf(
            "10-04 22:20:00.000  1000  1000 I ActivityTaskManager: Displayed $targetPackage/.MainActivity: +210ms",
            "10-04 22:20:00.100  1000  1000 D OpenGLRenderer: RenderThread 0x123 completed frame"
        )

        val issues = LogcatMonitor.parseLogcatLines(lines, targetPackage)
        assertTrue("Clean logcat must yield 0 issues", issues.isEmpty())
    }
}
