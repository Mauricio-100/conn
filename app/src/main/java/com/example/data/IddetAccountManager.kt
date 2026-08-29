package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Dedicated System Singleton Manager for the Official IDDET Account.
 *
 * Responsibilities:
 * - Initializes and persists the official "IDDET" account with a verified yellow checkmark.
 * - Enforces read-only permissions for regular public users.
 * - Provides authorization checks allowing only designated admins (C.M.O, Crislem, Offranel, etc.)
 *   to post official messages and announcements on behalf of IDDET.
 * - Serves as the platform's official information hub.
 */
object IddetAccountManager {

    const val IDDET_USER_ID = "iddet-official-hub-id"
    const val IDDET_USERNAME = "IDDET"
    const val IDDET_DISPLAY_NAME = "IDDET"
    const val IDDET_AVATAR_URL = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80"
    const val IDDET_BIO = "🌟 Compte Officiel IDDET • Hub central d'informations, d'annonces et d'actualités certifiées de la plateforme STRIP."

    // Designated administrators authorized to manage and publish as IDDET
    val DESIGNATED_ADMINS = listOf(
        "C.M.O",
        "Crislem",
        "Offranel",
        "Doffranel",
        "doffranel",
        "Mauricio-100",
        "admin"
    )

    /**
     * Constructs the official IDDET User model.
     */
    fun getOfficialIddetUser(): User {
        return User(
            id = IDDET_USER_ID,
            username = IDDET_USERNAME,
            passwordHash = "iddet_system_secure_hash",
            avatarUrl = IDDET_AVATAR_URL,
            bio = IDDET_BIO,
            privacySetting = "Public",
            isVerified = true,
            followingCount = 1,
            followersCount = 125000,
            isGiant = true,
            level = 10,
            xp = 99999,
            badges = "IDDET,Official,Verified,Hub",
            email = "contact@iddet.system",
            preferredCategory = "Actu"
        )
    }

    /**
     * Checks if a username corresponds to the official IDDET account.
     */
    fun isOfficialIddetAccount(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        val clean = username.trim()
        return clean.equals("IDDET", ignoreCase = true) ||
               clean.equals("Iddet", ignoreCase = true) ||
               clean.equals("IDDET Official", ignoreCase = true)
    }

    /**
     * Checks if a user ID corresponds to the official IDDET account.
     */
    fun isOfficialIddetUserId(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        return userId == IDDET_USER_ID ||
               userId.equals("iddet-official-id", ignoreCase = true) ||
               userId.equals("IDDET", ignoreCase = true)
    }

    /**
     * Checks whether the given user is a designated administrator with official IDDET posting privileges.
     */
    fun isUserDesignatedAdmin(username: String?): Boolean {
        if (username.isNullOrBlank()) return false
        val clean = username.trim()
        return DESIGNATED_ADMINS.any { it.equals(clean, ignoreCase = true) }
    }

    /**
     * Determines whether the given user is authorized to publish or modify content under IDDET.
     */
    fun canPostAsIddet(user: User?): Boolean {
        if (user == null) return false
        return isUserDesignatedAdmin(user.username)
    }

    /**
     * Checks if the IDDET account is in read-only mode for the current user.
     * Public users cannot edit IDDET's profile or post under IDDET.
     */
    fun isReadOnlyForUser(currentUser: User?, targetUsername: String?): Boolean {
        if (!isOfficialIddetAccount(targetUsername)) return false
        return !canPostAsIddet(currentUser)
    }

    /**
     * Ensures that the IDDET official user and seed official hub actfiles exist in Room local database.
     */
    suspend fun ensureIddetAccountExists(userDao: UserDao, actfileDao: ActfileDao) = withContext(Dispatchers.IO) {
        try {
            // 1. Ensure User entity in Room
            val officialUser = getOfficialIddetUser()
            userDao.insertUser(officialUser)

            // Also insert alias lowercase if needed
            val legacyUser = officialUser.copy(id = "iddet-official-id", username = "Iddet")
            userDao.insertUser(legacyUser)

            // 2. Ensure initial official announcements from IDDET exist
            val officialPosts = listOf(
                Actfile(
                    id = "iddet-announcement-welcome-01",
                    userId = IDDET_USER_ID,
                    content = """# 🌟 Hub Officiel IDDET : Bienvenue !

Bienvenue sur le hub d'informations certifié **IDDET** de la plateforme STRIP.

### 📌 À quoi sert ce compte officiel ?
- 📢 **Annonces officielles** et nouveautés en temps réel
- ⚡ **Notes de version** et déploiements techniques
- 🛡️ **Sécurité, vérification** et directives de la communauté
- 💛 **Badge Jaune Certifié** garantissant l'authenticité de nos publications

> [!NOTE]
> Ce compte officiel est administré exclusivement par l'équipe officielle (*C.M.O, Crislem, Offranel*).

Suivez ce compte pour ne rien manquer des futures mises à jour ! ✨""",
                    tags = "iddet, officiel, annonce, hub, strip",
                    likesCount = 1420,
                    viewsCount = 18900,
                    commentsCount = 84,
                    createdAt = System.currentTimeMillis() - 86400000L * 2,
                    category = "Actu"
                ),
                Actfile(
                    id = "iddet-announcement-v7-update-02",
                    userId = IDDET_USER_ID,
                    content = """# 🚀 Mise à Jour STRIP & IDDET v7.0

Nous sommes ravis d'annoncer les dernières fonctionnalités disponibles :

### ✨ Nouveautés majeures :
1. 📝 **Actfiles Markdown enrichis** : Support complet du Markdown, aperçu temps réel et filtrage par catégories.
2. 🎵 **STRIP Sounds** : Studio audio collaboratif, remix et streaming de sons en haute qualité.
3. 💬 **Salons & Communautés** : Créez et rejoignez vos espaces de discussion favoris.
4. 🔐 **Système de Vérification** : Niveaux d'utilisateurs et badges de certification.

---
💬 *Vos retours sont précieux. Partagez vos impressions en commentaire !*""",
                    tags = "update, version7, markdown, sounds, strip",
                    likesCount = 980,
                    viewsCount = 14500,
                    commentsCount = 42,
                    createdAt = System.currentTimeMillis() - 86400000L,
                    category = "Tech"
                ),
                Actfile(
                    id = "iddet-announcement-safety-03",
                    userId = IDDET_USER_ID,
                    content = """# 🛡️ Guide de Certification & Sécurité

Pour maintenir un espace sain et authentique sur la plateforme, voici les critères pour obtenir votre badge vérifié :

### 🎯 Critères de vérification :
- ✅ Profil complet (Photo de profil, bio, coordonnées)
- 📹 Au moins 1 publication originale
- 👥 Engagement communautaire actif
- 🔞 Respect strict des conditions d'utilisation

> Le badge officiel vert est réservé aux administrateurs, le badge jaune au compte officiel **IDDET**, et le badge bleu à tous les profils vérifiés.""",
                    tags = "securite, verification, badge, officiel",
                    likesCount = 750,
                    viewsCount = 9600,
                    commentsCount = 28,
                    createdAt = System.currentTimeMillis() - 3600000L * 4,
                    category = "Business"
                )
            )

            for (post in officialPosts) {
                actfileDao.insertActfile(post)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
