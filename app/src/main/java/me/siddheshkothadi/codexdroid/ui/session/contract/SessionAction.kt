package me.siddheshkothadi.codexdroid.ui.session.contract

sealed interface SessionAction {
    data class SetSending(val sending: Boolean) : SessionAction

    data class SetError(val message: String?) : SessionAction

    data class SetActiveTurnId(val turnId: String?) : SessionAction

    data class SetPendingMessage(val text: String?) : SessionAction
}
