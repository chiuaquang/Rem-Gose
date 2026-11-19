package com.remgose.rem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE deletionTimestamp IS NULL ORDER BY creationTimestamp DESC")
    fun observeAll(): Flow<List<GameEntity>>
    @Query("SELECT * FROM games WHERE name = :name")
    suspend fun get(name: String): GameEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(game: GameEntity)
    @Query("UPDATE games SET statusPhase = :phase, statusMessage = :msg WHERE name = :name")
    suspend fun setStatus(name: String, phase: String, msg: String?)
    @Query("UPDATE games SET runtimeVersionOverride = :version WHERE name = :name")
    suspend fun setRuntimeOverride(name: String, version: String?)
    @Query("UPDATE games SET deletionTimestamp = :ts WHERE name = :name")
    suspend fun softDelete(name: String, ts: Long)
}

@Dao
interface RuntimeDao {
    @Query("SELECT * FROM runtimes WHERE deletionTimestamp IS NULL")
    fun observeAll(): Flow<List<RuntimeEntity>>
    @Query("SELECT * FROM runtimes WHERE version = :version")
    suspend fun get(version: String): RuntimeEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(runtime: RuntimeEntity)
    @Query("UPDATE runtimes SET statusPhase = :phase, statusMessage = :msg WHERE version = :version")
    suspend fun setStatus(version: String, phase: String, msg: String?)
}
