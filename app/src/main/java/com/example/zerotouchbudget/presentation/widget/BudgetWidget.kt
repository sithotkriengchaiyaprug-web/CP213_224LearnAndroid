package com.example.zerotouchbudget.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.zerotouchbudget.di.WidgetEntryPoint
import com.example.zerotouchbudget.domain.util.DateUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
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

        val (start, end) = DateUtils.getTodayBounds()
        val todaySpent = repository.getTotalSpentForDate(start, end)
        val remaining = 100.0 - todaySpent

        provideContent {
            GlanceTheme {
                WidgetUI(remaining = remaining, spent = todaySpent)
            }
        }
    }
}

@Composable
fun WidgetUI(remaining: Double, spent: Double) {
    val format = NumberFormat.getCurrencyInstance(Locale("th", "TH"))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Remaining",
            style = TextStyle(fontSize = 14.sp)
        )
        Text(
            text = format.format(remaining),
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Spent: ${format.format(spent)}",
            style = TextStyle(fontSize = 12.sp)
        )
    }
}