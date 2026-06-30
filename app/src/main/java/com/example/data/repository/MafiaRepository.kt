package com.example.data.repository

import com.example.data.dao.MafiaDao
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.GameHistoryEntity
import com.example.data.model.GameSessionEntity
import kotlinx.coroutines.flow.Flow

class MafiaRepository(private val mafiaDao: MafiaDao) {

    val allPlayers: Flow<List<PlayerEntity>> = mafiaDao.getAllPlayersFlow()
    val allRoles: Flow<List<RoleEntity>> = mafiaDao.getAllRolesFlow()
    val allLogs: Flow<List<GameLogEntity>> = mafiaDao.getAllLogsFlow()
    val allGameHistory: Flow<List<GameHistoryEntity>> = mafiaDao.getAllGameHistoryFlow()
    val allGameSessions: Flow<List<GameSessionEntity>> = mafiaDao.getAllGameSessionsFlow()

    suspend fun insertGameSession(session: GameSessionEntity) = mafiaDao.insertGameSession(session)
    suspend fun updateGameSession(session: GameSessionEntity) = mafiaDao.updateGameSession(session)
    suspend fun getGameSessionById(id: Int) = mafiaDao.getGameSessionById(id)
    suspend fun deleteGameSession(session: GameSessionEntity) = mafiaDao.deleteGameSession(session)
    suspend fun deleteGameSessionById(id: Int) = mafiaDao.deleteGameSessionById(id)

    suspend fun getAllPlayersList() = mafiaDao.getAllPlayersList()
    suspend fun getPlayerById(id: Int) = mafiaDao.getPlayerById(id)
    suspend fun insertPlayer(player: PlayerEntity) = mafiaDao.insertPlayer(player)
    suspend fun insertPlayers(players: List<PlayerEntity>) = mafiaDao.insertPlayers(players)
    suspend fun updatePlayer(player: PlayerEntity) = mafiaDao.updatePlayer(player)
    suspend fun deletePlayer(player: PlayerEntity) = mafiaDao.deletePlayer(player)
    suspend fun deleteAllPlayers() = mafiaDao.deleteAllPlayers()

    suspend fun getAllRolesList() = mafiaDao.getAllRolesList()
    suspend fun insertRole(role: RoleEntity) = mafiaDao.insertRole(role)
    suspend fun insertRoles(roles: List<RoleEntity>) = mafiaDao.insertRoles(roles)
    suspend fun updateRole(role: RoleEntity) = mafiaDao.updateRole(role)
    suspend fun deleteRole(role: RoleEntity) = mafiaDao.deleteRole(role)
    suspend fun deleteAllRoles() = mafiaDao.deleteAllRoles()

    suspend fun addLog(message: String, phase: String = "Day") {
        mafiaDao.insertLog(GameLogEntity(message = message, phase = phase))
    }
    suspend fun clearLogs() = mafiaDao.deleteAllLogs()

    suspend fun insertGameHistory(history: GameHistoryEntity) = mafiaDao.insertGameHistory(history)
    suspend fun deleteGameHistory(history: GameHistoryEntity) = mafiaDao.deleteGameHistory(history)

