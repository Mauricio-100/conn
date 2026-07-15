package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme(val displayName: String) {
    DEFAULT("Défaut (Chat Rouge)"),
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
    val isDark: Boolean
)

val DefaultThemeColors = AppColorScheme(
    primary = Color(0xFFDC2626), // Red
    secondary = Color(0xFF991B1B),
    tertiary = Color(0xFFFCA5A5),
    background = Color(0xFF0F172A), // Dark blue background
    surface = Color(0xFF1E293B),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    isDark = true
)

val BlackThemeColors = AppColorScheme(
    primary = Color.White,
    secondary = Color.Gray,
    tertiary = Color.LightGray,
    background = Color.Black,
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    isDark = true
)

val WhiteThemeColors = AppColorScheme(
    primary = Color.Black,
    secondary = Color.Gray,
    tertiary = Color.DarkGray,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    isDark = false
)

val RedThemeColors = AppColorScheme(
    primary = Color(0xFFFF5252),
    secondary = Color(0xFFFF1744),
    tertiary = Color(0xFFD50000),
    background = Color(0xFFFFEBEE),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFB71C1C),
    onSurface = Color(0xFFB71C1C),
    isDark = false
)

val GreenThemeColors = AppColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF388E3C),
    tertiary = Color(0xFF1B5E20),
    background = Color(0xFFE8F5E9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20),
    isDark = false
)

// Thematic colors (using palettes inspired by the names)
val FlowersThemeColors = AppColorScheme(
    primary = Color(0xFFE91E63), // Pink
    secondary = Color(0xFF9C27B0), // Purple
    tertiary = Color(0xFFFFC107), // Yellow
    background = Color(0xFFFCE4EC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF880E4F),
    onSurface = Color(0xFF880E4F),
    isDark = false
)

val DogThemeColors = AppColorScheme(
    primary = Color(0xFF795548), // Brown
    secondary = Color(0xFF5D4037),
    tertiary = Color(0xFFA1887F),
    background = Color(0xFFEFEBE9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
    isDark = false
)

val CatThemeColors = AppColorScheme(
    primary = Color(0xFF607D8B), // Blue Gray
    secondary = Color(0xFF455A64),
    tertiary = Color(0xFF90A4AE),
    background = Color(0xFFECEFF1),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF263238),
    onSurface = Color(0xFF263238),
    isDark = false
)

val HousesThemeColors = AppColorScheme(
    primary = Color(0xFF03A9F4), // Light Blue
    secondary = Color(0xFF0288D1),
    tertiary = Color(0xFF01579B),
    background = Color(0xFFE1F5FE),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF01579B),
    onSurface = Color(0xFF01579B),
    isDark = false
)

val FoodThemeColors = AppColorScheme(
    primary = Color(0xFFFF9800), // Orange
    secondary = Color(0xFFF57C00),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFFFF3E0),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE65100),
    onSurface = Color(0xFFE65100),
    isDark = false
)

val DesertThemeColors = AppColorScheme(
    primary = Color(0xFFFFC107), // Amber
    secondary = Color(0xFFFFA000),
    tertiary = Color(0xFFFF6F00),
    background = Color(0xFFFFF8E1),
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF795548),
    onSurface = Color(0xFF795548),
    isDark = false
)

val MoonThemeColors = AppColorScheme(
    primary = Color(0xFF9FA8DA), // Indigo Light
    secondary = Color(0xFF7986CB),
    tertiary = Color(0xFFC5CAE9),
    background = Color(0xFF1A237E), // Deep Indigo
    surface = Color(0xFF283593),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    isDark = true
)

val SunThemeColors = AppColorScheme(
    primary = Color(0xFFFFEB3B), // Yellow
    secondary = Color(0xFFFBC02D),
    tertiary = Color(0xFFFFF176),
    background = Color(0xFFFFFDE7),
    surface = Color.White,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF57F17),
    onSurface = Color(0xFFF57F17),
    isDark = false
)

fun AppTheme.toColorScheme(): AppColorScheme {
    return when (this) {
        AppTheme.DEFAULT -> DefaultThemeColors
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
