package com.censustracker.ui.screen

import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.ui.components.ProcessingOverlay
import com.censustracker.ui.components.ToastNotification
import com.censustracker.ui.viewmodel.AddContactViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddContactScreen(
    viewModel: AddContactViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var presetDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) {
        if (state.success) {
            delay(800)
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Respondent Contact") },
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Respondent Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Owner Name *") },
                    placeholder = { Text("e.g. Robert Johnson") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    isError = state.nameError != null,
                    supportingText = {
                        state.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Auto-format checkbox: name field stays manual; saved name becomes "B<id> H<house> <name>"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.autoFormatName,
                        onCheckedChange = { viewModel.onAutoFormatNameChanged(it) }
                    )
                    Column {
                        Text(
                            text = "Auto-format contact name",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Saved as: B<Building> H<House> <Owner Name>",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phone: country code prefix + national number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.countryCode,
                        onValueChange = { viewModel.onCountryCodeChanged(it) },
                        label = { Text("Code") },
                        placeholder = { Text("+91") },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.width(96.dp)
                    )
                    OutlinedTextField(
                        value = state.phoneNumber,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label = { Text("Phone Number * (10 digits)") },
                        placeholder = { Text("98765 43210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        maxLines = 1,
                        isError = state.phoneError != null,
                        supportingText = {
                            when {
                                state.phoneError != null ->
                                    Text(state.phoneError!!, color = MaterialTheme.colorScheme.error)
                                state.phoneNumber.length == 10 ->
                                    Text("10 digits — ready", color = Color(0xFF2E7D32))
                                state.phoneNumber.isNotEmpty() ->
                                    Text("${state.phoneNumber.length}/10 digits", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Text("Enter 10-digit mobile number")
                            }
                        },
                        colors = if (state.phoneNumber.length == 10 && state.phoneError == null) {
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E7D32),
                                unfocusedBorderColor = Color(0xFF2E7D32),
                                focusedLabelColor = Color(0xFF2E7D32),
                                cursorColor = Color(0xFF2E7D32)
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Building ID (used only for auto-generating the contact name)
                OutlinedTextField(
                    value = state.buildingId,
                    onValueChange = { viewModel.onBuildingIdChanged(it) },
                    label = { Text("Building ID") },
                    placeholder = { Text("e.g. B-7 (used only for name combos)") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // House No / Address (single field holds the full address)
                OutlinedTextField(
                    value = state.houseNo,
                    onValueChange = { viewModel.onHouseNoChanged(it) },
                    label = { Text("House / Address") },
                    placeholder = { Text("e.g. H-14, Green Valley, Ward 5") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email (Optional)") },
                    placeholder = { Text("respondent@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Automation & Settings Toggles (Values are dynamically remembered)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Save to Device Contacts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically writes to Android phone book (remembered preference)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.savedToPhone,
                                onCheckedChange = { viewModel.onSavedToPhoneChanged(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send SMS Notification Now",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Automatically dispatch SMS to respondent upon save",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.sendNow,
                                onCheckedChange = { viewModel.onSendNowChanged(it) }
                            )
                        }
                    }
                }

                if (state.sendNow) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Message Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Channel selector (gated by system-level knobs in Settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Channel:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = state.channel == "SMS",
                            onClick = { viewModel.onChannelChanged("SMS") },
                            enabled = state.smsEnabled,
                            label = {
                                Text(
                                    if (state.smsEnabled) "SMS" else "SMS (disabled)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                        FilterChip(
                            selected = state.channel == "WHATSAPP",
                            onClick = { viewModel.onChannelChanged("WHATSAPP") },
                            enabled = state.whatsappEnabled,
                            label = {
                                Text(
                                    if (state.whatsappEnabled) "WhatsApp" else "WhatsApp (disabled)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { presetDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.selectedPreset?.title ?: "Select Preset Message Template",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = presetDropdownExpanded,
                            onDismissRequest = { presetDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Selection (Custom Message)") },
                                onClick = {
                                    viewModel.onPresetSelected(null)
                                    presetDropdownExpanded = false
                                }
                            )
                            state.presetMessages.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.title) },
                                    onClick = {
                                        viewModel.onPresetSelected(preset)
                                        presetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Message Body
                    OutlinedTextField(
                        value = state.customBody,
                        onValueChange = { viewModel.onCustomBodyChanged(it) },
                        label = { Text("Message Body") },
                        placeholder = { Text("Type message or insert placeholders below...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap to Insert Placeholder:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlaceholderResolver.availablePlaceholders().forEach { placeholder ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.onInsertPlaceholder(placeholder) },
                                label = { Text(placeholder, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SMS Live Preview Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Live Message Preview (Placeholders Resolved):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.renderBody().ifBlank { "[Message body is empty]" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                state.error?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.saveContact()
                        },
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(if (state.isLoading) "Saving..." else "Save Contact")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        ProcessingOverlay(
            isProcessing = state.isLoading,
            title = "Saving Respondent...",
            subtitle = "Storing contact & dispatching message"
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
}
