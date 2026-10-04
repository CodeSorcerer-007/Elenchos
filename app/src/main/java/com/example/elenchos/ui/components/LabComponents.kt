package com.example.elenchos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elenchos.domain.model.IssueSeverity
import com.example.elenchos.domain.model.TerminalLogEntry
import com.example.elenchos.theme.LabBackground
import com.example.elenchos.theme.LabBorderSubtle
import com.example.elenchos.theme.LabCardBorder
import com.example.elenchos.theme.LabPrimary
import com.example.elenchos.theme.LabPrimaryBorder
import com.example.elenchos.theme.LabPrimaryLight
import com.example.elenchos.theme.LabSeverityP0
import com.example.elenchos.theme.LabSeverityP0Bg
import com.example.elenchos.theme.LabSeverityP1
import com.example.elenchos.theme.LabSeverityP1Bg
import com.example.elenchos.theme.LabSeverityP2
import com.example.elenchos.theme.LabSeverityP2Bg
import com.example.elenchos.theme.LabSeverityP3
import com.example.elenchos.theme.LabSeverityP3Bg
import com.example.elenchos.theme.LabSeverityP4
import com.example.elenchos.theme.LabSeverityP4Bg
import com.example.elenchos.theme.LabStatusFail
import com.example.elenchos.theme.LabStatusPass
import com.example.elenchos.theme.LabStatusPassBg
import com.example.elenchos.theme.LabStatusWarn
import com.example.elenchos.theme.LabSurface
import com.example.elenchos.theme.LabSurfaceVariant
import com.example.elenchos.theme.LabTerminalAccent
import com.example.elenchos.theme.LabTerminalBg
import com.example.elenchos.theme.LabTerminalFail
import com.example.elenchos.theme.LabTerminalSuccess
import com.example.elenchos.theme.LabTerminalText
import com.example.elenchos.theme.LabTerminalWarn
import com.example.elenchos.theme.LabTextPrimary
import com.example.elenchos.theme.LabTextSecondary
import com.example.elenchos.theme.LabTextTertiary

@Composable
fun SeverityBadge(
    severity: IssueSeverity,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderCol) = when (severity) {
        IssueSeverity.P0 -> Triple(LabSeverityP0Bg, LabSeverityP0, LabSeverityP0.copy(alpha = 0.3f))
        IssueSeverity.P1 -> Triple(LabSeverityP1Bg, LabSeverityP1, LabSeverityP1.copy(alpha = 0.3f))
        IssueSeverity.P2 -> Triple(LabSeverityP2Bg, LabSeverityP2, LabSeverityP2.copy(alpha = 0.3f))
        IssueSeverity.P3 -> Triple(LabSeverityP3Bg, LabSeverityP3, LabSeverityP3.copy(alpha = 0.3f))
        IssueSeverity.P4 -> Triple(LabSeverityP4Bg, LabSeverityP4, LabSeverityP4.copy(alpha = 0.3f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${severity.code} ${severity.label}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun LabCard(
    modifier: Modifier = Modifier,
    border: Dp = 1.dp,
    borderColor: Color = LabCardBorder,
    backgroundColor: Color = LabSurface,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(border, borderColor, shape),
        shape = shape,
        color = backgroundColor
    ) {
        content()
    }
}

@Composable
fun HealthScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "scoreProgress"
    )

    val scoreColor = when {
        score >= 85 -> LabStatusPass
        score >= 70 -> LabSeverityP2
        else -> LabStatusFail
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            color = LabSurfaceVariant,
            strokeWidth = 8.dp
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(size),
            color = scoreColor,
            strokeWidth = 8.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LabTextPrimary
            )
            Text(
                text = "/ 100",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = LabTextSecondary
            )
        }
    }
}

@Composable
fun TerminalConsoleView(
    logs: List<TerminalLogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(LabTerminalBg)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        if (logs.isEmpty()) {
            Text(
                text = "Elenchos Technical Terminal Ready. Waiting for execution events...",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = LabTextTertiary
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { entry ->
                    val color = when (entry.level) {
                        TerminalLogEntry.LogLevel.INFO -> LabTerminalText
                        TerminalLogEntry.LogLevel.TEST -> LabTerminalAccent
                        TerminalLogEntry.LogLevel.SUCCESS -> LabTerminalSuccess
                        TerminalLogEntry.LogLevel.WARN -> LabTerminalWarn
                        TerminalLogEntry.LogLevel.FAIL,
                        TerminalLogEntry.LogLevel.CRITICAL -> LabTerminalFail
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = entry.timestamp,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.width(55.dp)
                        )
                        Text(
                            text = "[${entry.tag}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LabTerminalAccent,
                            modifier = Modifier.width(65.dp)
                        )
                        Text(
                            text = entry.message,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = color,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeverityBadgePreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
        SeverityBadge(IssueSeverity.P0)
        SeverityBadge(IssueSeverity.P1)
        SeverityBadge(IssueSeverity.P2)
        SeverityBadge(IssueSeverity.P3)
        SeverityBadge(IssueSeverity.P4)
    }
}

@Preview(showBackground = true)
@Composable
fun HealthScoreGaugePreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(12.dp)) {
        HealthScoreGauge(score = 92, size = 80.dp)
        HealthScoreGauge(score = 68, size = 80.dp)
        HealthScoreGauge(score = 45, size = 80.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun LabCardPreview() {
    LabCard(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Laboratory Card Component", fontWeight = FontWeight.Bold)
            Text("Consistent 1dp bordered surface for dark and light telemetry panels.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusChipPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
        StatusChip(text = "EXPORTED", color = Color(0xFFF59E0B), bgColor = Color(0xFFFFFBEB))
        StatusChip(text = "INTERNAL", color = Color(0xFF10B981), bgColor = Color(0xFFECFDF5))
    }
}

@Preview(showBackground = true)
@Composable
fun TerminalConsoleViewPreview() {
    val sampleLogs = listOf(
        TerminalLogEntry("12:00:01", TerminalLogEntry.LogLevel.INFO, "ENGINE", "Elenchos Autonomous Engine initialized"),
        TerminalLogEntry("12:00:02", TerminalLogEntry.LogLevel.TEST, "STATIC", "Analyzing manifest security flags..."),
        TerminalLogEntry("12:00:03", TerminalLogEntry.LogLevel.CRITICAL, "SECURITY", "[P0] android:debuggable is true in release build"),
        TerminalLogEntry("12:00:04", TerminalLogEntry.LogLevel.SUCCESS, "EXPLORE", "Exercised 24 interactive controls")
    )
    TerminalConsoleView(logs = sampleLogs, modifier = Modifier.height(180.dp).padding(12.dp))
}
