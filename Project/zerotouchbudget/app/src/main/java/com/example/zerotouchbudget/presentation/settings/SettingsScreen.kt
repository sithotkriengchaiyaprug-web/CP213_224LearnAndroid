package com.example.zerotouchbudget.presentation.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentBudget by viewModel.currentBudget.collectAsState()
    val isAutoScanEnabled by viewModel.isAutoScanEnabled.collectAsState()
    var budgetInput by remember { mutableStateOf(currentBudget.toString()) }
    var saved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(scrollState)
        ) {
            // 1. Auto-Scan Control
            Text(
                text = "Automation",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoScanEnabled) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Scan Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAutoScanEnabled) "ระบบทำงานอัตโนมัติเมื่อมีสลิปโอนเงิน (Zero-Touch)" else "Scanning is paused",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isAutoScanEnabled,
                        onCheckedChange = { viewModel.toggleAutoScan(it) }
                    )
                }
                
                // ปุ่ม Sync ย้อนหลัง
                if (isAutoScanEnabled) {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    TextButton(
                        onClick = { viewModel.scanExistingImages() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ดึงสลิปย้อนหลัง (Sync History)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Budget Settings
            Text(
                text = "Budgeting",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Budget Limit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Current: ฿${String.format("%.2f", currentBudget)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = {
                            budgetInput = it
                            saved = false
                        },
                        label = { Text("New Budget (THB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val newBudget = budgetInput.toDoubleOrNull()
                            if (newBudget != null && newBudget > 0) {
                                viewModel.saveBudget(newBudget)
                                saved = true
                                Toast.makeText(context, "Budget saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saved) "Saved ✓" else "Update Budget")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quick Presets", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100.0, 300.0, 500.0).forEach { amount ->
                            OutlinedButton(
                                onClick = {
                                    budgetInput = amount.toString()
                                    viewModel.saveBudget(amount)
                                    saved = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("฿${amount.toInt()}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. System Permissions
            Text(
                text = "System Permissions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure Notification Access")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. About Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Zero-Touch Budget v1.1.0", fontWeight = FontWeight.Bold)
                    Text(
                        "Smart Realtime Scan + AI Deduplication",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        val isLoading by viewModel.isLoading.collectAsState()
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("กำลังซิงค์และสแกนสลิปย้อนหลัง...", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
