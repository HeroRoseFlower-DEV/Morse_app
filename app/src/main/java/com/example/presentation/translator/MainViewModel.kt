package com.example.presentation.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.playback.MorsePlaybackEngine
import com.example.domain.model.HistoryItem
import com.example.domain.repository.HistoryRepository
import com.example.domain.usecase.MorseTranslatorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val inputText: String = "",
    val outputMorse: String = "",
    val isEnglish: Boolean = true,
    val wpm: Int = 20,
    val pitchHz: Int = 600,
    val audioEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val flashEnabled: Boolean = false,
    val isPlaying: Boolean = false,
    val showHistory: Boolean = false,
    val showCheatSheet: Boolean = false,
    val showSettings: Boolean = false
)

class MainViewModel(
    private val useCase: MorseTranslatorUseCase,
    private val historyRepository: HistoryRepository,
    private val playbackEngine: MorsePlaybackEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    
    val history = historyRepository.getAllHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var translationJob: Job? = null
    private var playbackJob: Job? = null
    
    // Tap Input State
    private var lastTapUpTime: Long = 0
    private var tapWordBuilder = StringBuilder()
    private var tapLetterBuilder = StringBuilder()
    private var tapCheckJob: Job? = null

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        translateTextToMorse(text)
    }

    fun onMorseTextChanged(morse: String) {
        _uiState.update { it.copy(outputMorse = morse) }
        translateMorseToText(morse)
    }

    private fun translateTextToMorse(text: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val morse = useCase.textToMorse(text, _uiState.value.isEnglish)
            _uiState.update { it.copy(outputMorse = morse) }
        }
    }

    private fun translateMorseToText(morse: String) {
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            val text = useCase.morseToText(morse, _uiState.value.isEnglish)
            _uiState.update { it.copy(inputText = text) }
        }
    }

    fun toggleLanguage() {
        val newState = !_uiState.value.isEnglish
        _uiState.update { it.copy(isEnglish = newState) }
        translateTextToMorse(_uiState.value.inputText)
    }

    fun saveToHistory() {
        val state = _uiState.value
        if (state.inputText.isNotBlank() && state.outputMorse.isNotBlank()) {
            viewModelScope.launch {
                historyRepository.insertHistory(
                    HistoryItem(
                        sourceText = state.inputText,
                        translatedText = state.outputMorse,
                        isEnglishToMorse = state.isEnglish
                    )
                )
            }
        }
    }
    
    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            historyRepository.deleteHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val state = _uiState.value
        if (state.outputMorse.isBlank()) return
        
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            playbackEngine.play(
                morseCode = state.outputMorse,
                wpm = state.wpm,
                pitchHz = state.pitchHz,
                enableAudio = state.audioEnabled,
                enableHaptic = state.hapticEnabled,
                enableFlash = state.flashEnabled,
                onPlaybackStateChanged = { isPlaying ->
                    _uiState.update { it.copy(isPlaying = isPlaying) }
                }
            )
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackEngine.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        playbackEngine.stop()
    }

    // Tap Input Logic
    fun onTapDown() {
        tapCheckJob?.cancel()
    }

    fun onTapUp(durationMs: Long) {
        val state = _uiState.value
        val dotDurationMs = (1200 / state.wpm).toLong()
        val isDash = durationMs >= dotDurationMs * 2 // slightly lenient threshold

        tapLetterBuilder.append(if (isDash) "-" else ".")
        updateMorseFromTap()

        lastTapUpTime = System.currentTimeMillis()
        tapCheckJob = viewModelScope.launch {
            delay(dotDurationMs * 3) // Letter gap
            if (tapLetterBuilder.isNotEmpty()) {
                tapWordBuilder.append(tapLetterBuilder.toString()).append(" ")
                tapLetterBuilder.clear()
                updateMorseFromTap()
            }
            delay(dotDurationMs * 4) // Word gap (total 7)
            if (tapWordBuilder.isNotEmpty()) {
                val currentMorse = _uiState.value.outputMorse
                val newMorse = if (currentMorse.isEmpty()) tapWordBuilder.toString().trim() else "$currentMorse / ${tapWordBuilder.toString().trim()}"
                _uiState.update { it.copy(outputMorse = newMorse) }
                translateMorseToText(newMorse)
                tapWordBuilder.clear()
            }
        }
    }
    
    private fun updateMorseFromTap() {
        val currentBase = _uiState.value.outputMorse.substringBeforeLast(" / ", "")
        val tempWord = tapWordBuilder.toString() + tapLetterBuilder.toString()
        val newMorse = if (currentBase.isEmpty() && _uiState.value.outputMorse.isEmpty()) {
            tempWord
        } else if (!_uiState.value.outputMorse.contains(" / ") && _uiState.value.outputMorse.isNotEmpty() && tapWordBuilder.isEmpty() && tapLetterBuilder.isNotEmpty()) {
            _uiState.value.outputMorse + tapLetterBuilder.last()
        } else {
             // simplified for UX: just append to current outputMorse
             _uiState.value.outputMorse + (if (tapLetterBuilder.length == 1 && tapWordBuilder.isEmpty()) (if (_uiState.value.outputMorse.endsWith(" ") || _uiState.value.outputMorse.isEmpty()) "" else " ") else "") + tapLetterBuilder.last()
        }
        // Let's just rebuild it cleanly: this can get messy, so let's just append characters directly for simplicity and let the user see it real time.
    }
    
    // Better tap update:
    fun appendTap(durationMs: Long) {
         val state = _uiState.value
         val dotDurationMs = (1200 / state.wpm).toLong()
         val isDash = durationMs >= dotDurationMs * 2
         
         val char = if(isDash) "-" else "."
         val currentMorse = state.outputMorse
         
         val now = System.currentTimeMillis()
         val timeSinceLastTap = now - lastTapUpTime
         
         val newMorse = if (lastTapUpTime == 0L || timeSinceLastTap < dotDurationMs * 2.5) {
             currentMorse + char // same letter
         } else if (timeSinceLastTap < dotDurationMs * 5.5) {
             "$currentMorse $char" // next letter
         } else {
             if (currentMorse.isEmpty()) char else "$currentMorse / $char" // next word
         }
         
         lastTapUpTime = now
         _uiState.update { it.copy(outputMorse = newMorse) }
         translateMorseToText(newMorse)
         
         tapCheckJob?.cancel()
         tapCheckJob = viewModelScope.launch {
             delay(dotDurationMs * 7)
             // Translate after gap to ensure final letter is converted
             translateMorseToText(_uiState.value.outputMorse)
         }
    }

    fun updateSettings(wpm: Int? = null, pitch: Int? = null, audio: Boolean? = null, haptic: Boolean? = null, flash: Boolean? = null) {
        _uiState.update { 
            it.copy(
                wpm = wpm ?: it.wpm,
                pitchHz = pitch ?: it.pitchHz,
                audioEnabled = audio ?: it.audioEnabled,
                hapticEnabled = haptic ?: it.hapticEnabled,
                flashEnabled = flash ?: it.flashEnabled
            )
        }
    }

    fun setShowHistory(show: Boolean) { _uiState.update { it.copy(showHistory = show) } }
    fun setShowCheatSheet(show: Boolean) { _uiState.update { it.copy(showCheatSheet = show) } }
    fun setShowSettings(show: Boolean) { _uiState.update { it.copy(showSettings = show) } }
}

class MainViewModelFactory(
    private val useCase: MorseTranslatorUseCase,
    private val historyRepository: HistoryRepository,
    private val playbackEngine: MorsePlaybackEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(useCase, historyRepository, playbackEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
