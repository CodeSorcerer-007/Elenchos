package com.example.elenchos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.service.ElenchosLabAccessibilityService
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabStatusPassBg
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.components.LabCard
import com.example.elenchos.ui.viewmodel.ElenchosViewModel

@Composable
fun SettingsScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isA11yEnabled by viewModel.isAccessibilityEnabled.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Laboratory Settings & Privileges",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Text(
                text = "Configure test budgets, testing services, and local data storage",
                fontSize = 12.sp,
                color = LabTextSecondary
            )
        }

        // 1. Accessibility Service Card
        item {
            LabCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = LabPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Elenchos Autonomous Testing Service",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LabTextPrimary
                                )
                                Text(
                                    text = if (isA11yEnabled) "Active — Live element inspection enabled" else "Disabled — Tap to grant on-device inspection",
                                    fontSize = 11.sp,
                                    color = if (isA11yEnabled) LabStatusPass else LabSeverityP0
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Enabling the Elenchos Accessibility Service allows the laboratory to legitimately inspect active window hierarchies, auto-tap buttons, fuzz text inputs, and detect system crash popups on target APKs.",
                        fontSize = 11.sp,
                        color = LabTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            ElenchosLabAccessibilityService.openAccessibilitySettings(context)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isA11yEnabled) LabStatusPass else LabPrimary
                        )
                    ) {
                        Text(
                            text = if (isA11yEnabled) "Accessibility Service Enabled" else "Open Accessibility Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Local-First Privacy Card
        item {
            LabCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LabStatusPass,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local-First Architecture & Privacy",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your APK files and test telemetry stay exclusively on this Android device. No APK bytes or crash dumps are ever silently uploaded to external cloud servers.",
                        fontSize = 12.sp,
                        color = LabTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 3. Test Budgets Card
        item {
            LabCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEST BUDGET LIMITS & SAFEGUARDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    BudgetRow("Maximum Test Duration", "5 minutes (300s)")
                    BudgetRow("Maximum UI Interactions", "200 actions")
                    BudgetRow("Maximum Screen Discovered Nodes", "30 screens")
                    BudgetRow("Input Fuzzing Intensity", "Standard (15 boundary vectors)")
                    BudgetRow("Safe Destructive Action Filter", "Active (blocks payments/deletions)")
                }
            }
        }

        // 4. Data Management & Clean Up
        item {
            LabCard(
                borderColor = LabSeverityP0.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DATA MANAGEMENT & CLEANUP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabSeverityP0,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Clear imported APK files, historical test sessions, generated reports, and diagnostic logs from local device storage.",
                        fontSize = 12.sp,
                        color = LabTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LabSeverityP0)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Test Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Laboratory Data?") },
            text = { Text("This will permanently delete all imported APK artifacts, recorded test sessions, and diagnostic logs from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    }
                ) {
                    Text("Delete Everything", color = LabSeverityP0, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = LabTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LabTextPrimary)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun BudgetRowPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        BudgetRow("Maximum Test Duration", "5 minutes (300s)")
        BudgetRow("Maximum UI Interactions", "200 actions")
    }
}
