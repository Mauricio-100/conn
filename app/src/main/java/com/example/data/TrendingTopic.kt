package com.example.data

data class TrendingTopic(
    val id: String,
    val title: String,
    val source: String,
    val link: String,
    val pubDateFormatted: String,
    val category: TrendingCategory,
    val snippet: String? = null,
    val tags: List<String> = emptyList(),
    val isHot: Boolean = false
)

enum class TrendingCategory(val label: String, val emoji: String, val searchQuery: String) {
    ALL("Tous", "🔥", "Intelligence Artificielle OR Markdown OR tech OR programmation"),
    AI("IA & LLMs", "🤖", "Intelligence Artificielle OR LLM OR Gemini OR Claude OR ChatGPT"),
    MARKDOWN("Markdown & Docs", "📝", "Markdown OR documentation OR Obsidian OR Notion OR GitHub"),
    DEV("Dev & Outils", "⚡", "programmation OR open source OR developpeur OR Kotlin"),
    OPEN_SOURCE("Open Source", "🌐", "open source OR GitHub OR Linux OR logiciel libre")
}
