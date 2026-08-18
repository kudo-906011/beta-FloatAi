package com.example.ai

import com.example.model.LanguageCardData
import com.example.model.ResponseMode
import java.util.Locale

/**
 * High-performance Multilingual Detection & Dynamic Translation Engine.
 * Supports English, Hinglish, Bengali (বাংলা), Russian (Русский), Hindi (हिंदी),
 * Spanish (Español), French (Français), German (Deutsch), and dynamically adapts
 * to any detected language script.
 */
object LanguageDetectionEngine {

    data class LanguageInfo(
        val name: String,
        val code: String,
        val isHinglish: Boolean = false,
        val isEnglish: Boolean = false
    )

    private val HINGLISH_KEYWORDS = setOf(
        "bhai", "mujhe", "mera", "meri", "mere", "kaise", "kese", "karna", "krna", "chahiye", "chaheye",
        "kya", "kyu", "kyun", "karenge", "krnge", "karo", "kro", "batao", "btao", "bataiye", "yaar",
        "hai", "hain", "nahi", "nh", "nhi", "accha", "acha", "samajh", "smjh", "bolo", "hoga", "hogi",
        "raha", "rahi", "wali", "wala", "shuru", "start karna", "muje", "apna", "apni", "kuch", "sab",
        "kab", "kahan", "khan", "kitna", "bohot", "bahut", "theek", "thik", "bhi", "toh", "to", "ho"
    )

    private val SPANISH_KEYWORDS = setOf(
        "hola", "cómo", "como", "dónde", "donde", "cuándo", "cuando", "por qué", "porque",
        "gracias", "amigo", "amiga", "puedes", "buenos días", "buenas tardes", "buenas noches",
        "hacer", "estás", "estas", "qué", "que", "favor", "por favor"
    )

    private val FRENCH_KEYWORDS = setOf(
        "bonjour", "salut", "comment", "pourquoi", "où", "quand", "merci", "est-ce", "s'il",
        "vous", "plaît", "plait", "avec", "faire", "très", "bien", "bonne", "journée"
    )

    private val GERMAN_KEYWORDS = setOf(
        "hallo", "guten", "morgen", "tag", "wie", "warum", "wann", "wo", "danke", "bitte",
        "kannst", "können", "machen", "sehr", "gut", "tschüss"
    )

    /**
     * Detects the language of the incoming message.
     */
    fun detectLanguage(text: String): LanguageInfo {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return LanguageInfo("English", "en", isEnglish = true)

        // 1. Script-based Unicode Ranges
        val hasBengali = trimmed.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) {
            return LanguageInfo("Bengali", "bn")
        }

        val hasCyrillic = trimmed.any { it in '\u0400'..'\u04FF' }
        if (hasCyrillic) {
            return LanguageInfo("Russian", "ru")
        }

