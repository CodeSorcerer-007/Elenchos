package com.example.elenchos.reporting

import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.TestSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HtmlReportGenerator {

    private fun escapeHtml(text: String?): String {
        if (text == null) return ""
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun generateHtml(session: TestSession, apk: APKArtifact): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date(session.startTimestamp))
        val health = session.healthScore

        val safeAppName = escapeHtml(apk.appName)
        val safePkgName = escapeHtml(apk.packageName)
        val safeVersion = escapeHtml(apk.versionName)
        val safeDevice = escapeHtml(session.deviceModel)
        val safeAndroid = escapeHtml(session.androidVersion)
        val safeSessionId = escapeHtml(session.id)

        val issuesHtml = session.issues.joinToString("\n") { issue ->
            val badgeColor = when (issue.severity.code) {
                "P0", "P1" -> "#EF4444"
                "P2" -> "#F59E0B"
                "P3" -> "#3B82F6"
                else -> "#64748B"
            }
            val safeTitle = escapeHtml(issue.title)
            val safeId = escapeHtml(issue.id)
            val safeDesc = escapeHtml(issue.description)
            val safeRootCause = escapeHtml(issue.rootCauseHypothesis)
            val safeRemedy = escapeHtml(issue.recommendedFix)
            val safeScreen = escapeHtml(issue.affectedScreen)
            val safeExpected = escapeHtml(issue.expectedBehavior)
            val safeActual = escapeHtml(issue.actualBehavior)

            val stepsHtml = if (issue.reproductionSteps.isNotEmpty()) {
                val items = issue.reproductionSteps.joinToString("") { "<li>${escapeHtml(it)}</li>" }
                """<div style="margin: 8px 0;"><strong style="font-size: 11px; text-transform: uppercase; color: #64748B;">Reproduction Steps:</strong><ol style="margin: 4px 0 8px 18px; padding: 0; font-size: 13px; color: #334155;">$items</ol></div>"""
            } else ""

            val stackTraceHtml = if (!issue.stackTrace.isNullOrBlank()) {
                """<div style="background: #0F172A; color: #38BDF8; font-family: monospace; font-size: 11px; padding: 10px; border-radius: 6px; overflow-x: auto; margin-top: 8px;"><pre style="margin: 0;">${escapeHtml(issue.stackTrace)}</pre></div>"""
            } else ""

            """
            <div style="border: 1px solid #E2E8F0; border-radius: 8px; padding: 18px; margin-bottom: 16px; background: #FFFFFF;">
                <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;">
                    <span style="background: $badgeColor; color: #FFFFFF; font-weight: 700; font-size: 11px; padding: 4px 10px; border-radius: 4px;">
                        ${issue.severity.code} - ${issue.severity.label}
                    </span>
                    <span style="font-size: 12px; color: #64748B; font-family: monospace;">$safeId</span>
                </div>
                <h3 style="margin: 0 0 6px 0; font-size: 16px; color: #0F172A;">$safeTitle</h3>
                <div style="font-size: 11px; font-family: monospace; color: #64748B; margin-bottom: 8px;">Component: $safeScreen</div>
                <p style="margin: 0 0 10px 0; font-size: 13px; color: #334155; line-height: 1.5;">$safeDesc</p>
                $stepsHtml
                <div style="background: #F8FAFC; border-left: 3px solid #3B82F6; padding: 10px 14px; font-size: 12px; color: #1E293B; margin-bottom: 8px;">
                    <strong>Root Cause Hypothesis:</strong> $safeRootCause
                </div>
                <div style="background: #ECFDF5; border-left: 3px solid #10B981; padding: 10px 14px; font-size: 12px; color: #065F46;">
                    <strong>Recommended Fix:</strong> $safeRemedy
                </div>
                $stackTraceHtml
            </div>
            """
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Elenchos QA Report - $safeAppName</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #F8FAFC; color: #0F172A; margin: 0; padding: 32px 16px; }
        .container { max-width: 900px; margin: 0 auto; background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 12px; padding: 32px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
        .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid #E2E8F0; padding-bottom: 24px; margin-bottom: 24px; }
        .score-box { background: #F1F5F9; border-radius: 10px; padding: 16px 24px; text-align: center; min-width: 100px; }
        .score-val { font-size: 42px; font-weight: 800; color: #0F172A; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 12px; margin-bottom: 24px; }
        .card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 12px; text-align: center; }
        .card-num { font-size: 20px; font-weight: 700; color: #0F172A; }
        .card-lbl { font-size: 11px; color: #64748B; text-transform: uppercase; margin-top: 4px; }
        @media print {
            body { background: #FFFFFF; padding: 0; }
            .container { border: none; box-shadow: none; max-width: 100%; padding: 0; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div>
                <h1 style="margin: 0; font-size: 22px; color: #0F172A; letter-spacing: 0.5px;">ELENCHOS QA LABORATORY REPORT</h1>
                <p style="margin: 6px 0 0 0; color: #475569; font-size: 14px;">Target: <strong>$safeAppName</strong> ($safePkgName v$safeVersion)</p>
                <p style="margin: 4px 0 0 0; color: #94A3B8; font-size: 12px; font-family: monospace;">Device: $safeDevice | OS: $safeAndroid | Session: $safeSessionId | $dateStr</p>
            </div>
            <div class="score-box">
                <div class="score-val">${health?.overallScore ?: 0}</div>
                <div style="font-size: 10px; font-weight: 700; color: #64748B; text-transform: uppercase;">Health Score</div>
            </div>
        </div>

        <div class="grid">
            <div class="card"><div class="card-num">${health?.stabilityScore ?: 0}</div><div class="card-lbl">Stability</div></div>
            <div class="card"><div class="card-num">${health?.securityScore ?: 0}</div><div class="card-lbl">Security</div></div>
            <div class="card"><div class="card-num">${health?.functionalityScore ?: 0}</div><div class="card-lbl">Function</div></div>
            <div class="card"><div class="card-num">${health?.performanceScore ?: 0}</div><div class="card-lbl">Performance</div></div>
            <div class="card"><div class="card-num">${health?.accessibilityScore ?: 0}</div><div class="card-lbl">A11y</div></div>
            <div class="card"><div class="card-num">${health?.compatibilityScore ?: 0}</div><div class="card-lbl">Compat</div></div>
        </div>

        <h2 style="font-size: 18px; margin: 32px 0 16px 0; border-bottom: 1px solid #E2E8F0; padding-bottom: 8px;">Detected Issues (${session.issues.size})</h2>
        $issuesHtml
    </div>
</body>
</html>
        """.trimIndent()
    }
}
