package com.example.zerotouchbudget.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zerotouchbudget.domain.model.DailySummary
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAnalysisScreen(
    onBackClick: () -> Unit
) {
    // ในอนาคตสามารถใช้ ViewModel ดึงข้อมูล SummariesInRange ได้
    // ตอนนี้ขอแสดงเป็น UI ตัวอย่างและโครงสร้างสรุปรายเดือนก่อน
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("This Month Spending", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "฿12,450.00", 
                        fontSize = 32.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Avg: ฿415.00", style = MaterialTheme.typography.bodySmall)
                        Text("Over Budget: 3 Days", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Monthly History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // รายการสรุปแต่ละวัน (Mock data)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..30).toList().reversed()) { day ->
                    DaySummaryRow(day)
                }
            }
        }
    }
}

@Composable
fun DaySummaryRow(day: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("April $day, 2024", fontWeight = FontWeight.Medium)
                Text("5 Transactions", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                text = "฿${(200..800).random()}.00",
                fontWeight = FontWeight.Bold,
                color = if (day % 5 == 0) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
