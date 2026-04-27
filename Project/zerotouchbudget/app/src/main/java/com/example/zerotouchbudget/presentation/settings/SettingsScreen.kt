package com.example.zerotouchbudget.presentation.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ─── Design tokens ────────────────────────────────────────────────────────────
private val Accent     = Color(0xFF3730F5)
private val AccentBg   = Color(0xFFEEEDFF)
private val TextSec    = Color(0xFF8C8CA1)
private val Success    = Color(0xFF00C97A)
private val Danger     = Color(0xFFE8433A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentBudget by viewModel.currentBudget.collectAsState()
    val isAutoScanEnabled by viewModel.isAutoScanEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val autoScannedCount by viewModel.autoScannedCount.collectAsState()
    var budgetInput by remember { mutableStateOf(currentBudget.toString()) }
    var saved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(currentBudget) {
        budgetInput = currentBudget.toInt().toString()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Settings", fontWeight = FontWeight.ExtraBold)
                            Text("Preferences & Automation",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSec)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── 1. Automation ────────────────────────────────────────────
                SettingSection(label = "AUTOMATION") {
                    // Auto-scan toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-scan notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(
                                if (isAutoScanEnabled) "ทำงานอัตโนมัติ (สแกนสำเร็จแล้ว: $autoScannedCount รูป)"
                                else "Scanning is paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAutoScanEnabled) Success else TextSec
                            )
                        }
                        Switch(
                            checked = isAutoScanEnabled,
                            onCheckedChange = { viewModel.toggleAutoScan(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Accent
                            )
                        )
                    }

                    if (isAutoScanEnabled) {
                        HorizontalDivider(thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SettingRow(
                            icon = { Icon(Icons.Default.Sync, null, Modifier.size(18.dp), tint = Accent) },
                            label = "Sync history",
                            sublabel = "Pull past slips",
                            onClick = { viewModel.scanExistingImages() }
                        )

                        HorizontalDivider(thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SettingRow(
                            icon = { Icon(Icons.Default.Refresh, null, Modifier.size(18.dp), tint = Danger) },
                            label = "Force rescan",
                            sublabel = "Re-process all notifications",
                            labelColor = Danger,
                            onClick = { viewModel.forceRescan() }
                        )
                    }
                }

                // ── 2. Daily Budget ──────────────────────────────────────────
                SettingSection(label = "DAILY BUDGET") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSec)
                            Text("฿${currentBudget.toInt()} / day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Accent)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Quick presets
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100, 300, 500).forEach { amount ->
                                val selected = currentBudget.toInt() == amount
                                OutlinedButton(
                                    onClick = {
                                        budgetInput = amount.toString()
                                        viewModel.saveBudget(amount.toDouble())
                                        saved = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) AccentBg else Color.Transparent,
                                        contentColor = if (selected) Accent else TextSec
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        width = if (selected) 2.dp else 1.dp
                                    )
                                ) {
                                    Text("฿$amount", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Custom input
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it; saved = false },
                            label = { Text("Custom amount (THB)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val v = budgetInput.toDoubleOrNull()
                                if (v != null && v > 0) {
                                    viewModel.saveBudget(v)
                                    saved = true
                                    Toast.makeText(context, "Budget saved ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D0D14))
                        ) {
                            Text(if (saved) "Saved ✓" else "Update budget",
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── 3. Widget ────────────────────────────────────────────────
                SettingSection(label = "WIDGET") {
                    SettingRow(
                        icon = { Icon(Icons.Default.Refresh, null, Modifier.size(18.dp), tint = Accent) },
                        label = "Reload Widget",
                        sublabel = "Force refresh home screen widget",
                        onClick = { viewModel.refreshWidget() }
                    )
                }

                // ── 4. System ────────────────────────────────────────────────
                SettingSection(label = "SYSTEM") {
                    SettingRow(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(TextSec.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp))
                            )
                        },
                        label = "Configure notification access",
                        sublabel = "Required for auto-scan",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }

                // ── About ────────────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Zero-Touch Budget v1.1.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSec)
                    Text("Smart Realtime Scan + AI Deduplication",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSec.copy(alpha = 0.6f))
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Accent, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("กำลังสแกนสลิปย้อนหลัง...", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ─── Reusable section card ────────────────────────────────────────────────────
@Composable
private fun SettingSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSec,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

// ─── Tappable settings row ────────────────────────────────────────────────────
@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    label: String,
    sublabel: String,
    labelColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (labelColor == Color.Unspecified)
                        MaterialTheme.colorScheme.onSurface else labelColor)
                Text(sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSec)
            }
            Text("›", fontSize = 18.sp, color = TextSec)
        }
    }
}
