package me.siddheshkothadi.codexdroid.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Connection(
    val id: String,
    val name: String,
    val baseUrl: String,
    val secret: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
