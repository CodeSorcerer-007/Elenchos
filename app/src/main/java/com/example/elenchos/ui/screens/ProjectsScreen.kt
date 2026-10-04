package com.example.elenchos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.FileProvider
import com.example.elenchos.domain.model.APKArtifact
import com.example.elenchos.domain.model.ComponentInfo
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP2
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
import java.io.File

@Composable
fun ProjectsScreen(
    viewModel: ElenchosViewModel,
    modifier: Modifier = Modifier
) {
    val apks by viewModel.apks.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importApkFromUri(uri)
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Components", "Permissions", "Security Scan")

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
                    text = "APK Intelligence & Projects",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabTextPrimary
                )
                Text(
                    text = "${apks.size} APK artifact(s) managed in local laboratory",
                    fontSize = 12.sp,
                    color = LabTextSecondary
                )
            }

            Button(
                onClick = { filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LabPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (apks.isEmpty()) {
            EmptyApkState(onImportClick = {
                filePicker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
            })
        } else {
            // Horizontal list of loaded APKs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(apks) { apk ->
                    val isSelected = selectedApk?.id == apk.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) LabPrimaryLight else LabSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) LabPrimary else LabCardBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.selectApk(apk) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = apk.appName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) LabPrimary else LabTextPrimary
                            )
                            Text(
                                text = "${apk.packageName.take(24)}... (${apk.formattedSize})",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = LabTextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            selectedApk?.let { apk ->
                // Tabs
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

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                ApkOverviewCard(
                                    apk = apk,
                                    onTestClick = { viewModel.startTestSession() },
                                    onInstallClick = {
                                        try {
                                            val file = File(apk.filePath)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.android.package-archive")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(installIntent)
                                        } catch (_: Exception) {}
                                    },
                                    onDeleteClick = { viewModel.deleteSelectedApk() }
                                )
                            }
                        }
                        1 -> {
                            item {
                                ComponentsCard(
                                    title = "Activities (${apk.activities.size})",
                                    components = apk.activities
                                )
                            }
                            item {
                                ComponentsCard(
                                    title = "Services (${apk.services.size})",
                                    components = apk.services
                                )
                            }
                            item {
                                ComponentsCard(
                                    title = "Broadcast Receivers (${apk.receivers.size})",
                                    components = apk.receivers
                                )
                            }
                            item {
                                ComponentsCard(
                                    title = "Content Providers (${apk.providers.size})",
                                    components = apk.providers
                                )
                            }
                        }
                        2 -> {
                            item {
                                PermissionsCard(
                                    dangerous = apk.dangerousPermissions,
                                    all = apk.permissions
                                )
                            }
                        }
                        3 -> {
                            item {
                                SecurityOverviewCard(apk = apk)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyApkState(onImportClick: () -> Unit) {
    LabCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = LabTextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No APK Imported",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Text(
                text = "Select an Android APK file from your phone storage to generate its Intelligence Summary and run diagnostics.",
                fontSize = 12.sp,
                color = LabTextSecondary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onImportClick,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
            ) {
                Text("Select APK File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApkOverviewCard(
    apk: APKArtifact,
    onTestClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    LabCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = apk.appName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LabTextPrimary
                    )
                    Text(
                        text = apk.packageName,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = LabTextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete APK",
                            tint = LabSeverityP0
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTestClick,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LabPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START TEST LAB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onInstallClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install APK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spec Table
            Text(
                text = "APK METADATA & SPECS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            MetadataRow("Version", "${apk.versionName} (Code: ${apk.versionCode})")
            MetadataRow("SDK Levels", "Min: ${apk.minSdk} | Target: ${apk.targetSdk} | Compile: ${apk.compileSdk}")
            MetadataRow("File Size", "${apk.formattedSize} (${apk.fileSizeBytes} bytes)")
            MetadataRow("DEX & Assets", "${apk.dexCount} Dex file(s) | ${apk.totalAssetsCount} Asset(s)")
            MetadataRow("Native ABIs", if (apk.nativeArchitectures.isNotEmpty()) apk.nativeArchitectures.joinToString() else "Pure JVM / No native code")
            MetadataRow("16KB Page Aligned", if (apk.is16KbPageAligned) "Compatible (Android 15+)" else "Risk: 4KB Non-aligned")
            MetadataRow("Installed Status", if (apk.isInstalledOnDevice) "Installed on Device" else "Not Installed")
            MetadataRow("SHA-256", apk.sha256)
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = LabTextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = LabTextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun ComponentsCard(
    title: String,
    components: List<ComponentInfo>
) {
    LabCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (components.isEmpty()) {
                Text("None declared in manifest", fontSize = 11.sp, color = LabTextTertiary)
            } else {
                components.forEach { comp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comp.name.substringAfterLast('.'),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LabTextPrimary
                        )
                        if (comp.isExported) {
                            StatusChip(
                                text = "EXPORTED",
                                color = LabSeverityP2,
                                bgColor = Color(0xFFFFFBEB)
                            )
                        } else {
                            StatusChip(
                                text = "INTERNAL",
                                color = LabStatusPass,
                                bgColor = LabStatusPassBg
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    dangerous: List<String>,
    all: List<String>
) {
    LabCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Dangerous / Sensitive Permissions (${dangerous.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (dangerous.isNotEmpty()) LabSeverityP2 else LabTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (dangerous.isEmpty()) {
                Text("No dangerous permissions requested", fontSize = 11.sp, color = LabStatusPass)
            } else {
                dangerous.forEach { perm ->
                    Text(
                        text = "• ${perm.substringAfterLast('.')}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = LabSeverityP2,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "All Declared Permissions (${all.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            all.forEach { perm ->
                Text(
                    text = perm,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = LabTextSecondary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SecurityOverviewCard(apk: APKArtifact) {
    LabCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Static Security Checks",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LabTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            SecurityCheckRow("Debuggable Build", !apk.isDebuggable, "android:debuggable is false", "android:debuggable is TRUE (P0 Blocker)")
            SecurityCheckRow("Cleartext HTTP", !apk.usesCleartextTraffic, "Enforces TLS/HTTPS", "Permits cleartext HTTP traffic (P2 High)")
            SecurityCheckRow("Backup Exposure", !apk.allowsBackup, "Backup disabled", "Backup enabled (P3 Medium)")
            SecurityCheckRow("Embedded Secrets", apk.detectedSecrets.isEmpty(), "No secrets detected in DEX", "${apk.detectedSecrets.size} potential credentials found in DEX (P1)")
        }
    }
}

@Composable
private fun SecurityCheckRow(title: String, isPass: Boolean, passDesc: String, failDesc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isPass) LabStatusPass else LabSeverityP0,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LabTextPrimary)
            Text(
                text = if (isPass) passDesc else failDesc,
                fontSize = 11.sp,
                color = if (isPass) LabTextSecondary else LabSeverityP0
            )
        }
    }
}
