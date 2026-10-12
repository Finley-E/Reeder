package com.example.data

import kotlinx.coroutines.flow.Flow

class PhraseRepository(private val phraseDao: PhraseDao) {
    val allPhrases: Flow<List<Phrase>> = phraseDao.getAllPhrases()
    val favoritePhrases: Flow<List<Phrase>> = phraseDao.getFavoritePhrases()

    fun getPhrasesByLanguage(langCode: String): Flow<List<Phrase>> {
        return phraseDao.getPhrasesByLanguage(langCode)
    }

    suspend fun insert(phrase: Phrase): Long {
        return phraseDao.insertPhrase(phrase)
    }

    suspend fun update(phrase: Phrase) {
        phraseDao.updatePhrase(phrase)
    }

    suspend fun delete(phrase: Phrase) {
        phraseDao.deletePhrase(phrase)
    }

    suspend fun deleteById(id: Int) {
        phraseDao.deletePhraseById(id)
    }

    suspend fun toggleFavorite(phrase: Phrase) {
        val updated = phrase.copy(isFavorite = !phrase.isFavorite)
        phraseDao.updatePhrase(updated)
    }

    suspend fun clearAll() {
        phraseDao.clearAll()
    }

    suspend fun getCount(): Int {
        return phraseDao.getCount()
    }

    suspend fun seedDefaultPhrasesIfEmpty() {
        if (phraseDao.getCount() == 0) {
            seedDefaultPhrases()
        }
    }

