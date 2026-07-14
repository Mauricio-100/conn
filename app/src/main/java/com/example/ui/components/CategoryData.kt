package com.example.ui.components

import androidx.compose.ui.graphics.Color

data class CategoryInfo(
    val id: String, // e.g. "@(fun)"
    val name: String, // e.g. "Fun & Humour"
    val emoji: String, // e.g. "🎭"
    val description: String,
    val color: Color
)

val APP_CATEGORIES = listOf(
    CategoryInfo("@(fun)", "Fun", "🎭", "Humour, mèmes, blagues et moments drôles.", Color(0xFFFBBF24)),
    CategoryInfo("@(gaming)", "Gaming", "🎮", "Jeux vidéo, gameplays, astuces et consoles.", Color(0xFF818CF8)),
    CategoryInfo("@(tech)", "Tech", "💻", "Nouvelles technologies, gadgets et innovations.", Color(0xFF34D399)),
    CategoryInfo("@(music)", "Musique", "🎵", "Partage de morceaux, chansons et instruments.", Color(0xFFF472B6)),
    CategoryInfo("@(movies)", "Cinéma", "🎬", "Films, séries, critiques et bandes-annonces.", Color(0xFFF87171)),
    CategoryInfo("@(science)", "Science", "🔬", "Découvertes, astronomie, physique et biologie.", Color(0xFF60A5FA)),
    CategoryInfo("@(art)", "Art", "🎨", "Dessin, peinture, design graphique et sculpture.", Color(0xFFF59E0B)),
    CategoryInfo("@(sports)", "Sports", "⚽", "Football, basketball, fitness et athlétisme.", Color(0xFF10B981)),
    CategoryInfo("@(news)", "Actu", "📰", "Actualités mondiales, informations et médias.", Color(0xFF6B7280)),
    CategoryInfo("@(crypto)", "Crypto", "🪙", "Bitcoin, Ethereum, web3 et blockchain.", Color(0xFFF59E0B)),
    CategoryInfo("@(anime)", "Anime", "💮", "Mangas, animés japonais et culture otaku.", Color(0xFFEC4899)),
    CategoryInfo("@(finance)", "Finance", "📈", "Économie, investissement et finances personnelles.", Color(0xFF059669)),
    CategoryInfo("@(food)", "Cuisine", "🍳", "Recettes, gastronomie, restaurants et gourmandises.", Color(0xFFF97316)),
    CategoryInfo("@(travel)", "Voyage", "✈️", "Destinations, aventures, paysages et conseils.", Color(0xFF06B6D4)),
    CategoryInfo("@(fashion)", "Mode", "👗", "Vêtements, tendances, styles et beauté.", Color(0xFFD946EF)),
    CategoryInfo("@(nature)", "Nature", "🌿", "Faune, flore, écologie et grands espaces.", Color(0xFF22C55E)),
    CategoryInfo("@(photography)", "Photo", "📷", "Appareils, clichés, cadrages et retouches.", Color(0xFF84CC16)),
    CategoryInfo("@(history)", "Histoire", "🏛️", "Faits historiques, archives et civilisations.", Color(0xFF78350F)),
    CategoryInfo("@(books)", "Livres", "📚", "Romans, littérature, poésie et bibliothèques.", Color(0xFF1E3A8A)),
    CategoryInfo("@(memes)", "Mèmes", "🤪", "Mèmes d'internet, culture web et délires.", Color(0xFFEAB308)),
    CategoryInfo("@(diy)", "Bricolage", "🛠️", "Do It Yourself, travaux manuels et artisanat.", Color(0xFFB45309)),
    CategoryInfo("@(fitness)", "Fitness", "💪", "Musculation, cardio, yoga et nutrition.", Color(0xFFEF4444)),
    CategoryInfo("@(pets)", "Animaux", "🐱", "Chats, chiens, astuces de soins et vidéos mignonnes.", Color(0xFFF59E0B)),
    CategoryInfo("@(education)", "Savoir", "🎓", "Cours, tutoriels, langues et apprentissage.", Color(0xFF3B82F6)),
    CategoryInfo("@(cars)", "Auto", "🚗", "Voitures, motos, mécanique et courses.", Color(0xFFEF4444)),
    CategoryInfo("@(design)", "Design", "📐", "UI/UX, architecture, modélisation et croquis.", Color(0xFF6366F1)),
    CategoryInfo("@(politics)", "Politique", "🗳️", "Débats, élections, géopolitique et opinions.", Color(0xFFEF4444)),
    CategoryInfo("@(health)", "Santé", "🏥", "Médecine, bien-être mental et physique.", Color(0xFF10B981)),
    CategoryInfo("@(career)", "Carrière", "💼", "Emploi, entrepreneuriat, CV et productivité.", Color(0xFF4B5563)),
    CategoryInfo("@(philosophy)", "Philosophie", "🤔", "Pensées, citations, réflexion et sagesse.", Color(0xFF8B5CF6)),
    CategoryInfo("@(coding)", "Code", "💻", "Programmation, Kotlin, Python et algorithmes.", Color(0xFF06B6D4)),
    CategoryInfo("@(relationships)", "Amour", "❤️", "Relations, amitié, conseils de vie et famille.", Color(0xFFEC4899)),
    CategoryInfo("@(business)", "Business", "🏢", "Stratégies d'entreprise, startups et marketing.", Color(0xFF0F172A)),
    CategoryInfo("@(marketing)", "Marketing", "📣", "Publicité, réseaux sociaux, growth hacking.", Color(0xFFF97316))
)

fun getCategoryById(id: String?): CategoryInfo? {
    if (id == null) return null
    return APP_CATEGORIES.firstOrNull { it.id.equals(id, ignoreCase = true) }
}
