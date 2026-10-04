package com.example.elenchos.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TestSession
import com.example.elenchos.domain.model.TestStatus
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
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.components.StatusChip
import com.example.elenchos.ui.viewmodel.ElenchosViewModel
import com.example.elenchos.ui.navigation.NavigationScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val apks by viewModel.apks.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()
    val sessions by viewModel.sessionsHistory.collectAsState()
    val isRunning by viewModel.isTestingRunning.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importApkFromUri(uri)
        }
    }

    val totalIssuesFound = sessions.sumOf { it.issues.size }
    val totalCrashes = sessions.sumOf { s -> s.issues.count { it.severity == IssueSeverity.P0 || it.severity == IssueSeverity.P1 } }

    val statusLabel = when {
        isRunning -> "TESTING IN PROGRESS"
        sessions.any { it.status == TestStatus.CRITICAL_ISSUES_FOUND } -> "CRITICAL ISSUES FOUND"
        sessions.isNotEmpty() -> "READY FOR TESTING"
        else -> "READY FOR TESTING"
    }

    val statusColor = when {
        isRunning -> LabPrimary
        sessions.any { it.status == TestStatus.CRITICAL_ISSUES_FOUND } -> LabSeverityP0
        else -> LabStatusPass
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Status Banner
        item {
            LabCard(
                borderColor = statusColor.copy(alpha = 0.3f),
                backgroundColor = LabSurface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                letterSpacing = 1.sp
                            )
                        }

                        if (selectedApk != null) {
                            Text(
                                text = selectedApk!!.appName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LabTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Autonomous Android QA Laboratory",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextPrimary
                    )
                    Text(
                        text = "Upload APK → Deep Static Analysis → Live Execution → Prove Defects → AI Fix Package",
                        fontSize = 12.sp,
                        color = LabTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent TEST APK button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedApk != null) {
                                    viewModel.startTestSession()
                                } else {
                                    filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LabPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedApk != null) "TEST APK NOW" else "SELECT APK TO TEST",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import APK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Metrics Grid
        item {
            Text(
                text = "LABORATORY METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "APKs Tested",
                        value = "${apks.size}",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Sessions",
                        value = "${sessions.size}",
                        icon = Icons.Default.Science,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Issues Discovered",
                        value = "$totalIssuesFound",
                        icon = Icons.Default.Warning,
                        accentColor = if (totalIssuesFound > 0) LabSeverityP2 else LabTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Critical / Crashes",
                        value = "$totalCrashes",
                        icon = Icons.Default.BugReport,
                        accentColor = if (totalCrashes > 0) LabSeverityP0 else LabTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Quick Navigation Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    label = "Projects / APKs",
                    subtitle = "${apks.size} loaded",
                    icon = Icons.Default.Folder,
                    onClick = { viewModel.navigateTo(NavigationScreen.PROJECTS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    label = "AI Fix Package",
                    subtitle = "One-click Handoff",
                    icon = Icons.Default.Shield,
                    onClick = { viewModel.navigateTo(NavigationScreen.AI_FIX) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionItem(
                    label = "Compare Runs",
                    subtitle = "Regressions & Diffs",
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    onClick = { viewModel.navigateTo(NavigationScreen.HISTORY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Recent Test Runs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TEST SESSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabTextSecondary,
                    letterSpacing = 1.sp
                )
                if (sessions.isNotEmpty()) {
                    Text(
                        text = "View All (${sessions.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LabPrimary,
                        modifier = Modifier.clickable { viewModel.navigateTo(NavigationScreen.REPORTS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (sessions.isEmpty()) {
                LabCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = LabTextTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No test sessions recorded yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LabTextPrimary
                        )
                        Text(
                            text = "Import an APK above to launch the autonomous testing engine.",
                            fontSize = 12.sp,
                            color = LabTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        items(sessions.take(4)) { session ->
            RecentSessionCard(
                session = session,
                onClick = {
                    viewModel.selectSession(session)
                    viewModel.navigateTo(NavigationScreen.REPORTS)
                }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = LabTextPrimary
) {
    LabCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LabSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LabTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = LabTextSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LabCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LabPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = LabTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    session: TestSession,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)
    val dateStr = dateFormat.format(Date(session.startTimestamp))
    val score = session.healthScore?.overallScore ?: 0

    LabCard(
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "v${session.versionName}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LabTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = LabTextTertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${session.issues.size} issues found",
                        fontSize = 11.sp,
                        color = if (session.issues.isNotEmpty()) LabSeverityP2 else LabStatusPass,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            score >= 85 -> LabStatusPassBg
                            score >= 70 -> Color(0xFFFFFBEB)
                            else -> Color(0xFFFEF2F2)
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score / 100",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        score >= 85 -> LabStatusPass
                        score >= 70 -> LabSeverityP2
                        else -> LabStatusFail
                    }
                )
            }
        }
    }
}
