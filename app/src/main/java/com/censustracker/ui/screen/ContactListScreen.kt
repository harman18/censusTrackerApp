package com.censustracker.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.censustracker.contacts.DeviceContact
import com.censustracker.domain.model.Contact
import com.censustracker.ui.components.ProcessingOverlay
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.ContactFilter
import com.censustracker.ui.viewmodel.ContactSort
import com.censustracker.ui.viewmodel.ContactListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactListViewModel,
    onAddContact: () -> Unit,
    onContactClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenErrorLog: () -> Unit,
    onOpenBroadcast: () -> Unit,
    onOpenFailedMessages: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    var contactToSavePhone by remember { mutableStateOf<Contact?>(null) }
    var importSearchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Backup export (user picks save location, e.g. Downloads) and import (user picks JSON file)
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            snackbarHostState.showSnackbar(message = err)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { info ->
            snackbarHostState.showSnackbar(message = info)
            viewModel.clearInfoMessage()
        }
    }

    // Delete Confirmation Dialog
    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete '${contactToDelete?.name}'? Their chat message history will also be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    contactToDelete?.let { viewModel.onDeleteContact(it) }
                    contactToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save to Device Phone Dialog
    if (contactToSavePhone != null) {
        AlertDialog(
            onDismissRequest = { contactToSavePhone = null },
            title = { Text("Save to Device Contacts") },
            text = { Text("Write '${contactToSavePhone?.name}' (${contactToSavePhone?.phoneNumber}) into your Android device contact book?") },
            confirmButton = {
                Button(onClick = {
                    contactToSavePhone?.let { viewModel.onSaveToDevicePhone(it) }
                    contactToSavePhone = null
                }) {
                    Text("Save Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToSavePhone = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import from Device Phone Dialog
    if (state.showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                importSearchQuery = ""
                viewModel.onDismissImportDialog()
            },
            title = { Text("Import from Phone Contacts") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = importSearchQuery,
                        onValueChange = { importSearchQuery = it },
                        placeholder = { Text("Search phone contacts...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredDevice = state.deviceContacts.filter { dc ->
                        importSearchQuery.isBlank() ||
                                dc.name.contains(importSearchQuery, ignoreCase = true) ||
                                dc.phoneNumber.contains(importSearchQuery)
                    }

                    if (filteredDevice.isEmpty()) {
                        Text(
                            text = "No contacts found in device phone book",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(filteredDevice) { dc ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.onImportContactSelected(dc)
                                            importSearchQuery = ""
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContactPhone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dc.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = dc.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "Import",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    importSearchQuery = ""
                    viewModel.onDismissImportDialog()
                }) {
                    Text("Close")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Census Contacts") },
                    actions = {
                        IconButton(onClick = { viewModel.onOpenImportFromPhone() }) {
                            Icon(
                                imageVector = Icons.Default.ContactPhone,
                                contentDescription = "Import from Phone",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onOpenPresets) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Presets",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export Backup") },
                                    onClick = {
                                        showOverflowMenu = false
                                        exportBackupLauncher.launch("census_backup_${System.currentTimeMillis()}.json")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Backup") },
                                    onClick = {
                                        showOverflowMenu = false
                                        importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Error Log") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenErrorLog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Broadcast SMS") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenBroadcast()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Retry Queue") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenFailedMessages()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDarkMode) "Light Theme" else "Dark Theme") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onToggleTheme()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddContact,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Respondent") }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search by name, phone, house, location...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips + Quick Import Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.selectedFilter == ContactFilter.ALL,
                        onClick = { viewModel.onFilterChanged(ContactFilter.ALL) },
                        label = { Text("All (${state.allContacts.size})") }
                    )
                    FilterChip(
                        selected = state.selectedFilter == ContactFilter.SAVED_TO_PHONE,
                        onClick = { viewModel.onFilterChanged(ContactFilter.SAVED_TO_PHONE) },
                        label = { Text("Saved to Phone") }
                    )
                    FilterChip(
                        selected = state.selectedFilter == ContactFilter.NOT_SAVED,
                        onClick = { viewModel.onFilterChanged(ContactFilter.NOT_SAVED) },
                        label = { Text("Unsaved Phone") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sort options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = state.sortBy == ContactSort.NAME,
                        onClick = { viewModel.onSortChanged(ContactSort.NAME) },
                        label = { Text("Name (A–Z)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = state.sortBy == ContactSort.LAST_ACTIVITY,
                        onClick = { viewModel.onSortChanged(ContactSort.LAST_ACTIVITY) },
                        label = { Text("Last Message", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.searchQuery.isNotBlank()) "No matching respondents found" else "No census respondents added yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap + to add a respondent or tap the phone icon above to import from phone book",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.onOpenImportFromPhone() }) {
                                Icon(imageVector = Icons.Default.ContactPhone, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import from Phone Book")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.filteredContacts, key = { it.id }) { contact ->
                            val activity = state.messageSummaries[contact.id]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onContactClick(contact.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val initial = contact.name.trim().firstOrNull()?.uppercaseChar() ?: 'R'
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val locationInfo = listOfNotNull(
                                            contact.houseNo?.takeIf { it.isNotBlank() }?.let { "House: $it" },
                                            contact.location?.takeIf { it.isNotBlank() }
                                        ).joinToString(" | ")

                                        if (locationInfo.isNotEmpty()) {
                                            Text(
                                                text = locationInfo,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Last message preview
                                        if (activity != null && activity.lastBody.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = (if (activity.lastDirection == "RECEIVED") "← " else "→ ") + activity.lastBody,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // If contact is not saved to phone, provide single-click save badge
                                        if (!contact.savedToPhone) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "+ Save to Phone Book",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                    .clickable { contactToSavePhone = contact }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Unread incoming messages badge
                                        activity?.unreadCount?.takeIf { it > 0 }?.let { count ->
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (count > 9) "9+" else count.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        if (!contact.savedToPhone) {
                                            IconButton(onClick = { contactToSavePhone = contact }) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = "Save to Phone Book",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onContactClick(contact.id) }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                                contentDescription = "Chat",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { contactToDelete = contact }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Contact",
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

        ProcessingOverlay(
            isProcessing = state.isProcessingAction,
            title = state.processingTitle
        )

        // Corner Toast Notifications (Green Tick / Red Error)
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
