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
    val wasBlockedLastNight: Boolean = false,
    val isInsuredThisNight: Boolean = false,
    val doctorSelfSavesCount: Int = 0,
    val isRevealedMafia: Boolean = false,
    val willDieNextNight: Boolean = false,
    val isRevivedThisNight: Boolean = false,
    val hasBlankGunThisRound: Boolean = false,
    val hasLiveGunThisRound: Boolean = false,
    val hasBlankGun: Boolean = false,
    val hasCombatGun: Boolean = false,
    val usedLiveGun: Boolean = false,
    val isSilencedThisRound: Boolean = false,
    val isSabotaged: Boolean = false,
    val isBulletproof: Boolean = false,
    val isProtected: Boolean = false,
    val challengeCount: Int = 0
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
    val iconName: String = "", // Added icon selection support
    val abilitiesJson: String = "[]" // JSON representation of chosen ability IDs or direct list
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

@Entity(tableName = "game_sessions")
@Serializable
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "IN_PROGRESS", // "IN_PROGRESS" or "FINISHED"
    val moderatorName: String = "",
    val gameStage: String = "PLAY",
    val gamePhase: String = "Day",
    val playersJson: String,
    val logsJson: String,
    val rolesJson: String = "",
    val remainingInquiries: Int = 3,
    val totalInquiries: Int = 3,
    val sagiCooldownNight: Int = 0,
    val sagiPastTargetsJson: String = "[]",
    val isGravedigActiveThisNight: Boolean = false,
    val natoWrongGuessesCount: Int = 0,
    val musketeerLiveGunExhausted: Boolean = false
)

@Serializable
data class RoleCapability(
    val name: String,
    val totalCount: Int,
    val remainingCount: Int
)
