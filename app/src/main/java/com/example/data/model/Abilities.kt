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
    ),

    "UNSILENCE" to Ability(
        id = "UNSILENCE",
        name = "رفع سکوت (کشیش)",
        description = "کشیش باید تشخیص بدهد که چه کسی توسط مافیا سایلنت شده و او را از سایلنت بودن خارج کند.",
        nightPriority = 20,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "HACK" to Ability(
        id = "HACK",
        name = "استعلام هکر",
        description = "سه نفر را انتخاب میکند. اگر دقیقاً یک مافیا در بین آنها باشد، لیست خطرناک است.",
        nightPriority = 75,
        actionType = "TARGET_THREE_ALIVE"
    ),
    "INTOXICATE" to Ability(
        id = "INTOXICATE",
        name = "ساقی (محروم کردن)",
        description = "یک نفر (حتی خودش) را انتخاب میکند تا قابلیت شبانهاش مسدود شود. یک شب در میان استفاده میشود و هدف تکراری مجاز نیست.",
        nightPriority = 11,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "GRAVEDIG" to Ability(
        id = "GRAVEDIG",
        name = "نبش قبر (گورکن)",
        description = "در شب اعلام میکند که میخواهد نبش قبر کند. روز بعد، گاد نقش تمامی بازیکنانی که تا این لحظه از بازی حذف شدهاند را به صورت عمومی اعلام میکند.",
        nightPriority = 80,
        actionType = "NO_TARGET"
    ),
    "TERROR" to Ability(
        id = "TERROR",
        name = "ترور در روز (تروریست)",
        description = "هنگام خروج با رایگیری در روز، میتواند یک بازیکن زنده دیگر را همراه خود حذف کند. اگر شب قبل توسط ساقی مسدود شده باشد، این قابلیت کار نمیکند.",
        nightPriority = 0,
        actionType = "DAY_EXECUTION_REACTION"
    ),
    "NATO_GUESS" to Ability(
        id = "NATO_GUESS",
        name = "حدس نقش (ناتو)",
        description = "به دنبال نقش شهروندان است. گرداننده نام یک بازیکن و نقش حدس زده شده را وارد میکند. در صورت ۳ حدس اشتباه، ناتو از بازی حذف میشود.",
        nightPriority = 48,
        actionType = "TARGET_PLAYER_AND_ROLE"
    ),
    "SABOTAGE" to Ability(
        id = "SABOTAGE",
        name = "خرابکاری تفنگ (خرابکار)",
        description = "یک نفر را انتخاب میکند. اگر آن شخص تفنگ جنگی داشته باشد و در روز شلیک کند، تیر به خودش برمیگردد. روی نقش ساقی بیاثر است.",
        nightPriority = 35,
        actionType = "TARGET_ONE_ALIVE"
    ),
    "KILLER_SHOOT" to Ability(
        id = "KILLER_SHOOT",
        name = "شلیک و سلاخی کیلر",
        description = "یک نقش مستقل که هر دو شب یک بار بیدار شده و قابلیت شلیک یا سلاخی یک نفر را دارد.",
        nightPriority = 42,
        actionType = "TARGET_ONE_ALIVE"
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

        normalized.contains("ساقی") || normalized.contains("sagi") || normalized.contains("intoxicate") -> listOf("INTOXICATE")
        normalized.contains("گورکن") || normalized.contains("gravedigger") || normalized.contains("gravedig") -> listOf("GRAVEDIG")
        normalized.contains("تروریست") || normalized.contains("terrorist") || normalized.contains("terror") -> listOf("TERROR")
        normalized.contains("کشیش") || normalized.contains("priest") || normalized.contains("unsilence") -> listOf("UNSILENCE")
        normalized.contains("هکر") || normalized.contains("hacker") || normalized.contains("hack") -> listOf("HACK")
        normalized.contains("دکتر") || normalized.contains("doctor") || normalized.contains("lector") || normalized.contains("لکتور") -> listOf("HEAL")
        normalized.contains("تفنگدار") || normalized.contains("musketeer") -> listOf("GIVE_GUN")
        normalized.contains("پدرخوانده") || normalized.contains("godfather") || normalized.contains("حرفه‌ای") || normalized.contains("sniper") || normalized.contains("professional") -> listOf("SHOOT", "SLAUGHTER")
        normalized.contains("خریدار") || normalized.contains("مذاکره") || normalized.contains("buyer") -> listOf("RECRUIT")
        normalized.contains("کین") || normalized.contains("kane") -> listOf("REVEAL_MAFIA")
        normalized.contains("کنستانتین") || normalized.contains("constantine") -> listOf("REVIVE")
        normalized.contains("وتو") || normalized.contains("veto") -> listOf("VETO")
        normalized.contains("ناتو") || normalized.contains("nato") -> listOf("NATO_GUESS")
        normalized.contains("خرابکار") || normalized.contains("saboteur") || normalized.contains("sabotage") -> listOf("SABOTAGE")
        normalized.contains("کیلر") || normalized.contains("killer") -> listOf("KILLER_SHOOT")
        else -> emptyList()
    }
}

data class NightActionQueueItem(
    val player: PlayerEntity,
    val ability: Ability
)

fun getRoleNightPriority(roleName: String?, roleTeam: String?): Int {
    if (roleName == null) return 999
    return when {
        // 1. INDEPENDENT TEAM (Top Priority: 100-199)
        roleName.contains("چرچیل") -> 100
        roleName.contains("هزارچهره") -> 101
        roleName.contains("نوستراداموس") -> 102
        roleName.contains("مجهول") -> 103
        roleName.contains("کیلر") -> 104
        roleTeam == "Independent" -> 199 // Any unlisted independent role falls here

        // 2. MAFIA TEAM (Second Priority: 200-299)
        roleName.contains("پدرخوانده") || roleName.contains("رئیس") -> 200
        roleName.contains("لکتور") -> 201
        roleName.contains("ماتادور") -> 202
        roleName.contains("خریدار") -> 203
        roleName.contains("خرابکار") -> 204
        roleTeam == "Mafia" -> 299 // Any unlisted mafia role (e.g., Simple Mafia) falls here

        // 3. CITIZEN TEAM (Third Priority: 300-399)
        roleName.contains("ساقی") -> 300
        roleName.contains("کشیش") -> 301
        roleName.contains("کارآگاه") || roleName.contains("کاراگاه") -> 302
        roleName.contains("دکتر") && !roleName.contains("لکتور") -> 303
        roleName.contains("حرفهای") || roleName.contains("حرفه ای") -> 304
        roleName.contains("اوشن") || roleName.contains("ژنرال") -> 305
        roleName.contains("تفنگدار") -> 306
        roleName.contains("همشهری کین") -> 307
        roleName.contains("روانپزشک") || roleName.contains("روان پزشک") -> 308
        roleTeam == "Citizen" -> 399 // Any unlisted citizen role (e.g., Simple Citizen) falls here

        else -> 900 // Absolute fallback for completely unknown teams
    }
}

fun getNightPriority(roleName: String, teamName: String): Int {
    return getRoleNightPriority(roleName, teamName)
}

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
    
    return queue.sortedBy { item ->
        val role = roles.find { it.id == item.player.assignedRoleId || it.name == item.player.assignedRoleName }
        val roleName = item.player.assignedRoleName ?: role?.name ?: ""
        val roleTeam = item.player.assignedRoleTeam ?: role?.team ?: ""
        getRoleNightPriority(roleName, roleTeam)
    }
}
