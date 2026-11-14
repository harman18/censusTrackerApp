package com.censustracker.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.censustracker.ui.Screen
import com.censustracker.ui.components.ProcessingOverlay
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---- Collector Profile ----
            SettingsSection(title = "Collector Profile") {
                SettingsRow(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = state.collectorProfile?.name ?: "Not configured",
                    subtitle = state.collectorProfile?.phoneNumber ?: "Tap to set up your census collector profile",
                    onClick = { onNavigate(Screen.CollectorSetup) }
                )
            }

            // ---- Appearance ----
            SettingsSection(title = "Appearance") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark Theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Reduce eye strain in low light",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { onToggleTheme() })
                }
            }

            // ---- Defaults ----
            SettingsSection(title = "Defaults for New Contacts") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save to Phone Contacts", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Write new respondents to device contact book",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.defaultSaveToPhone,
                        onCheckedChange = { viewModel.onSaveToPhoneDefaultChanged(it) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Send SMS on Save", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Automatically text the respondent when adding a contact",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.defaultSendNow,
                        onCheckedChange = { viewModel.onSendNowDefaultChanged(it) }
                    )
                }
            }

            // ---- Messaging defaults ----
            SettingsSection(title = "Messaging Defaults") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default Country Code", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Prepended to respondent numbers, e.g. +91",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = state.defaultCountryCode,
                        onValueChange = { viewModel.onDefaultCountryCodeChanged(it) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        ),
                        modifier = Modifier.width(96.dp)
                    )
                }
            }

            // ---- Developer Options (system-level kill switches) ----
            SettingsSection(title = "Developer Options") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMS Messaging", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Send/receive SMS across the app. Turn off to disable SMS entirely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.smsEnabled,
                        onCheckedChange = { viewModel.onSmsEnabledChanged(it) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WhatsApp Messaging", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Send via WhatsApp deep links. Turn off to hide/disable WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.whatsappEnabled,
                        onCheckedChange = { viewModel.onWhatsappEnabledChanged(it) }
                    )
                }
            }

            // ---- Messaging ----
            SettingsSection(title = "Messaging") {
                SettingsRow(
                    icon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = "Preset Templates",
                    subtitle = "Manage reusable SMS templates",
                    onClick = { onNavigate(Screen.PresetMessage) }
                )
                SettingsRow(
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = "Broadcast SMS",
                    subtitle = "Send one message to many respondents",
                    onClick = { onNavigate(Screen.Broadcast) }
                )
                SettingsRow(
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = "Message Retry Queue",
                    subtitle = "Retry or remove failed / pending SMS",
                    onClick = { onNavigate(Screen.FailedMessages) }
                )
            }

            // ---- Data & Backup ----
            SettingsSection(title = "Data & Backup") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            exportLauncher.launch("census_backup_${System.currentTimeMillis()}.json")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("Export")
                    }
                    TextButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("Import")
                    }
                }
            }

            // ---- Diagnostics ----
            SettingsSection(title = "Diagnostics") {
                SettingsRow(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = "Error Log",
                    subtitle = "Review failed actions with timestamps",
                    onClick = { onNavigate(Screen.ErrorLog) }
                )
            }

            // ---- About ----
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Developer: Harmanjot Singh", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Census Tracker — Phase 1 (SMS)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    ProcessingOverlay(
        isProcessing = state.isProcessing,
        title = state.processingTitle
    )

    ToastNotification(
        message = state.toastSuccess,
        isError = false,
        onDismiss = { viewModel.clearToast() }
    )
    ToastNotification(
        message = state.toastError,
        isError = true,
        onDismiss = { viewModel.clearToast() }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
