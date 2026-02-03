package me.siddheshkothadi.codexdroid.navigation

import android.content.Intent

data class CodexDroidAppLink(
    val connectionId: String? = null,
    val threadId: String? = null,
    val turnId: String? = null,
    val openLatest: Boolean = false,
)

object CodexDroidAppLinkKeys {
    const val EXTRA_CONNECTION_ID = "me.siddheshkothadi.codexdroid.extra.CONNECTION_ID"
    const val EXTRA_THREAD_ID = "me.siddheshkothadi.codexdroid.extra.THREAD_ID"
    const val EXTRA_TURN_ID = "me.siddheshkothadi.codexdroid.extra.TURN_ID"
    const val EXTRA_OPEN_LATEST = "me.siddheshkothadi.codexdroid.extra.OPEN_LATEST"
}

fun Intent.toCodexDroidAppLinkOrNull(): CodexDroidAppLink? {
    val connectionId = getStringExtra(CodexDroidAppLinkKeys.EXTRA_CONNECTION_ID)
    val threadId = getStringExtra(CodexDroidAppLinkKeys.EXTRA_THREAD_ID)
    val turnId = getStringExtra(CodexDroidAppLinkKeys.EXTRA_TURN_ID)
    val openLatest = getBooleanExtra(CodexDroidAppLinkKeys.EXTRA_OPEN_LATEST, false)

    if (connectionId.isNullOrBlank() && threadId.isNullOrBlank() && turnId.isNullOrBlank() && !openLatest) {
        return null
    }

    return CodexDroidAppLink(
        connectionId = connectionId?.takeIf { it.isNotBlank() },
        threadId = threadId?.takeIf { it.isNotBlank() },
        turnId = turnId?.takeIf { it.isNotBlank() },
        openLatest = openLatest,
    )
}

