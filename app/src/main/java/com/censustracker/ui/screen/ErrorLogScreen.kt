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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.censustracker.logging.ErrorLogEntry
import com.censustracker.ui.viewmodel.ErrorLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorLogScreen(
    viewModel: ErrorLogViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val filtered = if (filter == null) entries else entries.filter { it.action == filter }
    val actions = entries.map { it.action }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Error Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        val text = filtered.joinToString("\n\n") { e ->
                            "[${dateFormat.format(Date(e.timestamp))}] ${e.action}: ${e.message}" +
                                    (e.detail?.let { "\nDetail: $it" } ?: "")
                        }
                        if (text.isNotBlank()) clipboardManager.setText(AnnotatedString(text))
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy all")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear log")
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
            if (actions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text("All (${entries.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    actions.take(4).forEach { action ->
                        FilterChip(
                            selected = filter == action,
                            onClick = { filter = if (filter == action) null else action },
                            label = { Text(action, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "No errors logged",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Failed actions will appear here with timestamp and reason.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        ErrorLogCard(entry = entry, dateFormat = dateFormat)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Error Log") },
            text = { Text("Delete all ${entries.size} logged error entries? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ErrorLogCard(entry: ErrorLogEntry, dateFormat: SimpleDateFormat) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = { },
                    enabled = false,
                    label = { Text(entry.action, style = MaterialTheme.typography.labelSmall) }
                )
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            entry.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
