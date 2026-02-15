package me.siddheshkothadi.codexdroid.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexColorsContractTest {
    @Test
    fun accentRolesRemainDistinctAcrossThemes() {
        listOf(LightCodexColors, DarkCodexColors).forEach { colors ->
            assertNotEquals(colors.accentAction, colors.monochromeActionBackground)
            assertNotEquals(colors.chipAccent, colors.userMessageBackground)
        }
    }

    @Test
    fun semanticControlTokensAreVisibleAndOpaqueEnough() {
        listOf(LightCodexColors, DarkCodexColors).forEach { colors ->
            assertNotEquals(Color.Transparent, colors.neutralIconButtonBackground)
            assertNotEquals(Color.Transparent, colors.neutralIconButtonContent)
            assertNotEquals(Color.Transparent, colors.controlStrong)
            assertNotEquals(Color.Transparent, colors.controlStrongOn)
            assertTrue(colors.emptyStateLogoAlpha in 0f..1f)
        }
    }
}
