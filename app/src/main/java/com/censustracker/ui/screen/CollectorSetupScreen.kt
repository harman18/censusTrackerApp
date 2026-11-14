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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.censustracker.ui.Screen
import com.censustracker.ui.components.ProcessingOverlay
import com.censustracker.ui.viewmodel.CollectorSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorSetupScreen(
    viewModel: CollectorSetupViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Census Collector Setup") },
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

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Collector Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Collector Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your details are inserted into automated SMS templates sent to respondents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Collector Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Collector / Enumerator Name *") },
                    placeholder = { Text("e.g. Jane Doe") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    isError = state.nameError != null,
                    supportingText = {
                        state.nameError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Phone: country code + 10-digit national number (same rules as Add Contact)
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
                        label = { Text("Contact Phone Number * (10 digits)") },
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

                // Email
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email (Optional)") },
                    placeholder = { Text("collector@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reference Link / ID
                OutlinedTextField(
                    value = state.referenceLink,
                    onValueChange = { viewModel.onReferenceLinkChanged(it) },
                    label = { Text("Census Reference Link / Survey ID (Optional)") },
                    placeholder = { Text("https://census.gov/form/123") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (viewModel.saveProfile()) {
                                onNavigate(Screen.ContactList)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                state.error?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.existingProfile != null) {
                        OutlinedButton(
                            onClick = { onNavigate(Screen.ContactList) },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("Skip / Contacts")
                        }
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (viewModel.saveProfile()) {
                                onNavigate(Screen.ContactList)
                            }
                        },
                        enabled = !state.isLoading
                    ) {
                        Text(if (state.existingProfile != null) "Update Profile" else "Save & Continue")
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Developer & Beta Testing Disclaimer Card
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Developer & Beta Release Notice",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Developer: Harmanjot Singh",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Contact: harman062001@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.height(14.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = "Notice: This application is a Beta Testing Release provided as-is without any express warranty or guarantee of cellular carrier delivery.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        ProcessingOverlay(
            isProcessing = state.isLoading,
            title = "Saving Profile...",
            subtitle = "Updating enumerator metadata"
        )
    }
}
