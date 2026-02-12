package me.siddheshkothadi.codexdroid.codex

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadEventReducerTest {

    @Test
    fun turnStarted_upsertsTurn() {
        val initial = Thread(id = "thread-1")
        val params =
            CodexJson
                .encodeToJsonElement(
                    TurnStartedNotification.serializer(),
                    TurnStartedNotification(
                        threadId = "thread-1",
                        turn = Turn(id = "turn-1", status = TurnStatus.inProgress)
                    ),
                ).jsonObject

        val updated = ThreadEventReducer.applyNotification(initial, "turn/started", params)

        assertEquals(1, updated.turns.size)
        assertEquals("turn-1", updated.turns.first().id)
        assertEquals(TurnStatus.inProgress, updated.turns.first().status)
    }

    @Test
    fun agentMessageDelta_appendsTextMonotonically() {
        val initial = Thread(id = "thread-1", turns = listOf(Turn(id = "turn-1")))

        val firstDelta =
            CodexJson
                .encodeToJsonElement(
                    AgentMessageDeltaNotification.serializer(),
                    AgentMessageDeltaNotification(
                        threadId = "thread-1",
                        turnId = "turn-1",
                        itemId = "item-1",
                        delta = "Hello",
                    ),
                ).jsonObject

        val secondDelta =
            CodexJson
                .encodeToJsonElement(
                    AgentMessageDeltaNotification.serializer(),
                    AgentMessageDeltaNotification(
                        threadId = "thread-1",
                        turnId = "turn-1",
                        itemId = "item-1",
                        delta = " world",
                    ),
                ).jsonObject

        val afterFirst = ThreadEventReducer.applyNotification(initial, "item/agentMessage/delta", firstDelta)
        val afterSecond = ThreadEventReducer.applyNotification(afterFirst, "item/agentMessage/delta", secondDelta)

        val message = afterSecond.turns.first().items.first() as ThreadItem.AgentMessage
        assertEquals("Hello world", message.text)
    }

    @Test
    fun planUpdated_createsPlanItemForTurn() {
        val initial = Thread(id = "thread-1", turns = listOf(Turn(id = "turn-1")))
        val params =
            CodexJson
                .encodeToJsonElement(
                    TurnPlanUpdatedNotification.serializer(),
                    TurnPlanUpdatedNotification(
                        turnId = "turn-1",
                        explanation = "Apply small refactor",
                        plan = listOf(PlanEntry(step = "Extract queue", status = PlanEntryStatus.inProgress)),
                    ),
                ).jsonObject

        val updated = ThreadEventReducer.applyNotification(initial, "turn/plan/updated", params)

        val plan = updated.turns.first().items.firstOrNull { it is ThreadItem.PlanUpdate } as? ThreadItem.PlanUpdate
        assertNotNull(plan)
        assertEquals("Apply small refactor", plan?.explanation)
        assertEquals(1, plan?.plan?.size)
    }

    @Test
    fun itemStarted_isIdempotentByItemId() {
        val initial = Thread(id = "thread-1", turns = listOf(Turn(id = "turn-1")))
        val params =
            buildJsonObject {
                put("threadId", "thread-1")
                put("turnId", "turn-1")
                put(
                    "item",
                    buildJsonObject {
                        put("type", "agentMessage")
                        put("id", "item-1")
                        put("text", "")
                    }
                )
            }

        val once = ThreadEventReducer.applyNotification(initial, "item/started", params)
        val twice = ThreadEventReducer.applyNotification(once, "item/started", params)

        val items = twice.turns.first().items.filter { it.id == "item-1" }
        assertEquals(1, items.size)
        assertTrue(items.first() is ThreadItem.AgentMessage)
    }
}
