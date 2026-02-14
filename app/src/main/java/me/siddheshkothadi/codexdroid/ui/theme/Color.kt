package me.siddheshkothadi.codexdroid.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private object PurpleScale {
    val Purple25 = Color(0xFFF9F5FE)
    val Purple50 = Color(0xFFEFE5FE)
    val Purple75 = Color(0xFFE0CEFD)
    val Purple100 = Color(0xFFCEB0FB)
    val Purple200 = Color(0xFFBE95FA)
    val Purple300 = Color(0xFFAD7BF9)
    val Purple400 = Color(0xFF924FF7)
    val Purple500 = Color(0xFF8046D9)
    val Purple600 = Color(0xFF6B3AB4)
    val Purple700 = Color(0xFF532D8D)
    val Purple800 = Color(0xFF3F226A)
    val Purple900 = Color(0xFF2C184A)
    val Purple950 = Color(0xFF160C25)
    val Purple1000 = Color(0xFF100A19)
    val PurpleA25 = Color(0x0F924FF7)
    val PurpleA50 = Color(0x26924FF7)
    val PurpleA75 = Color(0x47924FF7)
    val PurpleA100 = Color(0x73924FF7)
    val PurpleA200 = Color(0x99924FF7)
    val PurpleA300 = Color(0xBF924FF7)
}

@Immutable
data class CodexColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverted: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconTertiary: Color,
    val iconInverted: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val accentPrimary: Color,
    val accentError: Color,
    val accentWarning: Color,
    val accentSuccess: Color,
    val accentInfo: Color,
    val accentUi: Color,
    val accentAction: Color,
    val onAccentAction: Color,
    val userMessageBackground: Color,
    val userMessageText: Color,
    val chipAccent: Color,
    val chipAccentActiveBackground: Color,
    val inputFieldBackground: Color,
    val inputFieldBorder: Color,
    val inputButtonBackground: Color,
    val inputButtonContent: Color,
)

internal val LightCodexColors =
    CodexColors(
        bgPrimary = Color(0xFFF3F3F3),
        bgSecondary = Color(0xFFE8E8E8),
        bgTertiary = Color(0xFFF3F3F3),
        textPrimary = Color(0xFF0D0D0D),
        textSecondary = Color(0xFF5D5D5D),
        textTertiary = Color(0xFF8F8F8F),
        textInverted = Color(0xFFF9F5FE),
        iconPrimary = Color(0xFF0D0D0D),
        iconSecondary = Color(0xFF5D5D5D),
        iconTertiary = Color(0xFF8F8F8F),
        iconInverted = Color(0xFFF9F5FE),
        borderSubtle = Color(0xFFF0F0F0),
        borderDefault = Color(0xFFDDDDDD),
        accentPrimary = Color(0xFF0285FF),
        accentError = Color(0xFFE02E2A),
        accentWarning = Color(0xFFE25507),
        accentSuccess = Color(0xFF008635),
        accentInfo = Color(0xFF0285FF),
        accentUi = PurpleScale.Purple400,
        accentAction = PurpleScale.Purple400,
        onAccentAction = PurpleScale.Purple25,
        userMessageBackground = PurpleScale.Purple50,
        userMessageText = PurpleScale.Purple900,
        chipAccent = Color(0xFF0285FF),
        chipAccentActiveBackground = Color(0xFFEBF4FF),
        inputFieldBackground = Color(0xFFFFFFFF),
        inputFieldBorder = Color(0x26000000),
        inputButtonBackground = Color(0xFFFFFFFF),
        inputButtonContent = Color(0xFF0D0D0D),
    )

internal val DarkCodexColors =
    CodexColors(
        bgPrimary = Color(0xFF212121),
        bgSecondary = Color(0xFF303030),
        bgTertiary = Color(0xFF414141),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCDCDCD),
        textTertiary = Color(0xFFAFAFAF),
        textInverted = Color(0xFF212121),
        iconPrimary = Color(0xFFFFFFFF),
        iconSecondary = Color(0xFFAFAFAF),
        iconTertiary = Color(0xFFAFAFAF),
        iconInverted = Color(0xFF212121),
        borderSubtle = Color(0xFF252525),
        borderDefault = Color(0xFF4C4C4C),
        accentPrimary = Color(0xFF99CEFF),
        accentError = Color(0xFFFF8583),
        accentWarning = Color(0xFFFF9E6C),
        accentSuccess = Color(0xFF40C977),
        accentInfo = Color(0xFF99CEFF),
        accentUi = PurpleScale.Purple300,
        accentAction = PurpleScale.Purple500,
        onAccentAction = PurpleScale.Purple25,
        userMessageBackground = PurpleScale.Purple700,
        userMessageText = PurpleScale.Purple25,
        chipAccent = Color(0xFF99CEFF),
        chipAccentActiveBackground = Color(0xFF414141),
        inputFieldBackground = Color(0xFF303030),
        inputFieldBorder = Color(0x33FFFFFF),
        inputButtonBackground = PurpleScale.Purple500,
        inputButtonContent = PurpleScale.Purple25,
    )
