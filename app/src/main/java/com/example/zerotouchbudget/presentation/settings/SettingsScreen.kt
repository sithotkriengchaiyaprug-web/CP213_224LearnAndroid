package com.example.zerotouchbudget.presentation.settings

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.documentfile.provider.DocumentFile
import com.example.zerotouchbudget.domain.model.AutoScanSettings
import com.example.zerotouchbudget.domain.model.AutoScanSource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentBudget by viewModel.currentBudget.collectAsState()
    val autoScanSettings by viewModel.autoScanSettings.collectAsState()

    var budgetInput by remember(currentBudget) { mutableStateOf(currentBudget.toString()) }
    var saved by remember { mutableStateOf(false) }

    var autoScanEnabled by remember(autoScanSettings.enabled) {
        mutableStateOf(autoScanSettings.enabled)
    }
    var selectedInterval by remember(autoScanSettings.intervalMinutes) {
        mutableStateOf(autoScanSettings.intervalMinutes)
    }
    var selectedSource by remember(autoScanSettings.source) {
        mutableStateOf(autoScanSettings.source)
    }
    var startAtMillis by remember(autoScanSettings.startAtMillis) {
        mutableStateOf(autoScanSettings.startAtMillis ?: System.currentTimeMillis())
    }
    var customFolderUri by remember(autoScanSettings.customFolderUri) {
        mutableStateOf(autoScanSettings.customFolderUri.orEmpty())
    }
    var photoPermissionGranted by remember {
        mutableStateOf(isPhotoPermissionGranted(context))
    }

    val mediaPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        photoPermissionGranted = granted
        Toast.makeText(
            context,
            if (granted) "Photo access granted" else "Photo access denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            customFolderUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: ${formatCurrency(currentBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = {
                            budgetInput = it
                            saved = false
                        },
                        label = { Text("New Budget (THB)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val newBudget = budgetInput.toDoubleOrNull()
                            if (newBudget != null && newBudget > 0) {
                                viewModel.saveBudget(newBudget)
                                saved = true
                                Toast.makeText(
                                    context,
                                    "Budget saved: ${formatCurrency(newBudget)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please enter a valid amount",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saved) "Saved ✓" else "Save Budget")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Scan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "One-time start delay -> periodic scan every 15/30/60 minutes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoScanEnabled,
                            onCheckedChange = { autoScanEnabled = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Interval")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntervalChoiceButton(
                            label = "15 min",
                            selected = selectedInterval == 15L,
                            onClick = { selectedInterval = 15L }
                        )
                        IntervalChoiceButton(
                            label = "30 min",
                            selected = selectedInterval == 30L,
                            onClick = { selectedInterval = 30L }
                        )
                        IntervalChoiceButton(
                            label = "60 min",
                            selected = selectedInterval == 60L,
                            onClick = { selectedInterval = 60L }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Start Time")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            pickStartDateTime(
                                context = context,
                                currentTimeMillis = startAtMillis
                            ) { picked ->
                                startAtMillis = picked
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(formatDateTime(startAtMillis))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Source")
                    Spacer(modifier = Modifier.height(8.dp))
                    SourceOptionRow(
                        label = "Screenshots",
                        selected = selectedSource == AutoScanSource.SCREENSHOTS,
                        onClick = { selectedSource = AutoScanSource.SCREENSHOTS }
                    )
                    SourceOptionRow(
                        label = "Camera",
                        selected = selectedSource == AutoScanSource.CAMERA,
                        onClick = { selectedSource = AutoScanSource.CAMERA }
                    )
                    SourceOptionRow(
                        label = "Custom Folder",
                        selected = selectedSource == AutoScanSource.CUSTOM_FOLDER,
                        onClick = { selectedSource = AutoScanSource.CUSTOM_FOLDER }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedSource == AutoScanSource.CUSTOM_FOLDER) {
                        Text(
                            text = "Folder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = resolveFolderLabel(context, customFolderUri)
                                ?: "No folder selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { folderLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose Folder")
                        }
                    } else {
                        Text(
                            text = if (photoPermissionGranted) {
                                "Photo access granted"
                            } else {
                                "Grant photo access to read screenshots/camera images"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (photoPermissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { photoPermissionLauncher.launch(mediaPermission) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Photo Access")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (autoScanEnabled) {
                            "Auto scan is enabled."
                        } else {
                            "Auto scan is disabled."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (selectedSource == AutoScanSource.CUSTOM_FOLDER && customFolderUri.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Please choose a custom folder first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            viewModel.saveAutoScanSettings(
                                AutoScanSettings(
                                    enabled = autoScanEnabled,
                                    intervalMinutes = selectedInterval,
                                    startAtMillis = startAtMillis,
                                    source = selectedSource,
                                    customFolderUri = if (selectedSource == AutoScanSource.CUSTOM_FOLDER) {
                                        customFolderUri
                                    } else {
                                        null
                                    },
                                    lastScannedAtMillis = autoScanSettings.lastScannedAtMillis
                                )
                            )

                            Toast.makeText(
                                context,
                                "Auto scan saved",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Auto Scan")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Auto-Track Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow app to read bank notifications to auto-track spending",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Notification Settings")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Zero-Touch Daily Budget App")
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Features:\n• Auto-track bank notifications\n• Manual transaction entry\n• Edit & Delete transactions\n• Daily budget tracking\n• Home screen widget\n• Receipt auto scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun IntervalChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

@Composable
private fun SourceOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

private fun pickStartDateTime(
    context: android.content.Context,
    currentTimeMillis: Long,
    onPicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val timeCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    timeCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    timeCalendar.set(Calendar.MINUTE, minute)
                    timeCalendar.set(Calendar.SECOND, 0)
                    timeCalendar.set(Calendar.MILLISECOND, 0)
                    onPicked(timeCalendar.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("th", "TH")).format(amount)
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun resolveFolderLabel(
    context: android.content.Context,
    folderUriString: String
): String? {
    if (folderUriString.isBlank()) return null
    val uri = runCatching { android.net.Uri.parse(folderUriString) }.getOrNull() ?: return null
    val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return null
    return documentFile.name ?: folderUriString
}

private fun isPhotoPermissionGranted(context: android.content.Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        permission
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
