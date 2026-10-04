package com.example.elenchos.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP1
import com.example.elenchos.theme.LabSeverityP2
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.HealthScoreGauge
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.components.SeverityBadge
import com.example.elenchos.ui.viewmodel.ElenchosViewModel
import com.example.elenchos.ui.navigation.NavigationScreen

@Composable
fun ReportsScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()

    if (session == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(LabBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Assessment, contentDescription = null, tint = LabTextTertiary, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No Active Report Available", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LabTextPrimary)
            Text("Run an automated test from the Dashboard or Projects view to generate a report.", fontSize = 12.sp, color = LabTextSecondary)
        }
        return
    }

    val s = session!!
    val a = selectedApk ?: com.example.elenchos.domain.model.APKArtifact(
        id = s.apkArtifactId,
        filePath = "",
        sha256 = s.apkSha256,
        fileSizeBytes = 0L,
        formattedSize = "Archived",
        appName = s.appName,
        packageName = s.packageName,
        versionName = s.versionName,
        versionCode = 0,
        minSdk = 24,
        targetSdk = 35
    )
    val health = s.healthScore

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overall Health Score Card
        item {
            LabCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "APP HEALTH REPORT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LabTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = a.appName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LabTextPrimary
                            )
                            Text(
                                text = "${a.packageName} v${a.versionName}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LabTextSecondary
                            )
                        }

                        HealthScoreGauge(
                            score = health?.overallScore ?: 0,
                            size = 90.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = health?.formulaExplanation ?: "Weighted laboratory score calculation.",
                        fontSize = 11.sp,
                        color = LabTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sub-score grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScoreTile("Stability", "${health?.stabilityScore ?: 0}", Modifier.weight(1f))
                        ScoreTile("Security", "${health?.securityScore ?: 0}", Modifier.weight(1f))
                        ScoreTile("Function", "${health?.functionalityScore ?: 0}", Modifier.weight(1f))
                        ScoreTile("Perf", "${health?.performanceScore ?: 0}", Modifier.weight(1f))
                        ScoreTile("A11y", "${health?.accessibilityScore ?: 0}", Modifier.weight(1f))
                        ScoreTile("Compat", "${health?.compatibilityScore ?: 0}", Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. Export & Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(NavigationScreen.AI_FIX) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI FIX PACKAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(NavigationScreen.ISSUES) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View ${s.issues.size} Issues", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Share Report Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportAndShareReport("md") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share MD", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.exportAndShareReport("html") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share HTML", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.exportAndShareReport("json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share JSON", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. Test Coverage Metrics
        item {
            LabCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEST COVERAGE METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CoverageRow("Screens Discovered & Tested", "${s.coverage.screensTested} / ${s.coverage.screensDiscovered}")
                    CoverageRow("Interactive Elements Exercised", "${s.coverage.interactiveElementsExercised} / ${s.coverage.interactiveElementsDiscovered}")
                    CoverageRow("Navigation Paths Traversed", "${s.coverage.navigationPathsExercised}")
                    CoverageRow("Boundary Input Fields Fuzzed", "${s.coverage.inputFieldsTested}")
                    CoverageRow("Permissions Audited", "${s.coverage.permissionFlowsTested}")
                    CoverageRow("Lifecycle & State Recreations", "${s.coverage.lifecycleScenariosTested}")
                }
            }
        }

        // Screen Graph Topology Card
        if (s.screenGraph.nodes.isNotEmpty()) {
            item {
                LabCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCREEN GRAPH TOPOLOGY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LabTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${s.screenGraph.nodes.size} nodes • ${s.screenGraph.transitions.size} transitions",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LabPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        s.screenGraph.nodes.take(5).forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${node.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LabTextPrimary
                                )
                                Text(
                                    text = "${node.interactiveElementCount} interactive elements",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = LabTextTertiary
                                )
                            }
                        }
                        if (s.screenGraph.nodes.size > 5) {
                            Text(
                                text = "+ ${s.screenGraph.nodes.size - 5} more screen nodes mapped",
                                fontSize = 11.sp,
                                color = LabTextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Top Problems Prioritized
        item {
            Text(
                text = "TOP DEFECTS & FINDINGS (${s.issues.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextSecondary,
                letterSpacing = 1.sp
            )
        }

        items(s.issues.take(6)) { issue ->
            LabCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeverityBadge(severity = issue.severity)
                        Text(
                            text = issue.id,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LabTextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = issue.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = issue.description,
                        fontSize = 11.sp,
                        color = LabTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Probable Cause: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabPrimary
                        )
                        Text(
                            text = issue.rootCauseHypothesis,
                            fontSize = 11.sp,
                            color = LabTextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTile(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(LabSurfaceVariant, RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = LabTextPrimary
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = LabTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CoverageRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = LabTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = LabTextPrimary)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ReportsScreenComponentsPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreTile("Stability", "95", Modifier.weight(1f))
            ScoreTile("Security", "88", Modifier.weight(1f))
            ScoreTile("Function", "92", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        CoverageRow("Screens Discovered", "8 / 8")
        CoverageRow("Interactive Elements Exercised", "32 / 38")
    }
}
