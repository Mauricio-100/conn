package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme(val displayName: String) {
    SYSTEM("Système (Auto)"),
    DEFAULT("IDDET Moderne (Système)"),
    LIGHT("Mode Clair"),
    DARK("Mode Sombre"),
    BLACK("Noir"),
    WHITE("Blanc"),
    RED("Rouge"),
    GREEN("Vert"),
    FLOWERS("Fleurs"),
    DOG("Chien"),
    CAT("Chat"),
    HOUSES("Maisons"),
    FOOD("Nourriture"),
    DESERT("Désert"),
    MOON("Lune"),
    SUN("Soleil")
}

data class AppColorScheme(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onTertiary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val surfaceVariant: Color = ShadcnLightMuted,
    val onSurfaceVariant: Color = ShadcnLightMutedForeground,
    val outline: Color = ShadcnLightBorder,
    val outlineVariant: Color = ShadcnLightInput,
    val ring: Color = ShadcnLightRing,
    val error: Color = ShadcnLightDestructive,
    val onError: Color = ShadcnLightOnDestructive,
    val sidebarBackground: Color = ShadcnLightSidebarBackground,
    val sidebarForeground: Color = ShadcnLightSidebarForeground,
    val sidebarActive: Color = ShadcnLightSidebarActive,
    val isDark: Boolean
)

// === IDDET MODERN SHADCN PALETTES ===

// Mode Clair (Spécifications IDDET)
val ShadcnLightColorScheme = AppColorScheme(
    primary = ShadcnLightPrimary,             // #5548E5 (indigo-violet)
    secondary = ShadcnLightSecondary,         // #F5F5F5
    tertiary = DatavizColor2,                 // #2A9D8F
    background = ShadcnLightBackground,       // #FFFFFF
    surface = ShadcnLightBackground,          // #FFFFFF
    onPrimary = ShadcnLightOnPrimary,         // #FFFFFF
    onSecondary = ShadcnLightOnSecondary,     // #0A0A0A
    onTertiary = Color.White,
    onBackground = ShadcnLightForeground,     // #0A0A0A
    onSurface = ShadcnLightForeground,        // #0A0A0A
    surfaceVariant = ShadcnLightMuted,        // #F5F5F5
    onSurfaceVariant = ShadcnLightMutedForeground, // #737373
    outline = ShadcnLightBorder,              // #E5E5E5
    outlineVariant = ShadcnLightInput,        // #E5E5E5
    ring = ShadcnLightRing,                   // #5548E5
    error = ShadcnLightDestructive,           // #EF4444
    onError = ShadcnLightOnDestructive,       // #FFFFFF
    sidebarBackground = ShadcnLightSidebarBackground, // #FAFAFA
    sidebarForeground = ShadcnLightSidebarForeground, // #3F3F46
    sidebarActive = ShadcnLightSidebarActive,         // #18181B
    isDark = false
)

// Mode Sombre (Spécifications IDDET)
val ShadcnDarkColorScheme = AppColorScheme(
    primary = ShadcnDarkPrimary,              // #FAFAFA (blanc)
    secondary = ShadcnDarkSecondary,          // #262626
    tertiary = DatavizColor4,                 // #E8C468
    background = ShadcnDarkBackground,        // #0A0A0A
    surface = ShadcnDarkBackground,           // #0A0A0A
    onPrimary = ShadcnDarkOnPrimary,          // #171717
    onSecondary = ShadcnDarkOnSecondary,      // #FAFAFA
    onTertiary = Color.Black,
    onBackground = ShadcnDarkForeground,      // #FAFAFA
    onSurface = ShadcnDarkForeground,         // #FAFAFA
    surfaceVariant = ShadcnDarkMuted,         // #262626
    onSurfaceVariant = ShadcnDarkMutedForeground, // #A3A3A3
    outline = ShadcnDarkBorder,               // #262626
    outlineVariant = ShadcnDarkInput,         // #262626
    ring = ShadcnDarkRing,                    // #5548E5
    error = ShadcnDarkDestructive,            // #7F1D1D
    onError = ShadcnDarkOnDestructive,        // #FAFAFA
    sidebarBackground = ShadcnDarkSidebarBackground, // #18181B
    sidebarForeground = ShadcnDarkSidebarForeground, // #F4F4F5
    sidebarActive = ShadcnDarkSidebarActive,         // #1D4ED8
    isDark = true
)

