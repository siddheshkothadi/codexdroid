package me.siddheshkothadi.codexdroid.feature.session.ui.contract

import me.siddheshkothadi.codexdroid.feature.session.ui.SessionUiState

fun interface SessionStateReducer {
    fun reduce(state: SessionUiState, action: SessionAction): SessionUiState
}

object DefaultSessionStateReducer : SessionStateReducer {
    override fun reduce(state: SessionUiState, action: SessionAction): SessionUiState {
        return when (action) {
            is SessionAction.SetActiveTurnId -> state.copy(activeTurnId = action.turnId)
            is SessionAction.SetError -> state.copy(error = action.message)
            is SessionAction.SetPendingMessage -> state.copy(pendingUserMessage = action.text)
            is SessionAction.SetSending -> state.copy(isSending = action.sending)
        }
    }
}