    suspend fun seedDefaultPhrases() {
        val defaultList = listOf(
            // English
            Phrase(
                text = "Hello! This voice synthesis engine is running completely on-device in offline mode with natural cadence.",
                languageCode = "EN",
                languageName = "English",
                label = "Offline Status Briefing",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "The quick brown fox jumps over the lazy dog. Testing digits: zero, one, two, three, four, five, six, seven, eight, nine.",
                languageCode = "EN",
                languageName = "English",
                label = "Phonetics & Digits Pangram",
                category = "Pronunciation",
                isFavorite = false,
                pitch = 1.0f,
                speed = 1.0f
            ),
            Phrase(
                text = "Field operations checkpoint: Base telemetry is fully autonomous. Radio beacon transmitting on frequency 144.390 megahertz.",
                languageCode = "EN",
                languageName = "English",
                label = "Off-Grid Field Ops",
                category = "Off-Grid Ops",
                isFavorite = true,
                pitch = 0.96f,
                speed = 0.92f
            ),
            // French
            Phrase(
                text = "Bonjour ! Ce système vocal fonctionne de façon entièrement locale et autonome, sans aucune connexion Internet requise.",
                languageCode = "FR",
                languageName = "Français",
                label = "Salutation & Mode Hors-Ligne",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "Rapport météo d'expédition : Température 19 degrés, vent modéré de secteur ouest. Aucun serveur distant nécessaire.",
                languageCode = "FR",
                languageName = "Français",
                label = "Rapport Météo Terrain",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 0.96f,
                speed = 0.92f
            ),
            // German
            Phrase(
                text = "Guten Tag! Die Sprachausgabe läuft vollkommen autonom auf diesem Gerät, ideal für autarke Offline-Einsätze.",
                languageCode = "DE",
                languageName = "Deutsch",
                label = "Autonome Sprachausgabe",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.96f,
                speed = 0.94f
            ),
            Phrase(
                text = "Systemprüfung erfolgreich: Lokale SQLite-Datenbank und On-Device TTS-Synthese sind betriebsbereit.",
                languageCode = "DE",
                languageName = "Deutsch",
                label = "Diagnosebericht Offline",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 1.0f,
                speed = 1.0f
            ),
            // Polish
            Phrase(
                text = "Dzień dobry! Ta aplikacja działa w pełni lokalnie bez połączenia z siecią. Wszystkie dane są zapisane w lokalnej bazie Room.",
                languageCode = "PL",
                languageName = "Polski",
                label = "Status Lokalny Bazy",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "W Szczebrzeszynie chrząszcz brzmi w trzcinie, a Szczebrzeszyn z tego słynie. Test intonacji i głosek syczących.",
                languageCode = "PL",
                languageName = "Polski",
                label = "Test Wymowy i Dykcji",
                category = "Pronunciation",
                isFavorite = false,
                pitch = 1.0f,
                speed = 0.95f
            ),
            // Portuguese
            Phrase(
                text = "Olá! Este motor de voz opera de maneira totalmente autônoma e segura no dispositivo, sem depender de rede externa.",
                languageCode = "PT",
                languageName = "Português",
                label = "Operação Autônoma",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "Coordenadas táticas recebidas: Ponto de encontro confirmado às 16 horas e 45 minutos com energia auxiliar.",
                languageCode = "PT",
                languageName = "Português",
                label = "Transmissão Tática",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 0.96f,
                speed = 0.92f
            ),
            // Spanish
            Phrase(
                text = "¡Hola! La síntesis vocal está procesando todo directamente en el procesador local, sin enviar datos a la nube.",
                languageCode = "ES",
                languageName = "Español",
                label = "Privacidad y Modo Offline",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.97f,
                speed = 0.94f
            ),
            Phrase(
                text = "Tres tristes tigres tragaban trigo en un trigal. Verificando articulación de consonantes y fluidez fonética.",
                languageCode = "ES",
                languageName = "Español",
                label = "Trabalenguas y Dicción",
                category = "Pronunciation",
                isFavorite = false,
                pitch = 1.0f,
                speed = 0.96f
            ),
            // Turkish
            Phrase(
                text = "Merhaba! Bu ses sentezleyici cihaz üzerinde yüzde yüz çevrimdışı çalışarak kesintisiz test imkanı sunar.",
                languageCode = "TR",
                languageName = "Türkçe",
                label = "Çevrimdışı Durum Testi",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "Saha telemetrisi devrede: Saat 14:30 itibarıyla tüm yerel sensörler çevrimdışı modda stabil.",
                languageCode = "TR",
                languageName = "Türkçe",
                label = "Saha Görev Raporu",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 0.96f,
                speed = 0.92f
            ),
            // Bulgarian
            Phrase(
                text = "Здравейте! Гласовият синтез се извършва директно на устройството без необходимост от интернет мрежа.",
                languageCode = "BG",
                languageName = "Български",
                label = "Локален Автономен Статус",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "Полеви доклад: Всички измервания са завършени успешно в режим на пълна автономия.",
                languageCode = "BG",
                languageName = "Български",
                label = "Полеви Доклад",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 0.96f,
                speed = 0.92f
            ),
            // Russian
            Phrase(
                text = "Здравствуйте! Голосовой движок синтезирует речь локально на устройстве без отправки запросов во внешнюю сеть.",
                languageCode = "RU",
                languageName = "Русский",
                label = "Автономный Режим Синтеза",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.96f,
                speed = 0.94f
            ),
            Phrase(
                text = "Шли сорок мышей, несли сорок грошей, две мыши поплоше несли по два гроша. Проверка естественности пауз.",
                languageCode = "RU",
                languageName = "Русский",
                label = "Артикуляция и Паузы",
                category = "Pronunciation",
                isFavorite = false,
                pitch = 1.0f,
                speed = 0.95f
            ),
            // Ukrainian
            Phrase(
                text = "Привіт! Ця система генерації мовлення працює автономно в локальній базі Room без жодного інтернет-трафіку.",
                languageCode = "UA",
                languageName = "Українська",
                label = "Повна Автономність",
                category = "Conversational",
                isFavorite = true,
                pitch = 0.98f,
                speed = 0.95f
            ),
            Phrase(
                text = "Звіт польового моніторингу: Локальні сенсори активні, передача інформації готова до виконання.",
                languageCode = "UA",
                languageName = "Українська",
                label = "Польовий Звіт Моніторингу",
                category = "Off-Grid Ops",
                isFavorite = false,
                pitch = 0.96f,
                speed = 0.92f
            )
        )
        phraseDao.insertAll(defaultList)
    }
}
