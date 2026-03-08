package me.siddheshkothadi.codexdroid.domain.repository

import me.siddheshkothadi.codexdroid.domain.model.SessionControlDefaults

interface SessionPreferencesRepository {
    suspend fun getSessionControlDefaults(): SessionControlDefaults

    suspend fun saveSessionControlDefaults(model: String?, effort: String?, followUpMessageBehavior: String? = null)

    suspend fun getApprovalAllowRules(connectionId: String, workspaceKey: String): List<List<String>>

    suspend fun addApprovalAllowRule(connectionId: String, workspaceKey: String, command: List<String>)
}
