package me.siddheshkothadi.codexdroid.domain.model

data class SessionControlDefaults(
    val model: String? = null,
    val effort: String? = null,
    val followUpMessageBehavior: String = "queue",
)
