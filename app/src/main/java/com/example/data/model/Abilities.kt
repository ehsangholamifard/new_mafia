package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ability(
    val id: String,
    val name: String,
    val description: String,
    val nightPriority: Int?, // Lower number = wakes up earlier. Day actions set to null or 0.
    val actionType: String // e.g., 'TARGET_ONE_ALIVE', 'TARGET_ONE_DEAD', 'NO_TARGET', 'DAY_ACTION'
)

// Global registry serving as the dictionary for all game abilities
val ABILITY_REGISTRY = mapOf(
    "BLOCK" to Ability(
        id = "BLOCK",
        name = "مسدود کردن",
        description = "مسدود کردن نقش بازیکن انتخاب شده برای یک شب (توسط ماتادور یا مسدود کننده)",
        nightPriority = 10,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "INSURE" to Ability(
        id = "INSURE",
        name = "بیمه کردن (بیمه کننده)",
        description = "یک بازیکن را انتخاب میکند تا در طول شب از هرگونه قابلیت، شلیک، مسدودسازی یا تغییر وضعیت در امان باشد.",
        nightPriority = 1,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "SLAUGHTER" to Ability(
        id = "SLAUGHTER",
        name = "سلاخی (پدرخوانده/حرفه‌ای)",
        description = "یک بازیکن را به طور قطعی حذف میکند (نادیده گرفتن نجات پزشک).",
        nightPriority = 45,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "HEAL" to Ability(
        id = "HEAL",
        name = "نجات دادن",
        description = "نجات دادن یکی از شهروندان یا خودش از تیر شلیک شده مافیا",
        nightPriority = 20,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "GIVE_GUN" to Ability(
        id = "GIVE_GUN",
        name = "اعطای تفنگ",
        description = "اعطای تفنگ (جنگی یا مشقی) به یکی از بازیکنان بازی",
        nightPriority = 30,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "SHOOT" to Ability(
        id = "SHOOT",
        name = "شلیک کردن",
        description = "شلیک شبانه تفنگ یا شلیک جنگی مافیا یا حرفه‌ای",
        nightPriority = 40,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "RECRUIT" to Ability(
        id = "RECRUIT",
        name = "خریداری / مذاکره",
        description = "خریداری (مذاکره) با شهروندان جهت جذب به مافیا",
        nightPriority = 50,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "REVEAL_MAFIA" to Ability(
        id = "REVEAL_MAFIA",
        name = "تشخیص مافیا (همشهری کین)",
        description = "استعلام هویت مافیایی یکی از بازیکنان در شب",
        nightPriority = 60,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "REVIVE" to Ability(
        id = "REVIVE",
        name = "زنده کردن (کنستانتین)",
        description = "احیا کردن و بازگرداندن یکی از بازیکنان مرده به بازی",
        nightPriority = 70,
        actionType = "TARGET_ONE_DEAD"
    ),
    "VETO" to Ability(
        id = "VETO",
        name = "وتو",
        description = "وتو کردن رأی‌گیری روزانه و لغو آرای شهروندان",
        nightPriority = 75,
        actionType = "TARGET_ONE_DEAD"
    )
)

/**
 * Maps a predefined role identifier (by its role name or short-string identifier)
 * to an array of associated ability IDs.
 */
fun getRoleAbilities(roleId: String): List<String> {
    val normalized = roleId.trim().lowercase()
    return when {
        normalized.contains("بیمه") || normalized.contains("insurer") || normalized.contains("insurance") || normalized.contains("insure") -> listOf("INSURE")
        normalized.contains("سلاخ") || normalized.contains("slaughter") -> listOf("SLAUGHTER")
        normalized.contains("ماتادور") || normalized.contains("matador") -> listOf("BLOCK")
        normalized.contains("دکتر") || normalized.contains("doctor") || normalized.contains("lector") || normalized.contains("لکتور") -> listOf("HEAL")
        normalized.contains("تفنگدار") || normalized.contains("musketeer") -> listOf("GIVE_GUN")
        normalized.contains("پدرخوانده") || normalized.contains("godfather") || normalized.contains("حرفه‌ای") || normalized.contains("sniper") || normalized.contains("professional") -> listOf("SHOOT")
        normalized.contains("خریدار") || normalized.contains("مذاکره") || normalized.contains("buyer") -> listOf("RECRUIT")
        normalized.contains("کین") || normalized.contains("kane") -> listOf("REVEAL_MAFIA")
        normalized.contains("کنستانتین") || normalized.contains("constantine") -> listOf("REVIVE")
        normalized.contains("وتو") || normalized.contains("veto") -> listOf("VETO")
        else -> emptyList()
    }
}

data class NightActionQueueItem(
    val player: PlayerEntity,
    val ability: Ability
)

fun buildNightQueue(players: List<PlayerEntity>, roles: List<RoleEntity>): List<NightActionQueueItem> {
    val queue = mutableListOf<NightActionQueueItem>()
    val aliveSelectedPlayers = players.filter { it.isSelected && it.isAlive }
    
    for (player in aliveSelectedPlayers) {
        val role = roles.find { it.id == player.assignedRoleId || it.name == player.assignedRoleName }
        val rawAbilities = if (role != null) {
            try {
                if (role.abilitiesJson.isNotBlank()) {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(role.abilitiesJson)
                } else {
                    getRoleAbilities(role.name)
                }
            } catch (e: Exception) {
                getRoleAbilities(role?.name ?: "")
            }
        } else {
            emptyList()
        }
        
        for (abilityId in rawAbilities) {
            val ability = ABILITY_REGISTRY[abilityId]
            if (ability != null) {
                val priority = ability.nightPriority
                if (priority != null && priority > 0) {
                    queue.add(NightActionQueueItem(player, ability))
                }
            }
        }
    }
    
    return queue.sortedBy { it.ability.nightPriority ?: Int.MAX_VALUE }
}