    suspend fun seedDefaultRolesIfNeeded() {
        val existing = mafiaDao.getAllRolesList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                RoleEntity(
                    name = "کارآگاه 🔍",
                    description = "استعلام زنده بودن و مشخص کردن هویت بازیکنان در شب (همکار شهروندان)",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"استعلام وضعیت 🔍","totalCount":10,"remainingCount":10}]"""
                ),
                RoleEntity(
                    name = "دکتر 🩺",
                    description = "نجات دادن یکی از شهروندان یا خودش از تیر شلیک شده مافیا",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"شفا / نجات 🩺","totalCount":10,"remainingCount":10}]"""
                ),
                RoleEntity(
                    name = "حرفه‌ای 🔫",
                    description = "تیرانداز شهروندان. اگر به مافیا شلیک کند او کشته می‌شود، وگرنه خودش حذف می‌شود",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"شلیک شبانه 🔫","totalCount":2,"remainingCount":2},{"name":"سلاخی 🔪","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "تفنگدار 🪖",
                    description = "واگذاری تفنگ جنگی یا مشقی به بازیکنان در شب برای استفاده در روز بعد",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"تفنگ جنگی ⚔️","totalCount":2,"remainingCount":2},{"name":"تفنگ مشقی 🔫","totalCount":2,"remainingCount":2}]"""
                ),
                RoleEntity(
                    name = "زره‌پوش 🛡️",
                    description = "در شب شلیک مافیا به او کارساز نیست و در روز نیز با رای مستقیم بیرون نمی‌رود",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"محافظ زره 🛡️","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "جان‌سخت 💪",
                    description = "سر سخت شهروندان که شب اول با تیر مافیا نمی‌میرد و به تعداد ۲ بار استعلام کشته‌شدگان شب را می‌گیرد",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"استعلام کشته‌شدگان شب 📰","totalCount":2,"remainingCount":2}]"""
                ),
                RoleEntity(
                    name = "اوشن - ژنرال 🌊",
                    description = "هر شب یک بازیکن زنده را انتخاب می‌کند. اگر او مافیا باشد، ژنرال کشته می‌شود (غیرقابل شفا توسط پزشک)، وگرنه هدف بیدار می‌شود تا ژنرال را شناسایی کند",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"تشخیص هویت اوشن 🌊","totalCount":10,"remainingCount":10}]"""
                ),
                RoleEntity(
                    name = "کنستانتین ⚡",
                    description = "شخصیت جناح شهروند که یک بار در طول بازی می‌تواند یکی از بازیکنان از دست رفته یا اعدام شده را احیا کرده و زنده کند",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"احیای مردگان ⚡","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "همشهری کین 📰",
                    description = "یک بار در شب یک بازیکن را انتخاب می‌کند. اگر او مافیا باشد، به عنوان مافیای شناسایی شده علامت‌گذاری می‌شود اما خود کین شب بعد قربانی می‌گردد",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"افشاگری کین 📰","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "کشیش ⛪",
                    description = "کشیش باید تشخیص بدهد که چه کسی توسط مافیا سایلنت شده و او را از سایلنت بودن خارج کند.",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"رفع سکوت ⛪","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["UNSILENCE"]"""
                ),
                RoleEntity(
                    name = "شهروند ساده 🕊️",
                    description = "عضو معمولی ارتش شهروندان با قدرت تصمیم‌گیری، نطق و رأی‌دهی بالا",
                    team = "Citizen"
                ),
                RoleEntity(
                    name = "رئیس مافیا (پدرخوانده) 👑",
                    description = "رهبر گروه مافیا که استعلام او برای کارآگاه همیشه منفی (شهروند) است و فرمان شلیک شب را صادر میکند و دارای قابلیت خریداری (مذاکره) است",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"شلیک شبانه مافیا 💀","totalCount":10,"remainingCount":10},{"name":"خریداری (مذاکره) 🤝","totalCount":1,"remainingCount":1},{"name":"سلاخی 🔪","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "دکتر لکتور 💊",
                    description = "پزشک تیم مافیا که اعضای مافیا را در شب شفا داده و محافظت می‌کند",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"شفا‌یار مافیا 💊","totalCount":10,"remainingCount":10}]"""
                ),
                RoleEntity(
                    name = "خریدار (مذاکره کننده) 🤝",
                    description = "می‌تواند با حدس زدن درست نقش شهروند ساده، او را خریداری کند تا هم‌تیم مافیا شود",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"خریداری (مذاکره) 🤝","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "تروریست 💣",
                    description = "اگر با رأی شهروندان در روز اعدام شود، می‌تواند یکی از بازیکنان را با خود ترور و حذف کند",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"انفجار انتحاری 💣","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "مافیای ساده 👤",
                    description = "یار معمولی تیم مافیا که به رهبر کمک می‌کند و در رای‌گیری‌ها تاثیرگذار است",
                    team = "Mafia"
                ),
                RoleEntity(
                    name = "ماتادور 🧣",
                    description = "عضو باسابقه مافیا که هر شب می‌تواند قابلیت‌های یکی از بازیکنان را مسدود کند تا او نتواند کاری انجام دهد",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"مسدود کردن نقش 🧣","totalCount":10,"remainingCount":10}]"""
                ),

                RoleEntity(
                    name = "هکر 📡",
                    description = "سه نفر را انتخاب میکند. اگر دقیقاً یک مافیا در بین آنها باشد، لیست خطرناک است. توجه: این قابلیت معمولاً فقط در شب دوم بازی استفاده می‌شود.",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"استعلام هکر 📡","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["HACK"]"""
                ),
                RoleEntity(
                    name = "ساقی 🍷",
                    description = "یک نفر (حتی خودش) را انتخاب میکند تا قابلیت شبانهاش مسدود شود. یک شب در میان استفاده میشود و هدف تکراری مجاز نیست.",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"مستی / ساقی 🍷","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["INTOXICATE"]"""
                ),
                RoleEntity(
                    name = "گورکن 🪦",
                    description = "در شب اعلام میکند که میخواهد نبش قبر کند. روز بعد، گاد نقش تمامی بازیکنانی که تا این لحظه از بازی حذف شدهاند را به صورت عمومی اعلام میکند.",
                    team = "Citizen",
                    capabilitiesJson = """[{"name":"نبش قبر 🪦","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["GRAVEDIG"]"""
                ),
                RoleEntity(
                    name = "تروریست 💣",
                    description = "هنگام خروج با رایگیری در روز، میتواند یک بازیکن زنده دیگر را همراه خود حذف کند. اگر شب قبل توسط ساقی مسدود شده باشد، این قابلیت کار نمیکند.",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"ترور در روز 💣","totalCount":1,"remainingCount":1}]""",
                    abilitiesJson = """["TERROR"]"""
                ),
                RoleEntity(
                    name = "ناتو 🎯",
                    description = "جزء جناح مافیا است که به دنبال حدس زدن نقش شهروندان است. در صورت ۳ حدس اشتباه او از بازی حذف می‌شود.",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"حدس نقش (ناتو) 🎯","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["NATO_GUESS"]"""
                ),
                RoleEntity(
                    name = "خرابکار 🔫",
                    description = "یک نفر را انتخاب میکند. اگر آن شخص تفنگ جنگی داشته باشد و در روز شلیک کند، تیر به خودش برمیگردد. روی نقش ساقی بیاثر است.",
                    team = "Mafia",
                    capabilitiesJson = """[{"name":"خرابکاری تفنگ (خرابکار) 🔫","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["SABOTAGE"]"""
                ),
                RoleEntity(
                    name = "هزارچهره 🎭",
                    description = "شخص مستقل بازی که نقش هر کس که حذف می‌شود را تا شب بعد تصاحب می‌کند",
                    team = "Independent",
                    capabilitiesJson = """[{"name":"تقلید نقش 🎭","totalCount":3,"remainingCount":3}]"""
                ),
                RoleEntity(
                    name = "نوستراداموس 🔮",
                    description = "شخص مستقلی که در ابتدای بازی تعداد و شانس برد مافیا یا شهروند را پیش‌بینی می‌کند",
                    team = "Independent",
                    capabilitiesJson = """[{"name":"رؤیت آینده 🔮","totalCount":1,"remainingCount":1}]"""
                ),
                RoleEntity(
                    name = "چرچیل 🎩",
                    description = "شخص مستقل بازی با قابلیت شلیک شبانه که پزشک نجات‌یافتنی نیست. روی رویین‌تن و محافظ اثری ندارد.",
                    team = "Independent",
                    capabilitiesJson = """[{"name":"شلیک شبانه ⚔️","totalCount":10,"remainingCount":10}]""",
                    abilitiesJson = """["CHURCHILL_SHOOT"]"""
                ),
                RoleEntity(
                    name = "مجهول 👤❓",
                    description = "یک نقش مستقل و خنثی که بیدار نمی‌شود. اگر مورد هدف مافیا قرار گیرد به [مافیای ساده] و اگر مورد هدف شهروند یا مستقل قرار گیرد به [شهروند ساده] تبدیل می‌شود.",
                    team = "Independent",
                    capabilitiesJson = "[]",
                    abilitiesJson = "[]"
                )
            )
            mafiaDao.insertRoles(defaults)
        }
    }
}
