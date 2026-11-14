package app.naerio.ui

// insane-design: pleos (2026-11-15). 토큰 출처: insane-design/pleos/design.md §05, §06
// pleos.ai CSS의 --color-pleos-* 팔레트 + Pleos Connect 디자인 가이드 grayscale(basic_00~900).

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** design.md §06-1, §06-2. 화면에서는 직접 쓰지 말고 [NgColors]를 거친다. */
object Pleos {
    val Blue1 = Color(0xFFCDDCFF); val Blue2 = Color(0xFF5087FF); val Blue3 = Color(0xFF2350FF); val Blue4 = Color(0xFF0F235A)
    val Green1 = Color(0xFFBEFFE1); val Green2 = Color(0xFF69F5B9); val Green3 = Color(0xFF00D791); val Green4 = Color(0xFF053C32)
    val Red1 = Color(0xFFFFD7DC); val Red2 = Color(0xFFFF96A0); val Red3 = Color(0xFFFA293C); val Red4 = Color(0xFF5F0F14)
    val Basic00 = Color(0xFFFFFFFF); val Basic50 = Color(0xFFF7F8FA); val Basic100 = Color(0xFFEDEEF2)
    val Basic200 = Color(0xFFE0E1E6); val Basic300 = Color(0xFFC7C9CE); val Basic400 = Color(0xFFA4A8AE)
    val Basic500 = Color(0xFF686A72); val Basic600 = Color(0xFF44464E); val Basic700 = Color(0xFF313236)
    val Basic800 = Color(0xFF1D1E21); val Basic900 = Color(0xFF131417)
}

/** 시스템 토큰. Light/Dark 값은 Pleos Connect 가이드의 basic 단계 뒤집기 표를 따른다. */
@Immutable
data class NgColors(
    val bg: Color, val surface: Color, val sunken: Color, val line: Color,
    val ink: Color, val ink2: Color, val ink3: Color, val muted: Color,
    val cta: Color, val ctaPressed: Color, val onCta: Color,
    val basicBtn: Color, val basicBtnPressed: Color,
    val accent: Color, val accentTint: Color, val onAccentTint: Color,
    val okBg: Color, val okFg: Color, val noBg: Color, val noFg: Color,
    val unknownBg: Color, val unknownFg: Color, val warn: Color, val parking: Color,
    val isDark: Boolean,
)

private val LightNg = with(Pleos) {
    NgColors(
        bg = Basic50, surface = Basic00, sunken = Basic100, line = Basic200,
        ink = Basic900, ink2 = Basic600, ink3 = Basic500, muted = Basic400,
        cta = Basic600, ctaPressed = Basic700, onCta = Basic00,
        basicBtn = Basic100, basicBtnPressed = Basic200,
        accent = Blue3, accentTint = Blue1, onAccentTint = Blue4,
        okBg = Green1, okFg = Green4, noBg = Red1, noFg = Red4,
        unknownBg = Basic100, unknownFg = Basic600, warn = Red3, parking = Blue3, isDark = false,
    )
}

// Dark: basic_00=#131417, basic_50=#1D1E21, basic_100=#313236, basic_200=#44464E, basic_500=#C7C9CE, basic_600=#E0E1E6
private val DarkNg = with(Pleos) {
    NgColors(
        bg = Basic800, surface = Basic900, sunken = Basic700, line = Basic600,
        ink = Basic00, ink2 = Basic200, ink3 = Basic300, muted = Basic400,
        cta = Basic200, ctaPressed = Basic300, onCta = Basic900,
        basicBtn = Basic700, basicBtnPressed = Basic600,
        accent = Blue2, accentTint = Blue4, onAccentTint = Blue1,
        okBg = Green4, okFg = Green2, noBg = Red4, noFg = Red2,
        unknownBg = Basic700, unknownFg = Basic200, warn = Red2, parking = Blue2, isDark = true,
    )
}

/**
 * Pleos Connect 위계(px = dp, density 160). 기기 sans-serif가 Asta Sans로 재정의되어 있어 별도 번들이 없다.
 * Asta Sans 매핑: W600 → Bold 파일, W700 → ExtraBold 파일.
 */
@Immutable
data class NgType(
    val headlineS: TextStyle, val titleL: TextStyle, val titleM: TextStyle, val titleS: TextStyle,
    val bodyL: TextStyle, val bodyM: TextStyle, val bodyS: TextStyle, val label: TextStyle,
    val button: TextStyle,
)

private val Asta = FontFamily.SansSerif
private val Tight = (-0.01).em

private val Type = NgType(
    headlineS = TextStyle(fontFamily = Asta, fontSize = 48.sp, lineHeight = 60.sp, fontWeight = FontWeight.W700, letterSpacing = Tight),
    titleL = TextStyle(fontFamily = Asta, fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.W700, letterSpacing = Tight),
    titleM = TextStyle(fontFamily = Asta, fontSize = 36.sp, lineHeight = 46.sp, fontWeight = FontWeight.W600),
    titleS = TextStyle(fontFamily = Asta, fontSize = 32.sp, lineHeight = 42.sp, fontWeight = FontWeight.W600),
    bodyL = TextStyle(fontFamily = Asta, fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.W400),
    bodyM = TextStyle(fontFamily = Asta, fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.W400),
    bodyS = TextStyle(fontFamily = Asta, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.W400),
    label = TextStyle(fontFamily = Asta, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.W500),
    button = TextStyle(fontFamily = Asta, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.W600),
)

private val LocalNgColors = staticCompositionLocalOf { LightNg }
private val LocalNgType = staticCompositionLocalOf { Type }

object Ng {
    val c: NgColors @Composable get() = LocalNgColors.current
    val t: NgType @Composable get() = LocalNgType.current
}

@Composable
fun NaerilgotTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkNg else LightNg
    // Material 기본 컴포넌트(Checkbox, ProgressIndicator 등)도 같은 토큰을 쓰도록 맞춘다.
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onCta, background = c.bg, surface = c.surface,
        onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.sunken, outline = c.line, error = c.warn,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onCta, background = c.bg, surface = c.surface,
        onSurface = c.ink, onBackground = c.ink, surfaceVariant = c.sunken, outline = c.line, error = c.warn,
    )
    val m3 = Typography(
        headlineSmall = Type.headlineS, titleLarge = Type.titleL, titleMedium = Type.titleM, titleSmall = Type.titleS,
        bodyLarge = Type.bodyL, bodyMedium = Type.bodyM, bodySmall = Type.bodyS,
        labelLarge = Type.button, labelMedium = Type.label, labelSmall = Type.label,
    )
    CompositionLocalProvider(LocalNgColors provides c, LocalNgType provides Type) {
        MaterialTheme(colorScheme = scheme, typography = m3, content = content)
    }
}
