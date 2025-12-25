package com.example.auralens.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VisionViewModel,
    onBack: () -> Unit
) {
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
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Detailed Description Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .toggleable(
                        value = viewModel.isDetailedDescription,
                        onValueChange = { viewModel.onDetailedDescriptionToggled(it) },
                        role = Role.Switch
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detailed Descriptions",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = viewModel.isDetailedDescription,
                    onCheckedChange = null // null because handled by toggleable
                )
            }
            
            // API Key Input
            OutlinedTextField(
                value = viewModel.apiKey,
                onValueChange = { viewModel.onApiKeyChanged(it) },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                singleLine = true
            )

            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AI Model",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Custom Model Input with Fetch Button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 OutlinedTextField(
                    value = viewModel.selectedModelName,
                    onValueChange = { viewModel.onModelSelected(it) },
                    label = { Text("Model Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.fetchModels() },
                    enabled = !viewModel.isFetchingModels && viewModel.apiKey.isNotBlank()
                ) {
                    if (viewModel.isFetchingModels) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Fetch")
                    }
                }
            }
            
            if (viewModel.fetchModelError != null) {
                Text(
                    text = viewModel.fetchModelError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text("Available Models:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            // Dynamic Radio Buttons for Model Selection
            // Use LazyColumn if list is long, but for a few items Column is fine. 
            // Since we are in a scrollable column already (from parent? No, parent is just Column), 
            // we should make this section scrollable if needed. The parent is using Column modifier fillMaxSize.
            // Ideally we wrap the whole screen content in a scrollable column.
            
            Column(Modifier.verticalScroll(rememberScrollState())) {
                viewModel.availableModels.forEach { modelName ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (modelName == viewModel.selectedModelName),
                                onClick = { viewModel.onModelSelected(modelName) },
                                role = Role.RadioButton
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (modelName == viewModel.selectedModelName),
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = modelName)
                    }
                }
            }
        }
    }
}

// Helper for selectable modifier which is not in standard material3 but useful
@Composable
fun Modifier.selectable(
    selected: Boolean,
    onClick: () -> Unit,
    role: Role? = null
) = this.then(
    Modifier.toggleable(
        value = selected,
        onValueChange = { if (it) onClick() },
        role = role
    )
)
