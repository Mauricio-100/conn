package com.example.data

import android.text.Html
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TrendingTopicsService {
    private const val TAG = "TrendingTopicsService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Curated high quality fallbacks tailored for Markdown, AI & Tech
    private val defaultCuratedTopics = listOf(
        TrendingTopic(
            id = "trend_1",
            title = "L'essor des éditeurs Markdown assistés par l'IA générative dans les flux de travail des développeurs",
            source = "Google Actualités • Tech & IA",
            link = "https://news.google.com/search?q=Markdown+IA+developpeurs",
            pubDateFormatted = "Il y a 35 min",
            category = TrendingCategory.MARKDOWN,
            snippet = "Comment les modèles d'IA réinventent la prise de notes structurée, la rédaction technique et la documentation collaborative en syntaxe Markdown.",
            tags = listOf("#Markdown", "#IA", "#Docs", "#Dev"),
            isHot = true
        ),
        TrendingTopic(
            id = "trend_2",
            title = "Google annonce de nouvelles avancées pour Gemini et l'intégration du format Markdown dans les réponses multimodales",
            source = "Google Blog & News",
            link = "https://news.google.com/search?q=Google+Gemini+AI",
            pubDateFormatted = "Il y a 1 h",
            category = TrendingCategory.AI,
            snippet = "Gemini enrichit la structuration des données et l'exportation directe en Markdown pour les notebooks et environnements de développement.",
            tags = listOf("#Gemini", "#Google", "#LLM", "#IA"),
            isHot = true
        ),
        TrendingTopic(
            id = "trend_3",
            title = "Open Source : GitHub renforce ses outils de documentation Markdown et de rendu interactif",
            source = "GitHub & Open Source News",
            link = "https://news.google.com/search?q=GitHub+Markdown+Open+Source",
            pubDateFormatted = "Il y a 2 h",
            category = TrendingCategory.OPEN_SOURCE,
            snippet = "Nouvelles fonctionnalités pour les README, diagrammes Mermaid et blocs de code avec coloration syntaxique instantanée.",
            tags = listOf("#GitHub", "#OpenSource", "#Mermaid", "#Markdown"),
            isHot = false
        ),
        TrendingTopic(
            id = "trend_4",
            title = "Les agents d'intelligence artificielle locale révolutionnent la gestion des connaissances personnelles en Markdown",
            source = "TechCrunch & Le Monde Informatique",
            link = "https://news.google.com/search?q=Local+LLM+Markdown+Notes",
            pubDateFormatted = "Il y a 3 h",
            category = TrendingCategory.AI,
            snippet = "L'exécution locale de modèles légers permet d'indexer et d'interroger des bibliothèques entières de fichiers .md en toute confidentialité.",
            tags = listOf("#LocalAI", "#Obsidian", "#Privacy", "#Tech"),
            isHot = false
        ),
        TrendingTopic(
            id = "trend_5",
            title = "Kotlin & Jetpack Compose : Les architectures réactives modernes adoptées par les géants de la Tech",
            source = "Android Developers & Google",
            link = "https://news.google.com/search?q=Kotlin+Jetpack+Compose+Android",
            pubDateFormatted = "Il y a 4 h",
            category = TrendingCategory.DEV,
            snippet = "Pourquoi Jetpack Compose et les composants déclaratifs deviennent la norme pour concevoir des interfaces ultra-rapides et accessibles.",
            tags = listOf("#Kotlin", "#JetpackCompose", "#Dev", "#Android"),
            isHot = false
        ),
        TrendingTopic(
            id = "trend_6",
            title = "LLMs et génération de code : Pourquoi la standardisation en Markdown évite les erreurs de parsing",
            source = "Hacker News & Google Search",
            link = "https://news.google.com/search?q=LLM+Code+Generation+Markdown",
            pubDateFormatted = "Il y a 5 h",
            category = TrendingCategory.AI,
            snippet = "Les balises de code Markdown permettent une séparation stricte entre explications et code exécutable dans les interfaces de chat IA.",
            tags = listOf("#PromptEngineering", "#LLM", "#DevTools"),
            isHot = false
        )
    )

    suspend fun fetchTrendingTopics(category: TrendingCategory = TrendingCategory.ALL): List<TrendingTopic> = withContext(Dispatchers.IO) {
        try {
            val queryParam = when (category) {
                TrendingCategory.ALL -> "Intelligence Artificielle OR Markdown OR \"tech\" OR programmation"
                TrendingCategory.AI -> "Intelligence Artificielle OR LLM OR Gemini OR Claude OR ChatGPT"
                TrendingCategory.MARKDOWN -> "Markdown OR documentation OR Obsidian OR Notion OR GitHub"
                TrendingCategory.DEV -> "programmation OR \"open source\" OR developpeur OR Kotlin"
                TrendingCategory.OPEN_SOURCE -> "\"open source\" OR GitHub OR Linux OR \"logiciel libre\""
            }

            val encodedQuery = URLEncoder.encode(queryParam, "UTF-8")
            val url = "https://news.google.com/rss/search?q=$encodedQuery&hl=fr&gl=FR&ceid=FR:fr"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Google News RSS response failed: code ${response.code}")
                return@withContext filterCurated(category)
            }

            val xmlBody = response.body?.string()
            if (xmlBody.isNullOrBlank()) {
                return@withContext filterCurated(category)
            }

            val parsedList = parseGoogleNewsRss(xmlBody, category)
            if (parsedList.isNotEmpty()) {
                return@withContext parsedList
            } else {
                return@withContext filterCurated(category)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching trending topics: ${e.message}")
            return@withContext filterCurated(category)
        }
    }

    private fun filterCurated(category: TrendingCategory): List<TrendingTopic> {
        return if (category == TrendingCategory.ALL) {
            defaultCuratedTopics
        } else {
            defaultCuratedTopics.filter { it.category == category }.ifEmpty { defaultCuratedTopics }
        }
    }

    private fun parseGoogleNewsRss(xml: String, category: TrendingCategory): List<TrendingTopic> {
        val topics = mutableListOf<TrendingTopic>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var currentTitle: String? = null
            var currentLink: String? = null
            var currentPubDate: String? = null
            var currentSource: String? = null
            var currentDescription: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            inItem = true
                            currentTitle = null
                            currentLink = null
                            currentPubDate = null
                            currentSource = null
                            currentDescription = null
                        } else if (inItem) {
                            when (tagName.lowercase(Locale.ROOT)) {
                                "title" -> currentTitle = parser.nextText()
                                "link" -> currentLink = parser.nextText()
                                "pubdate" -> currentPubDate = parser.nextText()
                                "source" -> currentSource = parser.nextText()
                                "description" -> currentDescription = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) && inItem) {
                            inItem = false
                            if (!currentTitle.isNullOrBlank()) {
                                val (cleanTitle, extractedSource) = sanitizeTitleAndSource(currentTitle, currentSource)
                                val cleanSnippet = sanitizeSnippet(currentDescription)
                                val relativeDate = formatPubDate(currentPubDate)
                                val tags = generateTagsForTitle(cleanTitle, category)

                                val itemCategory = when {
                                    cleanTitle.contains("markdown", ignoreCase = true) || cleanTitle.contains("obsidian", ignoreCase = true) || cleanTitle.contains("notion", ignoreCase = true) -> TrendingCategory.MARKDOWN
                                    cleanTitle.contains("ia", ignoreCase = true) || cleanTitle.contains("gemini", ignoreCase = true) || cleanTitle.contains("gpt", ignoreCase = true) || cleanTitle.contains("llm", ignoreCase = true) || cleanTitle.contains("intelligence", ignoreCase = true) -> TrendingCategory.AI
                                    cleanTitle.contains("open source", ignoreCase = true) || cleanTitle.contains("github", ignoreCase = true) || cleanTitle.contains("linux", ignoreCase = true) -> TrendingCategory.OPEN_SOURCE
                                    else -> if (category != TrendingCategory.ALL) category else TrendingCategory.DEV
                                }

                                topics.add(
                                    TrendingTopic(
                                        id = "goog_${System.currentTimeMillis()}_${topics.size}",
                                        title = cleanTitle,
                                        source = extractedSource,
                                        link = currentLink ?: "https://news.google.com",
                                        pubDateFormatted = relativeDate,
                                        category = itemCategory,
                                        snippet = cleanSnippet,
                                        tags = tags,
                                        isHot = topics.isEmpty() || topics.size == 1
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parsing error: ${e.message}")
        }
        return topics.take(12)
    }

    private fun sanitizeTitleAndSource(rawTitle: String, sourceTag: String?): Pair<String, String> {
        val decoded = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        val lastDash = decoded.lastIndexOf(" - ")
        if (lastDash > 0) {
            val titlePart = decoded.substring(0, lastDash).trim()
            val sourcePart = decoded.substring(lastDash + 3).trim()
            val sourceFinal = if (!sourceTag.isNullOrBlank()) sourceTag else sourcePart
            return Pair(titlePart, "Google Actualités • $sourceFinal")
        }
        val sourceFinal = if (!sourceTag.isNullOrBlank()) sourceTag else "Google Recherche & News"
        return Pair(decoded, "Google Actualités • $sourceFinal")
    }

    private fun sanitizeSnippet(rawSnippet: String?): String? {
        if (rawSnippet.isNullOrBlank()) return null
        val decoded = Html.fromHtml(rawSnippet, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        val cleaned = decoded.replace(Regex("<.*?>"), "").replace("\n", " ").trim()
        return if (cleaned.length > 180) cleaned.substring(0, 180) + "..." else cleaned
    }

    private fun formatPubDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "Récemment"
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
            val date = format.parse(rawDate) ?: return "Récemment"
            val diffMs = System.currentTimeMillis() - date.time
            val diffMin = diffMs / (1000 * 60)
            val diffHours = diffMin / 60
            val diffDays = diffHours / 24

            when {
                diffMin < 2 -> "À l'instant"
                diffMin < 60 -> "Il y a ${diffMin} min"
                diffHours < 24 -> "Il y a ${diffHours} h"
                diffDays == 1L -> "Hier"
                else -> "Il y a ${diffDays} j"
            }
        } catch (e: Exception) {
            "Récemment"
        }
    }

    private fun generateTagsForTitle(title: String, category: TrendingCategory): List<String> {
        val tags = mutableListOf<String>()
        val lower = title.lowercase(Locale.ROOT)
        
        if (lower.contains("markdown") || lower.contains(".md")) tags.add("#Markdown")
        if (lower.contains("gemini") || lower.contains("google")) tags.add("#Google")
        if (lower.contains("ia") || lower.contains("intelligence") || lower.contains("ai")) tags.add("#IA")
        if (lower.contains("llm") || lower.contains("gpt") || lower.contains("claude")) tags.add("#LLM")
        if (lower.contains("github")) tags.add("#GitHub")
        if (lower.contains("open source")) tags.add("#OpenSource")
        if (lower.contains("kotlin") || lower.contains("android")) tags.add("#Kotlin")
        if (lower.contains("code") || lower.contains("dev") || lower.contains("developp")) tags.add("#Dev")

        if (tags.isEmpty()) {
            when (category) {
                TrendingCategory.AI -> tags.addAll(listOf("#IA", "#Tech"))
                TrendingCategory.MARKDOWN -> tags.addAll(listOf("#Markdown", "#Docs"))
                TrendingCategory.DEV -> tags.addAll(listOf("#Dev", "#Tech"))
                TrendingCategory.OPEN_SOURCE -> tags.addAll(listOf("#OpenSource", "#Dev"))
                TrendingCategory.ALL -> tags.addAll(listOf("#Tech", "#Tendance"))
            }
        }
        return tags.distinct().take(4)
    }
}
