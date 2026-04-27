package com.example.zerotouchbudget.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.zerotouchbudget.di.WidgetEntryPoint
import com.example.zerotouchbudget.domain.util.DateUtils
import com.example.zerotouchbudget.presentation.home.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

class BudgetWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context, WidgetEntryPoint::class.java
        )
        val repository = entryPoint.transactionRepository()
        val prefs = entryPoint.appPreferences()
        val summaryRepo = entryPoint.dailySummaryRepository()

        // ดึง budget จาก DailySummaryRepository (เหมือนกับที่แอปหลักใช้)
        val today = DateUtils.getCurrentDateString()
        val todaySummary = summaryRepo.getSummaryForDate(today).first()
        val budgetLimit = todaySummary?.budgetLimit ?: prefs.dailyBudget // fallback to prefs

        val (start, end) = DateUtils.getTodayBounds()
        val todaySpent = repository.getTotalSpentForDate(start, end)
        val remaining = budgetLimit - todaySpent
        val todayTransactions = repository.getTodayTransactions(start, end).first()
        val transactionCount = todayTransactions.size
        val lastTransaction = todayTransactions.firstOrNull()
        val spentPercentage = if (budgetLimit > 0) (todaySpent / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

        provideContent {
            GlanceTheme {
                WidgetContent(
                    remaining = remaining,
                    spent = todaySpent,
                    budget = budgetLimit,
                    transactionCount = transactionCount,
                    spentPercentage = spentPercentage,
                    lastBrand = lastTransaction?.brand ?: ""
                )
            }
        }
    }
}

@Composable
fun WidgetContent(
    remaining: Double,
    spent: Double,
    budget: Double,
    transactionCount: Int,
    spentPercentage: Float,
    lastBrand: String
) {
    val fmt = NumberFormat.getNumberInstance(Locale("th", "TH")).apply {
        maximumFractionDigits = 0
    }

    val isOverBudget = remaining < 0
    val isWarning = spentPercentage > 0.75f && !isOverBudget
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", Locale("th")))
    val percent = (spentPercentage * 100).toInt()

    // Color palette
    val bgColor = Color(0xFF0D1117)
    val cardColor = Color(0xFF161B22)
    val accentGreen = Color(0xFF00E676)
    val accentOrange = Color(0xFFFFB300)
    val accentRed = Color(0xFFFF5252)
    val textSecondary = Color(0xFF8B949E)
    val progressBg = Color(0xFF21262D)

    val amountColor = when {
        isOverBudget -> accentRed
        isWarning -> accentOrange
        else -> accentGreen
    }
    val statusEmoji = when {
        isOverBudget -> "🔴"
        isWarning -> "🟡"
        else -> "🟢"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Header Row ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$statusEmoji งบวันนี้",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(textSecondary)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = today,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(textSecondary)
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                // ปุ่ม Reload บน Widget
                Text(
                    text = "↻",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFF58A6FF))
                    ),
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<RefreshWidgetAction>()
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // ── Remaining Amount ──
            Text(
                text = "คงเหลือ",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(textSecondary)
                )
            )
            Text(
                text = "฿${fmt.format(kotlin.math.abs(remaining))}${if (isOverBudget) " (เกิน)" else ""}",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(amountColor),
                    textAlign = TextAlign.Start
                )
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ── Progress Bar (text-based for Glance compatibility) ──
            val filled = ((spentPercentage * 12).toInt()).coerceIn(0, 12)
            val empty = 12 - filled
            val barText = "${"||".repeat(filled)}${"  ".repeat(empty)}"
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color(0xFF21262D))
                    .cornerRadius(4.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = barText,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(amountColor)
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            // ── Progress Label ──
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "ใช้ไป $percent%",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(textSecondary)),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "฿${fmt.format(budget)}",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(textSecondary))
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ── Stats Row ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Spent
                Column {
                    Text(
                        text = "ใช้ไปแล้ว",
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(textSecondary))
                    )
                    Text(
                        text = "฿${fmt.format(spent)}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(accentRed)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(20.dp))

                // Transactions
                Column {
                    Text(
                        text = "รายการ",
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(textSecondary))
                    )
                    Text(
                        text = "$transactionCount รายการ",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFFCDD9E5))
                        )
                    )
                }

                // Last brand (if available)
                if (lastBrand.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width(20.dp))
                    Column {
                        Text(
                            text = "ล่าสุด",
                            style = TextStyle(fontSize = 10.sp, color = ColorProvider(textSecondary))
                        )
                        Text(
                            text = lastBrand.take(10),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFFCDD9E5))
                            )
                        )
                    }
                }
            }
        }
    }
}