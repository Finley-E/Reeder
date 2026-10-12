package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrases")
data class Phrase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val languageCode: String,     // e.g. "FR", "DE", "PL", "PT", "ES", "TR", "BG", "RU", "UA", "EN"
    val languageName: String,     // e.g. "Français", "Deutsch", etc.
    val label: String,            // short custom title or description
    val category: String = "General", // "Conversational", "Pronunciation", "Numbers & Time", "Off-Grid Ops", "Emergency"
    val isFavorite: Boolean = false,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)
