package com.example.auralens.ui

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Pageview
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.auralens.ui.components.GlassButton
import com.example.auralens.ui.components.ScannerOverlay
import com.example.auralens.ui.theme.GlassWhite
import com.example.auralens.ui.theme.NeonCyan
import com.example.auralens.util.CameraManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VisionViewModel,
    cameraManager: CameraManager,
    onSettingsClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI State from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    
    // Helper for safe photo capture to prevent crashes
    suspend fun safeTakePhoto(): android.graphics.Bitmap? {
        return try {
            cameraManager.takePhoto()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Camera Error: ${e.localizedMessage}")
            null
        }
    }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasAudioPermission by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    
    // Bottom Sheet for Results
    val sheetState = rememberModalBottomSheetState()
    var showResultSheet by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    
    // Input Dialog State
    var showInputDialog by remember { mutableStateOf(false) }
    var inputMode by remember { mutableStateOf<InputMode?>(null) }
    var inputText by remember { mutableStateOf("") }

    // Logic to handle state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is VisionUiState.Success -> {
                resultText = state.text
                showResultSheet = true
                showInputDialog = false
            }
            is VisionUiState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // 1. Full Screen Camera View
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        cameraManager.startCamera(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission needed", color = Color.White)
            }
        }
        
        // 2. Scanner Overlay (Visual Tech)
        ScannerOverlay(isScanning = uiState is VisionUiState.Loading)

        // 3. Top Bar (Transparent)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AURA LENS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            )
            
            IconButton(
                onClick = onSettingsClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = GlassWhite)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // 4. Loading Indicator Overlay
        if (uiState is VisionUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        }
        
        // 6. Error Snackbar (Placed above controls)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 350.dp) // Position above control deck
        )

        // 5. Control Deck (Bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Core
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = "Explore",
                    icon = Icons.Default.ImageSearch,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        if (hasCameraPermission) {
                            scope.launch {
                                val bitmap = safeTakePhoto()
                                bitmap?.let { viewModel.describeScene(it) }
                            }
                        }
                    }
                )
                GlassButton(
                    text = if (viewModel.isLiveMode) "Stop Live" else "Live Guide",
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        if (hasCameraPermission) {
                            // Live mode handles its own errors internally in the loop, 
                            // but we wrap the toggle just in case
                            try {
                                viewModel.toggleLiveMode(cameraManager)
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Error: ${e.message}") }
                            }
                        }
                    }
                )
            }
            
            // Row 2: Text/Find
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = "Read",
                    icon = Icons.Default.TextFields,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        if (hasCameraPermission) {
                            scope.launch {
                                val bitmap = safeTakePhoto()
                                bitmap?.let { viewModel.readText(it) }
                            }
                        }
                    }
                )
                GlassButton(
                    text = "Find",
                    icon = Icons.Default.Search,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        if (hasCameraPermission) {
                            inputMode = InputMode.FIND
                            inputText = ""
                            showInputDialog = true
                        }
                    }
                )
            }
            
            // Row 3: Ask/History
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(
                    text = "Ask",
                    icon = Icons.Default.QuestionMark,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        if (hasCameraPermission) {
                            inputMode = InputMode.ASK
                            inputText = ""
                            showInputDialog = true
                        }
                    }
                )
                 GlassButton(
                    text = "History",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f).height(80.dp),
                    onClick = {
                        inputMode = InputMode.HISTORY
                        showInputDialog = true // We reuse the dialog state logic, but will show history
                    }
                )
            }
        }
    }

    // Input Dialog (Stylized)
    if (showInputDialog && inputMode != null) {
        Dialog(onDismissRequest = { showInputDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 500.dp) // Limit height for history
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val title = when(inputMode) {
                        InputMode.FIND -> "Locate Object"
                        InputMode.ASK -> "Ask Question"
                        InputMode.HISTORY -> "Smart History"
                        else -> ""
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )
                    Spacer(Modifier.height(16.dp))

                    if (inputMode == InputMode.HISTORY) {
                        val history = viewModel.history
                        if (history.isEmpty()) {
                            Text("No history yet.", color = Color.Gray)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(history.reversed()) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = item,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                         Spacer(Modifier.height(16.dp))
                         Button(onClick = { showInputDialog = false }) {
                            Text("Close")
                        }
                    } else {
                        // Locate / Ask UI
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (inputMode == InputMode.FIND) "What are you looking for?" else "Ask about the scene...") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showInputDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    showInputDialog = false
                                    scope.launch {
                                        val bitmap = safeTakePhoto()
                                        bitmap?.let {
                                            when (inputMode) {
                                                InputMode.FIND -> {
                                                    viewModel.updateTargetObject(inputText)
                                                    viewModel.findObject(it)
                                                }
                                                InputMode.ASK -> {
                                                    viewModel.updateQuestion(inputText)
                                                    viewModel.askAboutPhoto(it)
                                                }
                                                null -> {}
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                            ) {
                                Text("Go")
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        // Voice input hint
                        TextButton(onClick = { viewModel.startListening() }) {
                            Text("Microphone Input", color = NeonCyan)
                        }
                        val speechResult by viewModel.speechResult.collectAsState()
                        LaunchedEffect(speechResult) {
                            speechResult?.let {
                                inputText = it
                                viewModel.stopListening()
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Result Bottom Sheet
    if (showResultSheet) {
        ModalBottomSheet(
            onDismissRequest = { showResultSheet = false },
            sheetState = sheetState,
            containerColor = Color.Black.copy(alpha = 0.9f),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Analysis Result",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = Color.White
                )
                Spacer(Modifier.height(48.dp)) // Spacing for safe area
            }
        }
    }
}

enum class InputMode {
    FIND, ASK, HISTORY
}
