package me.siddheshkothadi.codexdroid.feature.session.ui

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class TurnSpeechPlaybackState {
    Idle,
    Speaking,
    Paused,
}

class TurnTextToSpeechController(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady: Boolean = false
    private var pendingStart: PendingStart? = null
    private var currentUtteranceId: String? = null
    private var utteranceStartOffset: Int = 0
    private var pauseRequested: Boolean = false

    private data class PendingStart(
        val turnId: String,
        val text: String,
        val startOffset: Int,
    )

    var activeTurnId: String? by mutableStateOf(null)
        private set

    var playbackState: TurnSpeechPlaybackState by mutableStateOf(TurnSpeechPlaybackState.Idle)
        private set

    private var activeText: String = ""
    private var resumeOffset: Int = 0

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            isReady = false
            playbackState = TurnSpeechPlaybackState.Idle
            return
        }

        isReady = true
        textToSpeech?.language = Locale.getDefault()
        textToSpeech?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId != currentUtteranceId) return
                    playbackState = TurnSpeechPlaybackState.Speaking
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != currentUtteranceId) return
                    clearPlayback()
                }

                override fun onError(utteranceId: String?) {
                    if (utteranceId != currentUtteranceId) return
                    clearPlayback()
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (utteranceId != currentUtteranceId) return
                    if (pauseRequested && activeTurnId != null) {
                        pauseRequested = false
                        playbackState = TurnSpeechPlaybackState.Paused
                        return
                    }
                    clearPlayback()
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int,
                ) {
                    if (utteranceId != currentUtteranceId) return
                    // Track a safe resume offset in the original full text.
                    resumeOffset = (utteranceStartOffset + end).coerceIn(0, activeText.length)
                }
            }
        )

        pendingStart?.let { pending ->
            pendingStart = null
            speakInternal(
                turnId = pending.turnId,
                text = pending.text,
                startOffset = pending.startOffset,
            )
        }
    }

    fun start(turnId: String, text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        activeTurnId = turnId
        activeText = normalized
        resumeOffset = 0
        playbackState = TurnSpeechPlaybackState.Speaking
        speakInternal(turnId = turnId, text = normalized, startOffset = 0)
    }

    fun pause() {
        if (playbackState != TurnSpeechPlaybackState.Speaking) return
        pauseRequested = true
        textToSpeech?.stop()
    }

    fun resume() {
        val turnId = activeTurnId ?: return
        if (playbackState != TurnSpeechPlaybackState.Paused) return
        if (resumeOffset >= activeText.length) {
            clearPlayback()
            return
        }
        playbackState = TurnSpeechPlaybackState.Speaking
        speakInternal(turnId = turnId, text = activeText, startOffset = resumeOffset)
    }

    fun release() {
        pauseRequested = false
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        clearPlayback()
    }

    private fun speakInternal(turnId: String, text: String, startOffset: Int) {
        val safeStart = startOffset.coerceIn(0, text.length)
        val remaining = text.substring(safeStart)
        if (remaining.isBlank()) {
            clearPlayback()
            return
        }

        if (!isReady || textToSpeech == null) {
            pendingStart = PendingStart(turnId = turnId, text = text, startOffset = safeStart)
            return
        }

        activeTurnId = turnId
        activeText = text
        utteranceStartOffset = safeStart
        resumeOffset = safeStart
        pauseRequested = false

        val utteranceId = "turn-$turnId-${System.nanoTime()}"
        currentUtteranceId = utteranceId
        val params = Bundle()
        textToSpeech?.speak(remaining, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun clearPlayback() {
        pauseRequested = false
        currentUtteranceId = null
        activeTurnId = null
        activeText = ""
        resumeOffset = 0
        utteranceStartOffset = 0
        playbackState = TurnSpeechPlaybackState.Idle
    }
}

@Composable
fun rememberTurnTextToSpeechController(): TurnTextToSpeechController {
    val context = LocalContext.current
    val controller = remember(context.applicationContext) {
        TurnTextToSpeechController(context.applicationContext)
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