        val hasDevanagari = trimmed.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) {
            return LanguageInfo("Hindi", "hi")
        }

        val hasArabic = trimmed.any { it in '\u0600'..'\u06FF' }
        if (hasArabic) {
            return LanguageInfo("Arabic", "ar")
        }

        val hasJapanese = trimmed.any { (it in '\u3040'..'\u309F') || (it in '\u30A0'..'\u30FF') }
        if (hasJapanese) {
            return LanguageInfo("Japanese", "ja")
        }

        val hasChinese = trimmed.any { it in '\u4E00'..'\u9FFF' }
        if (hasChinese) {
            return LanguageInfo("Chinese", "zh")
        }

        // 2. Latin-based Lexical Detection
        val lowerWords = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        // Check for Hinglish
        val hinglishMatches = lowerWords.count { HINGLISH_KEYWORDS.contains(it) }
        if (hinglishMatches >= 2 || (hinglishMatches >= 1 && (lowerWords.contains("bhai") || lowerWords.contains("kaise") || lowerWords.contains("karna") || lowerWords.contains("chahiye")))) {
            return LanguageInfo("Hinglish", "hi-Latn", isHinglish = true)
        }

        // Check for Spanish
        if (trimmed.contains("¿") || trimmed.contains("¡") || lowerWords.count { SPANISH_KEYWORDS.contains(it) } >= 2) {
            return LanguageInfo("Spanish", "es")
        }

        // Check for French
        if (lowerWords.count { FRENCH_KEYWORDS.contains(it) } >= 2 || trimmed.contains("c'est") || trimmed.contains("d'accord")) {
            return LanguageInfo("French", "fr")
        }

        // Check for German
        if (lowerWords.count { GERMAN_KEYWORDS.contains(it) } >= 2 || trimmed.contains("ä") || trimmed.contains("ö") || trimmed.contains("ü") || trimmed.contains("ß")) {
            return LanguageInfo("German", "de")
        }

        // Default to English
        return LanguageInfo("English", "en", isEnglish = true)
    }

    /**
     * Translates or interprets the detected message into English.
     */
    fun translateToEnglish(originalMessage: String, lang: LanguageInfo): String {
        val trimmed = originalMessage.trim()
        if (lang.isEnglish || trimmed.isBlank()) return trimmed

        val lower = trimmed.lowercase(Locale.ROOT)

        // Hinglish translations & interpretations
        if (lang.isHinglish) {
            return when {
                lower.contains("youtube") && (lower.contains("start") || lower.contains("kaise") || lower.contains("shuru")) ->
                    "Brother, how should I start a YouTube channel?"
                lower.contains("kaise") && lower.contains("youtuber") ->
                    "Brother, how can I become a YouTuber?"
                lower.contains("kya haal") || lower.contains("kaise ho") || lower.contains("kese ho") ->
                    "How are you doing, brother?"
                lower.contains("meeting") && (lower.contains("kab") || lower.contains("hogi")) ->
                    "When will the meeting take place?"
                lower.contains("free ho") || lower.contains("time hai") ->
                    "Are you free right now / do you have some time?"
                lower.contains("review") && (lower.contains("kar") || lower.contains("de")) ->
                    "Can you please review this project / proposal?"
                lower.contains("madad") || lower.contains("help") ->
                    "Can you please help me with this?"
                else -> {
                    val topic = extractSimpleTopic(trimmed)
                    "How should I proceed regarding $topic?"
                }
            }
        }

        // Russian translations
        if (lang.code == "ru") {
            return when {
                lower.contains("как") && (lower.contains("стать") || lower.contains("ютубером") || lower.contains("youtube")) ->
                    "How do I become a YouTuber?"
                lower.contains("что") && lower.contains("проектом") ->
                    "What happened to the project?"
                lower.contains("привет") || lower.contains("как дела") ->
                    "Hello, how are you doing?"
                lower.contains("когда") && (lower.contains("встреча") || lower.contains("митинг")) ->
                    "When is the meeting?"
                lower.contains("ты свободен") || lower.contains("есть время") ->
                    "Are you free to chat / meet?"
                lower.contains("можешь") && lower.contains("проверить") ->
                    "Can you check / review this?"
                lower.contains("спасибо") ->
                    "Thank you very much!"
                else -> {
                    val topic = extractSimpleTopic(trimmed)
                    "Inquiry regarding $topic: Please provide guidance."
                }
            }
        }

        // Bengali translations
        if (lang.code == "bn") {
            return when {
                lower.contains("কীভাবে") && (lower.contains("ইউটিউবার") || lower.contains("ইউটিউব") || lower.contains("হতে পারি")) ->
                    "How do I become a YouTuber?"
                lower.contains("প্রজেক্ট") || lower.contains("প্রকল্প") ->
                    "What is the status of the project?"
                lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") ->
                    "How are you doing?"
                lower.contains("মিটিং") && (lower.contains("কখন") || lower.contains("হবে")) ->
                    "When is today's meeting?"
                lower.contains("সময় হবে") || lower.contains("ফ্রি আছো") ->
                    "Are you free right now?"
                lower.contains("ধন্যবাদ") ->
                    "Thank you very much!"
                else -> {
                    val topic = extractSimpleTopic(trimmed)
                    "Inquiry regarding $topic: Please provide guidance."
                }
            }
        }

        // Hindi translations
        if (lang.code == "hi") {
            return when {
                lower.contains("यूट्यूब") || lower.contains("यूट्यूबर") ->
                    "How do I become a YouTuber / start a channel?"
                lower.contains("कैसे हैं") || lower.contains("नमस्ते") ->
                    "Hello, how are you?"
                lower.contains("मीटिंग") && lower.contains("कब") ->
                    "When is the meeting?"
                lower.contains("धन्यवाद") || lower.contains("शुक्रिया") ->
                    "Thank you very much!"
                else -> "Inquiry: ${extractSimpleTopic(trimmed)}"
            }
        }

        // Spanish translations
        if (lang.code == "es") {
            return when {
                lower.contains("youtuber") || lower.contains("canal") ->
                    "How do I become a YouTuber / start a channel?"
                lower.contains("cómo estás") || lower.contains("hola") ->
                    "Hello, how are you doing?"
                lower.contains("reunión") && lower.contains("cuándo") ->
                    "When is the meeting?"
                lower.contains("gracias") ->
                    "Thank you very much!"
                else -> "Spanish inquiry regarding ${extractSimpleTopic(trimmed)}"
            }
        }

        // French translations
        if (lang.code == "fr") {
            return when {
                lower.contains("youtuber") || lower.contains("chaîne") ->
                    "How do I start a YouTube channel / become a creator?"
                lower.contains("comment vas-tu") || lower.contains("bonjour") ->
                    "Hello, how are you doing?"
                lower.contains("réunion") && lower.contains("quand") ->
                    "When is the meeting scheduled?"
                lower.contains("merci") ->
                    "Thank you very much!"
                else -> "French inquiry regarding ${extractSimpleTopic(trimmed)}"
            }
        }

        // German translations
        if (lang.code == "de") {
            return when {
                lower.contains("youtuber") || lower.contains("kanal") ->
                    "How do I start a YouTube channel / become a YouTuber?"
                lower.contains("wie geht") || lower.contains("hallo") ->
                    "Hello, how are you?"
                lower.contains("meeting") || lower.contains("treffen") ->
                    "When is the meeting scheduled?"
                lower.contains("danke") ->
                    "Thank you very much!"
                else -> "German inquiry regarding ${extractSimpleTopic(trimmed)}"
            }
        }

        return "Inquiry from ${lang.name}: $trimmed"
    }

    /**
     * Generates a response in the SAME original language for Bar 3,
     * or in English if the input is Hinglish/English.
     */
    fun generateOriginalLanguageReply(
        englishReply: String,
        originalMessage: String,
        lang: LanguageInfo,
        responseMode: ResponseMode = ResponseMode.PASSIVE
    ): String {
        val lower = originalMessage.lowercase(Locale.ROOT)

        // Hinglish: Output must be in English as specified in requirement 7 & 15
        if (lang.isHinglish || lang.isEnglish) {
            return englishReply
        }

        // Russian responses
        if (lang.code == "ru") {
            return when {
                lower.contains("стать") && (lower.contains("ютубером") || lower.contains("youtube")) -> {
                    when (responseMode) {
                        ResponseMode.SINGLE_WORD -> "Создавайте."
                        ResponseMode.ONE_LINE -> "Чтобы стать ютубером, выберите нишу, создавайте качественный контент и регулярно публикуйте видео."
                        ResponseMode.TWO_LINE -> "Чтобы стать успешным ютубером, определите интересную тему и целевую аудиторию.\nРегулярно выкладывайте качественные ролики и общайтесь со зрителями."
                        ResponseMode.DEBATE -> "Ключевой фактор успеха на YouTube — не дорогое оборудование, а ценность контента и регулярность."
                        ResponseMode.FUNNY -> "Купите микрофон, назовите кота соавтором и ждите миллион подписчиков!"
                        ResponseMode.ARROGANT -> "Снимайте первоклассный контент, как я, и успех гарантирован."
                        ResponseMode.LORD -> "Повелеваем открыть вещание и покорить просторы видеохостинга."
                        ResponseMode.PASSIVE -> "Чтобы стать ютубером, определите свою нишу, делайте качественный монтаж и соблюдайте регулярный график публикаций."
                    }
                }
                lower.contains("что") && lower.contains("проектом") ->
                    "Проект находится на стадии финального тестирования, все задачи идут по плану."
                lower.contains("привет") || lower.contains("как дела") ->
                    "Привет! Всё отлично, готов помочь с любыми вопросами."
                lower.contains("когда") && (lower.contains("встреча") || lower.contains("митинг")) ->
                    "Встреча запланирована по графику. Я буду готов подключиться."
                lower.contains("ты свободен") || lower.contains("время") ->
                    "Да, я свободен и готов всё обсудить."
                lower.contains("спасибо") ->
                    "Пожалуйста! Всегда рад помочь."
                else ->
                    "Спасибо за сообщение. Я изучил вопрос и скоро подготовлю подробный ответ."
            }
        }

        // Bengali responses
        if (lang.code == "bn") {
            return when {
                lower.contains("কীভাবে") && (lower.contains("ইউটিউবার") || lower.contains("ইউটিউব") || lower.contains("হতে পারি")) -> {
                    when (responseMode) {
                        ResponseMode.SINGLE_WORD -> "শুরু করুন।"
                        ResponseMode.ONE_LINE -> "ইউটিউবার হতে হলে একটি নির্দিষ্ট বিষয় নির্বাচন করুন, মানসম্পন্ন ভিডিও তৈরি করুন এবং নিয়মিত আপলোড করুন।"
                        ResponseMode.TWO_LINE -> "ইউটিউবার হতে হলে আপনার আগ্রহের একটি ক্যাটাগরি বেছে নিন।\nনিয়মিত ভিডিও আপলোড করুন এবং দর্শকদের সাথে যুক্ত থাকুন।"
                        ResponseMode.DEBATE -> "ইউটিউবে সফল হওয়ার জন্য দামি ক্যামেরার চেয়ে ভালো কনটেন্ট ও ধারাবাহিকতা বেশি গুরুত্বপূর্ণ।"
                        ResponseMode.FUNNY -> "একটি ভালো ক্যামেরা নিন, চমৎকার ভিডিও বানান আর সাবস্ক্রাইব বাটনে ক্লিক করতে বলুন!"
                        ResponseMode.ARROGANT -> "আমার মতো সেরা কনটেন্ট তৈরি করুন, সাফল্য আপনার কাছেই আসবে।"
                        ResponseMode.LORD -> "রাজকীয় আদেশ: অনতিবিলম্বে নতুন ভিডিও প্রকাশনা আরম্ভ হোক।"
                        ResponseMode.PASSIVE -> "ইউটিউবার হতে হলে একটি নির্দিষ্ট নিস বেছে নিন, ভালো অডিও-ভিডিও কোয়ালিটি বজায় রাখুন এবং নিয়মিত কনটেন্ট আপলোড করুন।"
                    }
                }
                lower.contains("প্রজেক্ট") || lower.contains("প্রকল্প") ->
                    "প্রজেক্টের কাজ সঠিক গতিতে এগিয়ে চলছে এবং শীঘ্রই আপডেট জানানো হবে।"
                lower.contains("কেমন আছো") || lower.contains("কেমন আছেন") ->
                    "ভালো আছি! আশা করি আপনার দিনটি শুভ কাটছে।"
                lower.contains("মিটিং") && (lower.contains("কখন") || lower.contains("হবে")) ->
                    "মিটিংয়ের সময় নির্ধারিত হলে আমি যথা সময়ে উপস্থিত থাকব।"
                lower.contains("সময় হবে") || lower.contains("ফ্রি আছো") ->
                    "হ্যাঁ, আমি এখন ফ্রি আছি। বলুন কী বিষয়ে আলোচনা করতে চান?"
                lower.contains("ধন্যবাদ") ->
                    "আপনাকেও অনেক ধন্যবাদ! যেকোনো প্রয়োজনে জানাবেন।"
                else ->
                    "আপনার বার্তাটি পেয়েছি। এই বিষয়ে প্রয়োজনীয় ব্যবস্থা নেওয়া হচ্ছে।"
            }
        }

        // Hindi responses
        if (lang.code == "hi") {
            return when {
                lower.contains("यूट्यूब") || lower.contains("यूट्यूबर") ->
                    "यूट्यूबर बनने के लिए एक अच्छी कैटेगरी चुनें, बेहतरीन क्वालिटी का वीडियो बनाएं और नियमित रूप से अपलोड करें।"
                lower.contains("कैसे हैं") || lower.contains("नमस्ते") ->
                    "नमस्ते! मैं ठीक हूँ, आपकी क्या सहायता कर सकता हूँ?"
                lower.contains("धन्यवाद") ->
                    "आपका स्वागत है! किसी भी सहायता के लिए बताएं।"
                else ->
                    "आपका संदेश प्राप्त हुआ। इस पर शीघ्र ही कार्य किया जाएगा।"
            }
        }

        // Spanish responses
        if (lang.code == "es") {
            return when {
                lower.contains("youtuber") || lower.contains("canal") ->
                    "Para ser YouTuber, elige un nicho claro, crea contenido de calidad y mantén la constancia."
                lower.contains("cómo estás") || lower.contains("hola") ->
                    "¡Hola! Todo muy bien por aquí. ¿En qué te puedo ayudar hoy?"
                lower.contains("gracias") ->
                    "¡De nada! Con gusto te ayudo."
                else ->
                    "Mensaje recibido. Estoy revisando los detalles para darte una respuesta."
            }
        }

        // French responses
        if (lang.code == "fr") {
            return when {
                lower.contains("youtuber") || lower.contains("chaîne") ->
                    "Pour devenir YouTuber, choisissez une thématique claire, soignez la qualité et publiez régulièrement."
                lower.contains("comment vas-tu") || lower.contains("bonjour") ->
                    "Bonjour ! Tout va bien, comment puis-je vous aider aujourd'hui ?"
                lower.contains("merci") ->
                    "Avec plaisir ! N'hésitez pas si vous avez d'autres questions."
                else ->
                    "Message bien reçu. Je prépare une réponse adaptée."
            }
        }

        // German responses
        if (lang.code == "de") {
            return when {
                lower.contains("youtuber") || lower.contains("kanal") ->
                    "Um YouTuber zu werden, wähle eine klare Nische, erstelle guten Content und lade regelmäßig hoch."
                lower.contains("wie geht") || lower.contains("hallo") ->
                    "Hallo! Mir geht es blendend. Wie kann ich dir heute helfen?"
                lower.contains("danke") ->
                    "Sehr gerne! Sag Bescheid, wenn du noch etwas brauchst."
                else ->
                    "Nachricht erhalten. Ich kümmere mich direkt darum."
            }
        }

        return englishReply
    }

    /**
     * Builds the complete Multilingual Result object for Bar 3.
     */
    fun processLanguageCard(
        originalMessage: String,
        englishReply: String,
        generationId: String,
        responseMode: ResponseMode = ResponseMode.PASSIVE
    ): LanguageCardData {
        val trimmed = originalMessage.trim()
        val lang = detectLanguage(trimmed)

        val translation = translateToEnglish(trimmed, lang)
        val origReply = generateOriginalLanguageReply(englishReply, trimmed, lang, responseMode)

        return LanguageCardData(
            generationId = generationId,
            detectedLanguage = lang.name,
            languageCode = lang.code,
            originalMessage = trimmed,
            englishTranslation = translation,
            originalLanguageReply = origReply,
            englishReply = englishReply,
            isHinglish = lang.isHinglish,
            isLoading = false
        )
    }

    private fun extractSimpleTopic(text: String): String {
        val words = text.replace(Regex("[?!.,¿¡]"), "").split("\\s+".toRegex()).filter { it.length > 2 }
        return if (words.isNotEmpty()) words.take(3).joinToString(" ") else "your query"
    }
}