// Black Oled Theme
val BlackThemeColors = AppColorScheme(
    primary = Color(0xFFFAFAFA),
    secondary = Color(0xFF262626),
    tertiary = Color(0xFF52525B),
    background = Color(0xFF000000),
    surface = Color(0xFF09090B),
    onPrimary = Color(0xFF18181B),
    onSecondary = Color(0xFFFAFAFA),
    onTertiary = Color.Black,
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A),
    outlineVariant = Color(0xFF27272A),
    ring = Color(0xFF5548E5),
    error = Color(0xFF7F1D1D),
    onError = Color.White,
    sidebarBackground = Color(0xFF09090B),
    sidebarForeground = Color(0xFFF4F4F5),
    sidebarActive = Color(0xFF1D4ED8),
    isDark = true
)

// White Pure Theme
val WhiteThemeColors = AppColorScheme(
    primary = Color(0xFF5548E5),
    secondary = Color(0xFFF4F4F5),
    tertiary = Color(0xFF3F3F46),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color(0xFF18181B),
    onTertiary = Color.White,
    onBackground = Color(0xFF09090B),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFE4E4E7),
    ring = Color(0xFF5548E5),
    error = Color(0xFFEF4444),
    onError = Color.White,
    sidebarBackground = Color(0xFFFAFAFA),
    sidebarForeground = Color(0xFF3F3F46),
    sidebarActive = Color(0xFF18181B),
    isDark = false
)

// Legacy / Colored themes updated with modern 8px shadcn principles
val RedThemeColors = AppColorScheme(
    primary = Color(0xFFE11D48),
    secondary = Color(0xFFFEE2E2),
    tertiary = Color(0xFF9F1239),
    background = Color(0xFFFFF1F2),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF9F1239),
    onTertiary = Color.White,
    onBackground = Color(0xFF881337),
    onSurface = Color(0xFF881337),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFFBE123C),
    outline = Color(0xFFFECDD3),
    outlineVariant = Color(0xFFFECDD3),
    ring = Color(0xFFE11D48),
    error = Color(0xFFEF4444),
    onError = Color.White,
    sidebarBackground = Color(0xFFFFF1F2),
    sidebarForeground = Color(0xFF881337),
    sidebarActive = Color(0xFFE11D48),
    isDark = false
)

val GreenThemeColors = AppColorScheme(
    primary = Color(0xFF10B981),
    secondary = Color(0xFFD1FAE5),
    tertiary = Color(0xFF047857),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF047857),
    onTertiary = Color.White,
    onBackground = Color(0xFF064E3B),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF059669),
    outline = Color(0xFFA7F3D0),
    outlineVariant = Color(0xFFA7F3D0),
    ring = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    onError = Color.White,
    sidebarBackground = Color(0xFFF0FDF4),
    sidebarForeground = Color(0xFF064E3B),
    sidebarActive = Color(0xFF10B981),
    isDark = false
)

