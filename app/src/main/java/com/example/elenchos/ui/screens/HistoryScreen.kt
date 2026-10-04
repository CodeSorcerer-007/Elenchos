package com.example.elenchos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.SessionDiff
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP2
import com.example.elenchos.theme.LabStatusFail
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabStatusPassBg
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.components.SeverityBadge
import com.example.elenchos.ui.viewmodel.ElenchosViewModel
import com.example.elenchos.ui.navigation.NavigationScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessionsHistory.collectAsState()
    var compareSessionA by remember { mutableStateOf<TestSession?>(null) }
    var compareSessionB by remember { mutableStateOf<TestSession?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Test History & Diff",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabTextPrimary
                )
                Text(
                    text = "${sessions.size} past session(s) archived locally",
                    fontSize = 12.sp,
                    color = LabTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Comparison Diff Section if 2 sessions selected
        if (compareSessionA != null && compareSessionB != null) {
            ComparisonDiffCard(
                sessionA = compareSessionA!!,
                sessionB = compareSessionB!!,
                onDismiss = {
                    compareSessionA = null
                    compareSessionB = null
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        } else if (sessions.size >= 2 && compareSessionA == null) {
            LabCard(
                borderColor = LabPrimary.copy(alpha = 0.3f),
                backgroundColor = LabPrimaryLight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compare Test Runs",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabPrimary
                        )
                        Text(
                            text = "Compare the two most recent runs to detect regressions and improvements.",
                            fontSize = 11.sp,
                            color = LabTextSecondary
                        )
                    }
                    Button(
                        onClick = {
                            compareSessionA = sessions[1] // Previous
                            compareSessionB = sessions[0] // Latest
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                    ) {
                        Text("COMPARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = LabTextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Test History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LabTextPrimary)
                    Text("Run tests to see historical sessions and track regressions.", fontSize = 12.sp, color = LabTextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { s ->
                    val dateFormat = SimpleDateFormat("MMM d, yyyy - HH:mm:ss", Locale.US)
                    val dateStr = dateFormat.format(Date(s.startTimestamp))
                    val score = s.healthScore?.overallScore ?: 0

                    LabCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.selectSession(s)
                                        viewModel.navigateTo(NavigationScreen.REPORTS)
                                    }
                            ) {
                                Text(
                                    text = s.appName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LabTextPrimary
                                )
                                Text(
                                    text = "${s.packageName} v${s.versionName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = LabTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$dateStr | ${s.issues.size} issues",
                                    fontSize = 10.sp,
                                    color = LabTextTertiary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                score >= 85 -> LabStatusPassBg
                                                score >= 70 -> Color(0xFFFFFBEB)
                                                else -> Color(0xFFFEF2F2)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$score / 100",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = when {
                                            score >= 85 -> LabStatusPass
                                            score >= 70 -> LabSeverityP2
                                            else -> LabStatusFail
                                        }
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteSession(s.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        tint = LabTextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonDiffCard(
    sessionA: TestSession,
    sessionB: TestSession,
    onDismiss: () -> Unit
) {
    val diff = SessionDiff.calculate(sessionA, sessionB)
    val scoreA = sessionA.healthScore?.overallScore ?: 0
    val scoreB = sessionB.healthScore?.overallScore ?: 0
    val p0A = sessionA.issues.count { it.severity == IssueSeverity.P0 }
    val p0B = sessionB.issues.count { it.severity == IssueSeverity.P0 }
    val p1A = sessionA.issues.count { it.severity == IssueSeverity.P1 }
    val p1B = sessionB.issues.count { it.severity == IssueSeverity.P1 }
    val p2A = sessionA.issues.count { it.severity == IssueSeverity.P2 }
    val p2B = sessionB.issues.count { it.severity == IssueSeverity.P2 }

    LabCard(
        borderColor = LabPrimary
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RUN COMPARISON (RUN A vs. RUN B)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Close",
                    fontSize = 11.sp,
                    color = LabTextTertiary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Metric", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabTextSecondary)
                Text("Previous → Current", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabTextSecondary)
                Text("Delta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabTextSecondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            DiffRow("Health Score", "$scoreA → $scoreB", if (diff.healthScoreDelta >= 0) "+${diff.healthScoreDelta}" else "${diff.healthScoreDelta}", diff.healthScoreDelta >= 0)
            DiffRow("P0 Blockers", "$p0A → $p0B", if (p0B <= p0A) "Resolved" else "+${p0B - p0A} (REGRESSION)", p0B <= p0A)
            DiffRow("P1 Critical", "$p1A → $p1B", if (p1B <= p1A) "Improved" else "+${p1B - p1A} (REGRESSION)", p1B <= p1A)
            DiffRow("P2 High", "$p2A → $p2B", if (p2B <= p2A) "Improved" else "+${p2B - p2A}", p2B <= p2A)

            if (diff.regressions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "REGRESSIONS INTRODUCED (${diff.regressions.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabSeverityP0
                )
                diff.regressions.take(3).forEach { r ->
                    Text(
                        text = "• [${r.severity.code}] ${r.title}",
                        fontSize = 11.sp,
                        color = LabTextPrimary
                    )
                }
            }

            if (diff.resolvedIssues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RESOLVED ISSUES (${diff.resolvedIssues.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabStatusPass
                )
                diff.resolvedIssues.take(3).forEach { res ->
                    Text(
                        text = "✓ [${res.severity.code}] ${res.title}",
                        fontSize = 11.sp,
                        color = LabTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffRow(label: String, transition: String, delta: String, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LabTextPrimary)
        Text(text = transition, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = LabTextSecondary)
        Text(
            text = delta,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isPositive) LabStatusPass else LabSeverityP0
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ComparisonDiffCardPreview() {
    val sampleA = TestSession(
        id = "SESSION-A",
        apkArtifactId = "apk-1",
        packageName = "com.sample.app",
        appName = "Sample App",
        versionName = "1.0.0",
        apkSha256 = "abc123sha",
        deviceModel = "Pixel 8",
        androidVersion = "Android 15",
        startTimestamp = 1700000000000L,
        healthScore = com.example.elenchos.domain.model.HealthScore(75, 70, 80, 80, 70, 80, 70, "Initial run")
    )
    val sampleB = sampleA.copy(
        id = "SESSION-B",
        versionName = "1.1.0",
        healthScore = com.example.elenchos.domain.model.HealthScore(88, 90, 85, 90, 80, 90, 85, "After fix")
    )
    ComparisonDiffCard(sessionA = sampleA, sessionB = sampleB, onDismiss = {})
}
