package com.censustracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.censustracker.domain.model.Message
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.FailedMessagesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedMessagesScreen(
    viewModel: FailedMessagesViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var messageToDelete by remember { mutableStateOf<Message?>(null) }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    if (messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Remove Queued Message") },
            text = { Text("Cancel and permanently delete this queued SMS to ${viewModel.contactName(messageToDelete!!)}? It will not be sent.") },
            confirmButton = {
                TextButton(onClick = {
                    messageToDelete?.let { viewModel.delete(it) }
                    messageToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) { Text("Keep") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Retry Queue") },
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
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Queue is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Messages that fail to send or get stuck pending will appear here for retry or removal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        val isPending = message.status == "PENDING"
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPending)
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPending) Icons.Default.HourglassEmpty else Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (isPending) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = viewModel.contactName(message),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = timeFormat.format(Date(message.sentAt ?: message.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (message.channel == "WHATSAPP") "WA" else "SMS"} · ${message.status}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPending) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.delete(message) },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.width(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = { viewModel.retry(message) },
                                        enabled = state.retryingId != message.id
                                    ) {
                                        if (state.retryingId == message.id) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.width(14.dp).height(14.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.width(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

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
    }
}
