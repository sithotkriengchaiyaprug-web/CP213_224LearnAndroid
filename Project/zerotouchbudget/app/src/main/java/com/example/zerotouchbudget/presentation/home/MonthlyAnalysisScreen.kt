package com.example.zerotouchbudget.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.NumberFormat
import java.util.*

// ─── Design tokens (inline to avoid import issues) ────────────────────────────
private val AccentColor   = Color(0xFF3730F5)
private val SuccessColor  = Color(0xFF00C97A)
private val DangerColor   = Color(0xFFE8433A)
private val CardDark      = Color(0xFF0D0D14)
private val TextSecColor  = Color(0xFF8C8CA1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAnalysisScreen(
    onBackClick: () -> Unit,
    viewModel: MonthlyAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics", fontWeight = FontWeight.ExtraBold)
                        Text("Monthly Overview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // ── Dark Summary Card ────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("THIS MONTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecColor,
                            letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("฿", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                formatSimple(uiState.thisMonthSpending),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            StatChip("Daily avg", "฿${formatSimple(uiState.dailyAvg)}")
                            StatChip("Transactions", "${uiState.dailyGroups.sumOf { it.transactions.size }}")
                            StatChip("Days active", "${uiState.dailyGroups.size}")
                        }
                    }
                }
            }

            // ── Bar Chart ────────────────────────────────────────────────────
            if (uiState.dailyGroups.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("SPENDING VS DAILY BUDGET",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecColor,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp))
                        SpendingBarChart(
                            groups = uiState.dailyGroups.takeLast(7).reversed()
                        )
                    }
                }
            }

            // ── History ───────────────────────────────────────────────────────
            item {
                Text("HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecColor,
                    letterSpacing = 1.5.sp)
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentColor)
                    }
                }
            } else if (uiState.dailyGroups.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions this month", color = TextSecColor)
                    }
                }
            } else {
                items(uiState.dailyGroups, key = { it.dateString }) { group ->
                    DaySummaryCard(group = group)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = TextSecColor)
    }
}

@Composable
private fun SpendingBarChart(groups: List<DailyTransactionGroup>) {
    val maxAmount = groups.maxOfOrNull { it.totalAmount } ?: 1.0
    val accent = AccentColor
    val danger = DangerColor
    val gridColor = Color(0xFF1A1A26)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D14))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)) {
            val barCount = groups.size
            if (barCount == 0) return@Canvas
            val barWidth = (size.width / barCount) * 0.62f
            val gap = (size.width / barCount) * 0.38f
            val chartHeight = size.height

            // Dashed grid at top
            drawLine(
                color = gridColor,
                start = Offset(0f, 2f),
                end = Offset(size.width, 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )

            groups.forEachIndexed { i, group ->
                val x = i * (barWidth + gap)
                val ratio = (group.totalAmount / maxAmount).toFloat()
                val barH = (ratio * chartHeight).coerceAtLeast(6.dp.toPx())
                val barTop = chartHeight - barH
                val color = if (ratio > 0.8f) danger else accent

                // Bar background (ghost)
                drawRoundRect(
                    color = color.copy(alpha = 0.12f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                // Filled bar
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, barTop),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }

        // X-axis labels — show day number only (e.g. "26")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            groups.forEach { group ->
                Text(
                    text = group.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun DaySummaryCard(group: DailyTransactionGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(group.dateString,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("${group.transactions.size} transaction${if (group.transactions.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "-฿${formatSimple(group.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DangerColor
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            group.transactions.forEach { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AccentColor.copy(alpha = 0.3f))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(tx.brand,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("${tx.category} • ${formatTime(tx.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("-฿${formatSimple(tx.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun formatSimple(amount: Double): String =
    if (amount % 1 == 0.0) amount.toInt().toString()
    else String.format("%.2f", amount)

private fun formatTime(timestamp: Long): String =
    java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(timestamp))
