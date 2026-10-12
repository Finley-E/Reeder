package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllPhrases(): Flow<List<Phrase>>

    @Query("SELECT * FROM phrases WHERE languageCode = :langCode ORDER BY isFavorite DESC, timestamp DESC")
    fun getPhrasesByLanguage(langCode: String): Flow<List<Phrase>>

    @Query("SELECT * FROM phrases WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritePhrases(): Flow<List<Phrase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: Phrase): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<Phrase>)

    @Update
    suspend fun updatePhrase(phrase: Phrase)

    @Delete
    suspend fun deletePhrase(phrase: Phrase)

    @Query("DELETE FROM phrases WHERE id = :id")
    suspend fun deletePhraseById(id: Int)

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun getCount(): Int

    @Query("DELETE FROM phrases")
    suspend fun clearAll()
}
