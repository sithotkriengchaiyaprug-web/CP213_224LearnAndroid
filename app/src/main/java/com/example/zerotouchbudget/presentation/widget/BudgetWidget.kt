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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}

class BudgetWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context, WidgetEntryPoint::class.java
        )
        val transactionRepository = entryPoint.transactionRepository()
        val dailySummaryRepository = entryPoint.dailySummaryRepository()

        val (start, end) = DateUtils.getTodayBounds()
        val today = DateUtils.getCurrentDateString()
        val todaySpent = transactionRepository.getTotalSpentForDate(start, end)
        val budgetLimit = dailySummaryRepository.getSummaryForDate(today).first()?.budgetLimit ?: 100.0
        val remaining = budgetLimit - todaySpent
        val todayTransactions = transactionRepository.getTodayTransactions(start, end).first()
        val transactionCount = todayTransactions.size
        val spentPercentage = if (budgetLimit > 0) (todaySpent / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f

        provideContent {
            GlanceTheme {
                WidgetContent(
                    remaining = remaining,
                    spent = todaySpent,
                    budget = budgetLimit,
                    transactionCount = transactionCount,
                    spentPercentage = spentPercentage
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
    spentPercentage: Float
) {
    val format = NumberFormat.getCurrencyInstance(Locale("th", "TH"))
    val isOverBudget = remaining < 0
    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    val today = dateFormat.format(Date())

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(
                if (isOverBudget) Color(0xFFFFEBEE)
                else Color(0xFFE8F5E9)
            )
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Zero-Touch Budget",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFF666666))
                    )
                )
            }

            Text(
                text = today,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(Color(0xFF999999))
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Remaining Amount
            Text(
                text = "Remaining",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = ColorProvider(Color(0xFF666666))
                )
            )

            Text(
                text = format.format(remaining),
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(
                        if (isOverBudget) Color(0xFFD32F2F)
                        else if (spentPercentage > 0.7f) Color(0xFFE65100)
                        else Color(0xFF2E7D32)
                    ),
                    textAlign = TextAlign.Center
                )
            )

            if (isOverBudget) {
                Text(
                    text = "Over budget!",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFFD32F2F))
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Stats Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spent
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Spent",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0xFF999999))
                        )
                    )
                    Text(
                        text = format.format(spent),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFFD32F2F))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Budget
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Budget",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0xFF999999))
                        )
                    )
                    Text(
                        text = format.format(budget),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF333333))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Count
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Items",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = ColorProvider(Color(0xFF999999))
                        )
                    )
                    Text(
                        text = "$transactionCount",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorProvider(Color(0xFF333333))
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Progress Text
            val percent = (spentPercentage * 100).toInt()
            Text(
                text = "Used $percent% of daily budget",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(
                        if (spentPercentage > 0.9f) Color(0xFFD32F2F)
                        else Color(0xFF666666)
                    )
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Tap hint
            Text(
                text = "Tap to open app",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color(0xFFBBBBBB))
                )
            )
        }
    }
}
