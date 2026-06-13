package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.GameHistoryEntity
import com.example.data.model.RoleCapability
import com.example.data.repository.MafiaRepository
import kotlinx.coroutines.Dispatchers
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
    val isSelected: Boolean = true,
    val isBurnt: Boolean = false
)

class MafiaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MafiaRepository
    val players: StateFlow<List<PlayerEntity>>
    val roles: StateFlow<List<RoleEntity>>
    val gameLogs: StateFlow<List<GameLogEntity>>
    val gameHistory: StateFlow<List<GameHistoryEntity>>

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

    // Last Move Cards list
    private val _lastMoveCards = MutableStateFlow(
        listOf(
            LastMoveCard(1, "ذهن زیبا 🧠", "بازیکن یک نقش را حدس می‌زند، در صورت حدس درست در بازی می‌ماند."),
            LastMoveCard(2, "شلیک پایانی 🔫", "بازیکن کشته شده روز، مستقیماً شلیک پایانی تیم خود را مشخص می‌کند."),
            LastMoveCard(3, "فرش قرمز 🔴", "بازیکن فردا مستقیماً بدون نیاز به پروسه رسمی رای به دفاعیه می‌رود."),
            LastMoveCard(4, "مسیر صحبت 🟢", "معافیت کامل بازیکن از روند رای‌گیری فردا."),
            LastMoveCard(5, "سرشماری 📊", "استعلام دقیق تعداد افراد زنده هر جناح توسط راوی بازی."),
            LastMoveCard(6, "وصیت‌نامه 📝", "فرصت نطق پایانی پایانی بمدت ۱ دقیقه کامل بدون امکان قطع صحبت.")
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
            LastMoveCard(1, "ذهن زیبا 🧠", "بازیکن یک نقش را حدس می‌زند، در صورت حدس درست در بازی می‌ماند."),
            LastMoveCard(2, "شلیک پایانی 🔫", "بازیکن کشته شده روز، مستقیماً شلیک پایانی تیم خود را مشخص می‌کند."),
            LastMoveCard(3, "فرش قرمز 🔴", "بازیکن فردا مستقیماً بدون نیاز به پروسه رسمی رای به دفاعیه می‌رود."),
            LastMoveCard(4, "مسیر صحبت 🟢", "معافیت کامل بازیکن از روند رای‌گیری فردا."),
            LastMoveCard(5, "سرشماری 📊", "استعلام دقیق تعداد افراد زنده هر جناح توسط راوی بازی."),
            LastMoveCard(6, "وصیت‌نامه 📝", "فرصت نطق پایانی پایانی بمدت ۱ دقیقه کامل بدون امکان قطع صحبت.")
        )
    }

    // Preset capabilities
    private val _capabilityTemplates = MutableStateFlow(
        listOf("استعلام وضعیت 🔍", "شفا / نجات 🩺", "شلیک شبانه 🔫", "تفنگ جنگی ⚔️", "تفنگ مشقی 🔫", "سکوت فردا 🔇", "محافظ زره 🛡️", "پیشنهاد مذاکره 🤝", "انفجار انتحاری 💣")
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
            "استعلام وضعیت 🔍", "شفا / نجات 🩺", "شلیک شبانه 🔫", "تفنگ جنگی ⚔️", "تفنگ مشقی 🔫", "سکوت فردا 🔇", "محافظ زره 🛡️", "پیشنهاد مذاکره 🤝", "انفجار انتحاری 💣"
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
            when {
                type == "KILL" -> {
                    if (p.isHealedThisNight) {
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
                    if (p.isHealedThisNight) {
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
                else -> "دلیل منطقی / شات بازی 🎯"
            }
            val updated = p.copy(isAlive = false)
            repository.updatePlayer(updated)
            repository.addLog("💀 بازیکن «${p.name}» به علت [$reasonText] از بازی حذف شد.")
            if (_selectedPlayerForSettings.value?.id == id) {
                _selectedPlayerForSettings.value = updated
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
                    val finalAlive = if (it.isKilledToday) false else it.isAlive
                    if (it.isKilledToday) {
                        repository.addLog("💀 مرگ بازیکن «${it.name}» در روز جاری به دلیل عدم ثبت نجات، نهایی شد.")
                    }
                    it.copy(
                        voteCount = 0,
                        isSaved = false,  // reset night buffers as we enter custom night
                        isHealedThisNight = false,
                        isShotThisNight = false,
                        isAlive = finalAlive,
                        isKilledToday = false,
                        isMuted = false, // Day ends, mute expires
                        isVoteRevoked = false // Day ends, vote restriction expires
                    )
                }
                repository.insertPlayers(updated)
            } else {
                // If entering day, we reset night variables like block
                val updated = players.value.map {
                    it.copy(
                        isBlocked = false // Night ends, block expires
                    )
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
                    doctorSelfSavesCount = 0,
                    capabilitiesJson = "",
                    hasUsedLastMoveCard = false,
                    note = "",
                    voteCount = 0,
                    warningsCount = 0,
                    isSlaughtered = false
                )
            }
            repository.insertPlayers(restored)
            repository.clearLogs()
            _remainingInquiries.value = _totalInquiries.value
            _gameStage.value = "SETUP"
            _gamePhase.value = "Night"
            _selectedPlayerForSettings.value = null
            repository.addLog("کل سوابق بازی بازنشانی شد و مجدداً به صفحه تنظیمات بازگشتیم 🔄")
        }
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

    fun professionalShoot(professionalId: Int, targetId: Int, overrideKill: Boolean? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = repository.getPlayerById(professionalId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch
            
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
                    repository.addLog("💀 بازیکن «${target.name}» (پدرخوانده/چرچیل) با تأیید گرداننده تحت شلیک مستقیم «${prof.name}» (حرفه‌ای) کشته و از بازی حذف شد.")
                } else {
                    // Survive
                    val updatedTarget = target.copy(isShotThisNight = true, isAlive = true)
                    repository.updatePlayer(updatedTarget)
                    repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به بازیکن «${target.name}» (پدرخوانده/چرچیل) شلیک کرد اما با تصمیم لغو گرداننده زنده ماند.")
                }
            } else {
                // Rules:
                // 1. Target is a Citizen (Team is Citizen) -> Professional is eliminated
                if (target.assignedRoleTeam == "Citizen") {
                    // Professional commits suicide
                    val deadProf = prof.copy(isAlive = false, isSaved = false)
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
                    val isChurchill = target.assignedRoleName?.contains("چرچیل") == true
                    if (isChurchill) {
                        // Survive
                        repository.addLog("🛡️ بازیکن «${prof.name}» (حرفه‌ای) به «${target.name}» (چرچیل) شلیک کرد اما تیر بر روی او بی‌اثر بود.")
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
                    if (target.isHealedThisNight) {
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
            
            val refreshedProf = repository.getPlayerById(professionalId)
            if (_selectedPlayerForSettings.value?.id == professionalId) {
                _selectedPlayerForSettings.value = refreshedProf
            }
        }
    }

    fun doctorHeal(doctorId: Int, targetId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = repository.getPlayerById(doctorId) ?: return@launch
            val target = repository.getPlayerById(targetId) ?: return@launch
            
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
            val target = repository.getPlayerById(targetId) ?: return@launch

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

            // Check if Tough Guy (جان‌سخت)
            val isToughGuy = freshTarget.assignedRoleName?.contains("جان") == true && freshTarget.assignedRoleName?.contains("سخت") == true
            
            if (isToughGuy) {
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
            
            val refreshedGf = repository.getPlayerById(godfatherId)
            if (_selectedPlayerForSettings.value?.id == godfatherId) {
                _selectedPlayerForSettings.value = refreshedGf
            }
        }
    }
}
