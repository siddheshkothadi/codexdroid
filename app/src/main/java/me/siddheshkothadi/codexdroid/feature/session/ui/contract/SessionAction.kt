package me.siddheshkothadi.codexdroid.feature.session.ui.contract

sealed interface SessionAction {
    data class SetSending(val sending: Boolean) : SessionAction

    data class SetError(val message: String?) : SessionAction

    data class SetActiveTurnId(val turnId: String?) : SessionAction

    data class SetPendingMessage(val text: String?) : SessionAction
}

