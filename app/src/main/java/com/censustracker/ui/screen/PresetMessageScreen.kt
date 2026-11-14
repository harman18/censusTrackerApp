package com.censustracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.censustracker.placeholder.PlaceholderResolver
import com.censustracker.ui.viewmodel.PresetMessageViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PresetMessageScreen(
    viewModel: PresetMessageViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preset SMS Templates") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saved Message Templates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.presets.isEmpty()) {
                Text(
                    text = "No preset templates saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.presets.forEach { preset ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(onClick = { viewModel.onDeletePreset(preset.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Template",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Text(
                                text = preset.bodyTemplate,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            if (preset.dynamicVariables.isNotEmpty()) {
                                Text(
                                    text = "Tokens: ${preset.dynamicVariables.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create New Preset Form
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create New Template",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        label = { Text("Template Title *") },
                        placeholder = { Text("e.g. Census Reminder") },
                        singleLine = true,
                        maxLines = 1,
                        isError = state.titleError != null,
                        supportingText = {
                            state.titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.bodyTemplate,
                        onValueChange = { viewModel.onBodyTemplateChanged(it) },
                        label = { Text("Body Template *") },
                        placeholder = { Text("Hello {respondent.name}, this is Collector {collector.name}...") },
                        maxLines = 4,
                        isError = state.bodyError != null,
                        supportingText = {
                            state.bodyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap to Insert Token:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    state.error?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            viewModel.onTitleChanged("")
                            viewModel.onBodyTemplateChanged("")
                        }) {
                            Text("Clear")
                        }
                        Button(onClick = { viewModel.addPreset() }) {
                            Text("Save Template")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}