package com.example.elenchos.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.elenchos.reporting.AIFixPackageGenerator
import com.example.elenchos.reporting.BugTrackerFormatter
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabTerminalBg
import com.example.elenchos.theme.LabTerminalText
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.viewmodel.ElenchosViewModel

@Composable
fun AIFixPackageScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val apk by viewModel.selectedApk.collectAsState()
    val context = LocalContext.current

    if (session == null || apk == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(LabBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = LabTextTertiary, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No AI Fix Package Generated", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LabTextPrimary)
            Text("Run an automated test first to generate actionable AI debugging context.", fontSize = 12.sp, color = LabTextSecondary)
        }
        return
    }

    val s = session!!
    val a = apk!!

    val pkg = remember(s, a) { AIFixPackageGenerator.generatePackage(s, a) }
    val promptText = remember(pkg) { AIFixPackageGenerator.generateAIAgentHandoffPrompt(pkg) }
    val jsonText = remember(pkg) { AIFixPackageGenerator.exportJson(pkg) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("AI Agent Prompt", "JSON Package", "Bug Tracker")

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
                    text = "AI Fix Package",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabTextPrimary
                )
                Text(
                    text = "Handoff context tailored for autonomous coding agents",
                    fontSize = 12.sp,
                    color = LabTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Prominent COPY AI FIX PROMPT action card
        LabCard(
            borderColor = LabPrimary.copy(alpha = 0.4f),
            backgroundColor = LabPrimaryLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "READY FOR AGENT CONSUMPTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Senior Engineer Handoff Prompt",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTextPrimary
                        )
                        Text(
                            text = "Contains project architecture, reproduction steps, stack traces, and root cause hypotheses for ${s.issues.size} issues.",
                            fontSize = 11.sp,
                            color = LabTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.copyAIFixPromptToClipboard() },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY AI FIX PROMPT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("Elenchos AI JSON", jsonText))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // View Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = LabSurface,
            contentColor = LabPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, LabCardBorder, RoundedCornerShape(6.dp))
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Text Viewer Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (selectedTab == 1) LabTerminalBg else LabSurface)
                .border(1.dp, LabCardBorder, RoundedCornerShape(6.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val displayText = when (selectedTab) {
                0 -> promptText
                1 -> jsonText
                else -> {
                    s.issues.joinToString("\n\n---\n\n") {
                        BugTrackerFormatter.formatIssueForGitHub(it, a.packageName)
                    }
                }
            }

            Text(
                text = displayText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (selectedTab == 1) LabTerminalText else LabTextPrimary,
                lineHeight = 16.sp
            )
        }
    }
}
