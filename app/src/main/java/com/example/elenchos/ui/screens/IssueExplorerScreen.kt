package com.example.elenchos.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.domain.model.Issue
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.reporting.BugTrackerFormatter
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP1
import com.example.elenchos.theme.LabSeverityP2
import com.example.elenchos.theme.LabSeverityP3
import com.example.elenchos.theme.LabSeverityP4
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabStatusPassBg
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTerminalBg
import com.example.elenchos.theme.LabTerminalText
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.components.SeverityBadge
import com.example.elenchos.ui.viewmodel.ElenchosViewModel

@Composable
fun IssueExplorerScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val apk by viewModel.selectedApk.collectAsState()
    val severityFilter by viewModel.issueSeverityFilter.collectAsState()
    val searchQuery by viewModel.issueSearchQuery.collectAsState()
    val context = LocalContext.current

    val allIssues = session?.issues ?: emptyList()

    val filteredIssues = allIssues.filter { issue ->
        val matchesSeverity = severityFilter == null || issue.severity == severityFilter
        val matchesSearch = searchQuery.isBlank() ||
                issue.title.contains(searchQuery, ignoreCase = true) ||
                issue.id.contains(searchQuery, ignoreCase = true) ||
                issue.description.contains(searchQuery, ignoreCase = true) ||
                issue.affectedScreen.contains(searchQuery, ignoreCase = true)
        matchesSeverity && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Issue Explorer",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = LabTextPrimary
        )
        Text(
            text = "${filteredIssues.size} of ${allIssues.size} issues matching active criteria",
            fontSize = 12.sp,
            color = LabTextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(LabSurface)
                .border(1.dp, LabCardBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = LabTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setIssueSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search by title, ID, component, or keyword...", fontSize = 12.sp, color = LabTextTertiary)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Severity Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    label = "ALL (${allIssues.size})",
                    isSelected = severityFilter == null,
                    onClick = { viewModel.setIssueSeverityFilter(null) }
                )
            }
            items(IssueSeverity.values()) { sev ->
                val count = allIssues.count { it.severity == sev }
                FilterChip(
                    label = "${sev.code} ($count)",
                    isSelected = severityFilter == sev,
                    onClick = { viewModel.setIssueSeverityFilter(sev) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredIssues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No issues match the current filter", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LabTextPrimary)
                    Text("Try clearing search keywords or selecting 'ALL'.", fontSize = 11.sp, color = LabTextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredIssues) { issue ->
                    ExpandableIssueCard(
                        issue = issue,
                        packageName = apk?.packageName ?: "com.target.app",
                        onCopyGitHub = {
                            val trackerText = BugTrackerFormatter.formatIssueForGitHub(issue, apk?.packageName ?: "com.target.app")
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("Issue #${issue.id}", trackerText))
                            viewModel.showToast("Copied Issue #${issue.id} to clipboard!")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) LabPrimary else LabSurface)
            .border(1.dp, if (isSelected) LabPrimary else LabCardBorder, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else LabTextSecondary
        )
    }
}

@Composable
private fun ExpandableIssueCard(
    issue: Issue,
    packageName: String,
    onCopyGitHub: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    LabCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(severity = issue.severity)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = issue.id,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LabTextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopyGitHub,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Bug Tracker Markdown",
                            tint = LabTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = LabTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = issue.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            Text(
                text = "Component: ${issue.affectedScreen}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = LabTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = issue.description,
                        fontSize = 12.sp,
                        color = LabTextPrimary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (issue.reproductionSteps.isNotEmpty()) {
                        Text(
                            text = "STEPS TO REPRODUCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        issue.reproductionSteps.forEach { step ->
                            Text(text = step, fontSize = 11.sp, color = LabTextPrimary, modifier = Modifier.padding(vertical = 1.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (!issue.stackTrace.isNullOrBlank()) {
                        Text(
                            text = "STACK TRACE / EVIDENCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(LabTerminalBg)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = issue.stackTrace,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LabTerminalText
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Root cause hypothesis
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(LabPrimaryLight)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Root Cause Hypothesis (${issue.hypothesisConfidence.label} Confidence):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LabPrimary
                            )
                            Text(
                                text = issue.rootCauseHypothesis,
                                fontSize = 11.sp,
                                color = LabTextPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recommended Fix
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(LabStatusPassBg)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Recommended Remediation:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LabStatusPass
                            )
                            Text(
                                text = issue.recommendedFix,
                                fontSize = 11.sp,
                                color = LabTextPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ExpandableIssueCardPreview() {
    val sampleIssue = Issue(
        id = "SEC-001",
        title = "Application debuggable flag enabled in production manifest",
        severity = IssueSeverity.P0,
        category = com.example.elenchos.domain.model.IssueCategory.SECURITY,
        confidence = com.example.elenchos.domain.model.IssueConfidence.HIGH,
        affectedScreen = "AndroidManifest.xml",
        description = "android:debuggable is set to true, permitting JDWP debugger attachment and memory dumping.",
        reproductionSteps = listOf("Extract APK manifest", "Verify android:debuggable attribute"),
        expectedBehavior = "android:debuggable should be omitted or false in release builds",
        actualBehavior = "android:debuggable=\"true\" found in manifest",
        rootCauseHypothesis = "Release build variant config did not disable debuggable",
        recommendedFix = "Set isMinifyEnabled = true and ensure debuggable is not forced true in release build type."
    )
    Box(modifier = Modifier.padding(16.dp)) {
        ExpandableIssueCard(issue = sampleIssue, packageName = "com.sample.app", onCopyGitHub = {})
    }
}
