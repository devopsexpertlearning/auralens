package com.example.auralens.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
            
            Divider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "AI Model",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Simple Radio Buttons for Model Selection
            val models = listOf("gemini-3-pro-preview", "gemini-2.0-flash-exp", "gemini-pro-vision")
            models.forEach { modelName ->
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
                        onClick = null // null recommended for accessibility with selectable
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = modelName)
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
