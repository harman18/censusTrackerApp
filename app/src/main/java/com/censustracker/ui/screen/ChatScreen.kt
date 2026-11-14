package com.censustracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.ui.components.ProcessingOverlay
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    contactId: Long,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    var showPlaceholders by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(contactId) {
        viewModel.init(contactId)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val contact = state.contact
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = contact?.name ?: "Respondent",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = listOfNotNull(
                                    contact?.phoneNumber,
                                    contact?.houseNo?.takeIf { it.isNotBlank() }?.let { "House: $it" }
                                ).joinToString(" | "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Contact",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No messages exchanged yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use presets or composer below to send an SMS notice with auto-resolved placeholders.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        state = lazyListState
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            val isSent = message.direction == "SENT"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.82f)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isSent) 16.dp else 4.dp,
                                                bottomEnd = if (isSent) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isSent) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = message.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSent) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Channel badge (SMS / WhatsApp)
                                            Text(
                                                text = if (message.channel == "WHATSAPP") "WA" else "SMS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val timeStr = timeFormat.format(Date(message.sentAt ?: message.receivedAt ?: message.createdAt))
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )

                                            if (isSent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val isFailedStatus = message.status == "FAILED" || message.status.startsWith("FAILED_")
                                                when {
                                                    message.status == "PENDING" -> Icon(
                                                        imageVector = Icons.Default.MoreHoriz,
                                                        contentDescription = "Pending carrier dispatch",
                                                        modifier = Modifier.height(14.dp),
                                                        tint = MaterialTheme.colorScheme.outline
                                                    )
                                                    message.status == "SENT" -> Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Sent to carrier",
                                                        modifier = Modifier.height(14.dp),
                                                        tint = MaterialTheme.colorScheme.outline
                                                    )
                                                    message.status == "DELIVERED" -> Icon(
                                                        imageVector = Icons.Default.DoneAll,
                                                        contentDescription = "Delivered to device",
                                                        modifier = Modifier.height(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    isFailedStatus -> Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ErrorOutline,
                                                            contentDescription = "Failed",
                                                            modifier = Modifier.height(14.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                        IconButton(
                                                            onClick = { viewModel.retrySendMessage(message) },
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "Retry",
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Preset Message Selector directly inside Chat
                if (state.presets.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Preset Templates (Tap to customize & load):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.presets) { preset ->
                                FilterChip(
                                    selected = state.selectedPreset?.id == preset.id,
                                    onClick = { viewModel.onSelectPreset(preset) },
                                    label = { Text(preset.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // Placeholder tokens bar (toggleable)
                if (showPlaceholders) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Tap to insert token into message:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PlaceholderResolver.availablePlaceholders().forEach { token ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { viewModel.onInsertPlaceholder(token) },
                                        label = { Text(token, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Input area: channel picker row + large composer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    // Channel selector row (SMS / WhatsApp, gated by system knobs)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = state.selectedChannel == "SMS",
                            onClick = { viewModel.onChannelChanged("SMS") },
                            enabled = state.smsEnabled,
                            label = {
                                Text(
                                    if (state.smsEnabled) "SMS" else "SMS (disabled)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = state.selectedChannel == "WHATSAPP",
                            onClick = { viewModel.onChannelChanged("WHATSAPP") },
                            enabled = state.whatsappEnabled,
                            label = {
                                Text(
                                    if (state.whatsappEnabled) "WhatsApp" else "WhatsApp (disabled)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(onClick = { showPlaceholders = !showPlaceholders }) {
                            Text(if (showPlaceholders) "Hide Tokens" else "{Tokens}")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Composer row with a roomier multi-line text field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = { viewModel.onInputChanged(it) },
                            placeholder = {
                                Text(
                                    if (state.selectedChannel == "WHATSAPP")
                                        "Type WhatsApp message or tap a preset..."
                                    else
                                        "Type SMS or tap a preset..."
                                )
                            },
                            minLines = 2,
                            maxLines = 6,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.onSendMessage() },
                            enabled = state.inputText.isNotBlank() && !state.isSendingMessage
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send via ${state.selectedChannel}",
                                tint = if (state.inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        ProcessingOverlay(
            isProcessing = state.isSendingMessage,
            title = "Sending SMS...",
            subtitle = "Writing to telephony store & sending via carrier"
        )

        // Corner Toast Notification for Chat
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

    // Edit Contact Dialog
    if (showEditDialog && contact != null) {
        var editName by remember(contact.id) { mutableStateOf(contact.name) }
        var editHouse by remember(contact.id) { mutableStateOf(contact.houseNo ?: "") }
        var editLocation by remember(contact.id) { mutableStateOf(contact.location ?: "") }
        var editEmail by remember(contact.id) { mutableStateOf(contact.email ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Respondent") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editHouse,
                        onValueChange = { editHouse = it },
                        label = { Text("House / Flat No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Area / Location") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateContact(editName, editHouse, editLocation, editEmail)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
