package me.siddheshkothadi.codexdroid.data.repository

import me.siddheshkothadi.codexdroid.data.local.AppSettingsManager
import me.siddheshkothadi.codexdroid.domain.model.SessionControlDefaults
import me.siddheshkothadi.codexdroid.domain.repository.SessionPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionPreferencesRepositoryImpl @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
) : SessionPreferencesRepository {
    override suspend fun getSessionControlDefaults(): SessionControlDefaults {
        return appSettingsManager.getSessionControlDefaults()
    }

    override suspend fun saveSessionControlDefaults(model: String?, effort: String?) {
        appSettingsManager.updateSessionControlDefaults(model = model, effort = effort)
    }

    override suspend fun getApprovalAllowRules(
        connectionId: String,
        workspaceKey: String,
    ): List<List<String>> {
        return appSettingsManager.getApprovalAllowRules(connectionId = connectionId, workspaceKey = workspaceKey)
    }

    override suspend fun addApprovalAllowRule(
        connectionId: String,
        workspaceKey: String,
        command: List<String>,
    ) {
        appSettingsManager.addApprovalAllowRule(
            connectionId = connectionId,
            workspaceKey = workspaceKey,
            command = command,
        )
    }
}
