package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "players")
@Serializable
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isSelected: Boolean = false,
    val isAlive: Boolean = true,
    val assignedRoleId: Int? = null,
    val assignedRoleName: String? = null,
    val assignedRoleTeam: String? = null,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isVoteRevoked: Boolean = false,
    val isSaved: Boolean = false,
    val capabilitiesJson: String = "", // Runtime state of player's capabilities
    val hasUsedLastMoveCard: Boolean = false,
    val note: String = "",
    val voteCount: Int = 0,
    val warningsCount: Int = 0,
    val isKilledToday: Boolean = false,
    val isSlaughtered: Boolean = false,
    val isHealedThisNight: Boolean = false,
    val isShotThisNight: Boolean = false,
    val isBlockedThisNight: Boolean = false,
    val doctorSelfSavesCount: Int = 0,
    val isRevealedMafia: Boolean = false,
    val willDieNextNight: Boolean = false,
    val isRevivedThisNight: Boolean = false
)

@Entity(tableName = "roles")
@Serializable
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val team: String, // "Mafia", "Citizen", "Independent"
    val count: Int = 0, // chosen setup count
    val capabilitiesJson: String = "", // list of default TemplateCapabilities
    val iconName: String = "" // Added icon selection support
)

@Entity(tableName = "game_logs")
@Serializable
data class GameLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val phase: String = "Day"
)

@Entity(tableName = "game_history")
@Serializable
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val winnerTeam: String,
    val reason: String,
    val playersJson: String, // Serialize the final state of players
    val logsJson: String, // Serialize the final list of logs
    val moderatorName: String = ""
)

@Serializable
data class RoleCapability(
    val name: String,
    val totalCount: Int,
    val remainingCount: Int
)
