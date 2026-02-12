package me.siddheshkothadi.codexdroid.ui.session.contract

sealed interface SessionEffect {
    data class ShowError(val message: String) : SessionEffect

    data class SendTurn(val text: String) : SessionEffect

    data object RefreshThreads : SessionEffect
}
