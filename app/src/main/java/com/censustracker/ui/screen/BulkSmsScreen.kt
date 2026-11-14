package com.censustracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.BulkSmsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkSmsScreen(
    viewModel: BulkSmsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }
    var presetMenuExpanded by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Send Broadcast SMS") },
            text = {
                Text(
                    "Send this message via SMS to ${state.selectedIds.size} selected respondent(s)? " +
                            "Each recipient gets their own personalized copy and it is logged in their chat."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false
                    viewModel.sendBroadcast()
                }) {
                    Text("Send Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast SMS") },
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
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Progress / result banner while or after sending
            if (state.isSending || state.finished) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (state.isSending) {
                            Text(
                                text = "Sending... ${state.sentCount + state.failedCount} / ${state.totalToSend}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (state.totalToSend > 0)
                                        (state.sentCount + state.failedCount).toFloat() / state.totalToSend
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.failedCount == 0) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (state.failedCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Done: ${state.sentCount} sent, ${state.failedCount} failed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (state.failedCount > 0) {
                                Text(
                                    text = "Failed messages are saved in each chat and the retry queue.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recipients header + select actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recipients (${state.selectedIds.size}/${state.contacts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = { viewModel.onSelectAll() }) { Text("All") }
                    TextButton(onClick = { viewModel.onClearSelection() }) { Text("None") }
                }
            }

            // Contact checklist
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.contacts, key = { it.id }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = contact.id in state.selectedIds,
                            onCheckedChange = { viewModel.onToggleContact(contact.id) },
                            enabled = !state.isSending
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Message composer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Message (placeholders resolved per recipient)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Preset picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { presetMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSending
                    ) {
                        Text(
                            text = state.presets.firstOrNull { it.bodyTemplate == state.body }?.title
                                ?: "Select preset template",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = presetMenuExpanded,
                        onDismissRequest = { presetMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Custom message") },
                            onClick = {
                                presetMenuExpanded = false
                                viewModel.onPresetSelected(null)
                            }
                        )
                        state.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.title) },
                                onClick = {
                                    presetMenuExpanded = false
                                    viewModel.onPresetSelected(preset)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.body,
                    onValueChange = { viewModel.onBodyChanged(it) },
                    placeholder = { Text("Type broadcast message or insert placeholders...") },
                    minLines = 3,
                    enabled = !state.isSending,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PlaceholderResolver.availablePlaceholders().forEach { token ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.onBodyChanged(state.body + " " + token) },
                            enabled = !state.isSending,
                            label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showConfirm = true },
                    enabled = state.selectedIds.isNotEmpty() && state.body.isNotBlank() && !state.isSending,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send to ${state.selectedIds.size} Recipient(s)")
                }
            }
        }

        ToastNotification(
            message = state.toastError,
            isError = true,
            onDismiss = { viewModel.clearToast() }
        )
    }
}
