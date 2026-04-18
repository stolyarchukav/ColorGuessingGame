package com.stolyarchuk.colorguessinggame.logic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stolyarchuk.colorguessinggame.ui.GameStatistics
import com.stolyarchuk.colorguessinggame.ui.RecordEntry
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")

class StatisticsRepository(private val context: Context) {
    private val gson = Gson()
    private val STATS_KEY = stringPreferencesKey("game_stats")

    val statsFlow: Flow<GameStatistics> = context.dataStore.data.map { preferences ->
        val json = preferences[STATS_KEY]
        if (json != null) {
            try {
                gson.fromJson(json, GameStatistics::class.java)
            } catch (e: Exception) {
                GameStatistics()
            }
        } else {
            GameStatistics()
        }
    }

    suspend fun saveRecord(
        name: String,
        timeSeconds: Long,
        attempts: Int,
        codeLength: Int
    ) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[STATS_KEY]
            val stats = if (currentJson != null) {
                try {
                    gson.fromJson(currentJson, GameStatistics::class.java)
                } catch (e: Exception) {
                    GameStatistics()
                }
            } else {
                GameStatistics()
            }

            val updatedTimeRecords = stats.timeRecords.toMutableMap()
            val timeList = (updatedTimeRecords[codeLength] ?: emptyList()).toMutableList()
            timeList.add(RecordEntry(name, timeSeconds))
            updatedTimeRecords[codeLength] = timeList.sortedBy { it.value }.take(10)

            val updatedAttemptRecords = stats.attemptRecords.toMutableMap()
            val attemptList = (updatedAttemptRecords[codeLength] ?: emptyList()).toMutableList()
            attemptList.add(RecordEntry(name, attempts.toLong()))
            updatedAttemptRecords[codeLength] = attemptList.sortedBy { it.value }.take(10)

            val newStats = stats.copy(
                timeRecords = updatedTimeRecords,
                attemptRecords = updatedAttemptRecords
            )
            preferences[STATS_KEY] = gson.toJson(newStats)
        }
    }

    fun isNewRecord(timeSeconds: Long, attempts: Int, codeLength: Int, stats: GameStatistics): Boolean {
        val timeList = stats.timeRecords[codeLength] ?: emptyList()
        val isTimeRecord = timeList.size < 10 || timeSeconds < (timeList.lastOrNull()?.value ?: Long.MAX_VALUE)

        val attemptList = stats.attemptRecords[codeLength] ?: emptyList()
        val isAttemptRecord = attemptList.size < 10 || attempts < (attemptList.lastOrNull()?.value ?: Long.MAX_VALUE)

        return isTimeRecord || isAttemptRecord
    }
}
