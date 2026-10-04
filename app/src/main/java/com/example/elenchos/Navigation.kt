package com.example.elenchos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabBorderSubtle
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary
import com.example.elenchos.ui.screens.AIFixPackageScreen
import com.example.elenchos.ui.screens.HistoryScreen
import com.example.elenchos.ui.screens.HomeScreen
import com.example.elenchos.ui.screens.IssueExplorerScreen
import com.example.elenchos.ui.screens.ProjectsScreen
import com.example.elenchos.ui.screens.ReportsScreen
import com.example.elenchos.ui.screens.SettingsScreen
import com.example.elenchos.ui.screens.TestLabScreen
import com.example.elenchos.ui.viewmodel.ElenchosViewModel
import com.example.elenchos.ui.navigation.NavigationScreen

@Composable
fun MainNavigation(
    viewModel: ElenchosViewModel = viewModel(factory = ElenchosViewModel.Factory)
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val selectedApk by viewModel.selectedApk.collectAsState()
    val isRunning by viewModel.isTestingRunning.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != NavigationScreen.HOME) {
        viewModel.navigateTo(NavigationScreen.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LabTopAppBar(
                currentScreen = currentScreen,
                selectedApkName = selectedApk?.appName,
                isRunning = isRunning,
                onHistoryClick = { viewModel.navigateTo(NavigationScreen.HISTORY) },
                onSettingsClick = { viewModel.navigateTo(NavigationScreen.SETTINGS) }
            )
        },
        bottomBar = {
            LabBottomNavigationBar(
                currentScreen = currentScreen,
                isRunning = isRunning,
                issuesCount = activeSession?.issues?.size ?: 0,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        containerColor = LabBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    if (forward) {
                        (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> width / 4 } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeOut(animationSpec = tween(200)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> -width / 4 } + fadeIn(animationSpec = tween(280)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> width / 4 } + fadeOut(animationSpec = tween(200)))
                    }
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    NavigationScreen.HOME -> HomeScreen(viewModel)
                    NavigationScreen.PROJECTS -> ProjectsScreen(viewModel)
                    NavigationScreen.TEST_LAB -> TestLabScreen(viewModel)
                    NavigationScreen.REPORTS -> ReportsScreen(viewModel)
                    NavigationScreen.ISSUES -> IssueExplorerScreen(viewModel)
                    NavigationScreen.AI_FIX -> AIFixPackageScreen(viewModel)
                    NavigationScreen.HISTORY -> HistoryScreen(viewModel)
                    NavigationScreen.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

@Composable
private fun LabTopAppBar(
    currentScreen: NavigationScreen,
    selectedApkName: String?,
    isRunning: Boolean,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = LabSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, LabCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.elenchos_logo),
                    contentDescription = "Elenchos Laboratory Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ELENCHOS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LabTextPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isRunning) LabPrimary else LabSurfaceVariant)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isRunning) "ACTIVE TEST" else "LAB v1.0",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) Color.White else LabTextSecondary
                            )
                        }
                    }

                    if (selectedApkName != null) {
                        Text(
                            text = "Target: $selectedApkName",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = LabTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History & Comparison",
                        tint = if (currentScreen == NavigationScreen.HISTORY) LabPrimary else LabTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Laboratory Settings",
                        tint = if (currentScreen == NavigationScreen.SETTINGS) LabPrimary else LabTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabBottomNavigationBar(
    currentScreen: NavigationScreen,
    isRunning: Boolean,
    issuesCount: Int,
    onNavigate: (NavigationScreen) -> Unit
) {
    NavigationBar(
        containerColor = LabSurface,
        contentColor = LabTextPrimary,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, LabCardBorder)
    ) {
        val navItems = listOf(
            Triple(NavigationScreen.HOME, "Home", Icons.Default.Home),
            Triple(NavigationScreen.PROJECTS, "Projects", Icons.Default.Folder),
            Triple(NavigationScreen.TEST_LAB, "Test Lab", Icons.Default.PlayCircle),
            Triple(NavigationScreen.REPORTS, "Reports", Icons.Default.Assessment),
            Triple(NavigationScreen.ISSUES, "Issues", Icons.Default.BugReport),
            Triple(NavigationScreen.AI_FIX, "AI Fix", Icons.Default.Shield)
        )

        navItems.forEach { (screen, label, icon) ->
            val isSelected = currentScreen == screen

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    if (screen == NavigationScreen.ISSUES && issuesCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = LabSeverityP0,
                                    contentColor = Color.White
                                ) {
                                    Text("$issuesCount", fontSize = 9.sp)
                                }
                            }
                        ) {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                        }
                    } else if (screen == NavigationScreen.TEST_LAB && isRunning) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = LabPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text("●", fontSize = 8.sp)
                                }
                            }
                        ) {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LabPrimary,
                    selectedTextColor = LabPrimary,
                    unselectedIconColor = LabTextSecondary,
                    unselectedTextColor = LabTextSecondary,
                    indicatorColor = LabPrimaryLight
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LabTopAppBarPreview() {
    LabTopAppBar(
        currentScreen = NavigationScreen.HOME,
        selectedApkName = "SampleTargetApp.apk",
        isRunning = true,
        onHistoryClick = {},
        onSettingsClick = {}
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LabBottomNavigationBarPreview() {
    LabBottomNavigationBar(
        currentScreen = NavigationScreen.HOME,
        isRunning = false,
        issuesCount = 3,
        onNavigate = {}
    )
}
