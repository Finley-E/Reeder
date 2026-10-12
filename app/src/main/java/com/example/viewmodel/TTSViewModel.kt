package com.example.viewmodel

import android.app.Application
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Phrase
import com.example.data.PhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

sealed class TTSStatus {
    object Initializing : TTSStatus()
    object Ready : TTSStatus()
    data class Error(val message: String) : TTSStatus()
}

data class TTSEngine(
    val name: String,     // package name
    val label: String,    // human-readable label
    val isDefault: Boolean,
    val isActive: Boolean
)

data class LanguageOption(
    val name: String,
    val code: String,
    val locale: Locale,
    val flag: String,
    val defaultPhrase: String
)

data class TTSVoiceItem(
    val name: String,
    val displayName: String,
    val isOfflineLocal: Boolean,
    val qualityLabel: String,
    val gender: String,
    val rawVoice: Voice
)

data class VocalPreset(
    val id: String,
    val name: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val icon: String
)

class TTSViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PhraseRepository
    private var textToSpeech: TextToSpeech? = null

    // Exposed States
    private val _status = MutableStateFlow<TTSStatus>(TTSStatus.Initializing)
    val status: StateFlow<TTSStatus> = _status.asStateFlow()

    private val _installedEngines = MutableStateFlow<List<TTSEngine>>(emptyList())
    val installedEngines: StateFlow<List<TTSEngine>> = _installedEngines.asStateFlow()

    private val _activeEngine = MutableStateFlow<TTSEngine?>(null)
    val activeEngine: StateFlow<TTSEngine?> = _activeEngine.asStateFlow()

    private val _pitch = MutableStateFlow(0.98f) // Slightly lower pitch avoids robotic tinny sound
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speed = MutableStateFlow(0.95f) // 0.95x cadence provides natural conversational rhythm
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Voices
    private val _availableVoices = MutableStateFlow<List<TTSVoiceItem>>(emptyList())
    val availableVoices: StateFlow<List<TTSVoiceItem>> = _availableVoices.asStateFlow()

    private val _selectedVoice = MutableStateFlow<TTSVoiceItem?>(null)
    val selectedVoice: StateFlow<TTSVoiceItem?> = _selectedVoice.asStateFlow()

    private val _filterOfflineOnly = MutableStateFlow(true) // Default to true for off-grid compliance
    val filterOfflineOnly: StateFlow<Boolean> = _filterOfflineOnly.asStateFlow()

    private val _naturalPacingEnabled = MutableStateFlow(true) // Humanizer for breathing pauses
    val naturalPacingEnabled: StateFlow<Boolean> = _naturalPacingEnabled.asStateFlow()

    private val _activePresetId = MutableStateFlow("conversational")
    val activePresetId: StateFlow<String> = _activePresetId.asStateFlow()

    val vocalPresets = listOf(
        VocalPreset(
            id = "conversational",
            name = "Conversational",
            description = "Natural flow with relaxed human pitch",
            pitch = 0.98f,
            speed = 0.95f,
            icon = "🗣️"
        ),
        VocalPreset(
            id = "warm_story",
            name = "Warm Storyteller",
            description = "Deeper acoustic resonance and steady pacing",
            pitch = 0.92f,
            speed = 0.88f,
            icon = "📖"
        ),
        VocalPreset(
            id = "news_anchor",
            name = "News Anchor",
            description = "Clear articulate diction and standard speed",
            pitch = 1.02f,
            speed = 1.00f,
            icon = "🎙️"
        ),
        VocalPreset(
            id = "expressive",
            name = "Expressive",
            description = "Lively vocal inflections and dynamic tone",
            pitch = 1.06f,
            speed = 0.94f,
            icon = "✨"
        ),
        VocalPreset(
            id = "offgrid_radio",
            name = "Tactical Radio",
            description = "Crisp clarity optimized for high ambient noise",
            pitch = 1.04f,
            speed = 1.10f,
            icon = "📻"
        ),
        VocalPreset(
            id = "flat_robot",
            name = "Mechanical",
            description = "Baseline unaltered robotic cadence",
            pitch = 1.00f,
            speed = 1.00f,
            icon = "🤖"
        )
    )

    val languages = listOf(
        LanguageOption("English", "EN", Locale.US, "🇬🇧", "Hello! This voice synthesis engine is running completely on-device in offline mode with natural cadence."),
        LanguageOption("Français", "FR", Locale.FRANCE, "🇫🇷", "Bonjour ! Ce système vocal fonctionne de façon entièrement locale et autonome, sans aucune connexion Internet requise."),
        LanguageOption("Deutsch", "DE", Locale.GERMANY, "🇩🇪", "Guten Tag! Die Sprachausgabe läuft vollkommen autonom auf diesem Gerät, ideal für autarke Offline-Einsätze."),
        LanguageOption("Polski", "PL", Locale.forLanguageTag("pl-PL"), "🇵🇱", "Dzień dobry! Ta aplikacja działa w pełni lokalnie bez połączenia z siecią. Wszystkie dane są zapisane w lokalnej bazie Room."),
        LanguageOption("Português", "PT", Locale.forLanguageTag("pt-PT"), "🇵🇹", "Olá! Este motor de voz opera de maneira totalmente autônoma e segura no dispositivo, sem depender de rede externa."),
        LanguageOption("Español", "ES", Locale.forLanguageTag("es-ES"), "🇪🇸", "¡Hola! La síntesis vocal está procesando todo directamente en el procesador local, sin enviar datos a la nube."),
        LanguageOption("Türk", "TR", Locale.forLanguageTag("tr-TR"), "🇹🇷", "Merhaba! Bu ses sentezleyici cihaz üzerinde yüzde yüz çevrimdışı çalışarak kesintisiz test imkanı sunar."),
        LanguageOption("Български", "BG", Locale.forLanguageTag("bg-BG"), "🇧🇬", "Здравейте! Гласовият синтез се извършва директно на устройството без необходимост от интернет мрежа."),
        LanguageOption("Русский", "RU", Locale.forLanguageTag("ru-RU"), "🇷🇺", "Здравствуйте! Голосовой движок синтезирует речь локально на устройстве без отправки запросов во внешнюю сеть."),
        LanguageOption("Українська", "UA", Locale.forLanguageTag("uk-UA"), "🇺🇦", "Привіт! Ця система генерації мовлення працює автономно в локальній базі Room без жодного інтернет-трафіку.")
    )

    private val _selectedLanguage = MutableStateFlow(languages[0])
    val selectedLanguage: StateFlow<LanguageOption> = _selectedLanguage.asStateFlow()

    // Room Database flow for customized collection of phrases
    val savedPhrases: StateFlow<List<Phrase>>

    init {
        val dao = AppDatabase.getDatabase(application).phraseDao()
        repository = PhraseRepository(dao)

        savedPhrases = repository.allPhrases.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default offline phrases if database is freshly created
        viewModelScope.launch {
            repository.seedDefaultPhrasesIfEmpty()
        }

        initTTS(null)
    }

    fun initTTS(enginePackageName: String?) {
        _status.value = TTSStatus.Initializing
        textToSpeech?.let {
            try {
                it.stop()
                it.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            if (enginePackageName != null) {
                textToSpeech = TextToSpeech(getApplication(), { status ->
                    onTTSInit(status, enginePackageName)
                }, enginePackageName)
            } else {
                textToSpeech = TextToSpeech(getApplication()) { status ->
                    onTTSInit(status, null)
                }
            }
        } catch (e: Exception) {
            _status.value = TTSStatus.Error("Failed to instantiate TTS: ${e.localizedMessage}")
        }
    }

    private fun onTTSInit(status: Int, initializedEnginePackage: String?) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech
            if (tts != null) {
                val defaultEngine = tts.defaultEngine
                val activeEnginePackage = initializedEnginePackage ?: defaultEngine

                // Extract list of engines currently installed on-device
                val engines = tts.engines.map { engineInfo ->
                    TTSEngine(
                        name = engineInfo.name,
                        label = engineInfo.label,
                        isDefault = engineInfo.name == defaultEngine,
                        isActive = engineInfo.name == activeEnginePackage
                    )
                }
                _installedEngines.value = engines
                _activeEngine.value = engines.find { it.isActive } ?: TTSEngine(
                    name = activeEnginePackage,
                    label = activeEnginePackage.substringAfterLast("."),
                    isDefault = activeEnginePackage == defaultEngine,
                    isActive = true
                )

                // Set language to match current StateFlow selections
                tts.setLanguage(_selectedLanguage.value.locale)

                // Query and load voice variants for this language
                refreshVoices()

                _status.value = TTSStatus.Ready
            } else {
                _status.value = TTSStatus.Error("TTS instance was null during callbacks.")
            }
        } else {
            _status.value = TTSStatus.Error("TTS Initialization failed (code $status)")
        }
    }

    fun refreshVoices() {
        val tts = textToSpeech ?: return
        try {
            val locale = _selectedLanguage.value.locale
            val voicesSet = tts.voices
            if (!voicesSet.isNullOrEmpty()) {
                val languageMatches = voicesSet.filter { voice ->
                    val vLoc = voice.locale
                    vLoc.language.equals(locale.language, ignoreCase = true)
                }

                val voiceItems = languageMatches.map { voice ->
                    val isOffline = !voice.isNetworkConnectionRequired
                    val gender = when {
                        voice.features?.contains("female") == true || voice.name.contains("female", ignoreCase = true) || voice.name.contains("#f", ignoreCase = true) -> "Female"
                        voice.features?.contains("male") == true || voice.name.contains("male", ignoreCase = true) || voice.name.contains("#m", ignoreCase = true) -> "Male"
                        else -> "Natural"
                    }
                    val qualityStr = when (voice.quality) {
                        Voice.QUALITY_VERY_HIGH -> "Ultra High Fidelity"
                        Voice.QUALITY_HIGH -> "High Quality"
                        Voice.QUALITY_NORMAL -> "Standard"
                        else -> "Compact"
                    }

                    val cleanLabel = formatVoiceLabel(voice.name, gender, isOffline)
                    TTSVoiceItem(
                        name = voice.name,
                        displayName = cleanLabel,
                        isOfflineLocal = isOffline,
                        qualityLabel = qualityStr,
                        gender = gender,
                        rawVoice = voice
                    )
                }.sortedWith(compareByDescending<TTSVoiceItem> { it.isOfflineLocal }
                    .thenByDescending { it.rawVoice.quality }
                    .thenBy { it.displayName })

                _availableVoices.value = voiceItems

                // Auto-select the highest quality offline voice
                val currentVoiceName = tts.voice?.name
                val bestVoice = voiceItems.find { it.name == currentVoiceName }
                    ?: voiceItems.find { it.isOfflineLocal }
                    ?: voiceItems.firstOrNull()

                _selectedVoice.value = bestVoice
                if (bestVoice != null) {
                    try {
                        tts.voice = bestVoice.rawVoice
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                _availableVoices.value = emptyList()
                _selectedVoice.value = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _availableVoices.value = emptyList()
            _selectedVoice.value = null
        }
    }

    private fun formatVoiceLabel(name: String, gender: String, isOffline: Boolean): String {
        val tag = if (name.contains("#")) {
            name.substringAfter("#").substringBefore("-")
        } else {
            name.substringAfterLast("-").ifEmpty { "Model" }
        }
        val locTag = if (isOffline) "Local" else "Cloud"
        return "$gender $tag ($locTag)"
    }

    fun selectVoice(voiceItem: TTSVoiceItem) {
        _selectedVoice.value = voiceItem
        textToSpeech?.let { tts ->
            try {
                tts.voice = voiceItem.rawVoice
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setFilterOfflineOnly(filter: Boolean) {
        _filterOfflineOnly.value = filter
    }

    fun setNaturalPacingEnabled(enabled: Boolean) {
        _naturalPacingEnabled.value = enabled
    }

    fun applyPreset(preset: VocalPreset) {
        _activePresetId.value = preset.id
        setPitch(preset.pitch)
        setSpeed(preset.speed)
    }

    fun setPitch(value: Float) {
        _pitch.value = value
        if (_activePresetId.value != "custom") {
            val matching = vocalPresets.find { it.pitch == value && it.speed == _speed.value }
            _activePresetId.value = matching?.id ?: "custom"
        }
    }

    fun setSpeed(value: Float) {
        _speed.value = value
        if (_activePresetId.value != "custom") {
            val matching = vocalPresets.find { it.speed == value && it.pitch == _pitch.value }
            _activePresetId.value = matching?.id ?: "custom"
        }
    }

    fun setSelectedLanguage(language: LanguageOption) {
        _selectedLanguage.value = language
        textToSpeech?.let { tts ->
            tts.setLanguage(language.locale)
            refreshVoices()
        }
    }

    /**
     * Enhances speech text by smoothing punctuation intervals and spacing
     * to eliminate mechanical/robotic run-on speech and induce natural breathing pauses.
     */
    private fun humanizeText(input: String): String {
        if (!_naturalPacingEnabled.value) return input
        var processed = input.trim()
        // Replace multiple dots or ellipses with smooth natural pauses
        processed = processed.replace(Regex("\\.{2,}"), "...")
        // Ensure commas have space to trigger prosody pauses
        processed = processed.replace(Regex(",(?=[^\\s])"), ", ")
        // Ensure semicolons, colons, and hyphens give natural cadences
        processed = processed.replace(Regex(";(?=[^\\s])"), "; ")
        processed = processed.replace(Regex("—|--"), ", ")
        return processed
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        val tts = textToSpeech
        if (tts != null) {
            _status.value = TTSStatus.Ready
            try {
                tts.stop()

                val voiceToUse = _selectedVoice.value
                if (voiceToUse != null) {
                    tts.voice = voiceToUse.rawVoice
                }

                tts.setPitch(_pitch.value)
                tts.setSpeechRate(_speed.value)
                tts.language = _selectedLanguage.value.locale

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _status.value = TTSStatus.Error("Playback error occurred.")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        _status.value = TTSStatus.Error("Playback error code: $errorCode")
                    }
                })

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "voice_lab_utterance")
                }

                val processedText = humanizeText(text)
                val result = tts.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, "voice_lab_utterance")
                if (result == TextToSpeech.ERROR) {
                    _status.value = TTSStatus.Error("Speak action failed. Check if engine supports this language.")
                }
            } catch (e: Exception) {
                _status.value = TTSStatus.Error("Error speaking: ${e.localizedMessage}")
            }
        } else {
            _status.value = TTSStatus.Error("TTS Engine not ready.")
        }
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    // Room Database Operations
    fun savePhrase(text: String, label: String, category: String = "General", pitchVal: Float = _pitch.value, speedVal: Float = _speed.value) {
        if (text.isBlank() || label.isBlank()) return
        viewModelScope.launch {
            val phrase = Phrase(
                text = text.trim(),
                languageCode = _selectedLanguage.value.code,
                languageName = _selectedLanguage.value.name,
                label = label.trim(),
                category = category,
                isFavorite = false,
                pitch = pitchVal,
                speed = speedVal
            )
            repository.insert(phrase)
        }
    }

    fun updatePhrase(phrase: Phrase) {
        viewModelScope.launch {
            repository.update(phrase)
        }
    }

    fun deletePhrase(phrase: Phrase) {
        viewModelScope.launch {
            repository.delete(phrase)
        }
    }

    fun toggleFavorite(phrase: Phrase) {
        viewModelScope.launch {
            repository.toggleFavorite(phrase)
        }
    }

    fun resetToDefaultPack() {
        viewModelScope.launch {
            repository.seedDefaultPhrases()
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.let {
            try {
                it.stop()
                it.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