val FlowersThemeColors = AppColorScheme(
    primary = Color(0xFFDB2777),
    secondary = Color(0xFFFCE7F3),
    tertiary = Color(0xFF9D174D),
    background = Color(0xFFFDF2F8),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF9D174D),
    onTertiary = Color.Black,
    onBackground = Color(0xFF831843),
    onSurface = Color(0xFF831843),
    surfaceVariant = Color(0xFFFCE7F3),
    onSurfaceVariant = Color(0xFFBE185D),
    outline = Color(0xFFFBCFE8),
    outlineVariant = Color(0xFFFBCFE8),
    ring = Color(0xFFDB2777),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val DogThemeColors = AppColorScheme(
    primary = Color(0xFFB45309),
    secondary = Color(0xFFFEF3C7),
    tertiary = Color(0xFF78350F),
    background = Color(0xFFFFFBEB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF78350F),
    onTertiary = Color.Black,
    onBackground = Color(0xFF451A03),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFF92400E),
    outline = Color(0xFFFDE68A),
    outlineVariant = Color(0xFFFDE68A),
    ring = Color(0xFFB45309),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val CatThemeColors = AppColorScheme(
    primary = Color(0xFF64748B),
    secondary = Color(0xFFF1F5F9),
    tertiary = Color(0xFF334155),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF334155),
    onTertiary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFE2E8F0),
    ring = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val HousesThemeColors = AppColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFFE0F2FE),
    tertiary = Color(0xFF0369A1),
    background = Color(0xFFF0F9FF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF0369A1),
    onTertiary = Color.White,
    onBackground = Color(0xFF082F49),
    onSurface = Color(0xFF082F49),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0284C7),
    outline = Color(0xFFBAE6FD),
    outlineVariant = Color(0xFFBAE6FD),
    ring = Color(0xFF0284C7),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val FoodThemeColors = AppColorScheme(
    primary = Color(0xFFEA580C),
    secondary = Color(0xFFFFEDD5),
    tertiary = Color(0xFF9A3412),
    background = Color(0xFFFFF7ED),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF9A3412),
    onTertiary = Color.White,
    onBackground = Color(0xFF431407),
    onSurface = Color(0xFF431407),
    surfaceVariant = Color(0xFFFFEDD5),
    onSurfaceVariant = Color(0xFFC2410C),
    outline = Color(0xFFFED7AA),
    outlineVariant = Color(0xFFFED7AA),
    ring = Color(0xFFEA580C),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val DesertThemeColors = AppColorScheme(
    primary = Color(0xFFD97706),
    secondary = Color(0xFFFEF3C7),
    tertiary = Color(0xFF92400E),
    background = Color(0xFFFFFBEB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF92400E),
    onTertiary = Color.White,
    onBackground = Color(0xFF451A03),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFFB45309),
    outline = Color(0xFFFDE68A),
    outlineVariant = Color(0xFFFDE68A),
    ring = Color(0xFFD97706),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

val MoonThemeColors = AppColorScheme(
    primary = Color(0xFF818CF8),
    secondary = Color(0xFF312E81),
    tertiary = Color(0xFFA5B4FC),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF334155),
    ring = Color(0xFF818CF8),
    error = Color(0xFFEF4444),
    onError = Color.White,
    sidebarBackground = Color(0xFF0F172A),
    sidebarForeground = Color(0xFFF8FAFC),
    sidebarActive = Color(0xFF818CF8),
    isDark = true
)

val SunThemeColors = AppColorScheme(
    primary = Color(0xFFCA8A04),
    secondary = Color(0xFFFEF9C3),
    tertiary = Color(0xFF854D0E),
    background = Color(0xFFFEFCE8),
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color(0xFF854D0E),
    onTertiary = Color.Black,
    onBackground = Color(0xFF422006),
    onSurface = Color(0xFF422006),
    surfaceVariant = Color(0xFFFEF9C3),
    onSurfaceVariant = Color(0xFFA16207),
    outline = Color(0xFFFEF08A),
    outlineVariant = Color(0xFFFEF08A),
    ring = Color(0xFFCA8A04),
    error = Color(0xFFEF4444),
    onError = Color.White,
    isDark = false
)

fun AppTheme.toColorScheme(isSystemDark: Boolean = false): AppColorScheme {
    return when (this) {
        AppTheme.SYSTEM, AppTheme.DEFAULT -> if (isSystemDark) ShadcnDarkColorScheme else ShadcnLightColorScheme
        AppTheme.LIGHT -> ShadcnLightColorScheme
        AppTheme.DARK -> ShadcnDarkColorScheme
        AppTheme.BLACK -> BlackThemeColors
        AppTheme.WHITE -> WhiteThemeColors
        AppTheme.RED -> RedThemeColors
        AppTheme.GREEN -> GreenThemeColors
        AppTheme.FLOWERS -> FlowersThemeColors
        AppTheme.DOG -> DogThemeColors
        AppTheme.CAT -> CatThemeColors
        AppTheme.HOUSES -> HousesThemeColors
        AppTheme.FOOD -> FoodThemeColors
        AppTheme.DESERT -> DesertThemeColors
        AppTheme.MOON -> MoonThemeColors
        AppTheme.SUN -> SunThemeColors
    }
}
