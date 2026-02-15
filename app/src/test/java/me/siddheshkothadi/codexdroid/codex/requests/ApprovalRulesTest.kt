package me.siddheshkothadi.codexdroid.codex.requests

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalRulesTest {
    @Test
    fun extractCommandTokens_readsArgvArrays() {
        val params =
            buildJsonObject {
                put(
                    "argv",
                    buildJsonArray {
                        add(JsonPrimitive("npm"))
                        add(JsonPrimitive("test"))
                    },
                )
            }

        val tokens = ApprovalRules.extractCommandTokens(params)

        assertNotNull(tokens)
        assertEquals(listOf("npm", "test"), tokens)
    }

    @Test
    fun extractCommandTokens_readsCommandStrings() {
        val params =
            buildJsonObject {
                put("command", "git commit -m \"hello world\"")
            }

        val tokens = ApprovalRules.extractCommandTokens(params)

        assertNotNull(tokens)
        assertEquals(listOf("git", "commit", "-m", "hello world"), tokens)
    }

    @Test
    fun matchesCommandPrefix_matchesConfiguredRule() {
        val command = listOf("pnpm", "test", "--watch")
        val allowlist = listOf(listOf("pnpm", "test"))

        assertTrue(ApprovalRules.matchesCommandPrefix(command, allowlist))
    }

    @Test
    fun matchesCommandPrefix_rejectsNonMatchingRule() {
        val command = listOf("pnpm", "lint")
        val allowlist = listOf(listOf("pnpm", "test"))

        assertFalse(ApprovalRules.matchesCommandPrefix(command, allowlist))
    }
}
