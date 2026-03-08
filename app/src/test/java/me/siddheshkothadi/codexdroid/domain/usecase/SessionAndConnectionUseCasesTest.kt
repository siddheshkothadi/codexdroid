package me.siddheshkothadi.codexdroid.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import me.siddheshkothadi.codexdroid.codex.CodexResponse
import me.siddheshkothadi.codexdroid.codex.EmptyResult
import me.siddheshkothadi.codexdroid.codex.ThreadReadResult
import me.siddheshkothadi.codexdroid.codex.ThreadResumeResult
import me.siddheshkothadi.codexdroid.codex.ThreadStartResult
import me.siddheshkothadi.codexdroid.codex.TurnSteerResult
import me.siddheshkothadi.codexdroid.codex.TurnStartResult
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.domain.repository.CodexSessionRepository
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionMigrationRepository
import me.siddheshkothadi.codexdroid.domain.repository.ConnectionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAndConnectionUseCasesTest {
    @Test
    fun pingConnectionUseCase_delegatesToRepository() =
        runBlocking {
            val connection =
                Connection(
                    id = "conn-1",
                    name = "Local",
                    baseUrl = "http://localhost:8080",
                    secret = "secret",
                    updatedAt = 123L,
                )
            val repository = FakeCodexSessionRepository(pingResult = true)
            val useCase = PingConnectionUseCase(repository)

            val result = useCase(connection)

            assertTrue(result)
            assertEquals(connection, repository.lastPingConnection)
        }

    @Test
    fun pingConnectionUseCase_returnsFalseWhenRepositoryReportsFalse() =
        runBlocking {
            val connection =
                Connection(
                    id = "conn-2",
                    name = "Remote",
                    baseUrl = "http://localhost:8090",
                    secret = "secret2",
                    updatedAt = 456L,
                )
            val repository = FakeCodexSessionRepository(pingResult = false)
            val useCase = PingConnectionUseCase(repository)

            val result = useCase(connection)

            assertFalse(result)
            assertEquals(connection, repository.lastPingConnection)
        }

    @Test
    fun ensureConnectionStorageMigrationUseCase_triggersMigration() =
        runBlocking {
            val repository = FakeConnectionMigrationRepository()
            val useCase = EnsureConnectionStorageMigrationUseCase(repository)

            useCase()

            assertTrue(repository.migrateCalled)
        }

    @Test
    fun addConnectionUseCase_passesArgumentsToRepository() =
        runBlocking {
            val repository = FakeConnectionRepository()
            val useCase = AddConnectionUseCase(repository)

            useCase(
                name = "Workspace",
                url = "http://localhost:8080",
                secret = "abc123",
            )

            assertEquals("Workspace", repository.addedName)
            assertEquals("http://localhost:8080", repository.addedBaseUrl)
            assertEquals("abc123", repository.addedSecret)
        }

    private class FakeConnectionRepository : ConnectionRepository {
        var addedName: String? = null
        var addedBaseUrl: String? = null
        var addedSecret: String? = null

        override fun observeConnections(): Flow<List<Connection>> = flowOf(emptyList())

        override suspend fun addConnection(name: String, baseUrl: String, secret: String) {
            addedName = name
            addedBaseUrl = baseUrl
            addedSecret = secret
        }

        override suspend fun updateConnection(id: String, name: String, baseUrl: String, secret: String) = Unit

        override suspend fun updateLastUsed(id: String) = Unit

        override suspend fun deleteConnection(id: String) = Unit

        override suspend fun importConnections(connections: List<Connection>) = Unit
    }

    private class FakeConnectionMigrationRepository : ConnectionMigrationRepository {
        var migrateCalled: Boolean = false

        override suspend fun migrateIfNeeded() {
            migrateCalled = true
        }
    }

    private class FakeCodexSessionRepository(
        private val pingResult: Boolean,
    ) : CodexSessionRepository {
        var lastPingConnection: Connection? = null

        override suspend fun ping(connection: Connection): Boolean {
            lastPingConnection = connection
            return pingResult
        }

        override suspend fun startThread(baseUrl: String, secret: String?, cwd: String?): CodexResponse<ThreadStartResult> {
            error("Not needed in test")
        }

        override suspend fun resumeThread(
            baseUrl: String,
            secret: String?,
            threadId: String,
        ): CodexResponse<ThreadResumeResult> {
            error("Not needed in test")
        }

        override suspend fun startTurn(
            baseUrl: String,
            secret: String?,
            threadId: String,
            text: String,
            cwd: String?,
            model: String?,
            effort: String?,
            collaborationMode: JsonElement?,
        ): CodexResponse<TurnStartResult> {
            error("Not needed in test")
        }

        override suspend fun steerTurn(
            baseUrl: String,
            secret: String?,
            threadId: String,
            turnId: String,
            text: String,
        ): CodexResponse<TurnSteerResult> {
            error("Not needed in test")
        }

        override suspend fun readThread(baseUrl: String, secret: String?, threadId: String): CodexResponse<ThreadReadResult> {
            error("Not needed in test")
        }

        override suspend fun listModels(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun listCollaborationModes(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun listExperimentalFeatures(
            baseUrl: String,
            secret: String?,
            cursor: String?,
            limit: Int?,
        ): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun listSkills(baseUrl: String, secret: String?, cwd: String?): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun readConfig(baseUrl: String, secret: String?): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun writeConfigValue(
            baseUrl: String,
            secret: String?,
            key: String,
            value: JsonElement,
        ): CodexResponse<JsonElement> {
            error("Not needed in test")
        }

        override suspend fun interruptTurn(
            baseUrl: String,
            secret: String?,
            threadId: String,
            turnId: String,
        ): CodexResponse<EmptyResult> {
            error("Not needed in test")
        }

        override suspend fun respondToApprovalRequest(baseUrl: String, secret: String?, requestId: Long, decision: String) {
            error("Not needed in test")
        }

        override suspend fun respondToUserInputRequest(
            baseUrl: String,
            secret: String?,
            requestId: Long,
            answers: Map<String, List<String>>,
        ) {
            error("Not needed in test")
        }
    }
}
