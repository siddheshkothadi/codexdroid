package me.siddheshkothadi.codexdroid.codex.requests

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import me.siddheshkothadi.codexdroid.codex.ServerRequest

class PendingRequestQueueTest {

    @Test
    fun parseApproval_mapsToApprovalRequest() {
        val parsed =
            PendingRequestParser.parse(
                ServerRequest(
                    method = "tool/requestApproval",
                    id = 11,
                    params = buildJsonObject { put("reason", "apply patch") },
                )
            )

        assertTrue(parsed is ApprovalPendingRequest)
        assertEquals(11L, parsed.requestId)
    }

    @Test
    fun parseUserInput_mapsToStructuredRequest() {
        val request =
            ServerRequest(
                method = "item/tool/requestUserInput",
                id = 22,
                params =
                    buildJsonObject {
                        put("threadId", "thread-1")
                        put("turnId", "turn-1")
                        put("itemId", "item-1")
                        put(
                            "questions",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("id", "merge_policy")
                                        put("header", "Merge policy")
                                        put("question", "Pick one")
                                        put(
                                            "options",
                                            buildJsonArray {
                                                add(
                                                    buildJsonObject {
                                                        put("label", "Fast loop + guardrails")
                                                        put("description", "Recommended")
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    },
            )

        val parsed = PendingRequestParser.parse(request)
        assertTrue(parsed is UserInputPendingRequest)
        parsed as UserInputPendingRequest
        assertEquals("thread-1", parsed.threadId)
        assertEquals(1, parsed.questions.size)
        assertEquals("merge_policy", parsed.questions.first().id)
    }

    @Test
    fun unknownRequests_areQueuedAndSurfaced() {
        val queue: PendingRequestQueue = InMemoryPendingRequestQueue()
        queue.enqueue(UnknownPendingRequest(requestId = 33, method = "item/tool/unknown"))

        val next = queue.nextUnknown(current = null)

        assertNotNull(next)
        assertEquals(33L, next?.requestId)
        assertEquals("item/tool/unknown", next?.method)
    }

    @Test
    fun parseUserInput_withMissingQuestions_returnsUnknown() {
        val request =
            ServerRequest(
                method = "item/tool/requestUserInput",
                id = 44,
                params =
                    buildJsonObject {
                        put("threadId", "thread-1")
                        put("turnId", "turn-1")
                        put("itemId", "item-1")
                    },
            )

        val parsed = PendingRequestParser.parse(request)

        assertTrue(parsed is UnknownPendingRequest)
        assertEquals(44L, parsed.requestId)
    }

    @Test
    fun approvals_areServedFifo() {
        val queue: PendingRequestQueue = InMemoryPendingRequestQueue()
        queue.enqueue(ApprovalPendingRequest(requestId = 101, method = "tool/requestApproval"))
        queue.enqueue(ApprovalPendingRequest(requestId = 102, method = "tool/requestApproval"))

        val first = queue.nextApproval(current = null)
        val second = queue.nextApproval(current = null)

        assertEquals(101L, first?.requestId)
        assertEquals(102L, second?.requestId)
    }
}
