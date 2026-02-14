package me.siddheshkothadi.codexdroid.feature.session.ui

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class TurnSpeechPlaybackState {
    Idle,
    Synthesizing,
    Speaking,
    Paused,
}

private enum class TurnSpeechEngine {
    Android,
    Sarvam,
}

class TurnTextToSpeechController(
    context: Context,
    private var synthesizeSarvam: suspend (String) -> ByteArray? = { null },
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var textToSpeech: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady: Boolean = false
    private var pendingStart: PendingStart? = null
    private var currentUtteranceId: String? = null
    private var utteranceStartOffset: Int = 0
    private var pauseRequested: Boolean = false
    private var activeEngine: TurnSpeechEngine? = null
    private var synthesisJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var mediaAudioFile: File? = null

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

    fun updateSynthesisProvider(provider: suspend (String) -> ByteArray?) {
        synthesizeSarvam = provider
    }

    fun start(turnId: String, text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return

        stopAllPlayback()
        activeTurnId = turnId
        activeText = normalized
        resumeOffset = 0
        playbackState = TurnSpeechPlaybackState.Synthesizing
        startSynthesis(turnId = turnId, text = normalized)
    }

    fun cancelSynthesis() {
        if (playbackState != TurnSpeechPlaybackState.Synthesizing) return
        synthesisJob?.cancel()
        synthesisJob = null
        clearPlayback()
    }

    fun pause() {
        if (playbackState != TurnSpeechPlaybackState.Speaking) return
        when (activeEngine) {
            TurnSpeechEngine.Sarvam -> {
                mediaPlayer?.pause()
                playbackState = TurnSpeechPlaybackState.Paused
            }
            TurnSpeechEngine.Android -> {
                pauseRequested = true
                textToSpeech?.stop()
            }
            null -> Unit
        }
    }

    fun resume() {
        val turnId = activeTurnId ?: return
        if (playbackState != TurnSpeechPlaybackState.Paused) return
        when (activeEngine) {
            TurnSpeechEngine.Sarvam -> {
                val player = mediaPlayer
                if (player == null) {
                    clearPlayback()
                    return
                }
                playbackState = TurnSpeechPlaybackState.Speaking
                player.start()
            }
            TurnSpeechEngine.Android -> {
                if (resumeOffset >= activeText.length) {
                    clearPlayback()
                    return
                }
                playbackState = TurnSpeechPlaybackState.Speaking
                speakInternal(turnId = turnId, text = activeText, startOffset = resumeOffset)
            }
            null -> clearPlayback()
        }
    }

    fun release() {
        stopAllPlayback()
        textToSpeech?.shutdown()
        textToSpeech = null
        controllerScope.cancel()
        clearPlayback()
    }

    private fun startSynthesis(turnId: String, text: String) {
        synthesisJob?.cancel()
        synthesisJob =
            controllerScope.launch {
                try {
                    val audioBytes =
                        withContext(Dispatchers.IO) {
                            synthesizeSarvam(text)
                        }

                    if (activeTurnId != turnId || playbackState != TurnSpeechPlaybackState.Synthesizing) {
                        return@launch
                    }

                    if (audioBytes == null || audioBytes.isEmpty()) {
                        playbackState = TurnSpeechPlaybackState.Speaking
                        speakInternal(turnId = turnId, text = text, startOffset = 0)
                        return@launch
                    }
                    try {
                        prepareAndPlaySarvam(turnId = turnId, text = text, audioBytes = audioBytes)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        if (activeTurnId == turnId) {
                            playbackState = TurnSpeechPlaybackState.Speaking
                            speakInternal(turnId = turnId, text = text, startOffset = 0)
                        }
                    }
                } catch (e: CancellationException) {
                    if (activeTurnId == turnId && playbackState == TurnSpeechPlaybackState.Synthesizing) {
                        clearPlayback()
                    }
                    throw e
                } catch (_: Exception) {
                    if (activeTurnId == turnId) {
                        playbackState = TurnSpeechPlaybackState.Speaking
                        speakInternal(turnId = turnId, text = text, startOffset = 0)
                    }
                } finally {
                    synthesisJob = null
                }
            }
    }

    private suspend fun prepareAndPlaySarvam(turnId: String, text: String, audioBytes: ByteArray) {
        val audioFile = withContext(Dispatchers.IO) { writeAudioCacheFile(audioBytes) }
        if (activeTurnId != turnId || playbackState != TurnSpeechPlaybackState.Synthesizing) {
            deleteAudioFile(audioFile)
            return
        }

        releaseMediaPlayer()
        val player = MediaPlayer()
        mediaPlayer = player
        mediaAudioFile = audioFile
        activeEngine = TurnSpeechEngine.Sarvam
        activeText = text
        resumeOffset = 0

        player.setOnPreparedListener { preparedPlayer ->
            if (activeTurnId != turnId) {
                preparedPlayer.release()
                deleteAudioFile(audioFile)
                return@setOnPreparedListener
            }
            playbackState = TurnSpeechPlaybackState.Speaking
            preparedPlayer.start()
        }
        player.setOnCompletionListener {
            clearPlayback()
        }
        player.setOnErrorListener { _, _, _ ->
            clearPlayback()
            true
        }
        player.setDataSource(audioFile.absolutePath)
        player.prepareAsync()
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

        releaseMediaPlayer()
        activeTurnId = turnId
        activeText = text
        activeEngine = TurnSpeechEngine.Android
        utteranceStartOffset = safeStart
        resumeOffset = safeStart
        pauseRequested = false

        val utteranceId = "turn-$turnId-${System.nanoTime()}"
        currentUtteranceId = utteranceId
        val params = Bundle()
        textToSpeech?.speak(remaining, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun writeAudioCacheFile(audioBytes: ByteArray): File {
        val file = File.createTempFile("sarvam-tts-", ".wav", appContext.cacheDir)
        file.writeBytes(audioBytes)
        return file
    }

    private fun clearPlayback() {
        pauseRequested = false
        pendingStart = null
        currentUtteranceId = null
        activeTurnId = null
        activeText = ""
        resumeOffset = 0
        utteranceStartOffset = 0
        activeEngine = null
        releaseMediaPlayer()
        playbackState = TurnSpeechPlaybackState.Idle
    }

    private fun stopAllPlayback() {
        pauseRequested = false
        pendingStart = null
        currentUtteranceId = null
        synthesisJob?.cancel()
        synthesisJob = null
        textToSpeech?.stop()
        releaseMediaPlayer()
        activeTurnId = null
        activeText = ""
        resumeOffset = 0
        utteranceStartOffset = 0
        activeEngine = null
        playbackState = TurnSpeechPlaybackState.Idle
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            runCatching {
                player.stop()
            }
            runCatching {
                player.release()
            }
        }
        mediaPlayer = null
        mediaAudioFile?.let(::deleteAudioFile)
        mediaAudioFile = null
    }

    private fun deleteAudioFile(file: File) {
        runCatching {
            if (file.exists()) file.delete()
        }
    }
}

@Composable
fun rememberTurnTextToSpeechController(
    synthesizeSarvam: suspend (String) -> ByteArray? = { null },
): TurnTextToSpeechController {
    val context = LocalContext.current
    val latestSynthesis by rememberUpdatedState(synthesizeSarvam)
    val controller =
        remember(context.applicationContext) {
            TurnTextToSpeechController(
                context = context.applicationContext,
                synthesizeSarvam = latestSynthesis,
            )
        }
    SideEffect {
        controller.updateSynthesisProvider(latestSynthesis)
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
