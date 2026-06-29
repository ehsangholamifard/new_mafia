package com.example.data.dao

import androidx.room.*
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.GameHistoryEntity
import com.example.data.model.GameSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MafiaDao {
    // --- Players ---
    @Query("SELECT * FROM players ORDER BY id ASC")
    fun getAllPlayersFlow(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players")
    suspend fun getAllPlayersList(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    // --- Roles ---
    @Query("SELECT * FROM roles ORDER BY id ASC")
    fun getAllRolesFlow(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles")
    suspend fun getAllRolesList(): List<RoleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRole(role: RoleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RoleEntity>)

    @Update
    suspend fun updateRole(role: RoleEntity)
    
    @Delete
    suspend fun deleteRole(role: RoleEntity)

    @Query("DELETE FROM roles")
    suspend fun deleteAllRoles()

    // --- Game Logs ---
    @Query("SELECT * FROM game_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<GameLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GameLogEntity)

    @Query("DELETE FROM game_logs")
    suspend fun deleteAllLogs()
    
    // --- Game History ---
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    fun getAllGameHistoryFlow(): Flow<List<GameHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameHistory(history: GameHistoryEntity)
    
    @Delete
    suspend fun deleteGameHistory(history: GameHistoryEntity)

    // --- Game Sessions ---
    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC")
    fun getAllGameSessionsFlow(): Flow<List<GameSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameSession(session: GameSessionEntity): Long

    @Update
    suspend fun updateGameSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions WHERE id = :id")
    suspend fun getGameSessionById(id: Int): GameSessionEntity?

    @Delete
    suspend fun deleteGameSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE id = :id")
    suspend fun deleteGameSessionById(id: Int)
}
