package com.example.elenchos.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP1
import com.example.elenchos.theme.LabSeverityP2
import com.example.elenchos.theme.LabSeverityP3
import com.example.elenchos.theme.LabSeverityP4
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.components.SeverityBadge
import com.example.elenchos.ui.components.TerminalConsoleView
import com.example.elenchos.ui.viewmodel.ElenchosViewModel
import com.example.elenchos.ui.viewmodel.NavigationScreen

@Composable
fun TestLabScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val isRunning by viewModel.isTestingRunning.collectAsState()
    val phases by viewModel.activePhases.collectAsState()
    val progressPercent by viewModel.activeProgressPercent.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        label = "testProgressBar"
    )

    val currentPhase = phases.firstOrNull { it.isCurrent }?.phaseName
        ?: if (isRunning) "Executing Test Phases" else "Test Complete"

    val issues = activeSession?.issues ?: emptyList()
    val p0Count = issues.count { it.severity == IssueSeverity.P0 }
    val p1Count = issues.count { it.severity == IssueSeverity.P1 }
    val p2Count = issues.count { it.severity == IssueSeverity.P2 }
    val p3Count = issues.count { it.severity == IssueSeverity.P3 }
    val p4Count = issues.count { it.severity == IssueSeverity.P4 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cockpit Header & Progress
        LabCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRunning) "TESTING IN PROGRESS" else "TEST SESSION COMPLETED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) LabPrimary else LabStatusPass,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = selectedApk?.appName ?: "Target Application",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTextPrimary
                        )
                    }

                    Text(
                        text = "$progressPercent%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = LabPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = LabPrimary,
                    trackColor = LabSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Phase: $currentPhase",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LabTextSecondary
                    )

                    if (isRunning) {
                        OutlinedButton(
                            onClick = { viewModel.abortCurrentTest() },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LabSeverityP0)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ABORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.navigateTo(NavigationScreen.REPORTS) },
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VIEW REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(NavigationScreen.AI_FIX) },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI FIX PACKAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Issues Summary Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IssueCountBadge("P0 BLOCKER", p0Count, LabSeverityP0, Modifier.weight(1f))
            IssueCountBadge("P1 CRITICAL", p1Count, LabSeverityP1, Modifier.weight(1f))
            IssueCountBadge("P2 HIGH", p2Count, LabSeverityP2, Modifier.weight(1f))
            IssueCountBadge("P3/P4", p3Count + p4Count, LabSeverityP3, Modifier.weight(1f))
        }

        // Terminal Live Console View
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TECHNICAL LABORATORY CONSOLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabTextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${activeSession?.terminalLogs?.size ?: 0} events",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LabTextTertiary
                )
            }

            TerminalConsoleView(
                logs = activeSession?.terminalLogs ?: emptyList(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun IssueCountBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    LabCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (count > 0) color else LabTextSecondary
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextTertiary,
                maxLines = 1
            )
        }
    }
}
