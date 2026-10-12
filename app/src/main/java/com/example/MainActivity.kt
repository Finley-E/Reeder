package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Phrase
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LanguageOption
import com.example.viewmodel.TTSEngine
import com.example.viewmodel.TTSStatus
import com.example.viewmodel.TTSVoiceItem
import com.example.viewmodel.TTSViewModel
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TTSAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSAppScreen(viewModel: TTSViewModel = viewModel()) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val installedEngines by viewModel.installedEngines.collectAsStateWithLifecycle()
    val activeEngine by viewModel.activeEngine.collectAsStateWithLifecycle()
    val pitch by viewModel.pitch.collectAsStateWithLifecycle()
    val speed by viewModel.speed.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val savedPhrases by viewModel.savedPhrases.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val filterOfflineOnly by viewModel.filterOfflineOnly.collectAsStateWithLifecycle()
    val naturalPacingEnabled by viewModel.naturalPacingEnabled.collectAsStateWithLifecycle()
    val activePresetId by viewModel.activePresetId.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf(selectedLanguage.defaultPhrase) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var phraseToEdit by remember { mutableStateOf<Phrase?>(null) }
    var phraseLabelInput by remember { mutableStateOf("") }
    var phraseCategoryInput by remember { mutableStateOf("Conversational") }
    var phraseTextInput by remember { mutableStateOf("") }
    
    var searchPhraseQuery by remember { mutableStateOf("") }

    // Sync input text with default phrase when user changes language
    LaunchedEffect(selectedLanguage) {
        textInput = selectedLanguage.defaultPhrase
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "TTS Voice Icon",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Voice Lab TTS",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "100% Local • Room SQLite • Off-Grid Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { openSystemTTSSettings(context) },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Speech Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Tester Icon") },
                    label = { Text("Speech Lab") },
                    modifier = Modifier.testTag("tab_tester")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Room Library Icon") },
                    label = { Text("Room DB (${savedPhrases.size})") },
                    modifier = Modifier.testTag("tab_database")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Guide Icon") },
                    label = { Text("Off-Grid Guide") },
                    modifier = Modifier.testTag("tab_guide")
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Off-Grid Local Offline Status Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Offline Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "SQLite Room",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = if (selectedVoice?.isOfflineLocal == true) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (selectedVoice?.isOfflineLocal == true) "On-Device Voice" else "System Voice",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedVoice?.isOfflineLocal == true) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Error notification banner if TTS has issues
            AnimatedVisibility(
                visible = status is TTSStatus.Error,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                val errMsg = (status as? TTSStatus.Error)?.message ?: "An error occurred with TTS Initialization"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error Icon",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errMsg,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.initTTS(activeEngine?.name) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry init"
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> TesterTabContent(
                        viewModel = viewModel,
                        textInput = textInput,
                        onTextInputChange = { textInput = it },
                        onSaveClick = {
                            phraseLabelInput = if (textInput.length > 25) textInput.take(25) + "..." else textInput
                            phraseTextInput = textInput
                            phraseCategoryInput = "Conversational"
                            showSaveDialog = true
                        },
                        installedEngines = installedEngines,
                        activeEngine = activeEngine,
                        pitch = pitch,
                        speed = speed,
                        isSpeaking = isSpeaking,
                        selectedLanguage = selectedLanguage,
                        availableVoices = availableVoices,
                        selectedVoice = selectedVoice,
                        filterOfflineOnly = filterOfflineOnly,
                        naturalPacingEnabled = naturalPacingEnabled,
                        activePresetId = activePresetId,
                        savedPhrases = savedPhrases,
                        onLoadQuickPhrase = { phrase ->
                            viewModel.languages.find { it.code == phrase.languageCode }?.let { lang ->
                                viewModel.setSelectedLanguage(lang)
                            }
                            textInput = phrase.text
                            viewModel.setPitch(phrase.pitch)
                            viewModel.setSpeed(phrase.speed)
                            viewModel.speak(phrase.text)
                        }
                    )

                    1 -> PhraseLibraryTabContent(
                        viewModel = viewModel,
                        savedPhrases = savedPhrases,
                        selectedLanguage = selectedLanguage,
                        onPhraseLoad = { phrase ->
                            viewModel.languages.find { it.code == phrase.languageCode }?.let { lang ->
                                viewModel.setSelectedLanguage(lang)
                            }
                            textInput = phrase.text
                            viewModel.setPitch(phrase.pitch)
                            viewModel.setSpeed(phrase.speed)
                            selectedTab = 0 // return to tester
                            viewModel.speak(phrase.text)
                        },
                        onPhraseEdit = { phrase ->
                            phraseToEdit = phrase
                            phraseLabelInput = phrase.label
                            phraseTextInput = phrase.text
                            phraseCategoryInput = phrase.category
                            showEditDialog = true
                        },
                        onAddCustomPhraseClick = {
                            phraseLabelInput = ""
                            phraseTextInput = ""
                            phraseCategoryInput = "Conversational"
                            showSaveDialog = true
                        },
                        searchQuery = searchPhraseQuery,
                        onSearchQueryChange = { searchPhraseQuery = it }
                    )

                    2 -> OffGridAndHuaweiGuideTabContent(
                        activeEngine = activeEngine,
                        installedEngines = installedEngines,
                        onOpenSettings = {
                            openSystemTTSSettings(context)
                        }
                    )
                }
            }
        }
    }

    // Save Phrase to Room Database Dialog
    if (showSaveDialog) {
        val categories = listOf("Conversational", "Pronunciation", "Numbers & Time", "Off-Grid Ops", "Emergency")
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Save to Room DB")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Store this phrase locally in SQLite for instant offline testing in ${selectedLanguage.name} (${selectedLanguage.code}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = phraseLabelInput,
                        onValueChange = { phraseLabelInput = it },
                        label = { Text("Title / Snippet Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phrase_label_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selector
                    Text("Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = phraseCategoryInput == cat,
                                onClick = { phraseCategoryInput = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phraseTextInput,
                        onValueChange = { phraseTextInput = it },
                        label = { Text("Phrase Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("phrase_content_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phraseLabelInput.isNotBlank() && phraseTextInput.isNotBlank()) {
                            viewModel.savePhrase(
                                text = phraseTextInput,
                                label = phraseLabelInput,
                                category = phraseCategoryInput,
                                pitchVal = pitch,
                                speedVal = speed
                            )
                            showSaveDialog = false
                            Toast.makeText(context, "Saved to local Room database!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_phrase_btn")
                ) {
                    Text("Save to DB")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Phrase Dialog
    if (showEditDialog && phraseToEdit != null) {
        val categories = listOf("Conversational", "Pronunciation", "Numbers & Time", "Off-Grid Ops", "Emergency")
        val currentEdit = phraseToEdit!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Saved Phrase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = phraseLabelInput,
                        onValueChange = { phraseLabelInput = it },
                        label = { Text("Label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = phraseCategoryInput == cat,
                                onClick = { phraseCategoryInput = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = phraseTextInput,
                        onValueChange = { phraseTextInput = it },
                        label = { Text("Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phraseLabelInput.isNotBlank() && phraseTextInput.isNotBlank()) {
                            viewModel.updatePhrase(
                                currentEdit.copy(
                                    label = phraseLabelInput.trim(),
                                    text = phraseTextInput.trim(),
                                    category = phraseCategoryInput
                                )
                            )
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Update")
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

@Composable
fun TesterTabContent(
    viewModel: TTSViewModel,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    installedEngines: List<TTSEngine>,
    activeEngine: TTSEngine?,
    pitch: Float,
    speed: Float,
    isSpeaking: Boolean,
    selectedLanguage: LanguageOption,
    availableVoices: List<TTSVoiceItem>,
    selectedVoice: TTSVoiceItem?,
    filterOfflineOnly: Boolean,
    naturalPacingEnabled: Boolean,
    activePresetId: String,
    savedPhrases: List<Phrase>,
    onLoadQuickPhrase: (Phrase) -> Unit
) {
    val context = LocalContext.current
    var isEngineMenuExpanded by remember { mutableStateOf(false) }
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    var isVoiceMenuExpanded by remember { mutableStateOf(false) }

    // Phrases for the current language
    val languagePhrases = remember(savedPhrases, selectedLanguage) {
        savedPhrases.filter { it.languageCode == selectedLanguage.code }
    }

    val displayVoices = remember(availableVoices, filterOfflineOnly) {
        if (filterOfflineOnly) {
            availableVoices.filter { it.isOfflineLocal }
        } else {
            availableVoices
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Active Engine Selection
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SPEECH SYNTHESIS ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = activeEngine?.label ?: "Detecting System Engine...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { openSystemTTSSettings(context) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "System Speech Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Dropdown triggers selection of alternate engines
                    Box {
                        OutlinedButton(
                            onClick = { isEngineMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("engine_dropdown_trigger"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Engine: " + (activeEngine?.label ?: "Default TTS System"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand Engines"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isEngineMenuExpanded,
                            onDismissRequest = { isEngineMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (installedEngines.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No alternate TTS engine detected") },
                                    onClick = { isEngineMenuExpanded = false }
                                )
                            } else {
                                installedEngines.forEach { engine ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = engine.label,
                                                        fontWeight = if (engine.isActive) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = engine.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (engine.isActive) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.initTTS(engine.name)
                                            isEngineMenuExpanded = false
                                        },
                                        modifier = Modifier.testTag("engine_item_${engine.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Target Language & Presets (FR, DE, PL, PT, ES, TR, BG, RU, UA, EN)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TARGET LANGUAGE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Language Dropdown Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        onClick = { isLanguageMenuExpanded = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("language_dropdown_trigger")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = selectedLanguage.flag,
                                    fontSize = 24.sp
                                )
                                Column {
                                    Text(
                                        text = "${selectedLanguage.name} (${selectedLanguage.code})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Locale: ${selectedLanguage.locale.displayName} • Offline Compatible",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Arrow",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isLanguageMenuExpanded,
                        onDismissRequest = { isLanguageMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        viewModel.languages.forEach { lang ->
                            DropdownMenuItem(
                                leadingIcon = { Text(lang.flag, fontSize = 20.sp) },
                                text = {
                                    Column {
                                        Text(text = "${lang.name} (${lang.code})", fontWeight = FontWeight.SemiBold)
                                        Text(text = lang.locale.toLanguageTag(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    viewModel.setSelectedLanguage(lang)
                                    isLanguageMenuExpanded = false
                                },
                                modifier = Modifier.testTag("lang_item_${lang.code}")
                            )
                        }
                    }
                }

                // Quick Select Preset Chips for FR, DE, PL, PT, ES, TR, BG, RU, UA, EN
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(viewModel.languages) { lang ->
                        val isSelected = lang.code == selectedLanguage.code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setSelectedLanguage(lang) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("quick_lang_chip_${lang.code}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = lang.flag, fontSize = 16.sp)
                                Text(
                                    text = lang.code,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Anti-Robotic Voice Selection & Natural Tuning Studio
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "NATURAL VOCAL PROFILES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Offline Voices Only Filter Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Offline Only", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = filterOfflineOnly,
                                onCheckedChange = { viewModel.setFilterOfflineOnly(it) },
                                modifier = Modifier.testTag("filter_offline_voices_switch")
                            )
                        }
                    }

                    // Voice Variant Dropdown Selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isVoiceMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_selector_trigger"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedVoice?.displayName ?: "System Default Natural Voice",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (selectedVoice != null) "${selectedVoice.qualityLabel} • ${if (selectedVoice.isOfflineLocal) "100% Offline Local" else "Network Voice"}"
                                        else "Standard voice active (${displayVoices.size} models found)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Voices")
                            }
                        }

                        DropdownMenu(
                            expanded = isVoiceMenuExpanded,
                            onDismissRequest = { isVoiceMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (displayVoices.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (filterOfflineOnly) "No local offline voices found. Toggle 'Offline Only' off or install voice data."
                                            else "Using system default voice package"
                                        )
                                    },
                                    onClick = { isVoiceMenuExpanded = false }
                                )
                            } else {
                                displayVoices.forEach { voiceItem ->
                                    val isSelected = voiceItem.name == selectedVoice?.name
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = voiceItem.displayName,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Text(
                                                        text = "${voiceItem.qualityLabel} • ${if (voiceItem.isOfflineLocal) "Offline Ready" else "Requires Network"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectVoice(voiceItem)
                                            isVoiceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vocal Cadence Presets (Removes robotic monotony)
                    Text(
                        "Cadence & Intonation Presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(viewModel.vocalPresets) { preset ->
                            val isSelected = activePresetId == preset.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.applyPreset(preset) }
                                    .testTag("preset_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(preset.icon, fontSize = 14.sp)
                                    Column {
                                        Text(
                                            preset.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${String.format("%.2f", preset.speed)}x / ${String.format("%.2f", preset.pitch)}x",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Natural Pacing / Breathing Pause Humanizer Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Natural Pacing & Breathing Pauses",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Inserts prosody micro-pauses at punctuation to prevent robotic run-on sentences",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = naturalPacingEnabled,
                            onCheckedChange = { viewModel.setNaturalPacingEnabled(it) },
                            modifier = Modifier.testTag("natural_pacing_switch")
                        )
                    }

                    // Fine Tuning Sliders
                    // Speech Speed Rate Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = "Speed Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Speech Rate (Cadence)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = String.format("%.2fx", speed),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = speed,
                            onValueChange = { viewModel.setSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.testTag("speed_slider")
                        )
                    }

                    // Voice Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = "Pitch Icon", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Text("Vocal Pitch (Acoustic Tone)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = String.format("%.2fx", pitch),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = { viewModel.setPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("pitch_slider")
                        )
                    }
                }
            }
        }

        // Section: Quick Test Phrases from Room Database
        if (languagePhrases.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROOM DB QUICK PHRASES (${selectedLanguage.code})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${languagePhrases.size} saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(languagePhrases, key = { it.id }) { phrase ->
                            SuggestionChip(
                                onClick = { onLoadQuickPhrase(phrase) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (phrase.isFavorite) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = "Starred",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(phrase.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.testTag("quick_phrase_chip_${phrase.id}")
                            )
                        }
                    }
                }
            }
        }

        // Section: Text To Say Input Box & Audio Visualizer
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNTHESIZE SPEECH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(
                        onClick = { onTextInputChange(selectedLanguage.defaultPhrase) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Interactive Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("text_to_speak_input"),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Enter text to synthesize offline...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // High fidelity Audio Visualizer Layer
                AudioVisualizerCard(isSpeaking = isSpeaking)

                // Direct Control Buttons Play/Stop and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save phrase to Room
                    OutlinedButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_phrase_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = "Save phrase")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to DB")
                    }

                    if (isSpeaking) {
                        Button(
                            onClick = { viewModel.stop() },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .testTag("stop_speak_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop speaking")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.speak(textInput) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .testTag("start_speak_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak text")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Speak Out")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioVisualizerCard(isSpeaking: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                if (isSpeaking) {
                    WavesAnimationCanvas()
                } else {
                    FlatStaticLineCanvas()
                }
            }
            Text(
                text = if (isSpeaking) "Synthesizing Speech Playback..." else "Ready (Click speak to synthesize audio)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun WavesAnimationCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_ripple")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    val amplitude1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = reverseTween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amp_1"
    )

    val amplitude2 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = reverseTween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amp_2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val points = 80
        val step = width / points

        // Wave 1 (Primary Accent)
        for (i in 0 until points) {
            val progress = i.toFloat() / points
            val x1 = i * step
            val x2 = (i + 1) * step
            val envelope = sin(progress * Math.PI.toFloat())
            val y1 = centerY + sin(progress * 10f - phase) * amplitude1 * envelope
            val y2 = centerY + sin((progress + 1f / points) * 10f - phase) * amplitude1 * envelope

            drawLine(
                color = primaryColor.copy(alpha = 0.65f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Wave 2 (Secondary Opposing Wave)
        for (i in 0 until points) {
            val progress = i.toFloat() / points
            val x1 = i * step
            val x2 = (i + 1) * step
            val envelope = sin(progress * Math.PI.toFloat())
            val y1 = centerY + sin(progress * 14f + phase) * amplitude2 * envelope
            val y2 = centerY + sin((progress + 1f / points) * 14f + phase) * amplitude2 * envelope

            drawLine(
                color = secondaryColor.copy(alpha = 0.45f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Wave 3 (Tertiary Fast Wave)
        for (i in 0 until points) {
            val progress = i.toFloat() / points
            val x1 = i * step
            val x2 = (i + 1) * step
            val envelope = sin(progress * Math.PI.toFloat())
            val y1 = centerY + sin(progress * 6f - phase * 1.5f) * (amplitude1 * 0.5f) * envelope
            val y2 = centerY + sin((progress + 1f / points) * 6f - phase * 1.5f) * (amplitude1 * 0.5f) * envelope

            drawLine(
                color = tertiaryColor.copy(alpha = 0.5f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

private fun reverseTween(duration: Int): TweenSpec<Float> {
    return tween(durationMillis = duration, easing = FastOutSlowInEasing)
}

@Composable
fun FlatStaticLineCanvas() {
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val centerY = size.height / 2f
        drawLine(
            color = strokeColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

fun openSystemTTSSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = "com.android.settings.TTS_SETTINGS"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "System voice settings could not be opened directly. Please search 'Text to Speech' in Android Settings.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PhraseLibraryTabContent(
    viewModel: TTSViewModel,
    savedPhrases: List<Phrase>,
    selectedLanguage: LanguageOption,
    onPhraseLoad: (Phrase) -> Unit,
    onPhraseEdit: (Phrase) -> Unit,
    onAddCustomPhraseClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedLanguageFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Conversational", "Pronunciation", "Numbers & Time", "Off-Grid Ops", "Favorites ⭐")
    val langCodes = listOf("All", "EN", "FR", "DE", "PL", "PT", "ES", "TR", "BG", "RU", "UA")

    val filteredPhrases = remember(savedPhrases, searchQuery, selectedCategoryFilter, selectedLanguageFilter) {
        savedPhrases.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.text.contains(searchQuery, ignoreCase = true) ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.languageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "All" -> true
                "Favorites ⭐" -> item.isFavorite
                else -> item.category == selectedCategoryFilter
            }

            val matchesLang = if (selectedLanguageFilter == "All") true else item.languageCode == selectedLanguageFilter

            matchesSearch && matchesCategory && matchesLang
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOCAL ROOM DATABASE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Offline Phrase Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.resetToDefaultPack()
                        Toast.makeText(context, "Preloaded 10-language test pack refreshed!", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset pack", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Pack", fontSize = 12.sp)
                }

                Button(
                    onClick = onAddCustomPhraseClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_phrase_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Phrase", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 12.sp)
                }
            }
        }

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phrase_search_input"),
            placeholder = { Text("Search phrases, languages, tags...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryFilter == cat,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(cat, fontSize = 12.sp) }
                )
            }
        }

        // Language Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(langCodes) { lang ->
                FilterChip(
                    selected = selectedLanguageFilter == lang,
                    onClick = { selectedLanguageFilter = lang },
                    label = { Text(lang, fontSize = 12.sp) }
                )
            }
        }

        if (filteredPhrases.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategoryFilter != "All") "No matching phrases in database"
                        else "Your Room speech collection is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Click 'Load Pack' above to install default offline phrases, or save phrases from Speech Lab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPhrases, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phrase_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.languageCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.category,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = item.label,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleFavorite(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star",
                                        tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Action Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Speed: ${String.format("%.2f", item.speed)}x • Pitch: ${String.format("%.2f", item.pitch)}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { onPhraseLoad(item) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("play_phrase_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak Phrase",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onPhraseEdit(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Phrase",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePhrase(item) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_phrase_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Phrase",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
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

@Composable
fun OffGridAndHuaweiGuideTabContent(
    activeEngine: TTSEngine?,
    installedEngines: List<TTSEngine>,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "OFF-GRID & NATURAL AUDIO GUIDE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fixing Robotic Voices & Off-Grid Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Learn why text-to-speech sounds robotic and how to configure high-definition neural voices on-device without any internet or cloud connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: Why Voices Sound Robotic & Solutions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Why Does TTS Sound Robotic?",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "1. Legacy Fallback Voice: By default, Android often loads an archaic 16kHz concatenative synthesis voice if HD voice data is not downloaded.\n\n" +
                                "2. Pitch & Cadence Monotony: Robotic speech occurs when pitch is flat and speech speed is too fast. Switching to the 'Conversational' (0.95x speed, 0.98x pitch) or 'Warm Storyteller' preset immediately restores acoustic warmth.\n\n" +
                                "3. Lack of Breathing Pauses: Enabling the app's 'Natural Pacing & Pauses' switch inserts prosodic micro-pauses at commas and clauses, stopping mechanical run-on speech.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section: Step by Step to Install HD Offline Voice Data
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Install HD Voices for 100% Off-Grid Use",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = "To get studio-quality human voices completely offline on Android or Huawei (EMUI/HarmonyOS):\n\n" +
                                "• Step 1: Open System TTS Settings using the button below.\n" +
                                "• Step 2: Tap the Gear icon next to your Preferred Engine (Google Speech Services, Celia Speech, or Samsung TTS).\n" +
                                "• Step 3: Tap 'Install voice data' (or 'Language Data').\n" +
                                "• Step 4: Select your languages (e.g. English, French, German, Polish, Spanish, etc.) and download Voice Pack #2, #3, or HD.\n" +
                                "• Step 5: Once downloaded, return to Voice Lab TTS — the HD voice will appear under 'Natural Vocal Profiles' with an '[Offline Local]' badge!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open System TTS Voice Settings")
                    }
                }
            }
        }

        // Section: Huawei P50 Pro & Non-GMS Devices
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Huawei P50 Pro (No GMS) Recommendations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Because Huawei P50 Pro devices run EMUI/HarmonyOS without Google Play Services:\n\n" +
                                "• Celia Speech Engine: Huawei preinstalls Celia TTS. Ensure Celia Speech is enabled in Accessibility.\n" +
                                "• eSpeak NG: Available offline via F-Droid / Huawei AppGallery. Provides 100% offline synthesis for over 100 languages with zero external dependencies.\n" +
                                "• Sherpa ONNX / RHVoice: Open-source on-device neural TTS engines with zero tracking and full offline capabilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
