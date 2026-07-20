package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.GameHistoryEntity
import com.example.data.model.GameSessionEntity
import com.example.data.model.RoleCapability
import com.example.data.repository.MafiaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ImportedRole(
    val name: String,
    val count: String
)

@Serializable
data class FullExportData(
    val players: List<PlayerEntity> = emptyList(),
    val roles: List<RoleEntity> = emptyList()
)

@Serializable
data class ImportedSetup(
    val players: List<String> = emptyList(),
    val roles: List<ImportedRole> = emptyList()
)

@Serializable
data class LastMoveCard(
    val id: Int,
    val name: String,
    val description: String,
    val isSelected: Boolean = false,
    val isBurnt: Boolean = false
)

class MafiaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MafiaRepository
    val players: StateFlow<List<PlayerEntity>>
    val roles: StateFlow<List<RoleEntity>>
    val gameLogs: StateFlow<List<GameLogEntity>>
    val gameHistory: StateFlow<List<GameHistoryEntity>>
    val gameSessions: StateFlow<List<GameSessionEntity>>

    // Active game session ID (null/0 if new game)
    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId.asStateFlow()

    // Game state: "SETUP", "DISTRIBUTION", "PLAY"
    private val _gameStage = MutableStateFlow("SETUP")
    val gameStage: StateFlow<String> = _gameStage.asStateFlow()

    // Moderator Name state
    private val _moderatorName = MutableStateFlow("")
    val moderatorName: StateFlow<String> = _moderatorName.asStateFlow()

    fun setModeratorName(name: String) {
        _moderatorName.value = name.trim()
    }

    // Phase: "Day" / "Night" (used during "PLAY" stage)
    private val _gamePhase = MutableStateFlow("Night")
    val gamePhase: StateFlow<String> = _gamePhase.asStateFlow()

    // Inquiry (Status Check) State
    private val _totalInquiries = MutableStateFlow(3)
    val totalInquiries: StateFlow<Int> = _totalInquiries.asStateFlow()

    private val _remainingInquiries = MutableStateFlow(3)
    val remainingInquiries: StateFlow<Int> = _remainingInquiries.asStateFlow()

    fun setTotalInquiries(count: Int) {
        val coerced = count.coerceAtLeast(0)
        _totalInquiries.value = coerced
        _remainingInquiries.value = coerced
    }

    fun decrementInquiry() {
        if (_remainingInquiries.value > 0) {
            _remainingInquiries.value -= 1
            viewModelScope.launch(Dispatchers.IO) {
                repository.addLog("🔍 یک استعلام (Inquiry) توسط هماهنگ‌کننده استفاده شد. تعداد باقی‌مانده: ${_remainingInquiries.value}")
            }
        }
    }

    fun resetInquiries() {
        _remainingInquiries.value = _totalInquiries.value
    }

    // Selected player for direct settings card
    private val _selectedPlayerForSettings = MutableStateFlow<PlayerEntity?>(null)
    val selectedPlayerForSettings: StateFlow<PlayerEntity?> = _selectedPlayerForSettings.asStateFlow()

    private val _musketeerLiveGunExhausted = MutableStateFlow(false)
    val musketeerLiveGunExhausted: StateFlow<Boolean> = _musketeerLiveGunExhausted.asStateFlow()

    private val _sagiCooldownNight = MutableStateFlow(0)
    val sagiCooldownNight: StateFlow<Int> = _sagiCooldownNight.asStateFlow()

    private val _sagiPastTargets = MutableStateFlow<List<Int>>(emptyList())
    val sagiPastTargets: StateFlow<List<Int>> = _sagiPastTargets.asStateFlow()

    private val _isGravedigActiveThisNight = MutableStateFlow(false)
    val isGravedigActiveThisNight: StateFlow<Boolean> = _isGravedigActiveThisNight.asStateFlow()

    private val _natoWrongGuessesCount = MutableStateFlow(0)
    val natoWrongGuessesCount: StateFlow<Int> = _natoWrongGuessesCount.asStateFlow()

    fun setNatoWrongGuessesCount(count: Int) {
        _natoWrongGuessesCount.value = count
    }

    fun setGravedigActive(active: Boolean) {
        _isGravedigActiveThisNight.value = active
    }

    fun setMusketeerLiveGunExhausted(exhausted: Boolean) {
        _musketeerLiveGunExhausted.value = exhausted
    }

    // Last Move Cards list
    private val _lastMoveCards = MutableStateFlow(
        listOf(
            LastMoveCard(1, "شلیک نهایی 🔫", "در اول شب به جای مافیا این شخص شلیک می کند.", isSelected = false),
            LastMoveCard(2, "مسیر سبز 🟢", "فردی که مسیر سبز بگیرد به هیچ عنوان فردا در دفاعیه نمی رود.", isSelected = true),
            LastMoveCard(3, "فرش قرمز 🔴", "فردی که فرش قرمز بگیرد فردا مستقیم به دفاعیه می رود.", isSelected = true),
            LastMoveCard(4, "دروغ سیزده 🤥", "یک دروغ درباره خودش می گوید و گرداننده بازی تایید می کند.", isSelected = false),
            LastMoveCard(5, "افشای نقش 🔍", "این شخص یک نفر از بازیکنان را انتخاب می کند و گرداننده نقش آن شخص را برای همه افشا می کند.", isSelected = false),
            LastMoveCard(6, "خداحافظی کن 👋", "در بازی هیچ اتفاقی رخ نمی دهد و به ادامه بازی می پردازیم.", isSelected = false),
            LastMoveCard(7, "بی خوابی 🌅", "شب نمی شود و مستقیم به روز بعد می رویم.", isSelected = true),
            LastMoveCard(8, "ذهن زیبا 🧠", "اگر نقش کسی را دقیق حدس بزند در بازی می ماند.", isSelected = true),
            LastMoveCard(9, "اعلام حضور 📢", "اگر رییس مافیا در بازی باشد توسط گرداننده اعلام می شود.", isSelected = false),
            LastMoveCard(10, "روز محاکمه ⚖️", "فردا به محض اینکه روز شود رای گیری می شود و صحبتی انجام نمی شود.", isSelected = false),
            LastMoveCard(11, "شهر در امان 🛡️", "امشب در شب هر شهروندی به هر دلیلی کشته شود از بازی خارج نمی شود حتی اگر کلانتر و یا حرفه ای اشتباه بزنند.", isSelected = false),
            LastMoveCard(12, "روز جشن مافیا 🎉", "فردا توی روز هیچکس از بازی بیرون نمی رود.", isSelected = false),
            LastMoveCard(13, "روز سکوت 🤫", "یک نفر را انتخاب می کند و این شخص فردا سکوت است.", isSelected = false),
            LastMoveCard(14, "افشای هویت 👤", "نقش این شخص توسط گرداننده اعلام می شود.", isSelected = true),
            LastMoveCard(15, "نبش قبر ⚰️", "اگر کسی از بازی بیرون رفته به انتخاب این شخص نقشش گفته می شود.", isSelected = false),
            LastMoveCard(16, "سر شماری 📊", "تعداد مافیا و تعداد شهروند مانده در بازی اعلام می شود.", isSelected = false),
            LastMoveCard(17, "تسخیر روح 👻", "یک نفر را انتخاب کن و فقط به گرداننده بگو و آن بازیکن در بازی هست اما واقعا حذف شده نه قابلیتی دارد نه توی آمار هست و نه رای او شمرده میشه.", isSelected = false),
            LastMoveCard(18, "سرگیجه 🌀", "به صورت پنهانی یک نفر رو به گرداننده بگو این شخص ۲۴ ساعت از نقشش نمی تواند استفاده کند.", isSelected = false),
            LastMoveCard(19, "شانس آوردی 🍀", "از بازی خارج نمی شوی و در بازی می مانی.", isSelected = false),
            LastMoveCard(20, "بخت و اقبال 🎲", "یک نفر را انتخاب می کند و گرداننده بین این دو بازیکن قرعه مرگ میاندازد و کسی که ببرد تو بازی می ماند.", isSelected = false),
            LastMoveCard(21, "سکوت بره ها 🐑", "دو نفر را انتخاب کن و آن دو بازیکن فردا حق صحبت کردن ندارن.", isSelected = false),
            LastMoveCard(22, "تغییر چهره 🎭", "کسی که از بازی بیرون می رود نقشش را با یکی از بازیکنان به صورت کامل عوض می کند و آن بازیکن با نقش جدید ادامه می دهد.", isSelected = true),
            LastMoveCard(23, "حذف یا حقیقت ❓", "یک بازیکن را انتخاب می کند و آن شخص بین حذف و یا حقیقت انتخاب می کند. اگر حذف انتخاب کند از بازی حذف می شود و اگر حقیقت انتخاب کند از او یک سوال می پرسد و باید درست جواب دهد.", isSelected = false),
            LastMoveCard(24, "پرسش و پاسخ 💬", "فردا هرکس حق دارد از یک سوال از کس دیگری بپرسد و آن شخص پاسخ دهد و هیچکس حق صحبت دیگری ندارد.", isSelected = false),
            LastMoveCard(25, "دستبند 🔗", "بازیکن اخراجی با کارت دستبند هرکس را نشان کند توانمندی های ان شب را از وی میگیرد.", isSelected = false),
            LastMoveCard(26, "وصیت 📜", "شخص خارج شده از بازی جمله ایی به گرداننده بازی میگوید که به یکی از بازیکنان انتقال دهد.", isSelected = false)
        )
    )
    val lastMoveCards: StateFlow<List<LastMoveCard>> = _lastMoveCards.asStateFlow()

    fun addLastMoveCard(name: String, description: String) {
        val trimmedName = name.trim()
        val trimmedDesc = description.trim()
        if (trimmedName.isNotBlank()) {
            val nextId = (_lastMoveCards.value.maxOfOrNull { it.id } ?: 0) + 1
            val newCard = LastMoveCard(nextId, trimmedName, trimmedDesc, true)
            _lastMoveCards.value = _lastMoveCards.value + newCard
            viewModelScope.launch {
                repository.addLog("کارت حرکت آخر جدید افزوده شد: $trimmedName")
            }
        }
    }

    fun toggleLastMoveCardSelection(id: Int) {
        _lastMoveCards.value = _lastMoveCards.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun burnLastMoveCard(id: Int) {
        _lastMoveCards.value = _lastMoveCards.value.map {
            if (it.id == id) it.copy(isBurnt = true) else it
        }
        val cardName = _lastMoveCards.value.find { it.id == id }?.name ?: ""
        viewModelScope.launch {
            repository.addLog("🔥 کارت حرکت آخر «$cardName» سوخت و از بازی خارج شد.")
        }
    }

    fun resetLastMoveCards() {
        _lastMoveCards.value = listOf(
            LastMoveCard(1, "شلیک نهایی 🔫", "در اول شب به جای مافیا این شخص شلیک می کند.", isSelected = false),
            LastMoveCard(2, "مسیر سبز 🟢", "فردی که مسیر سبز بگیرد به هیچ عنوان فردا در دفاعیه نمی رود.", isSelected = true),
            LastMoveCard(3, "فرش قرمز 🔴", "فردی که فرش قرمز بگیرد فردا مستقیم به دفاعیه می رود.", isSelected = true),
            LastMoveCard(4, "دروغ سیزده 🤥", "یک دروغ درباره خودش می گوید و گرداننده بازی تایید می کند.", isSelected = false),
            LastMoveCard(5, "افشای نقش 🔍", "این شخص یک نفر از بازیکنان را انتخاب می کند و گرداننده نقش آن شخص را برای همه افشا می کند.", isSelected = false),
            LastMoveCard(6, "خداحافظی کن 👋", "در بازی هیچ اتفاقی رخ نمی دهد و به ادامه بازی می پردازیم.", isSelected = false),
            LastMoveCard(7, "بی خوابی 🌅", "شب نمی شود و مستقیم به روز بعد می رویم.", isSelected = true),
            LastMoveCard(8, "ذهن زیبا 🧠", "اگر نقش کسی را دقیق حدس بزند در بازی می ماند.", isSelected = true),
            LastMoveCard(9, "اعلام حضور 📢", "اگر رییس مافیا در بازی باشد توسط گرداننده اعلام می شود.", isSelected = false),
            LastMoveCard(10, "روز محاکمه ⚖️", "فردا به محض اینکه روز شود رای گیری می شود و صحبتی انجام نمی شود.", isSelected = false),
            LastMoveCard(11, "شهر در امان 🛡️", "امشب در شب هر شهروندی به هر دلیلی کشته شود از بازی خارج نمی شود حتی اگر کلانتر و یا حرفه ای اشتباه بزنند.", isSelected = false),
            LastMoveCard(12, "روز جشن مافیا 🎉", "فردا توی روز هیچکس از بازی بیرون نمی رود.", isSelected = false),
            LastMoveCard(13, "روز سکوت 🤫", "یک نفر را انتخاب می کند و این شخص فردا سکوت است.", isSelected = false),
            LastMoveCard(14, "افشای هویت 👤", "نقش این شخص توسط گرداننده اعلام می شود.", isSelected = true),
            LastMoveCard(15, "نبش قبر ⚰️", "اگر کسی از بازی بیرون رفته به انتخاب این شخص نقشش گفته می شود.", isSelected = false),
            LastMoveCard(16, "سر شماری 📊", "تعداد مافیا و تعداد شهروند مانده در بازی اعلام می شود.", isSelected = false),
            LastMoveCard(17, "تسخیر روح 👻", "یک نفر را انتخاب کن و فقط به گرداننده بگو و آن بازیکن در بازی هست اما واقعا حذف شده نه قابلیتی دارد نه توی آمار هست و نه رای او شمرده میشه.", isSelected = false),
            LastMoveCard(18, "سرگیجه 🌀", "به صورت پنهانی یک نفر رو به گرداننده بگو این شخص ۲۴ ساعت از نقشش نمی تواند استفاده کند.", isSelected = false),
            LastMoveCard(19, "شانس آوردی 🍀", "از بازی خارج نمی شوی و در بازی می مانی.", isSelected = false),
            LastMoveCard(20, "بخت و اقبال 🎲", "یک نفر را انتخاب می کند و گرداننده بین این دو بازیکن قرعه مرگ میاندازد و کسی که ببرد تو بازی می ماند.", isSelected = false),
            LastMoveCard(21, "سکوت بره ها 🐑", "دو نفر را انتخاب کن و آن دو بازیکن فردا حق صحبت کردن ندارن.", isSelected = false),
            LastMoveCard(22, "تغییر چهره 🎭", "کسی که از بازی بیرون می رود نقشش را با یکی از بازیکنان به صورت کامل عوض می کند و آن بازیکن با نقش جدید ادامه می دهد.", isSelected = true),
            LastMoveCard(23, "حذف یا حقیقت ❓", "یک بازیکن را انتخاب می کند و آن شخص بین حذف و یا حقیقت انتخاب می کند. اگر حذف انتخاب کند از بازی حذف می شود و اگر حقیقت انتخاب کند از او یک سوال می پرسد و باید درست جواب دهد.", isSelected = false),
            LastMoveCard(24, "پرسش و پاسخ 💬", "فردا هرکس حق دارد از یک سوال از کس دیگری بپرسد و آن شخص پاسخ دهد و هیچکس حق صحبت دیگری ندارد.", isSelected = false),
            LastMoveCard(25, "دستبند 🔗", "بازیکن اخراجی با کارت دستبند هرکس را نشان کند توانمندی های ان شب را از وی میگیرد.", isSelected = false),
            LastMoveCard(26, "وصیت 📜", "شخص خارج شده از بازی جمله ایی به گرداننده بازی میگوید که به یکی از بازیکنان انتقال دهد.", isSelected = false)
        )
    }

    // Preset capabilities
    private val _capabilityTemplates = MutableStateFlow(
        listOf("استعلام وضعیت 🔍", "شفا / نجات 🩺", "شلیک شبانه 🔫", "سلاخی 🔪", "تفنگ جنگی ⚔️", "تفنگ مشقی 🔫", "سکوت فردا 🔇", "محافظ زره 🛡️", "خریداری (مذاکره) 🤝", "انفجار انتحاری 💣")
    )
    val capabilityTemplates: StateFlow<List<String>> = _capabilityTemplates.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MafiaRepository(database.mafiaDao())

        players = repository.allPlayers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        roles = repository.allRoles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        gameLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        gameHistory = repository.allGameHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        gameSessions = repository.allGameSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            try {
                repository.seedDefaultRolesIfNeeded()
            } catch (e: Exception) {
                // Safely log or ignore seed issues, since local DB might be loading
            }
        }
    }

    // --- Player Management ---
    fun addPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val player = PlayerEntity(name = name.trim(), isSelected = true)
                repository.insertPlayer(player)
                repository.addLog("بازیکن جدید اضافه شد: $name")
            } catch (e: Exception) {
                // Safely prevent process crashes on DB failures
            }
        }
    }

    fun togglePlayerSelection(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(isSelected = !p.isSelected)
            repository.updatePlayer(updated)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlayer(player)
            repository.addLog("بازیکن حذف شد: ${player.name}")
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePlayer(player)
        }
    }

    fun updatePlayerNote(id: Int, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(note = note)
            repository.updatePlayer(updated)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun selectPlayerForSettings(player: PlayerEntity?) {
        _selectedPlayerForSettings.value = player
    }

    // --- Role Configuration ---
    fun updateRoleCount(id: Int, count: Int) {
        if (count < 0) return
        viewModelScope.launch(Dispatchers.IO) {
            val roleList = roles.value
            val role = roleList.find { it.id == id } ?: return@launch
            val updated = role.copy(count = count)
            repository.updateRole(updated)
        }
    }

    fun updateRoleCapabilities(id: Int, capabilitiesJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val roleList = roles.value
            val role = roleList.find { it.id == id } ?: return@launch
            val updated = role.copy(capabilitiesJson = capabilitiesJson)
            repository.updateRole(updated)
        }
    }

    fun updateRoleCountAndCapabilities(id: Int, count: Int, capabilitiesJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val roleList = roles.value
            val role = roleList.find { it.id == id } ?: return@launch
            val updated = role.copy(count = count, capabilitiesJson = capabilitiesJson)
            repository.updateRole(updated)
        }
    }

    fun updateRoleAbilities(roleId: Int, abilitiesJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val roleList = roles.value
            val role = roleList.find { it.id == roleId } ?: return@launch
            val updated = role.copy(abilitiesJson = abilitiesJson)
            repository.updateRole(updated)
            repository.addLog("قابلیت‌های نقش «${role.name}» با موفقیت ویرایش شد ⚙️")
        }
    }

    fun deleteRole(role: RoleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRole(role)
            repository.addLog("نقش سفارشی حذف شد: ${role.name}")
        }
    }

    fun updateRoleFull(id: Int, name: String, team: String, description: String, iconName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val roleList = roles.value
            val role = roleList.find { it.id == id } ?: return@launch
            val updated = role.copy(
                name = name.trim(),
                team = team,
                description = description.trim(),
                iconName = iconName
            )
            repository.updateRole(updated)
            repository.addLog("نقش بروزرسانی شد: ${updated.name}")
        }
    }

    fun addCustomRole(name: String, team: String, description: String, iconName: String, capabilitiesJson: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val role = RoleEntity(
                name = name.trim(),
                team = team,
                description = description.trim(),
                iconName = iconName,
                count = 1,
                capabilitiesJson = capabilitiesJson
            )
            repository.insertRole(role)
            repository.addLog("نقش سفارشی جدید اضافه شد: ${role.name} 🎭")
        }
    }

    fun resetRolesToDefaults() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllRoles()
            repository.seedDefaultRolesIfNeeded()
            repository.addLog("تنظیمات نقش‌ها به حالت حافظه پیش‌فرض بازگشت 🎭")
        }
    }

    // --- Predefined Capabilities ---
    fun addCapabilityTemplate(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_capabilityTemplates.value.contains(trimmed)) {
            _capabilityTemplates.value = _capabilityTemplates.value + trimmed
        }
    }

    fun deleteCapabilityTemplate(name: String) {
        _capabilityTemplates.value = _capabilityTemplates.value.filter { it != name }
    }

    fun resetCapabilityTemplatesToDefaults() {
        _capabilityTemplates.value = listOf(
            "استعلام وضعیت 🔍", "شفا / نجات 🩺", "شلیک شبانه 🔫", "سلاخی 🔪", "تفنگ جنگی ⚔️", "تفنگ مشقی 🔫", "سکوت فردا 🔇", "محافظ زره 🛡️", "خریداری (مذاکره) 🤝", "انفجار انتحاری 💣"
        )
    }

    // --- Game Logic Sequences ---
    fun setGameStage(stage: String) {
        _gameStage.value = stage
    }

    fun distributeRolesAndStartGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val chosenPlayers = players.value.filter { it.isSelected }
            val chosenRoles = roles.value.filter { it.count > 0 }

            val totalRolesCount = chosenRoles.sumOf { it.count }
            if (chosenPlayers.size != totalRolesCount) {
                repository.addLog("خطا: تعداد بازیکنان انتخاب شده (${chosenPlayers.size}) با تعداد نقش‌های تعریف شده ($totalRolesCount) مطابقت ندارد!")
                return@launch
            }

            // Shuffle roles sequence
            val roleAssignmentList = mutableListOf<RoleEntity>()
            chosenRoles.forEach { role ->
                repeat(role.count) {
                    roleAssignmentList.add(role)
                }
            }
            roleAssignmentList.shuffle()

            repository.clearLogs()
            repository.addLog("--- شروع بازی مافیا گاد 🎭 ---")
            if (_moderatorName.value.isNotBlank()) {
                repository.addLog("🎤 گرداننده (خدا) بازی: ${_moderatorName.value}")
            }

            // Distribute
            val updatedPlayers = chosenPlayers.mapIndexed { index, player ->
                val assignedRole = roleAssignmentList[index]
                player.copy(
                    isAlive = true,
                    assignedRoleId = assignedRole.id,
                    assignedRoleName = assignedRole.name,
                    assignedRoleTeam = assignedRole.team,
                    capabilitiesJson = assignedRole.capabilitiesJson,
                    isBlocked = false,
                    isMuted = false,
                    isVoteRevoked = false,
                    isSaved = false,
                    voteCount = 0,
                    note = "",
                    warningsCount = 0
                )
            }

            // Save unselected players as inactive / or delete them? Keep them unchanged but clear their role assignments
            val unselectedPlayers = players.value.filter { !it.isSelected }.map {
                it.copy(assignedRoleId = null, assignedRoleName = null, assignedRoleTeam = null, capabilitiesJson = "", isAlive = false)
            }

            repository.insertPlayers(updatedPlayers + unselectedPlayers)
            _remainingInquiries.value = _totalInquiries.value
            _gameStage.value = "DISTRIBUTION"
            _gamePhase.value = "Night"
            repository.addLog("کارت نقش‌ها با موفقیت مابین بازیکنان توزیع شد 🃏")
        }
    }

    fun advanceToPlayStage() {
        _gameStage.value = "PLAY"
        _gamePhase.value = "Day"
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    // --- Night Events & Instant Actions ---
    fun registerNightEvent(playerId: Int, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(playerId) ?: return@launch
            val killerRolePrefix = "KILL_BY_ROLE_"
            val isTargetKiller = p.assignedRoleName?.contains("کیلر") == true
            val isTargetChurchill = p.assignedRoleName?.contains("چرچیل") == true
            val isTargetImmune = isTargetKiller || isTargetChurchill
            val roleLabel = if (isTargetKiller) "کیلر" else "چرچیل"
            when {
                type == "KILL" -> {
                    if (isTargetImmune) {
                        val updated = p.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updated)
                        repository.addLog("🛡️ به بازیکن «${p.name}» ($roleLabel) سوءقصد شد، اما به دلیل مصونیت شبانه جان سالم به در برد.")
                    } else if (p.isHealedThisNight) {
                        val updated = p.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updated)
                        repository.addLog("🛡️ به بازیکن «${p.name}» شلیک شد اما به دلیل شفا/نجات پزشک جان سالم به در برد.")
                    } else {
                        val updated = p.copy(isShotThisNight = true, isAlive = false)
                        repository.updatePlayer(updated)
                        repository.addLog("💀 بازیکن «${p.name}» در شب شلیک یا کشته شد.")
                    }
                }
                type == "KILL_BY_RULE" -> {
                    val updated = p.copy(isShotThisNight = true, isAlive = false)
                    repository.updatePlayer(updated)
                    repository.addLog("💀 بازیکن «${p.name}» در شب بدلیل استفاده اشتباه از نقش کشته شد.")
                }
                type.startsWith(killerRolePrefix) -> {
                    val killerRole = type.substring(killerRolePrefix.length)
                    if (isTargetImmune) {
                        val updated = p.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updated)
                        repository.addLog("🛡️ بازیکن «${p.name}» ($roleLabel) مورد سوءقصد [$killerRole] قرار گرفت، اما به دلیل مصونیت شبانه زنده ماند.")
                    } else if (p.isHealedThisNight) {
                        val updated = p.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updated)
                        repository.addLog("🛡️ بازیکن «${p.name}» مورد سوءقصد [$killerRole] قرار گرفت، اما شفا/نجات پزشک او را سر پا نگه داشت.")
                    } else {
                        val updated = p.copy(isShotThisNight = true, isAlive = false)
                        repository.updatePlayer(updated)
                        repository.addLog("💀 بازیکن «${p.name}» در شب توسط [$killerRole] کشته و حذف شد.")
                    }
                }
                type == "SLAUGHTER" -> {
                    val isChurchill = p.assignedRoleName?.contains("چرچیل") == true
                    val currentNight = gameLogs.value.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
                    val isNight1 = currentNight == 1 || currentNight == 0
                    if (isChurchill && isNight1) {
                        val updated = p.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updated)
                        repository.addLog("🛡️ اقدام ناموفق: بازیکن «${p.name}» (چرچیل) در شب اول مورد سلاخی قرار گرفت، اما به دلیل مصونیت مطلق شب اول جان سالم به در برد.")
                    } else {
                        val updated = p.copy(
                            isAlive = false,
                            isSlaughtered = true,
                            isSaved = false,
                            isBlocked = false,
                            isMuted = false
                        )
                        repository.updatePlayer(updated)
                        repository.addLog("🔪 بازیکن «${p.name}» سلاخی و از بازی حذف گردید.")
                    }
                }
                type == "MUTE" -> {
                    val updated = p.copy(isMuted = true)
                    repository.updatePlayer(updated)
                    repository.addLog("🔇 بازیکن «${p.name}» تا انتهای روز بعد سایلنت و سکوت شد.")
                }
                type == "BLOCK" -> {
                    val updated = p.copy(isBlocked = true)
                    repository.updatePlayer(updated)
                    repository.addLog("🚫 بازیکن «${p.name}» بسته شد (بلاک گردید).")
                }
                type == "SAVE" -> {
                    val isDoctor = p.assignedRoleName?.contains("دکتر") == true && p.assignedRoleName?.contains("لکتور") == false
                    if (isDoctor && p.doctorSelfSavesCount >= 2) {
                        repository.addLog("⚠️ خطا: دکتر قبلاً ۲ بار خود را نجات داده است و دیگر نمی‌تواند خود را نجات دهد.")
                    } else {
                        val newSelfSaves = if (isDoctor) p.doctorSelfSavesCount + 1 else p.doctorSelfSavesCount
                        if (p.isSlaughtered) {
                            val updated = p.copy(
                                isSaved = true,
                                isHealedThisNight = true,
                                doctorSelfSavesCount = newSelfSaves
                            )
                            repository.updatePlayer(updated)
                            repository.addLog("⚠️ تلاش پزشک برای نجات «${p.name}» ناکام ماند (این بازیکن سلاخی شده است و نجات روی او بی‌اثر است).")
                        } else {
                            val updated = p.copy(
                                isSaved = true,
                                isHealedThisNight = true,
                                isAlive = if (p.isShotThisNight) true else p.isAlive,
                                doctorSelfSavesCount = newSelfSaves
                            )
                            repository.updatePlayer(updated)
                            repository.addLog("🩺 بازیکن «${p.name}» نجات داده شد یا مورد شفا قرار گرفت.")
                        }
                    }
                    if (_selectedPlayerForSettings.value?.id == playerId) {
                        _selectedPlayerForSettings.value = repository.getPlayerById(playerId)
                    }
                }
                type == "INC_VOTE" -> {
                    val updated = p.copy(voteCount = p.voteCount + 1)
                    repository.updatePlayer(updated)
                }
                type == "DEC_VOTE" -> {
                    val updated = p.copy(voteCount = (p.voteCount - 1).coerceAtLeast(0))
                    repository.updatePlayer(updated)
                }
                type == "MARK_KILLED" -> {
                    val updated = p.copy(isKilledToday = true)
                    repository.updatePlayer(updated)
                    repository.addLog("💀 بازیکن «${p.name}» برچسب کشته امروز گرفت (در وضعیت تعلیق اعدام/شات).")
                }
                type == "UNMARK_KILLED" -> {
                    val updated = p.copy(isKilledToday = false)
                    repository.updatePlayer(updated)
                    repository.addLog("💖 بازیکن «${p.name}» نجات یافت و برچسب کشته امروز برداشته شد.")
                }
                type == "REVOKE_VOTE" -> {
                    val updated = p.copy(isVoteRevoked = true)
                    repository.updatePlayer(updated)
                    repository.addLog("⚖️ حق رأی انضباطی بازیکن «${p.name}» برای امروز سلب شد.")
                }
                type == "RESTORE_VOTE" -> {
                    val updated = p.copy(isVoteRevoked = false)
                    repository.updatePlayer(updated)
                    repository.addLog("✅ حق رأی بازیکن «${p.name}» بازگردانده شد.")
                }
                type == "UNMUTE" -> {
                    val updated = p.copy(isMuted = false)
                    repository.updatePlayer(updated)
                    repository.addLog("🔊 سکوت بازیکن «${p.name}» برداشته شد و آزادانه می‌تواند صحبت کند.")
                }
                type == "UNBLOCK" -> {
                    val updated = p.copy(isBlocked = false)
                    repository.updatePlayer(updated)
                    repository.addLog("✅ بلاک بازیکن «${p.name}» لغو شد (رفع مسدود).")
                }
            }
        }
    }

    fun togglePlayerBlock(id: Int) {
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(isBlocked = !p.isBlocked)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(isBlocked = !p.isBlocked)
            repository.updatePlayer(updated)
            repository.addLog("وضعیت بلاک بازیکن «${p.name}» تغییر یافت: ${if (updated.isBlocked) "مسدود 🚫" else "آزاد ✅"}")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun togglePlayerMute(id: Int) {
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(isMuted = !p.isMuted)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(isMuted = !p.isMuted)
            repository.updatePlayer(updated)
            repository.addLog("وضعیت سکوت بازیکن «${p.name}» تغییر یافت: ${if (updated.isMuted) "سکوت 🔇" else "گویا 🗣️"}")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun togglePlayerVoteRevoke(id: Int) {
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(isVoteRevoked = !p.isVoteRevoked)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = if (p.isVoteRevoked) {
                p.copy(isVoteRevoked = false)
            } else {
                p.copy(isVoteRevoked = true)
            }
            repository.updatePlayer(updated)
            if (updated.isVoteRevoked) {
                repository.addLog("⚖️ حق رأی انضباطی بازیکن «${p.name}» برای امروز سلب شد.")
            } else {
                repository.addLog("✅ حق رأی بازیکن «${p.name}» بازگردانده شد.")
            }
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun togglePlayerLife(id: Int) {
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(isAlive = !p.isAlive)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(isAlive = !p.isAlive)
            repository.updatePlayer(updated)
            repository.addLog("بازیکن «${p.name}» تغییر وضعیت حیات داد: ${if (updated.isAlive) "زنده 🟢" else "حذف شده ⚰️"}")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun updatePlayerWarnings(id: Int, count: Int) {
        val newCount = count.coerceIn(0, 3)
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(warningsCount = newCount)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(warningsCount = newCount)
            repository.updatePlayer(updated)
            repository.addLog("تعداد اخطارهای بازیکن «${p.name}» به $newCount تغییر یافت ⚠️")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun eliminatePlayerWithReason(id: Int, reasonType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val reasonText = when (reasonType) {
                "VOTE" -> "رأی‌گیری عمومی مجلس ⚖️"
                "DISCIPLINARY" -> "تصمیم انضباطی گاد 🛑"
                "RIFLE" -> "تنبلی تفنگ جنگی ⚔️"
                else -> "دلیل منطقی / شات بازی 🎯"
            }
            val updated = if (reasonType == "RIFLE") {
                p.copy(isAlive = false, hasCombatGun = false, hasLiveGunThisRound = false, usedLiveGun = true)
            } else {
                p.copy(isAlive = false)
            }
            repository.updatePlayer(updated)
            repository.addLog("💀 بازیکن «${p.name}» به علت [$reasonText] از بازی حذف شد.")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun executeTerroristAction(terroristId: Int, victimId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val terrorist = repository.getPlayerById(terroristId)
            val victim = repository.getPlayerById(victimId)
            if (terrorist != null) {
                val updatedTerrorist = terrorist.copy(isAlive = false)
                repository.updatePlayer(updatedTerrorist)
                repository.addLog("💀 بازیکن «${terrorist.name}» (تروریست) با رای مجلس از بازی حذف شد.")
                if (_selectedPlayerForSettings.value?.id == terroristId) {
                    _selectedPlayerForSettings.value = updatedTerrorist
                }
            }
            if (victim != null) {
                val updatedVictim = victim.copy(isAlive = false)
                repository.updatePlayer(updatedVictim)
                repository.addLog("💥 تروریست عملیات انتحاری انجام داد و «${victim.name}» را با خود برد!")
                if (_selectedPlayerForSettings.value?.id == victimId) {
                    _selectedPlayerForSettings.value = updatedVictim
                }
            }
        }
    }

    fun revivePlayerWithReason(id: Int, reasonType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val reasonText = when (reasonType) {
                "RETURN" -> "برگشت به بازی (توسط نقش یا گاد) 🤝"
                else -> "زنده شدن و احیا 😇"
            }
            val updated = p.copy(isAlive = true)
            repository.updatePlayer(updated)
            repository.addLog("😇 بازیکن «${p.name}» با موفقیت نجات یافت و احیا شد (علت: [$reasonText]).")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun setPlayerKilledToday(id: Int, killed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(isKilledToday = killed)
            repository.updatePlayer(updated)
            if (killed) {
                repository.addLog("💀 بازیکن «${p.name}» برچسب کشته شده روز گرفت (منتظر نجات یا کارت حرکت پایانی).")
            } else {
                repository.addLog("💖 برچسب کشته شده روز از «${p.name}» برداشته شد (نجات یافت).")
            }
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun saveGameOverReport(winnerTeam: String, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val winnerNameText = when (winnerTeam) {
                "Citizen" -> "تیم شهروندان 🕊️"
                "Mafia" -> "تیم مافیا 🕶️"
                else -> "ساید مستقل 🎭"
            }
            repository.addLog("🏆 بازی با پیروزی [$winnerNameText] به پایان رسید!")
            repository.addLog("📝 علت اتمام بازی: $reason")
            repository.addLog("--- پایان رسمی تاریخچه بازی 🔚 ---")
            
            // Save to GameHistory
            val playersJson = Json.encodeToString(players.value)
            val logsJson = Json.encodeToString(gameLogs.value)
            val historyEntry = com.example.data.model.GameHistoryEntity(
                winnerTeam = winnerTeam,
                reason = reason,
                playersJson = playersJson,
                logsJson = logsJson,
                moderatorName = _moderatorName.value
            )
            repository.insertGameHistory(historyEntry)

            // Also save as FINISHED game session
            val currentId = _activeSessionId.value ?: 0
            val sessionEntry = com.example.data.model.GameSessionEntity(
                id = currentId,
                status = "FINISHED_$winnerTeam",
                moderatorName = _moderatorName.value,
                gameStage = _gameStage.value,
                gamePhase = _gamePhase.value,
                playersJson = playersJson,
                logsJson = logsJson,
                rolesJson = Json.encodeToString(roles.value),
                remainingInquiries = _remainingInquiries.value,
                totalInquiries = _totalInquiries.value,
                sagiCooldownNight = _sagiCooldownNight.value,
                sagiPastTargetsJson = Json.encodeToString(_sagiPastTargets.value),
                isGravedigActiveThisNight = _isGravedigActiveThisNight.value,
                natoWrongGuessesCount = _natoWrongGuessesCount.value,
                musketeerLiveGunExhausted = _musketeerLiveGunExhausted.value
            )
            if (currentId != 0) {
                repository.updateGameSession(sessionEntry)
            } else {
                val savedId = repository.insertGameSession(sessionEntry)
                _activeSessionId.value = savedId.toInt()
            }
        }
    }

    fun usePlayerCapability(playerId: Int, capabilityName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = players.value.find { it.id == playerId } ?: return@launch
            if (p.capabilitiesJson.isBlank()) return@launch
            try {
                val caps = Json.decodeFromString<List<RoleCapability>>(p.capabilitiesJson)
                val updatedCaps = caps.map { cap ->
                    if (cap.name == capabilityName && cap.remainingCount > 0) {
                        cap.copy(remainingCount = cap.remainingCount - 1)
                    } else cap
                }
                val updatedJson = Json.encodeToString(updatedCaps)
                val updatedPlayer = p.copy(capabilitiesJson = updatedJson)
                repository.updatePlayer(updatedPlayer)
                repository.addLog("⚡ قابلیت «$capabilityName» توسط «${p.name}» استفاده گردید.")
                if (_selectedPlayerForSettings.value?.id == playerId) {
                    _selectedPlayerForSettings.value = updatedPlayer
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun toggleLastMoveCard(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(hasUsedLastMoveCard = !p.hasUsedLastMoveCard)
            repository.updatePlayer(updated)
            repository.addLog("کارت حرکت آخر برای «${p.name}» ویرایش شد.")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    fun updatePlayerVote(id: Int, count: Int) {
        val coercedCount = count.coerceAtLeast(0)
        players.value.find { it.id == id }?.let { p ->
            val updated = p.copy(voteCount = coercedCount)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val p = repository.getPlayerById(id) ?: return@launch
            val updated = p.copy(voteCount = coercedCount)
            repository.updatePlayer(updated)
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
            }
        }
    }

    // --- Phase Navigation ---
    fun toggleGamePhase() {
        val nextPhase = if (_gamePhase.value == "Day") "Night" else "Day"
        _gamePhase.value = nextPhase
        viewModelScope.launch(Dispatchers.IO) {
            repository.addLog("--- فاز بازی به «${if (nextPhase == "Day") "روز ☀️" else "شب 🌙"}» تغییر یافت ---")

            if (nextPhase == "Night") {
                // If entering night, we reset temporary day metrics like votes, confirm unresolved day deaths, and reset daily disciplinary actions
                val updated = players.value.map {
                    var finalAlive = if (it.isKilledToday) false else it.isAlive
                    if (it.isKilledToday) {
                        repository.addLog("💀 مرگ بازیکن «${it.name}» در روز جاری به دلیل عدم ثبت نجات، نهایی شد.")
                    }
                    if (it.willDieNextNight && finalAlive) {
                        finalAlive = false
                        repository.addLog("💀 بازیکن «${it.name}» (${it.assignedRoleName ?: "همشهری کین"}) به علت استفاده از قابلیت خود (جان فدا) قربانی شد.")
                    }
                    if (it.hasCombatGun && finalAlive) {
                        finalAlive = false
                        repository.addLog("☠️ جریمه تفنگ جنگی بلااستفاده: بازیکن «${it.name}» به علت عدم استفاده از تفنگ جنگی خود پیش از پایان فاز، کشته شد.")
                    }
                    it.copy(
                        voteCount = 0,
                        isSaved = false,  // reset night buffers as we enter custom night
                        isHealedThisNight = false,
                        isShotThisNight = false,
                        isInsuredThisNight = false,
                        isAlive = finalAlive,
                        willDieNextNight = false, // Reset penalty flag as it is now executed
                        isRevealedMafia = false, // Reset the night-revealed mafia status
                        isRevivedThisNight = false, // Reset the night-revived status
                        wasBlockedLastNight = false,
                        isKilledToday = false,
                        isMuted = false, // Day ends, mute expires
                        isVoteRevoked = false, // Day ends, vote restriction expires
                        hasBlankGunThisRound = false, // Day to Night cleanup (Part 5)
                        hasLiveGunThisRound = false, // Day to Night cleanup (Part 5)
                        hasBlankGun = false,
                        hasCombatGun = false,
                        usedLiveGun = false, // Day to Night cleanup (Part 5)
                        isSilencedThisRound = false,
                        isSabotaged = false
                    )
                }
                repository.insertPlayers(updated)
            } else {
                // If entering day, we check if player with live gun was killed during night
                var liveGunSurvived = false
                val updated = players.value.map { player ->
                    var hasLiveGun = player.hasLiveGunThisRound
                    var hasCombat = player.hasCombatGun
                    if (hasLiveGun) {
                        if (!player.isAlive) {
                            hasLiveGun = false // Clear flag, gun is returned! (Part 2)
                            hasCombat = false
                            repository.addLog("🔄 بازیکن تفنگدار با تفنگ جنگی «${player.name}» دیشب کشته شد، تفنگ جنگی به تفنگدار بازگردانده شد.")
                        } else {
                            liveGunSurvived = true // Survived, lock Musketeer's ability! (Part 2)
                            repository.addLog("🔒 بازیکن «${player.name}» تفنگ جنگی را با خود به روز برد. قابلیت تفنگ جنگی تفنگدار برای ادامه بازی قفل شد.")
                        }
                    }
                    var hasBlank = player.hasBlankGun
                    if (player.hasBlankGunThisRound && !player.isAlive) {
                        hasBlank = false
                    }
                    player.copy(
                        isBlocked = false, // Night ends, block expires
                        wasBlockedLastNight = player.isBlockedThisNight,
                        isBlockedThisNight = false, // Reset Matador's night block
                        isInsuredThisNight = false,
                        hasLiveGunThisRound = hasLiveGun,
                        hasCombatGun = hasCombat,
                        hasBlankGun = hasBlank
                    )
                }
                if (liveGunSurvived) {
                    _musketeerLiveGunExhausted.value = true
                }
                repository.insertPlayers(updated)
            }
        }
    }

    // --- Reset All Game State ---
    fun resetGame() {
        viewModelScope.launch(Dispatchers.IO) {
            // Restore all players to alive, no assignments
            val restored = players.value.map {
                it.copy(
                    isAlive = true,
                    assignedRoleId = null,
                    assignedRoleName = null,
                    assignedRoleTeam = null,
                    isBlocked = false,
                    isMuted = false,
                    isVoteRevoked = false,
                    isSaved = false,
                    isHealedThisNight = false,
                    isShotThisNight = false,
                    isBlockedThisNight = false,
                    wasBlockedLastNight = false,
                    isInsuredThisNight = false,
                    doctorSelfSavesCount = 0,
                    capabilitiesJson = "",
                    hasUsedLastMoveCard = false,
                    note = "",
                    voteCount = 0,
                    warningsCount = 0,
                    isSlaughtered = false,
                    isRevealedMafia = false,
                    willDieNextNight = false,
                    isRevivedThisNight = false,
                    hasBlankGunThisRound = false,
                    hasLiveGunThisRound = false,
                    hasBlankGun = false,
                    hasCombatGun = false,
                    usedLiveGun = false,
                    isSilencedThisRound = false,
                    isBulletproof = false,
                    isProtected = false
                )
            }
            repository.insertPlayers(restored)
            repository.clearLogs()
            _musketeerLiveGunExhausted.value = false
            _sagiCooldownNight.value = 0
            _sagiPastTargets.value = emptyList()
            _isGravedigActiveThisNight.value = false
            _natoWrongGuessesCount.value = 0
            _remainingInquiries.value = _totalInquiries.value
            _gameStage.value = "SETUP"
            _gamePhase.value = "Night"
            _selectedPlayerForSettings.value = null
            _activeSessionId.value = null
            repository.addLog("کل سوابق بازی بازنشانی شد و مجدداً به صفحه تنظیمات بازگشتیم 🔄")
        }
    }

    fun startNewGameSession() {
        _activeSessionId.value = null
        resetGame()
    }

    // --- Import / Export Configurations (JSON) ---
    fun exportSetupAsJson(): String {
        return try {
            val data = FullExportData(
                players = players.value,
                roles = roles.value
            )
            Json.encodeToString(data)
        } catch (e: Exception) {
            ""
        }
    }

    fun importSetupFromJson(jsonStr: String): Boolean {
        if (jsonStr.isBlank()) return false
        return try {
            try {
                // Try FullExportData first
                val fullData = Json.decodeFromString<FullExportData>(jsonStr)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        repository.deleteAllPlayers()
                        // Set IDs to 0 so that Room auto-generates sequential keys properly
                        repository.insertPlayers(fullData.players.map { it.copy(id = 0) })
                        repository.deleteAllRoles()
                        repository.insertRoles(fullData.roles.map { it.copy(id = 0) })
                        repository.addLog("پیکربندی کامل بازی و سناریو با موفقیت ایمپورت گردید 📥")
                    } catch (dbEx: Exception) {
                        repository.addLog("خطا در همگام‌سازی بازی ایمپورت شده با پایگاه داده ❌")
                    }
                }
                true
            } catch (ex: Exception) {
                // Fallback to simpler format
                val imported = Json.decodeFromString<ImportedSetup>(jsonStr)
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        repository.deleteAllPlayers()
                        val newPlayers = imported.players.map { name ->
                            PlayerEntity(name = name.trim(), isSelected = true)
                        }
                        repository.insertPlayers(newPlayers)

                        val allDbRoles = repository.getAllRolesList()
                        val updatedDbRoles = allDbRoles.map { dbRole ->
                            val importedRole = imported.roles.find { it.name == dbRole.name }
                            val countVal = importedRole?.count?.toIntOrNull() ?: 0
                            dbRole.copy(count = countVal)
                        }
                        repository.deleteAllRoles()
                        repository.insertRoles(updatedDbRoles)
                        repository.addLog("پیکربندی سناریو و بازیکنان (فرمت سازگار قدیمی) ایمپورت گردید 📥")
                    } catch (dbEx2: Exception) {
                        repository.addLog("خطا در همگام‌سازی داده‌های قدیمی ایمپورت شده با پایگاه داده ❌")
                    }
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun insurePlayer(insurerId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val insurer = repository.getPlayerById(insurerId) ?: return@launch
            val target = checkAndTransformMajhool(insurerId, targetId)

            if (insurer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت بیمه‌کننده «${insurer.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (insurer.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(insurer.capabilitiesJson)
                    val insureCap = caps.find { it.name.contains("بیمه") }
                    if (insureCap != null && insureCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: بیمه‌کننده دیگر بیمه مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement Insurer's capability count
            if (insurer.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(insurer.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("بیمه") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedInsurer = insurer.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedInsurer)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Mark target player as insured
            val updatedTarget = target.copy(isInsuredThisNight = true)
            repository.updatePlayer(updatedTarget)
            repository.addLog("🛡️ بیمه‌کننده «${insurer.name}» بازیکن «${target.name}» (${target.assignedRoleName ?: "بدون نقش"}) را امشب بیمه کرد. او از هرگونه قابلیت شبانه در امان خواهد بود.")

            // Refresh selected player settings
            if (_selectedPlayerForSettings.value?.id == insurerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(insurerId)
            }
        }
    }



    fun unsilencePlayer(priestId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val priest = repository.getPlayerById(priestId) ?: return@launch
            val target = checkAndTransformMajhool(priestId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه‌شده است و اثر قابلیت کشیش «${priest.name}» روی او خنثی شد.")
                return@launch
            }

            if (priest.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت کشیش «${priest.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (priest.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(priest.capabilitiesJson)
                    val unsilenceCap = caps.find { it.name.contains("رفع سکوت") || it.name.contains("unsilence") }
                    if (unsilenceCap != null && unsilenceCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: کشیش دیگر سهمیه رفع سکوت باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement capability count
            if (priest.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(priest.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if ((cap.name.contains("رفع سکوت") || cap.name.contains("unsilence")) && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedPriest = priest.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedPriest)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Mark target player as unsilenced
            val updatedTarget = target.copy(isSilencedThisRound = false)
            repository.updatePlayer(updatedTarget)
            repository.addLog("⛪ کشیش «${priest.name}» سکوت (سایلنت) بازیکن «${target.name}» (${target.assignedRoleName ?: "بدون نقش"}) را امشب باطل کرد.")

            // Refresh selected player settings
            if (_selectedPlayerForSettings.value?.id == priestId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(priestId)
            }
        }
    }

    fun hackerScan(hackerId: Int, playerIds: List<Int>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val hacker = repository.getPlayerById(hackerId) ?: return@launch
            
            if (hacker.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: هکر «${hacker.name}» امشب توسط ماتادور مسدود شده بود و استعلام ناموفق ماند.")
                withContext(Dispatchers.Main) {
                    onResult(false, "مسدودیت امشب: قابلیت شما توسط ماتادور غیرفعال شده است.")
                }
                return@launch
            }

            val targets = playerIds.mapNotNull { repository.getPlayerById(it) }
            if (targets.size < 3) {
                withContext(Dispatchers.Main) {
                    onResult(false, "خطا: باید دقیقاً ۳ بازیکن زنده برای هک انتخاب شوند.")
                }
                return@launch
            }

            // Check remaining capability count first
            if (hacker.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(hacker.capabilitiesJson)
                    val hackCap = caps.find { it.name.contains("هکر") || it.name.contains("استعلام") }
                    if (hackCap != null && hackCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: هکر دیگر سهمیه استعلام باقی‌مانده ندارد.")
                        withContext(Dispatchers.Main) {
                            onResult(false, "خطا: سهمیه استعلام شما به پایان رسیده است.")
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement capability count
            if (hacker.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(hacker.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if ((cap.name.contains("هکر") || cap.name.contains("استعلام")) && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedHacker = hacker.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedHacker)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Count Mafia members
            val mafiaCount = targets.count { target ->
                target.assignedRoleTeam?.lowercase()?.contains("mafia") == true || target.note.lowercase().contains("mafia")
            }

            val isDangerous = mafiaCount == 1
            val resultMessage = if (isDangerous) {
                "لیست خطرناک است (یک مافیا وجود دارد)"
            } else {
                "لیست خطرناک نیست (هیچ یا بیش از یک مافیا وجود دارد)"
            }

            repository.addLog("📡 هکر «${hacker.name}» بازیکنان [${targets.joinToString { it.name }}] را استعلام کرد. نتیجه: $resultMessage")

            withContext(Dispatchers.Main) {
                onResult(true, resultMessage)
            }

            // Refresh selected player settings
            if (_selectedPlayerForSettings.value?.id == hackerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(hackerId)
            }
        }
    }

    fun intoxicatePlayer(sagiId: Int, targetId: Int, currentRound: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val sagi = repository.getPlayerById(sagiId) ?: return@launch
            val target = checkAndTransformMajhool(sagiId, targetId)
            
            if (sagi.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: ساقی «${sagi.name}» امشب توسط ماتادور مسدود شده بود و مست کردن ناموفق ماند.")
                withContext(Dispatchers.Main) {
                    onResult(false, "مسدودیت امشب: قابلیت شما توسط ماتادور غیرفعال شده است.")
                }
                return@launch
            }

            // check if cooldown night matches currentRound
            if (currentRound == _sagiCooldownNight.value) {
                withContext(Dispatchers.Main) {
                    onResult(false, "قابلیت ساقی در این شب غیرفعال است (یک شب در میان).")
                }
                return@launch
            }

            // Check if déjà vu target
            if (_sagiPastTargets.value.contains(targetId)) {
                withContext(Dispatchers.Main) {
                    onResult(false, "ساقی نمیتواند یک نفر را دوباره انتخاب کند!")
                }
                return@launch
            }

            // Check standard capabilities counts if any
            if (sagi.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(sagi.capabilitiesJson)
                    val intoxCap = caps.find { it.name.contains("ساقی") || it.name.contains("مستی") }
                    if (intoxCap != null && intoxCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: ساقی دیگر سهمیه مستی باقی‌مانده ندارد.")
                        withContext(Dispatchers.Main) {
                            onResult(false, "خطا: سهمیه مستی شما به پایان رسیده است.")
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement remaining count
            if (sagi.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(sagi.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if ((cap.name.contains("ساقی") || cap.name.contains("مستی")) && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedSagi = sagi.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedSagi)
                } catch (e: Exception) {
                    // ignore
                }
            }

            val isActionPreventedByInsurance = target.isInsuredThisNight
            val finalMsg = if (isActionPreventedByInsurance) {
                repository.addLog("🛡️ مست کردن ساقی «${sagi.name}» روی «${target.name}» به علت بیمه بی‌اثر شد.")
                "مست کردن بازیکن «${target.name}» با موفقیت ثبت شد (توسط بیمه خنثی شد)."
            } else {
                val updatedTarget = target.copy(isBlockedThisNight = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🍷 ساقی «${sagi.name}» بازیکن «${target.name}» را مست (مسدود) کرد.")
                "بازیکن «${target.name}» با موفقیت مست و قابلیت شب وی مسدود گردید."
            }

            // Update Sagi constraints
            _sagiPastTargets.value = _sagiPastTargets.value + targetId
            _sagiCooldownNight.value = currentRound + 1

            withContext(Dispatchers.Main) {
                onResult(true, finalMsg)
            }

            // Refresh selected player settings
            if (_selectedPlayerForSettings.value?.id == sagiId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(sagiId)
            }
        }
    }

    fun professionalShoot(professionalId: Int, targetId: Int, overrideKill: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = repository.getPlayerById(professionalId) ?: return@launch
            val target = checkAndTransformMajhool(professionalId, targetId)
            
            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت حرفه‌ای «${prof.assignedRoleName ?: "حرفه‌ای"}» روی او خنثی شد.")
                return@launch
            }
            
            if (prof.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت حرفه‌ای «${prof.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (prof.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(prof.capabilitiesJson)
                    val shootCap = caps.find { it.name.contains("شلیک") }
                    if (shootCap != null && shootCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: حرفه‌ای دیگر شلیک مجاز باقی‌آمده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // First. decrement the professional's remaining capability count
            if (prof.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(prof.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("شلیک") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedProf = prof.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedProf)
                } catch (e: Exception) {
                    // ignore format errors
                }
            }

            if (overrideKill != null) {
                if (overrideKill) {
                    // Eliminated
                    val deadTarget = target.copy(isShotThisNight = true, isAlive = false)
                    repository.updatePlayer(deadTarget)
                    repository.addLog("💀 بازیکن «${target.name}» (پدرخوانده) با تأیید گرداننده تحت شلیک مستقیم «${prof.name}» (حرفه‌ای) کشته و از بازی حذف شد.")
                } else {
                    // Survive
                    val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                    repository.updatePlayer(updatedTarget)
                    repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به بازیکن «${target.name}» (پدرخوانده) شلیک کرد اما با تصمیم لغو گرداننده زنده ماند.")
                }
            } else {
                // Rules:
                // 1. Target is a Citizen (Team is Citizen) -> Professional is eliminated
                if (target.assignedRoleTeam == "Citizen") {
                    // Professional commits suicide
                    val deadProf = prof.copy(isAlive = false, isSaved = false, isShotThisNight = true)
                    repository.updatePlayer(deadProf)
                    repository.addLog("💀 بازیکن «${prof.name}» (حرفه‌ای) به اشتباه به هم‌تیمی خود «${target.name}» (شهروند) شلیک کرد و فوراً خودکشی انتحاری روی او اعمال گردید. شفا بر روی او بی‌اثر است.")
                } else if (target.assignedRoleTeam == "Mafia") {
                    val isGodfather = target.assignedRoleName?.contains("پدرخوانده") == true
                    if (isGodfather) {
                        // Survive
                        repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به رئیس مافیا (پدرخوانده) «${target.name}» شلیک کرد اما تیر بر روی او کارساز نبود.")
                    } else {
                        // Eliminated / Check if saved by doctor
                        if (target.isHealedThisNight) {
                            val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                            repository.updatePlayer(updatedTarget)
                            repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به «${target.name}» شلیک کرد، اما پزشک او را شفا داده بود و زنده ماند.")
                        } else {
                            val deadTarget = target.copy(isShotThisNight = true, isAlive = false)
                            repository.updatePlayer(deadTarget)
                            repository.addLog("💀 بازیکن «${target.name}» توسط شلیک مستقیم «${prof.name}» (حرفه‌ای) کشته و از بازی حذف شد.")
                        }
                    }
                } else if (target.assignedRoleTeam == "Independent") {
                    val isKiller = target.assignedRoleName?.contains("کیلر") == true
                    val isChurchill = target.assignedRoleName?.contains("چرچیل") == true
                    if (isKiller || isChurchill) {
                        // Killer/Churchill is immune to standard night kills
                        val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updatedTarget)
                        val roleName = if (isKiller) "کیلر" else "چرچیل"
                        repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به بازیکن «${target.name}» ($roleName) شلیک کرد اما $roleName به دلیل مصونیت شبانه زنده ماند.")
                    } else {
                        // Eliminated / Check if saved by doctor
                        if (target.isHealedThisNight) {
                            val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                            repository.updatePlayer(updatedTarget)
                            repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به «${target.name}» شلیک کرد، اما پزشک او را نجات داد.")
                        } else {
                            val deadTarget = target.copy(isShotThisNight = true, isAlive = false)
                            repository.updatePlayer(deadTarget)
                            repository.addLog("💀 بازیکن «${target.name}» توسط شلیک هدفمند «${prof.name}» (حرفه‌ای) حذف گردید.")
                        }
                    }
                } else {
                    // Catch any other independent or un-teamed players
                    val isKiller = target.assignedRoleName?.contains("کیلر") == true
                    val isChurchill = target.assignedRoleName?.contains("چرچیل") == true
                    if (isKiller || isChurchill) {
                        val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updatedTarget)
                        val roleName = if (isKiller) "کیلر" else "چرچیل"
                        repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به بازیکن «${target.name}» ($roleName) شلیک کرد اما $roleName به دلیل مصونیت شبانه زنده ماند.")
                    } else if (target.isHealedThisNight) {
                        val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                        repository.updatePlayer(updatedTarget)
                        repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به «${target.name}» شلیک کرد اما به دلیل نجات پزشک زنده ماند.")
                    } else {
                        val deadTarget = target.copy(isShotThisNight = true, isAlive = false)
                        repository.updatePlayer(deadTarget)
                        repository.addLog("💀 بازیکن «${target.name}» توسط «${prof.name}» (حرفه‌ای) شلیک و حذف شد.")
                    }
                }
            }
            
            // Also make sure to refresh selectedPlayerForSettings State if needed
            val refreshedProf = repository.getPlayerById(professionalId)
            if (_selectedPlayerForSettings.value?.id == professionalId) {
                _selectedPlayerForSettings.value = refreshedProf
            }
        }
    }

    fun professionalSlaughter(professionalId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = repository.getPlayerById(professionalId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch
            
            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت حرفه‌ای «${prof.assignedRoleName ?: "حرفه‌ای"}» روی او خنثی شد.")
                return@launch
            }
            
            if (prof.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت حرفه‌ای «${prof.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (prof.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(prof.capabilitiesJson)
                    val shootCap = caps.find { it.name.contains("شلیک") }
                    if (shootCap != null && shootCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: حرفه‌ای دیگر شلیک/سلاخی مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement remaining capability count of professional as well
            if (prof.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(prof.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("شلیک") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedProf = prof.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedProf)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Slaughter logic:
            // Bypasses doctor's save, abilities neutralized, and receives permanent Slaughtered status
            val isTargetChurchill = target.assignedRoleName?.contains("چرچیل") == true
            val currentNight = gameLogs.value.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
            val isNight1 = currentNight == 1 || currentNight == 0
            if (isTargetChurchill && isNight1) {
                val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ اقدام ناموفق: بازیکن «${prof.name}» (حرفه‌ای) بازیکن «${target.name}» (چرچیل) را سلاخی کرد، اما چرچیل به دلیل مصونیت مطلق شب اول جان سالم به در برد.")
            } else {
                val updatedTarget = target.copy(
                    isAlive = false,
                    isSlaughtered = true,
                    isSaved = false,
                    isBlocked = false,
                    isMuted = false,
                    isShotThisNight = true
                )
                repository.updatePlayer(updatedTarget)
                repository.addLog("🔪 بازیکن «${target.name}» توسط «${prof.name}» (حرفه‌ای) سلاخی شد و از بازی به طور کامل کنار رفت! پزشک نجات بی‌اثر است.")
            }
            
            val refreshedProf = repository.getPlayerById(professionalId)
            if (_selectedPlayerForSettings.value?.id == professionalId) {
                _selectedPlayerForSettings.value = refreshedProf
            }
        }
    }

    fun doctorHeal(doctorId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = repository.getPlayerById(doctorId) ?: return@launch
            val target = checkAndTransformMajhool(doctorId, targetId)
            
            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت پزشک «${doc.assignedRoleName ?: "پزشک"}» روی او خنثی شد.")
                return@launch
            }
            
            if (doc.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت پزشک «${doc.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (doc.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(doc.capabilitiesJson)
                    val healCap = caps.find { it.name.contains("شفا") || it.name.contains("نجات") }
                    if (healCap != null && healCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: پزشک دیگر شفا / نجات مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Check self-save count if target is the doctor himself
            val isDoctorSelfSave = (target.id == doc.id)
            if (isDoctorSelfSave && doc.doctorSelfSavesCount >= 2) {
                repository.addLog("⚠️ خطا: دکتر قبلاً ۲ بار خود را نجات داده است و دیگر نمی‌تواند.")
                return@launch
            }

            // Decrement remaining capability count of doctor
            if (doc.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(doc.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if ((cap.name.contains("شفا") || cap.name.contains("نجات")) && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedDoc = doc.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedDoc)
                } catch (e: Exception) {
                    // ignore format errors
                }
            }

            // Fetch current fresh states
            val freshDoc = repository.getPlayerById(doctorId) ?: doc
            val freshTarget = if (doctorId == targetId) freshDoc else (repository.getPlayerById(targetId) ?: target)

            // Calculate new self-save count
            val finalSelfSavesCount = if (isDoctorSelfSave) {
                freshDoc.doctorSelfSavesCount + 1
            } else {
                freshDoc.doctorSelfSavesCount
            }

            if (freshTarget.isSlaughtered) {
                // Slaughtered -> Doctor's cure is ignored, they die, but action is consumed
                val finalTarget = freshTarget.copy(
                    isSaved = true,
                    isHealedThisNight = true,
                    isAlive = false,
                    doctorSelfSavesCount = if (isDoctorSelfSave) finalSelfSavesCount else freshTarget.doctorSelfSavesCount
                )
                repository.updatePlayer(finalTarget)
                if (doctorId != targetId) {
                    val refreshedDocWithSaves = freshDoc.copy(doctorSelfSavesCount = finalSelfSavesCount)
                    repository.updatePlayer(refreshedDocWithSaves)
                }
                repository.addLog("🩺 تلاش پزشک برای نجات «${freshTarget.name}» ثبت شد اما با شکست مواجه گردید (بازیکن سلاخی شده است).")
            } else {
                // Healed!
                val finalTarget = freshTarget.copy(
                    isSaved = true,
                    isHealedThisNight = true,
                    isAlive = if (freshTarget.isShotThisNight) true else freshTarget.isAlive,
                    doctorSelfSavesCount = if (isDoctorSelfSave) finalSelfSavesCount else freshTarget.doctorSelfSavesCount
                )
                repository.updatePlayer(finalTarget)
                if (doctorId != targetId) {
                    val refreshedDocWithSaves = freshDoc.copy(doctorSelfSavesCount = finalSelfSavesCount)
                    repository.updatePlayer(refreshedDocWithSaves)
                }
                repository.addLog("🩺 پزشک («${freshDoc.name}») بازیکن «${freshTarget.name}» را نجات داد/شفا بخشید.")
            }

            // Refresh selected player settings dialog state
            if (_selectedPlayerForSettings.value?.id == doctorId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(doctorId)
            }
        }
    }

    fun godfatherShoot(godfatherId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val gf = repository.getPlayerById(godfatherId) ?: return@launch
            val target = checkAndTransformMajhool(godfatherId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» روی او خنثی شد.")
                return@launch
            }

            if (gf.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (gf.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(gf.capabilitiesJson)
                    val gfCap = caps.find { it.name.contains("شلیک") }
                    if (gfCap != null && gfCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: رئیس مافیا (پدرخوانده) دیگر شلیک مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement Godfather's "شلیک شبانه مافیا 💀" capability count
            if (gf.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(gf.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("شلیک") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedGf = gf.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedGf)
                } catch (e: Exception) {
                    // ignore
                }
            }

            val freshTarget = repository.getPlayerById(targetId) ?: target

            val isKiller = freshTarget.assignedRoleName?.contains("کیلر") == true
            val isChurchill = freshTarget.assignedRoleName?.contains("چرچیل") == true
            // Check if Tough Guy (جان‌سخت)
            val isToughGuy = freshTarget.assignedRoleName?.contains("جان") == true && freshTarget.assignedRoleName?.contains("سخت") == true
            
            if (isKiller || isChurchill) {
                // Killer/Churchill is immune to standard night kills
                val updatedTarget = freshTarget.copy(isShotThisNight = true, isAlive = true)
                repository.updatePlayer(updatedTarget)
                val roleName = if (isKiller) "کیلر" else "چرچیل"
                repository.addLog("🛡️ رئیس مافیا (پدرخوانده) به بازیکن «${freshTarget.name}» ($roleName) شلیک کرد اما $roleName به دلیل مصونیت شبانه زنده ماند.")
            } else if (isToughGuy) {
                // Tough Guy survives the shot
                val updatedTarget = freshTarget.copy(isShotThisNight = true, isAlive = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ رئیس مافیا (پدرخوانده) به بازیکن «${freshTarget.name}» (جان‌سخت) شلیک کرد اما تیر بی‌اثر بود و جان سالم به در برد.")
            } else {
                // Check if healed by Doctor
                if (freshTarget.isHealedThisNight) {
                    val updatedTarget = freshTarget.copy(isShotThisNight = true, isAlive = true)
                    repository.updatePlayer(updatedTarget)
                    repository.addLog("🛡️ رئیس مافیا (پدرخوانده) به «${freshTarget.name}» شلیک کرد، اما پزشک او را نجات داده بود و زنده ماند.")
                } else {
                    // Eliminated
                    val deadTarget = freshTarget.copy(isShotThisNight = true, isAlive = false)
                    repository.updatePlayer(deadTarget)
                    repository.addLog("💀 بازیکن «${freshTarget.name}» در شب توسط شلیک مستقیم رئیس مافیا (پدرخوانده) «${gf.name}» اعدام/کشته شد.")
                }
            }

            // Refresh selected player settings dialog state
            if (_selectedPlayerForSettings.value?.id == godfatherId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(godfatherId)
            }
        }
    }

    fun godfatherSlaughter(godfatherId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val gf = repository.getPlayerById(godfatherId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch
            
            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» روی او خنثی شد.")
                return@launch
            }
            
            if (gf.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (gf.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(gf.capabilitiesJson)
                    val gfCap = caps.find { it.name.contains("شلیک") }
                    if (gfCap != null && gfCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: رئیس مافیا (پدرخوانده) دیگر شلیک/سلاخی مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement Godfather's "شلیک شبانه مافیا 💀" capability count
            if (gf.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(gf.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("شلیک") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedGf = gf.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedGf)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Slaughter logic:
            // Bypasses doctor's save, abilities neutralized, and receives permanent Slaughtered status
            val isTargetChurchill = target.assignedRoleName?.contains("چرچیل") == true
            val currentNight = gameLogs.value.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
            val isNight1 = currentNight == 1 || currentNight == 0
            if (isTargetChurchill && isNight1) {
                val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ اقدام ناموفق: رئیس مافیا (پدرخوانده) «${gf.name}» به بازیکن «${target.name}» (چرچیل) حمله کرد، اما چرچیل به دلیل مصونیت مطلق شب اول جان سالم به در برد.")
            } else {
                val updatedTarget = target.copy(
                    isAlive = false,
                    isSlaughtered = true,
                    isSaved = false,
                    isBlocked = false,
                    isMuted = false,
                    isShotThisNight = true
                )
                repository.updatePlayer(updatedTarget)
                repository.addLog("🔪 بازیکن «${target.name}» توسط رئیس مافیا (پدرخوانده) «${gf.name}» سلاخی شد و به طور کامل حذف گردید! نجات پزشک بی‌اثر است.")
            }
            
            val refreshedGf = repository.getPlayerById(godfatherId)
            if (_selectedPlayerForSettings.value?.id == godfatherId) {
                _selectedPlayerForSettings.value = refreshedGf
            }
        }
    }

    fun matadorBlock(matadorId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val matador = repository.getPlayerById(matadorId) ?: return@launch
            val target = checkAndTransformMajhool(matadorId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت مسدود کردن «${matador.name}» روی او خنثی شد.")
                return@launch
            }

            // Check remaining capability count first
            if (matador.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(matador.capabilitiesJson)
                    val blockCap = caps.find { it.name.contains("مسدود") }
                    if (blockCap != null && blockCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: ماتادور دیگر مسدود‌سازی مجاز باقی‌مانده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement Matador's capability count
            if (matador.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(matador.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("مسدود") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedMatador = matador.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedMatador)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Apply direct block flag for this night
            val updatedTarget = target.copy(isBlockedThisNight = true)
            repository.updatePlayer(updatedTarget)
            repository.addLog("🧣 ماتادور «${matador.name}» قابلیت‌های بازیکن «${target.name}» (${target.assignedRoleName ?: "بدون نقش"}) را امشب مسدود کرد.")

            // Refresh selected player settings dialog state
            if (_selectedPlayerForSettings.value?.id == matadorId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(matadorId)
            }
        }
    }

    fun generalCheck(generalId: Int, targetId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val general = repository.getPlayerById(generalId) ?: return@launch
            val target = checkAndTransformMajhool(generalId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت اوشن - ژنرال «${general.name}» روی او خنثی شد.")
                return@launch
            }

            if (general.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت اوشن - ژنرال «${general.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Decrement General's remaining capability count
            if (general.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(general.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("تشخیص") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedGen = general.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedGen)
                } catch (e: Exception) {
                    // ignore
                }
            }

            val isMafia = target.assignedRoleTeam == "Mafia"
            if (isMafia) {
                // Eliminate general immediately
                val deadGen = general.copy(isAlive = false, isSaved = false, isShotThisNight = true)
                repository.updatePlayer(deadGen)
                repository.addLog("💀 بازیکن «${general.name}» (اوشن - ژنرال) به علت استعلام اشتباه روی بازیکن «${target.name}» (مافیا) حذف گردید.")
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } else {
                repository.addLog("🔍 بازیکن «${general.name}» (اوشن - ژنرال) بازیکن «${target.name}» را معارفه کرد. هدف شهروند یا مستقل است و او زنده می‌ماند.")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }

            // Refresh selected player settings dialog state
            if (_selectedPlayerForSettings.value?.id == generalId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(generalId)
            }
        }
    }

    fun constantineRevive(constantineId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val constantine = repository.getPlayerById(constantineId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch

            if (constantine.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت کنستانتین «${constantine.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Decrement Constantine's remaining capability count
            if (constantine.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(constantine.capabilitiesJson)
                    val reviveCap = caps.find { it.name.contains("احیا") }
                    if (reviveCap != null && reviveCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: کنستانتین قبلاً از قابلیت احیاء خود استفاده کرده است.")
                        return@launch
                    }
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("احیا") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedConst = constantine.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedConst)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Revive execution: change state to isAlive = true and clear death status
            val revivedTarget = target.copy(
                isAlive = true,
                isKilledToday = false,
                isSlaughtered = false,
                isShotThisNight = false,
                isSaved = false,
                isRevivedThisNight = true
            )
            repository.updatePlayer(revivedTarget)
            repository.addLog("⚡ کنستانتین «${constantine.name}» با موفقیت بازیکن «${target.name}» (${target.assignedRoleName ?: "بدون نقش"}) را احیا کرد و به بازی بازگرداند.")

            // Refresh selected player settings dialog state
            if (_selectedPlayerForSettings.value?.id == constantineId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(constantineId)
            }
        }
    }

    fun executeNatoGuess(
        natoPlayerId: Int,
        targetPlayerId: Int,
        selectedRole: String,
        onResult: (Boolean, Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val natoPlayer = repository.getPlayerById(natoPlayerId) ?: return@launch
            val targetPlayer = repository.getPlayerById(targetPlayerId) ?: return@launch

            if (natoPlayer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت ناتو «${natoPlayer.name}» امشب مسدود شده است.")
                onResult(false, _natoWrongGuessesCount.value)
                return@launch
            }

            // Decrement remaining count of capabilitiesJson of natoPlayer
            if (natoPlayer.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(natoPlayer.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("حدس") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedNato = natoPlayer.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedNato)
                } catch (e: Exception) {
                    // ignore
                }
            }

            val targetRoleName = targetPlayer.assignedRoleName ?: ""
            val isCorrect = targetRoleName.lowercase().contains(selectedRole.lowercase()) ||
                            selectedRole.lowercase().contains(targetRoleName.lowercase()) ||
                            (selectedRole == "پزشک" && targetRoleName.contains("دکتر")) ||
                            (selectedRole == "دکتر" && targetRoleName.contains("پزشک"))

            if (isCorrect) {
                val isKiller = targetPlayer.assignedRoleName?.contains("کیلر") == true
                if (isKiller) {
                    val updatedTarget = targetPlayer.copy(isShotThisNight = true, isAlive = true)
                    repository.updatePlayer(updatedTarget)
                    repository.addLog("🛡️ حدس ناتو درست بود، اما بازیکن «${targetPlayer.name}» (کیلر) به دلیل مصونیت شبانه حذف نگردید.")
                } else {
                    val killedTarget = targetPlayer.copy(isAlive = false, isShotThisNight = true)
                    repository.updatePlayer(killedTarget)
                    repository.addLog("🎯 حدس ناتو درست بود! بازیکن «${targetPlayer.name}» واقعاً نقش «${targetRoleName}» را دارد و حذف می‌شود.")
                }
                onResult(true, _natoWrongGuessesCount.value)
            } else {
                _natoWrongGuessesCount.value++
                val newCount = _natoWrongGuessesCount.value
                repository.addLog("❌ حدس ناتو اشتباه بود! او حدس زد بازیکن «${targetPlayer.name}» نقش «${selectedRole}» را دارد. تعداد خطاها: $newCount/3")

                if (newCount >= 3) {
                    val updatedNato = natoPlayer.copy(isAlive = false, isShotThisNight = true)
                    repository.updatePlayer(updatedNato)
                    repository.addLog("💀 ناتو «${natoPlayer.name}» به دلیل ۳ حدس اشتباه از بازی حذف شد.")
                }
                onResult(false, newCount)
            }

            if (_selectedPlayerForSettings.value?.id == natoPlayerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(natoPlayerId)
            }
        }
    }

    fun executeSabotage(
        saboteurId: Int,
        targetId: Int,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val saboteur = repository.getPlayerById(saboteurId) ?: return@launch
            val target = checkAndTransformMajhool(saboteurId, targetId)

            if (saboteur.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت خرابکار «${saboteur.name}» امشب توسط ماتادور بسته شده است.")
                viewModelScope.launch(Dispatchers.Main) { onResult("فعالیت خرابکار مسدود شده بود.") }
                return@launch
            }

            // Decrement remaining count of capabilitiesJson of saboteur
            if (saboteur.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(saboteur.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("خرابکاری") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedSaboteur = saboteur.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedSaboteur)
                } catch (e: Exception) {
                    // ignore
                }
            }

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن «${target.name}» امشب بیمه است و خرابکاری روی او بی‌اثر شد.")
                viewModelScope.launch(Dispatchers.Main) { onResult("بازیکن هدف بیمه بود و خرابکاری ناموفق ماند.") }
                return@launch
            }

            val targetRoleName = target.assignedRoleName ?: ""
            if (targetRoleName.contains("ساقی")) {
                repository.addLog("🍷 اقدام ناموفق: ساقی از خرابکاری تفنگ مصون است. خرابکاری تفنگ بر روی بازیکن «${target.name}» بی اثر ماند.")
                viewModelScope.launch(Dispatchers.Main) { onResult("ساقی در برابر خرابکاری تفنگ مصونیت دارد.") }
                return@launch
            }

            val finalTarget = target.copy(isSabotaged = true)
            repository.updatePlayer(finalTarget)
            repository.addLog("🔫 خرابکار «${saboteur.name}» تفنگ بازیکن «${target.name}» را خرابکاري کرد.")
            viewModelScope.launch(Dispatchers.Main) { onResult("خرابکاری تفنگ با موفقیت روی بازیکن انجام شد.") }

            if (_selectedPlayerForSettings.value?.id == saboteurId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(saboteurId)
            }
        }
    }

    fun executeVeto(vetoPlayerId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val vetoPlayer = repository.getPlayerById(vetoPlayerId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch

            if (vetoPlayer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت وتو کننده «${vetoPlayer.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            // Revive execution: change state to isAlive = true and clear death status
            val revivedTarget = target.copy(
                isAlive = true,
                isKilledToday = false,
                isSlaughtered = false,
                isShotThisNight = false,
                isSaved = false,
                isRevivedThisNight = true
            )
            repository.updatePlayer(revivedTarget)
            repository.addLog("⚡ بازیکن «${vetoPlayer.name}» رای‌گیری را وتو کرد! بازیکن «${target.name}» با موفقیت به بازی برگشت.")
        }
    }

    fun citizenKaneReveal(kaneId: Int, targetId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var kane = repository.getPlayerById(kaneId) ?: return@launch
            val target = checkAndTransformMajhool(kaneId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت همشهری کین «${kane.name}» روی او خنثی شد.")
                return@launch
            }

            if (kane.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت همشهری کین «${kane.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            if (kane.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(kane.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("افشاگری") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    kane = kane.copy(capabilitiesJson = updatedJson)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Apply sacrifice penalty for next night
            kane = kane.copy(willDieNextNight = true)
            repository.updatePlayer(kane)

            val isMafia = target.assignedRoleTeam?.equals("Mafia", ignoreCase = true) == true
            if (isMafia) {
                val updatedTarget = target.copy(isRevealedMafia = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🔍 همشهری کین با موفقیت بازیکن «${target.name}» را به عنوان مافیا شناسایی کرد. (قربانی کین در شب بعد فعال شد)")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(true)
                }
            } else {
                repository.addLog("🔍 همشهری کین بازیکن «${target.name}» را استعلام کرد اما او مافیا نبود. (قربانی کین در شب بعد فعال شد)")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(false)
                }
            }

            if (_selectedPlayerForSettings.value?.id == kaneId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(kaneId)
            }
        }
    }

    fun godfatherRecruit(godfatherId: Int, targetId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var gf = repository.getPlayerById(godfatherId) ?: return@launch
            val target = checkAndTransformMajhool(godfatherId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» روی او خنثی شد.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("⚠️ خطا: بازیکن هدف بیمه است و قابلیت روی او اثرگذار نبود.")
                }
                return@launch
            }

            if (gf.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت رئیس مافیا (پدرخوانده) «${gf.name}» امشب توسط ماتادور بسته شده است.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("⚠️ خطا: قابلیت رئیس مافیا (پدرخوانده) امشب توسط ماتادور بسته شده است.")
                }
                return@launch
            }

            // Decrement Godfather's "خریداری" capability count if exists
            if (gf.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(gf.capabilitiesJson)
                    val recruitCap = caps.find { it.name.contains("خریداری") }
                    if (recruitCap != null && recruitCap.remainingCount <= 0) {
                        viewModelScope.launch(Dispatchers.Main) {
                            onResult("⚠️ خطا: رئیس مافیا (پدرخوانده) دیگر قابلیت خریداری مجاز باقی‌مانده ندارد.")
                        }
                        return@launch
                    }
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("خریداری") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    gf = gf.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(gf)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Evaluate target's role: "شهروند ساده 🕊️" or "شهروند ساده"
            val isSimpleCitizen = target.assignedRoleName?.contains("شهروند ساده") == true
            if (isSimpleCitizen) {
                // Change to Simple Mafia (مافیای ساده 👤) and update faction to Mafia
                val recruitedTarget = target.copy(
                    assignedRoleName = "مافیای ساده 👤",
                    assignedRoleTeam = "Mafia"
                )
                repository.updatePlayer(recruitedTarget)
                repository.addLog("🤝 رئیس مافیا (پدرخوانده) «${gf.name}» با موفقیت بازیکن «${target.name}» را خریداری کرد. او به مافیای ساده تبدیل شد.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("حدس درست بود! این بازیکن به مافیای ساده تغییر یافت.")
                }
            } else {
                repository.addLog("🤝 تلاش رئیس مافیا (پدرخوانده) «${gf.name}» برای خریداری بازیکن «${target.name}» ناموفق بود (او شهروند ساده نیست).")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("حدس اشتباه بود. این بازیکن شهروند ساده نیست و تغییری نکرد.")
                }
            }

            if (_selectedPlayerForSettings.value?.id == godfatherId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(godfatherId)
            }
        }
    }

    fun buyerRecruit(buyerId: Int, targetId: Int, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var buyer = repository.getPlayerById(buyerId) ?: return@launch
            val target = checkAndTransformMajhool(buyerId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت خریدار (مذاکره کننده) «${buyer.name}» روی او خنثی شد.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("⚠️ خطا: بازیکن هدف بیمه است و قابلیت روی او اثرگذار نبود.")
                }
                return@launch
            }

            if (buyer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت خریدار (مذاکره کننده) «${buyer.name}» امشب توسط ماتادور بسته شده است.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("⚠️ خطا: قابلیت خریدار (مذاکره کننده) امشب توسط ماتادور بسته شده است.")
                }
                return@launch
            }

            // Decrement Buyer's "خریداری" capability count if exists
            if (buyer.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(buyer.capabilitiesJson)
                    val recruitCap = caps.find { it.name.contains("خریداری") }
                    if (recruitCap != null && recruitCap.remainingCount <= 0) {
                        viewModelScope.launch(Dispatchers.Main) {
                            onResult("⚠️ خطا: خریدار (مذاکره کننده) دیگر قابلیت خریداری مجاز باقی‌مانده ندارد.")
                        }
                        return@launch
                    }
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("خریداری") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    buyer = buyer.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(buyer)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Evaluate target's role: "شهروند ساده 🕊️" or "شهروند ساده"
            val isSimpleCitizen = target.assignedRoleName?.contains("شهروند ساده") == true
            if (isSimpleCitizen) {
                // Change to Simple Mafia (مافیای ساده 👤) and update faction to Mafia
                val recruitedTarget = target.copy(
                    assignedRoleName = "مافیای ساده 👤",
                    assignedRoleTeam = "Mafia"
                )
                repository.updatePlayer(recruitedTarget)
                repository.addLog("🤝 خریدار (مذاکره کننده) «${buyer.name}» با موفقیت بازیکن «${target.name}» را خریداری کرد. او به مافیای ساده تبدیل شد.")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("حدس درست بود! این بازیکن به مافیای ساده تغییر یافت.")
                }
            } else {
                repository.addLog("🤝 تلاش خریدار (مذاکره کننده) «${buyer.name}» برای خریداری بازیکن «${target.name}» ناموفق بود (او شهروند ساده نیست).")
                viewModelScope.launch(Dispatchers.Main) {
                    onResult("حدس اشتباه بود. این بازیکن شهروند ساده نیست و تغییری نکرد.")
                }
            }

            if (_selectedPlayerForSettings.value?.id == buyerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(buyerId)
            }
        }
    }

    fun giveMusketeerGun(musketeerId: Int, targetId: Int, isLive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val musketeer = repository.getPlayerById(musketeerId) ?: return@launch
            val target = checkAndTransformMajhool(musketeerId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت تفنگدار «${musketeer.name}» روی او خنثی شد.")
                return@launch
            }

            if (musketeer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت تفنگدار «${musketeer.name}» امشب توسط ماتادور بسته شده است.")
                return@launch
            }

            if (isLive) {
                if (_musketeerLiveGunExhausted.value) {
                    repository.addLog("⚠️ خطا: تفنگدار دیگر تفنگ جنگی مجاز باقی‌مانده ندارد.")
                    return@launch
                }

                // Clear any other player's current tonight live gun flag so there's at most 1 live gun assigned per night.
                val playersList = repository.getAllPlayersList()
                val updatedList = playersList.map {
                    if (it.hasLiveGunThisRound) it.copy(hasLiveGunThisRound = false, hasCombatGun = false) else it
                }
                repository.insertPlayers(updatedList)

                val freshTarget = repository.getPlayerById(targetId) ?: target
                val finalTarget = freshTarget.copy(hasLiveGunThisRound = true, hasBlankGunThisRound = false, hasCombatGun = true, hasBlankGun = false)
                repository.updatePlayer(finalTarget)
                repository.addLog("🔫 تفنگدار «${musketeer.name}» تفنگ جنگی به بازیکن «${target.name}» اعطا کرد.")
            } else {
                val finalTarget = target.copy(hasBlankGunThisRound = true, hasLiveGunThisRound = false, hasBlankGun = true, hasCombatGun = false)
                repository.updatePlayer(finalTarget)
                repository.addLog("🔫 تفنگدار «${musketeer.name}» تفنگ مشقی به بازیکن «${target.name}» اعطا کرد.")
            }

            // Decrement Musketeer's capability counts in capabilitiesJson if they exist
            if (musketeer.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(musketeer.capabilitiesJson)
                    val targetCapName = if (isLive) "جنگی" else "مشقی"
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains(targetCapName) && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    val updatedJson = Json.encodeToString(updatedCaps)
                    val updatedMusketeer = musketeer.copy(capabilitiesJson = updatedJson)
                    repository.updatePlayer(updatedMusketeer)
                } catch (e: Exception) {
                    // ignore
                }
            }

            if (_selectedPlayerForSettings.value?.id == musketeerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(musketeerId)
            }
        }
    }

    fun useLiveGun(shooterId: Int, targetId: Int, onResult: (String, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val shooter = repository.getPlayerById(shooterId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch

            if (shooter.isSabotaged) {
                // Victim survives, shooter is eliminated instead
                val killedShooter = shooter.copy(isAlive = false, isSabotaged = false, usedLiveGun = true)
                repository.updatePlayer(killedShooter)

                repository.addLog("💥 تفنگ خرابکاری شده بود! شلیک تفنگ جنگی به خود بازیکن «${shooter.name}» برگشت و او کشته شد.")

                viewModelScope.launch(Dispatchers.Main) {
                    onResult(shooter.name, "SABOTAGED")
                }
            } else {
                val updatedShooter = shooter.copy(usedLiveGun = true)
                repository.updatePlayer(updatedShooter)

                val deadTarget = target.copy(isAlive = false)
                repository.updatePlayer(deadTarget)

                val factionLabel = when (target.assignedRoleTeam) {
                    "Mafia" -> "مافیا"
                    "Citizen" -> "شهروند"
                    else -> "مستقل"
                }

                repository.addLog("💥 شلیک تفنگ جنگی: بازیکن «${shooter.name}» با تفنگ جنگی به سمت بازیکن «${target.name}» شلیک کرد و او را کشت! (جناح هدف: $factionLabel)")

                viewModelScope.launch(Dispatchers.Main) {
                    onResult(target.name, factionLabel)
                }
            }
        }
    }



    suspend fun checkAndTransformMajhool(actorId: Int, targetId: Int): PlayerEntity {
        val target = repository.getPlayerById(targetId) ?: return PlayerEntity(name = "")
        val isTargetMajhool = target.assignedRoleName?.contains("مجهول") == true
        if (isTargetMajhool) {
            val actor = repository.getPlayerById(actorId) ?: return target
            val actorTeam = actor.assignedRoleTeam ?: ""
            val (newRoleName, newTeam, alertTeamLabel) = if (actorTeam == "Mafia") {
                Triple("مافیای ساده 👤", "Mafia", "مافیای ساده")
            } else {
                Triple("شهروند ساده 🕊️", "Citizen", "شهروند ساده")
            }
            val transformedTarget = target.copy(
                assignedRoleName = newRoleName,
                assignedRoleTeam = newTeam,
                isInsuredThisNight = true
            )
            repository.updatePlayer(transformedTarget)
            val logMsg = "👤❓ مجهول («${target.name}») توسط «${actor.name}» (جناح ${if (actorTeam == "Mafia") "مافیا" else "شهروند/مستقل"}) انتخاب شد و به [$alertTeamLabel] تبدیل شد! (بیمه فعال شد)"
            repository.addLog(logMsg)
            
            if (_selectedPlayerForSettings.value?.id == targetId) {
                _selectedPlayerForSettings.value = transformedTarget
            }
            return transformedTarget
        }
        return target
    }

    fun executeDirectCombatShot(shooterId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val shooter = repository.getPlayerById(shooterId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch
            
            val updatedTarget = target.copy(isAlive = false)
            repository.updatePlayer(updatedTarget)
            
            val updatedShooter = shooter.copy(
                hasCombatGun = false,
                hasLiveGunThisRound = false,
                usedLiveGun = true
            )
            repository.updatePlayer(updatedShooter)
            
            repository.addLog("💥 شلیک مستقیم: بازیکن «${shooter.name}» با استفاده از تفنگ جنگی خود به بازیکن «${target.name}» شلیک کرد و او را به قتل رساند 💀")
            
            if (_selectedPlayerForSettings.value?.id == shooterId) {
                _selectedPlayerForSettings.value = null
            }
        }
    }

    fun saveActiveGameSession(status: String = "IN_PROGRESS") {
        viewModelScope.launch(Dispatchers.IO) {
            val playersJson = Json.encodeToString(players.value)
            val logsJson = Json.encodeToString(gameLogs.value)
            val rolesJson = Json.encodeToString(roles.value)
            val sagiPastTargetsJson = Json.encodeToString(sagiPastTargets.value)

            val currentId = _activeSessionId.value ?: 0
            val session = com.example.data.model.GameSessionEntity(
                id = currentId,
                status = status,
                moderatorName = _moderatorName.value,
                gameStage = _gameStage.value,
                gamePhase = _gamePhase.value,
                playersJson = playersJson,
                logsJson = logsJson,
                rolesJson = rolesJson,
                remainingInquiries = _remainingInquiries.value,
                totalInquiries = _totalInquiries.value,
                sagiCooldownNight = _sagiCooldownNight.value,
                sagiPastTargetsJson = sagiPastTargetsJson,
                isGravedigActiveThisNight = _isGravedigActiveThisNight.value,
                natoWrongGuessesCount = _natoWrongGuessesCount.value,
                musketeerLiveGunExhausted = _musketeerLiveGunExhausted.value
            )
            if (currentId != 0) {
                repository.updateGameSession(session)
            } else {
                val savedId = repository.insertGameSession(session)
                _activeSessionId.value = savedId.toInt()
            }
        }
    }

    fun resumeGameSession(sessionId: Int, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = repository.getGameSessionById(sessionId) ?: return@launch
            
            _activeSessionId.value = session.id
            
            // Restore VM memory states
            _gameStage.value = session.gameStage
            _gamePhase.value = session.gamePhase
            _moderatorName.value = session.moderatorName
            _remainingInquiries.value = session.remainingInquiries
            _totalInquiries.value = session.totalInquiries
            _sagiCooldownNight.value = session.sagiCooldownNight
            _isGravedigActiveThisNight.value = session.isGravedigActiveThisNight
            _natoWrongGuessesCount.value = session.natoWrongGuessesCount
            _musketeerLiveGunExhausted.value = session.musketeerLiveGunExhausted
            
            try {
                _sagiPastTargets.value = Json.decodeFromString<List<Int>>(session.sagiPastTargetsJson)
            } catch (e: Exception) {
                _sagiPastTargets.value = emptyList()
            }

            // Restore Database - Players
            try {
                val restoredPlayers = Json.decodeFromString<List<PlayerEntity>>(session.playersJson)
                repository.deleteAllPlayers()
                repository.insertPlayers(restoredPlayers)
            } catch (e: Exception) {
                // fallback
            }

            // Restore Database - Roles
            if (session.rolesJson.isNotBlank()) {
                try {
                    val restoredRoles = Json.decodeFromString<List<RoleEntity>>(session.rolesJson)
                    repository.deleteAllRoles()
                    repository.insertRoles(restoredRoles)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Restore Database - Logs
            try {
                val restoredLogs = Json.decodeFromString<List<GameLogEntity>>(session.logsJson)
                repository.clearLogs()
                // Insert logs in reverse order since addLog prepends or orders them. 
                // Let's add them so they have original message and order.
                restoredLogs.reversed().forEach { log ->
                    repository.addLog(log.message, log.phase)
                }
            } catch (e: Exception) {
                // ignore
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteGameSessionById(sessionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGameSessionById(sessionId)
        }
    }

    fun killerShoot(killerId: Int, targetId: Int, currentRound: Int, actionType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val killer = repository.getPlayerById(killerId) ?: return@launch
            val target = checkAndTransformMajhool(killerId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت کیلر «${killer.name}» روی او خنثی شد.")
                return@launch
            }

            if (killer.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت کیلر «${killer.name}» امشب توسط ماتادور مسدود شده است.")
                return@launch
            }

            val isGodfather = target.assignedRoleName?.contains("پدرخوانده") == true

            val isTargetChurchill = target.assignedRoleName?.contains("چرچیل") == true
            val currentNight = gameLogs.value.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
            val isNight1 = currentNight == 1 || currentNight == 0

            if (isTargetChurchill && isNight1) {
                val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ اقدام کیلر: کیلر به بازیکن «${target.name}» (چرچیل) حمله کرد اما چرچیل به دلیل مصونیت مطلق شب اول جان سالم به در برد.")
            } else if (actionType == "شلیک" && isGodfather) {
                // Godfather survives
                val updatedTarget = target.copy(isShotThisNight = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ اقدام کیلر: کیلر به بازیکن «${target.name}» (پدرخوانده) شلیک کرد اما پدرخوانده جان سالم به در برد.")
            } else {
                // Killer kills target absolutely (whether Shoot or Slaughter)
                val deadTarget = target.copy(
                    isAlive = false,
                    isSlaughtered = true, // Bypasses doctor's heal
                    isSaved = false, // Neutralizes standard save
                    isShotThisNight = true
                )
                repository.updatePlayer(deadTarget)
                if (actionType == "سلاخی") {
                    repository.addLog("💀 اقدام کیلر: بازیکن «${target.name}» توسط کیلر سلاخی شد و به طور کامل حذف گردید! نجات پزشک بی‌اثر است.")
                } else {
                    repository.addLog("💀 اقدام کیلر: بازیکن «${target.name}» مورد شلیک مستقیم کیلر قرار گرفت و حذف گردید! نجات پزشک بی‌اثر است.")
                }
            }

            if (_selectedPlayerForSettings.value?.id == killerId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(killerId)
            }
        }
    }

    fun churchillShoot(churchillId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val churchill = repository.getPlayerById(churchillId) ?: return@launch
            val target = checkAndTransformMajhool(churchillId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت چرچیل «${churchill.name}» روی او خنثی شد.")
                return@launch
            }

            if (churchill.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت چرچیل «${churchill.name}» امشب توسط ماتادور مسدود شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (churchill.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(churchill.capabilitiesJson)
                    val shootCap = caps.find { it.name.contains("شلیک") }
                    if (shootCap != null && shootCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: چرچیل دیگر شلیک مجاز باقی‌آمده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement the capability
            var updatedChurchill = churchill
            if (churchill.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(churchill.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("شلیک") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    updatedChurchill = churchill.copy(capabilitiesJson = Json.encodeToString(updatedCaps))
                    repository.updatePlayer(updatedChurchill)
                } catch (e: Exception) {
                    // ignore
                }
            }

            val isGodfather = target.assignedRoleName?.contains("پدرخوانده") == true

            if (isGodfather) {
                // Godfather survives
                val updatedTarget = target.copy(isShotThisNight = true)
                repository.updatePlayer(updatedTarget)
                repository.addLog("🛡️ اقدام چرچیل: چرچیل به بازیکن «${target.name}» (پدرخوانده) شلیک کرد اما پدرخوانده جان سالم به در برد.")
            } else {
                // Churchill kills target absolutely (cannot be saved)
                val deadTarget = target.copy(
                    isAlive = false,
                    isSlaughtered = true, // Bypasses doctor's heal
                    isSaved = false, // Neutralizes standard save
                    isShotThisNight = true
                )
                repository.updatePlayer(deadTarget)
                repository.addLog("💀 اقدام چرچیل: بازیکن «${target.name}» مورد شلیک مستقیم چرچیل قرار گرفت و حذف گردید! نجات پزشک بی‌اثر است.")
            }

            if (_selectedPlayerForSettings.value?.id == churchillId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(churchillId)
            }
        }
    }

    fun churchillSlaughter(churchillId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val churchill = repository.getPlayerById(churchillId) ?: return@launch
            val target = checkAndTransformMajhool(churchillId, targetId)

            if (target.isInsuredThisNight) {
                repository.addLog("🛡️ اقدام ناموفق: بازیکن هدف «${target.name}» بیمه است و اثر قابلیت چرچیل «${churchill.name}» روی او خنثی شد.")
                return@launch
            }

            if (churchill.isBlockedThisNight) {
                repository.addLog("⚠️ خطا: قابلیت چرچیل «${churchill.name}» امشب توسط ماتادور مسدود شده است.")
                return@launch
            }

            // Check remaining capability count first
            if (churchill.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(churchill.capabilitiesJson)
                    val slaughterCap = caps.find { it.name.contains("سلاخی") }
                    if (slaughterCap != null && slaughterCap.remainingCount <= 0) {
                        repository.addLog("⚠️ خطا: چرچیل دیگر سلاخی مجاز باقی‌آمده ندارد.")
                        return@launch
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Decrement the capability
            var updatedChurchill = churchill
            if (churchill.capabilitiesJson.isNotBlank()) {
                try {
                    val caps = Json.decodeFromString<List<RoleCapability>>(churchill.capabilitiesJson)
                    val updatedCaps = caps.map { cap ->
                        if (cap.name.contains("سلاخی") && cap.remainingCount > 0) {
                            cap.copy(remainingCount = cap.remainingCount - 1)
                        } else cap
                    }
                    updatedChurchill = churchill.copy(capabilitiesJson = Json.encodeToString(updatedCaps))
                    repository.updatePlayer(updatedChurchill)
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Churchill kills target absolutely (Even kills Godfather)
            val deadTarget = target.copy(
                isAlive = false,
                isSlaughtered = true, // Bypasses doctor's heal
                isSaved = false, // Neutralizes standard save
                isShotThisNight = true
            )
            repository.updatePlayer(deadTarget)
            repository.addLog("💀 اقدام چرچیل: بازیکن «${target.name}» توسط چرچیل سلاخی شد و به طور کامل حذف گردید! نجات پزشک بی‌اثر است.")

            if (_selectedPlayerForSettings.value?.id == churchillId) {
                _selectedPlayerForSettings.value = repository.getPlayerById(churchillId)
            }
        }
    }
}
