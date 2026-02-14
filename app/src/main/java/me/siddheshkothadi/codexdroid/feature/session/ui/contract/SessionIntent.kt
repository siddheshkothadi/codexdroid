package me.siddheshkothadi.codexdroid.feature.session.ui.contract

sealed interface SessionIntent {
    data class SendMessage(val text: String) : SessionIntent

    data object StopCurrentTurn : SessionIntent

    data object RefreshControls : SessionIntent

    data class StartNewSession(val cwd: String?) : SessionIntent
}

