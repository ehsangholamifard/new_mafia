package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.OutputStream
import java.io.InputStream
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.viewmodel.LastMoveCard
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.RoleCapability
import com.example.data.model.getRoleAbilities
import com.example.data.viewmodel.MafiaViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Premium Palette (Deep Charcoal/Navy & Elegant Accents)
val BackgroundDark = Color(0xFF0B0A14) // Sleek deep navy/charcoal background
val SurfaceDark = Color(0xFF141324)    // Elevated elegant card/dialog container
val BorderColor = Color(0xFF25233D)    // Subdued modern borders of containers
val AccentCrimson = Color(0xFFFF4D5A)  // Sleek vibrant crimson accent for Mafia
val AccentCitizen = Color(0xFF38BDF8)  // Subtle gorgeous azure blue for Citizens
val AccentGold = Color(0xFFF6C844)     // Subtle golden amber for independents/neutral/highlights
val TextPrimary = Color(0xFFF8FAFC)    // Clean modern off-white body/head text
val TextSecondary = Color(0xFF94A3B8)  // Readable slate-gray secondary metadata text

enum class AppScreen { HOME, SETUP, GAME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGameScreen(viewModel: MafiaViewModel) {
    // Force RTL local block for consistent elegant Farsi layouts
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val players by viewModel.players.collectAsStateWithLifecycle()
        val roles by viewModel.roles.collectAsStateWithLifecycle()
        val logs by viewModel.gameLogs.collectAsStateWithLifecycle()
        val currentRound = remember(logs) {
            logs.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
        }
        val stage by viewModel.gameStage.collectAsStateWithLifecycle()
        val phase by viewModel.gamePhase.collectAsStateWithLifecycle()
        val selectedPlayerSettings by viewModel.selectedPlayerForSettings.collectAsStateWithLifecycle()
        val capabilityTemplates by viewModel.capabilityTemplates.collectAsStateWithLifecycle()
        val lastMoveCards by viewModel.lastMoveCards.collectAsStateWithLifecycle()
        val gameHistory by viewModel.gameHistory.collectAsStateWithLifecycle()
        val totalInquiries by viewModel.totalInquiries.collectAsStateWithLifecycle()
        val remainingInquiries by viewModel.remainingInquiries.collectAsStateWithLifecycle()
        val moderatorName by viewModel.moderatorName.collectAsStateWithLifecycle()
        val musketeerLiveGunExhausted by viewModel.musketeerLiveGunExhausted.collectAsStateWithLifecycle()
        val sagiCooldownNight by viewModel.sagiCooldownNight.collectAsStateWithLifecycle()
        val sagiPastTargets by viewModel.sagiPastTargets.collectAsStateWithLifecycle()

        var currentScreen by remember {
            mutableStateOf(
                if (stage == "DISTRIBUTION" || stage == "PLAY") AppScreen.GAME else AppScreen.HOME
            )
        }

        LaunchedEffect(stage) {
            if (stage == "DISTRIBUTION" || stage == "PLAY") {
                currentScreen = AppScreen.GAME
            } else if (stage == "SETUP" && currentScreen == AppScreen.GAME) {
                currentScreen = AppScreen.HOME
            }
        }

        var showCapabilitiesTemplateDialog by remember { mutableStateOf(false) }
        var showExportImportDialog by remember { mutableStateOf(false) }
        var showAddCustomRoleDialog by remember { mutableStateOf(false) }
        var showHistoryDialog by remember { mutableStateOf(false) }
        var showModeratorNameDialog by remember { mutableStateOf(false) }
        var roleToConfigureCapabilities by remember { mutableStateOf<RoleEntity?>(null) }
        var roleToConfigureAbilities by remember { mutableStateOf<RoleEntity?>(null) }
        var shooterForLiveGun by remember { mutableStateOf<PlayerEntity?>(null) }
        var targetResultForLiveGun by remember { mutableStateOf<Pair<String, String>?>(null) }

        // --- Terrorist Custom Day-phase Reaction State ---
        var showTerroristSelectionDialog by remember { mutableStateOf<PlayerEntity?>(null) }
        var showTerrorAlertMessage by remember { mutableStateOf<String?>(null) }

        // Day Phase Timer Hoisted State
        var timerSelectedTime by rememberSaveable { mutableStateOf(60) }
        var timerRemaining by rememberSaveable { mutableStateOf(60) }
        var timerIsRunning by rememberSaveable { mutableStateOf(false) }
        var showTimerModal by rememberSaveable { mutableStateOf(false) }

        // --- Custom Polish Confirmation Dialog State ---
        var showConfirmDialog by remember { mutableStateOf(false) }
        var confirmDialogTitle by remember { mutableStateOf("") }
        var confirmDialogMessage by remember { mutableStateOf("") }
        var confirmDialogOnConfirm by remember { mutableStateOf<() -> Unit>({}) }
        var showMatadorBlockedAlert by remember { mutableStateOf(false) }

        var showExitDialog by remember { mutableStateOf(false) }
        BackHandler(enabled = true) {
            if (currentScreen == AppScreen.SETUP) {
                currentScreen = AppScreen.HOME
            } else {
                showExitDialog = true
            }
        }

        val triggerConfirmation = { title: String, message: String, onConfirm: () -> Unit ->
            confirmDialogTitle = title
            confirmDialogMessage = message
            confirmDialogOnConfirm = onConfirm
            showConfirmDialog = true
        }

        LaunchedEffect(timerIsRunning, timerRemaining) {
            if (timerIsRunning && timerRemaining > 0) {
                delay(1000L)
                timerRemaining -= 1
            } else if (timerRemaining == 0) {
                timerIsRunning = false
            }
        }

        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    onStartNewGame = { currentScreen = AppScreen.SETUP },
                    onShowHistory = {
                        Toast.makeText(context, "به زودی...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            AppScreen.SETUP, AppScreen.GAME -> {
                Scaffold(
                    topBar = {
                        GameHeaderBar(
                            stage = stage,
                            phase = phase,
                            onReset = { viewModel.resetGame() },
                            onExportImport = { showExportImportDialog = true }
                        )
                    },
                    containerColor = BackgroundDark,
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Content Wrapper (Adaptive for landscape or larger widths like tablets)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp)
                ) {
                    when (stage) {
                        "SETUP" -> SetupStageContent(
                            players = players,
                            roles = roles,
                            lastMoveCards = lastMoveCards,
                            onAddPlayer = { viewModel.addPlayer(it) },
                            onTogglePlayer = { viewModel.togglePlayerSelection(it) },
                            onDeletePlayer = { viewModel.deletePlayer(it) },
                            onIncrementRole = { roleId ->
                                val rObj = roles.find { it.id == roleId }
                                if (rObj != null) {
                                    val isUnique = !rObj.name.contains("ساده")
                                    if (isUnique && rObj.count >= 1) {
                                        Toast.makeText(context, "از این نقش فقط یک عدد میتواند در بازی حضور داشته باشد.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (rObj.name.contains("ساده")) {
                                            viewModel.updateRoleCount(roleId, rObj.count + 1)
                                        } else {
                                            roleToConfigureCapabilities = rObj
                                        }
                                    }
                                }
                            },
                            onDecrementRole = { roleId ->
                                val rObj = roles.find { it.id == roleId }
                                if (rObj != null) {
                                    if (rObj.name.contains("ساده")) {
                                        viewModel.updateRoleCount(roleId, (rObj.count - 1).coerceAtLeast(0))
                                    } else {
                                        viewModel.updateRoleCount(roleId, 0)
                                    }
                                }
                            },
                            onResetRoles = { viewModel.resetRolesToDefaults() },
                            onManageTemplates = { showCapabilitiesTemplateDialog = true },
                            onStartGame = { showModeratorNameDialog = true },
                            onAddCustomRoleRequest = { showAddCustomRoleDialog = true },
                            onExportImport = { showExportImportDialog = true },
                            onShowHistory = { showHistoryDialog = true },
                            onAddLastMoveCard = { name, desc -> viewModel.addLastMoveCard(name, desc) },
                            onToggleLastMoveCardSelection = { viewModel.toggleLastMoveCardSelection(it) },
                            onResetLastMoveCards = { viewModel.resetLastMoveCards() },
                            totalInquiries = totalInquiries,
                            onSetTotalInquiries = { viewModel.setTotalInquiries(it) },
                            onEditAbilities = { roleToConfigureAbilities = it }
                        )
                        "DISTRIBUTION" -> SecretDistributionContent(
                            players = players.filter { it.isSelected },
                            onConfirmStart = { viewModel.advanceToPlayStage() }
                        )
                        "PLAY" -> PlayStageContent(
                            players = players,
                            roles = roles,
                            phase = phase,
                            logs = logs,
                            lastMoveCards = lastMoveCards,
                            onTogglePhase = {
                                viewModel.toggleGamePhase()
                            },
                            onUpdatePlayerVoteDirectly = { id, votes ->
                                viewModel.updatePlayerVote(id, votes)
                            },
                            onPlayerClick = { clickedPlayer ->
                                if (phase == "Night" && clickedPlayer.isBlockedThisNight) {
                                    showMatadorBlockedAlert = true
                                } else {
                                    viewModel.selectPlayerForSettings(clickedPlayer)
                                }
                            },
                            onRegisterEvent = { id, type ->
                                val p = players.find { it.id == id }
                                val pName = p?.name ?: ""
                                when (type) {
                                    "KILL" -> triggerConfirmation(
                                        "ثبت قتل بازیکن 💀",
                                        "آیا مایل به ثبت شلیک/قتل برای بازیکن «$pName» در فاز شب هستید؟"
                                    ) { viewModel.registerNightEvent(id, type) }
                                    "SLAUGHTER" -> triggerConfirmation(
                                        "ثبت سلاخی بازیکن 🔪",
                                        "آیا مایل به ثبت سلاخی برای بازیکن «$pName» در فاز شب هستید؟ (نجات دکتری روی این وضعیت بی اثر خواهد بود)"
                                    ) { viewModel.registerNightEvent(id, type) }
                                    "MUTE" -> triggerConfirmation(
                                        "ثبت سکوت بازیکن 🔇",
                                        "آیا مایل به سایلنت کردن بازیکن «$pName» تا پایان روز بعد هستید؟"
                                    ) { viewModel.registerNightEvent(id, type) }
                                    "BLOCK" -> triggerConfirmation(
                                        "بلاک کردن بازیکن 🚫",
                                        "آیا از مسدودیت (بلاک) قابلیت شب بازیکن «$pName» اطمینان دارید؟"
                                    ) { viewModel.registerNightEvent(id, type) }
                                    "SAVE" -> triggerConfirmation(
                                        "نجات بازیکن 🩺",
                                        "آیا مایل به ثبت نجات (شفا) شب برای بازیکن «$pName» هستید؟"
                                    ) { viewModel.registerNightEvent(id, type) }
                                    else -> viewModel.registerNightEvent(id, type)
                                }
                            },
                            onToggleBlock = { id ->
                                val p = players.find { it.id == id }
                                val pName = p?.name ?: ""
                                val isBlocked = p?.isBlocked ?: false
                                val actionStr = if (isBlocked) "رفع مسدودیت" else "مسدود کردن (بلاک)"
                                triggerConfirmation(
                                    "$actionStr بازیکن 🚫",
                                    "آیا مطمئن هستید که می‌خواهید بازیکن «$pName» را $actionStr کنید؟"
                                ) { viewModel.togglePlayerBlock(id) }
                            },
                            onToggleMute = { id ->
                                val p = players.find { it.id == id }
                                val pName = p?.name ?: ""
                                val isMuted = p?.isMuted ?: false
                                val actionStr = if (isMuted) "رفع سکوت" else "اعمال سکوت"
                                triggerConfirmation(
                                    "$actionStr بازیکن 🔇",
                                    "آیا مایل به تغییر وضعیت سکوت بازیکن «$pName» به حالت [$actionStr] هستید؟"
                                ) { viewModel.togglePlayerMute(id) }
                            },
                            onToggleLife = { id ->
                                val p = players.find { it.id == id }
                                val pName = p?.name ?: ""
                                val isAlive = p?.isAlive ?: false
                                val actionStr = if (isAlive) "خروج/کشته شدن" else "احیا/زنده شدن مجدد"
                                triggerConfirmation(
                                    "تغییر وضعیت حیات 💀",
                                    "آیا مایل به تغییر وضعیت حیات بازیکن «$pName» به [$actionStr] هستید؟"
                                ) { viewModel.togglePlayerLife(id) }
                            },
                            onClearLogs = {
                                triggerConfirmation(
                                    "پاکسازی تاریخچه وقایع 📋",
                                    "آیا مطمئن هستید که می‌خواهید تمام لاگ‌ها و وقایع بازی فعلی را پاک کنید؟"
                                ) { viewModel.clearLogs() }
                            },
                            onBurnLastMoveCard = { viewModel.burnLastMoveCard(it) },
                            onSaveGameOverReport = { winner, reason -> viewModel.saveGameOverReport(winner, reason) },
                            onResetGame = {
                                triggerConfirmation(
                                    "شروع مجدد بازی 🔄",
                                    "آیا مطمئن هستید که می‌خواهید بازی کنونی را کلاً متوقف کرده و صفر کنید؟"
                                ) { viewModel.resetGame() }
                            },
                            timerSelectedTime = timerSelectedTime,
                            timerRemaining = timerRemaining,
                            timerIsRunning = timerIsRunning,
                            showTimerModal = showTimerModal,
                            onShowTimerModalChange = { showTimerModal = it },
                            onTimerSelectedTimeChange = { timerSelectedTime = it },
                            onTimerRemainingChange = { timerRemaining = it },
                            onTimerIsRunningChange = { timerIsRunning = it },
                            totalInquiries = totalInquiries,
                            remainingInquiries = remainingInquiries,
                            onDecrementInquiry = { viewModel.decrementInquiry() },
                            triggerConfirmation = triggerConfirmation,
                            onUseLiveGun = { shooterForLiveGun = it },
                            onExecuteVeto = { vetoId, targetId -> viewModel.executeVeto(vetoId, targetId) },
                            viewModel = viewModel,
                            musketeerLiveGunExhausted = musketeerLiveGunExhausted
                        )
                    }
                }

                // Selected Player Quick Settings Dialog
                val currentPlayerSettings = selectedPlayerSettings
                if (currentPlayerSettings != null) {
                    PlayerSettingsDialog(
                        player = currentPlayerSettings,
                        capabilityTemplates = capabilityTemplates,
                        phase = phase,
                        lastMoveCards = lastMoveCards,
                        onDismiss = { viewModel.selectPlayerForSettings(null) },
                        onUpdateNote = { id, note -> viewModel.updatePlayerNote(id, note) },
                        onUseCapability = { id, name ->
                            val pName = currentPlayerSettings.name
                            triggerConfirmation(
                                "استفاده از قابلیت ⚡",
                                "آیا مایل به استفاده از قابلیت «$name» توسط بازیکن «$pName» هستید؟"
                            ) { viewModel.usePlayerCapability(id, name) }
                        },
                        onToggleLastMove = { id ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val hasUsed = p?.hasUsedLastMoveCard ?: false
                            val actionStr = if (hasUsed) "حذف وضعیت وصیت صادر شده" else "اعمال قرعه‌کشی وصیت"
                            triggerConfirmation(
                                "کارت حرکت پایانی 🃏",
                                "آیا مایل به تغییر وضعیت صدور کارت وصیت برای بازیکن «$pName» هستید؟"
                            ) { viewModel.toggleLastMoveCard(id) }
                        },
                        onBurnLastMoveCard = { cardId -> viewModel.burnLastMoveCard(cardId) },
                        onToggleMute = { id ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val isMuted = p?.isMuted ?: false
                            val actionStr = if (isMuted) "رفع سکوت 🔊" else "سکوت انضباطی 🔇"
                            triggerConfirmation(
                                "سکوت انضباطی 🔇",
                                "آیا از اعمال/لغو وضعیت [$actionStr] برای بازیکن «$pName» اطمینان دارید؟"
                            ) { viewModel.togglePlayerMute(id) }
                        },
                        onToggleVoteRevoked = { id ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val isRevoked = p?.isVoteRevoked ?: false
                            val actionStr = if (isRevoked) "بازگرداندن حق رأی ✅" else "سلب حق رأی انضباطی ❌"
                            triggerConfirmation(
                                "حق رأی بازیکن ⚖️",
                                "آیا از [$actionStr] برای بازیکن «$pName» در رای‌گیری امروز اطمینان دارید؟"
                            ) { viewModel.togglePlayerVoteRevoke(id) }
                        },
                        onUpdateVotes = { id, count ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val originalVotes = p?.voteCount ?: 0
                            val targetVotes = count.coerceAtLeast(0)
                            if (targetVotes != originalVotes) {
                                triggerConfirmation(
                                    "تغییر آرای مأخوذه 🗳️",
                                    "آیا مطمئن هستید که می‌خواهید تعداد آرای ثبت شده بازیکن «$pName» را به [$targetVotes] رأی تغییر دهید؟"
                                ) { viewModel.updatePlayerVote(id, targetVotes) }
                            }
                        },
                        onUpdateWarnings = { id, count ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val originalWarnings = p?.warningsCount ?: 0
                            val targetWarnings = count.coerceIn(0, 3)
                            if (targetWarnings != originalWarnings) {
                                triggerConfirmation(
                                    "ثبت اخطار انضباطی ⚠️",
                                    "آیا مایلید وضعیت اخطارهای انضباطی بازیکن «$pName» را به [$targetWarnings اخطار] تغییر دهید؟"
                                ) { viewModel.updatePlayerWarnings(id, targetWarnings) }
                            }
                        },
                        onEliminateWithReason = { id, type ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val reasonStr = when (type) {
                                "VOTE" -> "با رأی‌گیری روز ⚖️"
                                "DISCIPLINARY" -> "به دلیل تصمیم انضباطی گاد 🛑"
                                "LOGICAL" -> "به دلیل استدلال منطقی سناریو 🎯"
                                else -> type
                            }
                            if (p != null && type == "VOTE" && (getRoleAbilities(p.assignedRoleName ?: "").contains("TERROR") || p.assignedRoleName?.contains("ترور") == true)) {
                                if (p.isBlockedThisNight || p.wasBlockedLastNight) {
                                    triggerConfirmation(
                                        "مرگ تروریست (مسدود شده) 💀",
                                        "بازیکن «$pName» تروریست است اما شب گذشته مسدود/مست شده بود. آیا مایل به حذف او هستید؟"
                                    ) {
                                        showTerrorAlertMessage = "تروریست شب گذشته مسدود/مست شده بود و قابلیت ترور عمل نمیکند!"
                                        viewModel.eliminatePlayerWithReason(id, type)
                                    }
                                } else {
                                    triggerConfirmation(
                                        "فعال‌سازی ترور 💣",
                                        "بازیکن «$pName» تروریست است و در رای‌گیری حذف شد! آیا مایل به فعال‌سازی ترور و انتخاب قربانی هستید؟"
                                    ) {
                                        showTerroristSelectionDialog = p
                                    }
                                }
                            } else {
                                triggerConfirmation(
                                    "مرگ قطعی بازیکن 💀",
                                    "آیا از حذف کامل بازیکن «$pName» از سناریو [$reasonStr] مطمئن هستید؟"
                                ) { viewModel.eliminatePlayerWithReason(id, type) }
                            }
                        },
                        onReviveWithReason = { id, type ->
                            val p = players.find { it.id == id }
                            val pName = p?.name ?: ""
                            val reasonStr = when (type) {
                                "RETURN" -> "بازگشت به سناریو 🤝"
                                "RESURRECT" -> "زنده شدن ثانویه سناریوی بازی 🩺"
                                else -> type
                            }
                            triggerConfirmation(
                                "احیای بازیکن از گورستان 🟢",
                                "آیا مایل به احیا و زنده کردن مجدد بازیکن «$pName» [$reasonStr] هستید؟"
                            ) { viewModel.revivePlayerWithReason(id, type) }
                        },
                        players = players,
                        onProfessionalShoot = { profId, targetId, overrideKill ->
                            viewModel.professionalShoot(profId, targetId, overrideKill)
                        },
                        onProfessionalSlaughter = { profId, targetId ->
                            viewModel.professionalSlaughter(profId, targetId)
                        },
                        onDoctorHeal = { docId, targetId ->
                            viewModel.doctorHeal(docId, targetId)
                        },
                        onGodfatherShoot = { gfId, targetId ->
                            viewModel.godfatherShoot(gfId, targetId)
                        },
                        onGodfatherSlaughter = { gfId, targetId ->
                            viewModel.godfatherSlaughter(gfId, targetId)
                        },
                        onGodfatherRecruit = { gfId, targetId, onResult ->
                            viewModel.godfatherRecruit(gfId, targetId, onResult)
                        },
                        onBuyerRecruit = { buyerId, targetId, onResult ->
                            viewModel.buyerRecruit(buyerId, targetId, onResult)
                        },
                        onMatadorBlock = { matadorId, targetId ->
                            viewModel.matadorBlock(matadorId, targetId)
                        },
                        onGeneralCheck = { generalId, targetId, onResult ->
                            viewModel.generalCheck(generalId, targetId, onResult)
                        },
                        onConstantineRevive = { constantineId, targetId ->
                            viewModel.constantineRevive(constantineId, targetId)
                        },
                        onCitizenKaneReveal = { kaneId, targetId, onResult ->
                            viewModel.citizenKaneReveal(kaneId, targetId, onResult)
                        },
                        onGiveGun = { musketeerId, targetId, isLive ->
                            viewModel.giveMusketeerGun(musketeerId, targetId, isLive)
                        },
                        musketeerLiveGunExhausted = musketeerLiveGunExhausted
                    )
                }

                // Predefined Capability Templates Management Dialog
                if (showCapabilitiesTemplateDialog) {
                    CapabilitiesManagementDialog(
                        templates = capabilityTemplates,
                        onDismiss = { showCapabilitiesTemplateDialog = false },
                        onAdd = { viewModel.addCapabilityTemplate(it) },
                        onDelete = { viewModel.deleteCapabilityTemplate(it) },
                        onReset = { viewModel.resetCapabilityTemplatesToDefaults() }
                    )
                }

                // Export/Import Game Settings Config Dialog
                if (showExportImportDialog) {
                    ExportImportDialog(
                        onDismiss = { showExportImportDialog = false },
                        exportData = { viewModel.exportSetupAsJson() },
                        onImport = { viewModel.importSetupFromJson(it) }
                    )
                }

                // 💥 Use Live Gun Shooting Selection Dialog
                val shooter = shooterForLiveGun
                if (shooter != null) {
                    val availableTargets = remember(players) {
                        players.filter { it.isSelected && it.isAlive && it.id != shooter.id }
                    }
                    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    val selectedTarget = remember(selectedTargetId, players) { players.find { it.id == selectedTargetId } }

                    Dialog(onDismissRequest = { shooterForLiveGun = null }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(AccentCrimson.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💥", fontSize = 24.sp)
                                }

                                Text(
                                    text = "شلیک با تفنگ جنگی 💥",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "بازیکن مسلح «${shooter.name}» می‌خواهد از تفنگ جنگی خود استفاده کند. هدف شلیک را از لیست زیر انتخاب کنید:",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .clickable { dropdownExpanded = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedTarget?.name ?: "انتخاب بازیکن هدف شلیک... 💀",
                                            color = if (selectedTarget != null) Color.White else Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentCrimson)
                                    }
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f).background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        if (availableTargets.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("هیچ بازیکن زنده دیگری یافت نشد", color = Color.Gray, fontSize = 12.sp) },
                                                onClick = { dropdownExpanded = false }
                                            )
                                        } else {
                                            availableTargets.forEach { t ->
                                                DropdownMenuItem(
                                                    text = { Text("${t.name} (${t.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 12.sp) },
                                                    onClick = {
                                                        selectedTargetId = t.id
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { shooterForLiveGun = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2A)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Text("انصراف", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (selectedTarget != null) {
                                                triggerConfirmation(
                                                    "تأیید نهایی اعدام با تفنگ 💀",
                                                    "آیا مطمئن هستید که می‌خواهید بازیکن «${selectedTarget.name}» مستقیماً توسط تفنگ جنگی «${shooter.name}» کشته و حذف شود؟"
                                                ) {
                                                    viewModel.useLiveGun(shooter.id, selectedTarget.id) { deadName, faction ->
                                                        targetResultForLiveGun = Pair(deadName, faction)
                                                    }
                                                    shooterForLiveGun = null
                                                }
                                            }
                                        },
                                        enabled = selectedTarget != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF2C2C35)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.0f).testTag("shoot_confirm_btn")
                                    ) {
                                        Text("شلیک 💥", color = if (selectedTarget != null) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 📢 Revealing Target Faction Modal
                val result = targetResultForLiveGun
                if (result != null) {
                    if (result.second == "SABOTAGED") {
                        StyledConfirmationDialog(
                            title = "شلیک ناموفق - تفنگ خرابکاری‌شده! ⚠️🔫",
                            message = "تفنگ خرابکاری شده بود! شلیک به خود «${result.first}» برگشت و او کشته شد.",
                            onConfirm = { targetResultForLiveGun = null },
                            onDismiss = { targetResultForLiveGun = null }
                        )
                    } else {
                        StyledConfirmationDialog(
                            title = "نتیجه شلیک تفنگ جنگی 📢💥",
                            message = "هدف غرق در خون شد و در دم کشته شد! \n\n👤 نام هدف جدید: «${result.first}»\n🕊️ جناح واقعی او: [${result.second}]",
                            onConfirm = { targetResultForLiveGun = null },
                            onDismiss = { targetResultForLiveGun = null }
                        )
                    }
                }

                // Add Custom Role Dialog
                if (showAddCustomRoleDialog) {
                    RoleManagerDialog(
                        roles = roles,
                        viewModel = viewModel,
                        templates = capabilityTemplates,
                        onDismiss = { showAddCustomRoleDialog = false }
                    )
                }

                // Game History Dialog
                if (showHistoryDialog) {
                    GameHistoryDialog(
                        history = gameHistory,
                        onDismiss = { showHistoryDialog = false }
                    )
                }

                // Moderator Name Prompt Dialog
                if (showModeratorNameDialog) {
                    ModeratorNamePromptDialog(
                        initialValue = moderatorName,
                        onConfirm = { name ->
                            viewModel.setModeratorName(name)
                            viewModel.distributeRolesAndStartGame()
                            showModeratorNameDialog = false
                        },
                        onDismissRequest = { showModeratorNameDialog = false }
                    )
                }

                // Configure Capabilities for an Existing Role Dialog
                val roleToConfig = roleToConfigureCapabilities
                if (roleToConfig != null) {
                    RoleCapabilitiesConfigDialog(
                        role = roleToConfig,
                        templates = capabilityTemplates,
                        onDismiss = { roleToConfigureCapabilities = null },
                        onConfirm = { id, capsJson ->
                            viewModel.updateRoleCountAndCapabilities(id, 1, capsJson)
                        }
                    )
                }

                val roleToConfigAbilities = roleToConfigureAbilities
                if (roleToConfigAbilities != null) {
                    RoleAbilityManagerDialog(
                        role = roleToConfigAbilities,
                        onDismiss = { roleToConfigureAbilities = null },
                        onSave = { id, selectedAbilityIds ->
                            val jsonString = Json.encodeToString(selectedAbilityIds)
                            viewModel.updateRoleAbilities(id, jsonString)
                            roleToConfigureAbilities = null
                        }
                    )
                }

                if (showConfirmDialog) {
                    StyledConfirmationDialog(
                        title = confirmDialogTitle,
                        message = confirmDialogMessage,
                        onConfirm = confirmDialogOnConfirm,
                        onDismiss = { showConfirmDialog = false }
                    )
                }

                if (showMatadorBlockedAlert) {
                    StyledConfirmationDialog(
                        title = "بلاک شبانه ماتادور 🧣",
                        message = "قابلیت این نقش امشب توسط ماتادور بسته شده است.",
                        onConfirm = { showMatadorBlockedAlert = false },
                        onDismiss = { showMatadorBlockedAlert = false }
                    )
                }

                // --- Terrorist Action Dialog ---
                val activeTerrorist = showTerroristSelectionDialog
                if (activeTerrorist != null) {
                    TerroristSelectionDialog(
                        activeTerrorist = activeTerrorist,
                        players = players,
                        onConfirmTerror = { victim ->
                            viewModel.executeTerroristAction(activeTerrorist.id, victim.id)
                            showTerrorAlertMessage = "تروریست عملیات انتحاری انجام داد و «${victim.name}» را با خود برد! 💥"
                            showTerroristSelectionDialog = null
                        },
                        onNormalElimination = {
                            viewModel.eliminatePlayerWithReason(activeTerrorist.id, "VOTE")
                            showTerroristSelectionDialog = null
                        },
                        onDismiss = { showTerroristSelectionDialog = null }
                    )
                }

                val alertMsg = showTerrorAlertMessage
                if (alertMsg != null) {
                    StyledConfirmationDialog(
                        title = "عملیات تروریست 💣",
                        message = alertMsg,
                        onConfirm = { showTerrorAlertMessage = null },
                        onDismiss = { showTerrorAlertMessage = null }
                    )
                }
            }
        }
    }
}

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        text = "خروج از برنامه",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "آیا مطمئن هستید که میخواهید از برنامه خارج شوید؟ تمام اطلاعات بازی فعلی از بین خواهد رفت.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "بله، خارج میشوم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showExitDialog = false },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "خیر، ادامه میدهم",
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                },
                containerColor = SurfaceDark,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun StyledConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentGold.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "بله، تأیید است 👍",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "خیر، انصراف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPOSABLES & SECTIONS
// ==========================================

@Composable
fun GameHeaderBar(
    stage: String,
    phase: String,
    onReset: () -> Unit,
    onExportImport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            Brush.linearGradient(listOf(AccentCrimson, Color(0xFF9E2A2B))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "مافیا گاد 🎭",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                    Text(
                        text = when (stage) {
                            "SETUP" -> "تنظیمات بازیکنان و سناریو"
                            "DISTRIBUTION" -> "توزیع کارت‌های محرمانه"
                            else -> "مدیریت لایو بازی (${if (phase == "Night") "فاز شب 🌙" else "فاز روز ☀️"})"
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stage == "SETUP") {
                    IconButton(
                        onClick = onExportImport,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1C1C2E))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "خروجی/ورودی", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// 1. SETUP STAGE UI
// ------------------------------------------
@Composable
fun SetupStageContent(
    players: List<PlayerEntity>,
    roles: List<RoleEntity>,
    lastMoveCards: List<LastMoveCard>,
    onAddPlayer: (String) -> Unit,
    onTogglePlayer: (Int) -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit,
    onIncrementRole: (Int) -> Unit,
    onDecrementRole: (Int) -> Unit,
    onResetRoles: () -> Unit,
    onManageTemplates: () -> Unit,
    onStartGame: () -> Unit,
    onAddCustomRoleRequest: () -> Unit,
    onExportImport: () -> Unit,
    onShowHistory: () -> Unit,
    onAddLastMoveCard: (String, String) -> Unit,
    onToggleLastMoveCardSelection: (Int) -> Unit,
    onResetLastMoveCards: () -> Unit,
    totalInquiries: Int,
    onSetTotalInquiries: (Int) -> Unit,
    onEditAbilities: (RoleEntity) -> Unit
) {
    var playerInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val selectedCount = remember(players) { players.filter { it.isSelected }.size }
    val rolesChooseCount = remember(roles) { roles.sumOf { it.count } }
    val isEnabled = selectedCount > 0 && selectedCount == rolesChooseCount

    val bannerTargetBg = if (selectedCount == rolesChooseCount && selectedCount > 0) {
        Color(0xFF10B981) // Green success background
    } else {
        Color(0xFFEF4444) // Red/Orange warning background
    }

    val bannerBgColor by animateColorAsState(
        targetValue = bannerTargetBg,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bannerBgTransition"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedCount > 0 || rolesChooseCount > 0) {
            val bannerText = if (selectedCount == rolesChooseCount) {
                "تعداد بازیکنان و نقشها برابر است. آماده شروع!"
            } else {
                "تعداد بازیکنان ([$selectedCount]) با نقشها ([$rolesChooseCount]) برابر نیست!"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(100f)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp),
                        clip = false
                    )
                    .background(
                        color = bannerBgColor,
                        shape = RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bannerText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                top = if (selectedCount > 0 || rolesChooseCount > 0) 64.dp else 16.dp,
                bottom = 80.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
        // App intro banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مدیریت سناریو به سبک حرفه‌ای‌ها 👔",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "ابتدا بازیکنان را ثبت کرده، سپس نقش‌های سناریو را انتخاب و نهایتاً کارت‌ها را توزیع کنید.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Action Buttons Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onExportImport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E38)),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خروجی/ورودی 📤", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onShowHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A212E)),
                    border = BorderStroke(1.dp, AccentCitizen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تاریخچه بازی‌ها 📜", color = AccentCitizen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Player Registration
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "۱. ثبت بازیکنان زنده گروه (کل: ${players.size} نفر)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = playerInputText,
                        onValueChange = { playerInputText = it },
                        placeholder = { Text("نام بازیکن جدید...", fontSize = 13.sp, color = Color.Gray) },
                        maxLines = 1,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCrimson,
                            unfocusedBorderColor = BorderColor
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (playerInputText.isNotBlank()) {
                                onAddPlayer(playerInputText)
                                playerInputText = ""
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (playerInputText.isNotBlank()) {
                                onAddPlayer(playerInputText)
                                playerInputText = ""
                                focusManager.clearFocus()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن")
                    }
                }
            }
        }

        // Selected/Unselected players chips list
        if (players.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10101A)),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "انتخاب بازیکنان برای دست فعلی: (${players.filter { it.isSelected }.size} نفر منتخب)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            players.forEach { player ->
                                Row(
                                    modifier = Modifier
                                        .background(
                                            color = if (player.isSelected) AccentCrimson.copy(alpha = 0.15f) else Color(0xFF161626),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (player.isSelected) AccentCrimson else BorderColor,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { onTogglePlayer(player.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (player.isSelected) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (player.isSelected) AccentCrimson else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = player.name,
                                        color = if (player.isSelected) Color.White else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (player.isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = Color.Gray.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeletePlayer(player) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                EmptyListTip(text = "لیست بازیکنان خالی است. در کادر بالا نام آنها را بنویسید.")
            }
        }

        // Section 2: Roles choosing counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "۲. گزینش ساختار نقش‌های سناریو (مجموع: ${roles.sumOf { it.count }} نقش)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddCustomRoleRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مدیریت نقش‌ها 🎭", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onManageTemplates,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("الگوها ⚙️", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onResetRoles,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("پیش‌فرض 🔄", fontSize = 11.sp)
                    }
                }
            }
        }

        // Roles counters lists divided by Teams (Order: Independent, Mafia, Citizen)
        val citizenRoles = roles.filter { it.team == "Citizen" }
        val mafiaRoles = roles.filter { it.team == "Mafia" }
        val independentRoles = roles.filter { it.team == "Independent" }

        if (independentRoles.isNotEmpty()) {
            item {
                TeamRolesSection(
                    title = "نقش‌های مستقل 🎭",
                    teamColor = AccentGold,
                    rolesList = independentRoles,
                    onInc = onIncrementRole,
                    onDec = onDecrementRole,
                    onEditAbilities = onEditAbilities
                )
            }
        }

        item {
            TeamRolesSection(
                title = "تیم مافیا 🕶️",
                teamColor = AccentCrimson,
                rolesList = mafiaRoles,
                onInc = onIncrementRole,
                onDec = onDecrementRole,
                onEditAbilities = onEditAbilities
            )
        }

        item {
            TeamRolesSection(
                title = "تیم شهروندان 🕊️",
                teamColor = AccentCitizen,
                rolesList = citizenRoles,
                onInc = onIncrementRole,
                onDec = onDecrementRole,
                onEditAbilities = onEditAbilities
            )
        }

        // Conflict Warning Banner is now implemented as a top sticky header

        // Section 3: Last Move Cards configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "۳. مدیریت کارت‌های حرکت آخر 🃏",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = onResetLastMoveCards,
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentGold)
                        ) {
                            Text("پیش‌فرض 🔄", fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = "جهت فعال/غیرفعال کردن هر کارت برای بازی، روی آن ضربه بزنید:",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    // Last Move selection list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        lastMoveCards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (card.isSelected) Color(0xFF1E142F) else Color(0xFF13131F),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (card.isSelected) AccentGold.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onToggleLastMoveCardSelection(card.id) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = card.isSelected,
                                        onCheckedChange = { onToggleLastMoveCardSelection(card.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = AccentGold)
                                    )
                                    Column {
                                        Text(
                                            text = card.name,
                                            color = if (card.isSelected) Color.White else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = card.description,
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                                if (card.isBurnt) {
                                    Text(
                                        text = "🔥 سوخته شده",
                                        color = AccentCrimson,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Form to add a new card
                    var newCardName by remember { mutableStateOf("") }
                    var newCardDesc by remember { mutableStateOf("") }

                    Text(text = "افزودن کارت حرکت پایانی جدید:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newCardName,
                        onValueChange = { newCardName = it },
                        placeholder = { Text("نام کارت (مثال: شلیک نهایی پایانی 🔫)...", fontSize = 11.sp, color = Color.Gray) },
                        maxLines = 1,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCardDesc,
                        onValueChange = { newCardDesc = it },
                        placeholder = { Text("توضیحات و عملکرد کارت...", fontSize = 11.sp, color = Color.Gray) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (newCardName.isNotBlank()) {
                                onAddLastMoveCard(newCardName, newCardDesc)
                                newCardName = ""
                                newCardDesc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newCardName.isNotBlank()
                    ) {
                        Text("ثبت و افزودن کارت 🃏", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 4: Inquiry Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "۴. مدیریت استعلام‌های بازی (Inquiries) 🔍",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "تعداد کل استعلام‌های مجاز هماهنگ‌کننده برای این بازی را تنظیم کنید:",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = totalInquiries.toString(),
                            onValueChange = { newValue ->
                                val intVal = newValue.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onSetTotalInquiries(intVal)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = BorderColor
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { onSetTotalInquiries((totalInquiries - 1).coerceAtLeast(0)) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E1E2F), CircleShape)
                            ) {
                                Text("−", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                            }

                            IconButton(
                                onClick = { onSetTotalInquiries(totalInquiries + 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E1E2F), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "افزایش", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Confirm Button spacer
        item {
            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartGame,
                enabled = isEnabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCitizen,
                    contentColor = BackgroundDark,
                    disabledContainerColor = Color(0xFF1E3A20)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Text(
                        text = "توزیع کارت‌ها و شروع بازی 🃏",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun TeamRolesSection(
    title: String,
    teamColor: Color,
    rolesList: List<RoleEntity>,
    onInc: (Int) -> Unit,
    onDec: (Int) -> Unit,
    onEditAbilities: (RoleEntity) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = teamColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            rolesList.forEachIndexed { idx, role ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = role.name,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { onEditAbilities(role) },
                                modifier = Modifier
                                    .height(24.dp)
                                    .padding(horizontal = 4.dp)
                                    .testTag("edit_abilities_${role.id}"),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentGold)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "ویرایش قابلیت‌ها",
                                        tint = AccentGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "قابلیت‌ها ⚙️",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (role.description.isNotBlank()) {
                            Text(
                                text = role.description,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val context = LocalContext.current
                    val isUnique = !role.name.contains("ساده")
                    val isUniqueAndMaxed = isUnique && role.count >= 1

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { onDec(role.id) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFF1E1E2F), CircleShape)
                        ) {
                            Text("−", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = role.count.toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (role.count > 0) teamColor else Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.width(18.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = {
                                if (isUniqueAndMaxed) {
                                    Toast.makeText(context, "از این نقش فقط یک عدد میتواند در بازی حضور داشته باشد.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onInc(role.id)
                                }
                            },
                            enabled = !isUniqueAndMaxed,
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    if (isUniqueAndMaxed) Color(0xFF1E1E2F).copy(alpha = 0.3f) else Color(0xFF1E1E2F),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزایش",
                                tint = if (isUniqueAndMaxed) Color.White.copy(alpha = 0.3f) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (idx < rolesList.size - 1) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// ------------------------------------------
// 2. SECRET ROLE DISTRIBUTION UI
// ------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecretDistributionContent(
    players: List<PlayerEntity>,
    onConfirmStart: () -> Unit
) {
    var currentPlayerIndex by remember { mutableStateOf(0) }
    var isCardRevealed by remember { mutableStateOf(false) }

    val activePlayer = players.getOrNull(currentPlayerIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Tracker Header
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "واگذاری به بازیکنان: ",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "${currentPlayerIndex + 1} از ${players.size}",
                    color = AccentGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activePlayer != null) {
            // Elegant secret 3D-like flip card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCardRevealed) {
                        when (activePlayer.assignedRoleTeam) {
                            "Mafia" -> Color(0xFF1A0A0A)
                            "Citizen" -> Color(0xFF0A1A0C)
                            else -> Color(0xFF1D1B0A)
                        }
                    } else Color(0xFF161624)
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isCardRevealed) {
                        when (activePlayer.assignedRoleTeam) {
                            "Mafia" -> AccentCrimson
                            "Citizen" -> AccentCitizen
                            else -> AccentGold
                        }
                    } else BorderColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(300.dp)
                    .clickable { isCardRevealed = !isCardRevealed }
                    .shadow(16.dp, shape = RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نوبت تفویض:",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Text(
                        text = activePlayer.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCardRevealed) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = activePlayer.assignedRoleName ?: "نامشخص",
                                    color = when (activePlayer.assignedRoleTeam) {
                                        "Mafia" -> AccentCrimson
                                        "Citizen" -> AccentCitizen
                                        else -> AccentGold
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = when (activePlayer.assignedRoleTeam) {
                                        "Mafia" -> "تیم مافیا 🕶️"
                                        "Citizen" -> "تیم شهروندان 🕊️"
                                        else -> "جناح مستقل 🎭"
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "روی کارت بزنید تا نقش دیده شود 👁️",
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isCardRevealed) "روی کارت بزنید تا مجدد پنهان شود 🔒" else "اطلاعات کاملاً محرمانه است",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Controller Button
            Button(
                onClick = {
                    if (currentPlayerIndex < players.size - 1) {
                        currentPlayerIndex++
                        isCardRevealed = false
                    } else {
                        onConfirmStart()
                    }
                },
                enabled = isCardRevealed, // Must see the card before jumping to next
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
            ) {
                Text(
                    text = if (currentPlayerIndex < players.size - 1) "تصدیق شد، نفر بعدی 👈" else "اتمام واگذاری و ورود به چرخه سناریو 🚀",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ------------------------------------------
// 3. PLAY STAGE PANEL CO-MODERATOR PANEL
// ------------------------------------------
@Composable
fun PlayStageContent(
    players: List<PlayerEntity>,
    roles: List<RoleEntity>,
    phase: String,
    logs: List<GameLogEntity>,
    lastMoveCards: List<LastMoveCard>,
    onTogglePhase: () -> Unit,
    onPlayerClick: (PlayerEntity) -> Unit,
    onRegisterEvent: (Int, String) -> Unit,
    onToggleBlock: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onToggleLife: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onBurnLastMoveCard: (Int) -> Unit,
    onSaveGameOverReport: (String, String) -> Unit,
    onResetGame: () -> Unit,
    timerSelectedTime: Int,
    timerRemaining: Int,
    timerIsRunning: Boolean,
    showTimerModal: Boolean,
    onShowTimerModalChange: (Boolean) -> Unit,
    onTimerSelectedTimeChange: (Int) -> Unit,
    onTimerRemainingChange: (Int) -> Unit,
    onTimerIsRunningChange: (Boolean) -> Unit,
    totalInquiries: Int,
    remainingInquiries: Int,
    onDecrementInquiry: () -> Unit,
    onUpdatePlayerVoteDirectly: (Int, Int) -> Unit,
    triggerConfirmation: (String, String, () -> Unit) -> Unit = { _, _, _ -> },
    onUseLiveGun: (PlayerEntity) -> Unit = {},
    onExecuteVeto: (Int, Int) -> Unit = { _, _ -> },
    viewModel: MafiaViewModel,
    musketeerLiveGunExhausted: Boolean
) {
    val sagiCooldownNight by viewModel.sagiCooldownNight.collectAsStateWithLifecycle()
    val sagiPastTargets by viewModel.sagiPastTargets.collectAsStateWithLifecycle()
    val isGravedigActive by viewModel.isGravedigActiveThisNight.collectAsStateWithLifecycle()
    val currentRound = remember(logs) {
        logs.count { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
    }

    var showLogsStream by remember { mutableStateOf(true) }

    var isVotingCompleted by remember(phase) { mutableStateOf(false) }
    val playersInDefense = remember(phase) { mutableStateListOf<Int>() }
    var showVotingDialog by remember { mutableStateOf(false) }
    var showDefenseResultDialog by remember { mutableStateOf(false) }
    var defenseEligibleNames by remember { mutableStateOf<List<String>>(emptyList()) }

    // Dialog states
    var playerForKillerSelection by remember { mutableStateOf<PlayerEntity?>(null) }
    var showLastMoveDrawDialog by remember { mutableStateOf(false) }
    var drawnCardResult by remember { mutableStateOf<LastMoveCard?>(null) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var winnerTeamSelection by remember { mutableStateOf("Citizen") }
    var gameOverReasonInput by remember { mutableStateOf("") }
    var showNightReportDialog by remember { mutableStateOf(false) }
    var nightReportContents by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNewNightSummaryDialog by remember { mutableStateOf(false) }
    var showInquiryPromptDialog by remember { mutableStateOf(false) }
    var showDayStatsDialog by remember { mutableStateOf(false) }
    var showEndDayConfirmationDialog by remember { mutableStateOf(false) }

    // Helper to generate a smart structured night summary
    fun generateNightSummary(allLogs: List<GameLogEntity>, playersList: List<PlayerEntity>): List<String> {
        val reversed = allLogs.reversed()
        val nightTransitionIdx = reversed.indexOfFirst { it.message.contains("فاز بازی به «شب 🌙» تغییر یافت") }
        val relevantLogs = if (nightTransitionIdx != -1) reversed.subList(0, nightTransitionIdx) else reversed

        val killsLogs = relevantLogs.filter { it.message.contains("💀") && !it.message.contains("کنش") && !it.message.contains("برچسب") }
        val slaughterLogs = relevantLogs.filter { it.message.contains("🔪") }
        val muteLogs = relevantLogs.filter { it.message.contains("🔇") }
        val blockLogs = relevantLogs.filter { it.message.contains("🚫") }
        val saveLogs = relevantLogs.filter { it.message.contains("🩺") }

        fun extractName(msg: String): String? {
            val start = msg.indexOf("«")
            val end = msg.indexOf("»")
            return if (start != -1 && end != -1 && end > start) {
                msg.substring(start + 1, end)
            } else null
        }

        val summary = mutableListOf<String>()

        val nightKilledNames = killsLogs.mapNotNull { extractName(it.message) }.distinct()
        if (nightKilledNames.isNotEmpty()) {
            summary.add("💀 تلفات دیشب: ${nightKilledNames.joinToString("، ")} کشته شدند.")
        }

        val slaughteredNames = slaughterLogs.mapNotNull { extractName(it.message) }.distinct()
        if (slaughteredNames.isNotEmpty()) {
            summary.add("🔪 سلاخی دیشب: بازیکن «${slaughteredNames.joinToString("، ")}» سلاخی شد!")
        } else if (slaughterLogs.isNotEmpty()) {
            summary.add("🔪 سلاخی: بله، دیشب سلاخی رخ داد!")
        }

        val mutedNames = muteLogs.mapNotNull { extractName(it.message) }.distinct()
        if (mutedNames.isNotEmpty()) {
            summary.add("🔇 سکوت دیشب: ${mutedNames.joinToString("، ")} امروز حق صحبت ندارند.")
        } else if (muteLogs.isNotEmpty()) {
            summary.add("🔇 سکوت: امروز برخی بازیکنان حق صحبت ندارند.")
        }

        val blockedNames = blockLogs.mapNotNull { extractName(it.message) }.distinct()
        if (blockedNames.isNotEmpty()) {
            summary.add("🚫 بلاک دیشب: بازیکنان «${blockedNames.joinToString("، ")}» بلاک شدند.")
        } else if (blockLogs.isNotEmpty()) {
            summary.add("🚫 بلاک: اقداماتی در شب مسدود شد.")
        }

        val savedNames = saveLogs.mapNotNull { extractName(it.message) }.distinct()
        if (savedNames.isNotEmpty()) {
            summary.add("🩺 دکتر/نجات‌دهنده دیشب: بازیکنان «${savedNames.joinToString("، ")}» نجات پیدا کردند.")
        } else if (saveLogs.isNotEmpty()) {
            summary.add("🩺 دکتر/نجات‌دهنده: اقدامات نجات در شب انجام شد.")
        }

        // Add raw events for details if any
        val detailedEvents = relevantLogs.filter { 
            it.message.contains("شات") || it.message.contains("تفنگ") 
        }.map { "- ${it.message}" }
        
        if (detailedEvents.isNotEmpty()) {
            summary.add("🔫 وضعیت تفنگ‌ها و وقایع خاص:\n" + detailedEvents.joinToString("\n"))
        }

        if (summary.isEmpty()) {
            summary.add("🕊️ شب آرامی بود و هیچ اتفاق خاصی رخ نداد.")
        }

        return summary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Actions & Phase info dashboard
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    color = if (phase == "Night") Color(0xFF1E1035) else Color(0xFF352B10),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (phase == "Night") "🌙" else "☀️",
                                fontSize = 20.sp
                            )
                        }

                        Column {
                            Text(
                                text = if (phase == "Night") "فاز شب سناریو 🌙" else "فاز روز سناریو ☀️",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "تعداد کل افراد زنده: ${players.filter { it.isSelected && it.isAlive }.size} نفر",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "استعلام مفسر: $remainingInquiries از $totalInquiries 🔍",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (phase == "Night") {
                                showNewNightSummaryDialog = true
                            } else {
                                showEndDayConfirmationDialog = true
                            }
                        },
                        enabled = if (phase == "Day") isVotingCompleted else true,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (phase == "Night") AccentGold else Color(0xFF1C1C2E),
                            contentColor = if (phase == "Night") BackgroundDark else Color.White,
                            disabledContainerColor = Color(0xFF23232C),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = if (phase == "Night") "طلوع آفتاب (شروع روز) ☀️" else "غروب آفتاب (شروع شب) 🌙",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (phase == "Day") {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Start Voting Button (شروع رای گیری)
                    Button(
                        onClick = {
                            val unusedLiveGunPlayers = players.filter { it.isSelected && it.isAlive && it.hasLiveGunThisRound && !it.usedLiveGun }
                            if (unusedLiveGunPlayers.isNotEmpty()) {
                                val names = unusedLiveGunPlayers.joinToString("، ") { it.name }
                                triggerConfirmation(
                                    "تنبلی تفنگ جنگی ⚠️",
                                    "دارنده تفنگ جنگی («$names») از تفنگش استفاده نکرده است و کشته می‌شود. تایید میکنید؟"
                                ) {
                                    unusedLiveGunPlayers.forEach { onToggleLife(it.id) }
                                    showVotingDialog = true
                                }
                            } else {
                                showVotingDialog = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = BackgroundDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_voting_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "شروع رای گیری 🗳️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showDayStatsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A5F),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("day_stats_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AccentGold
                            )
                            Text(
                                text = "📊 آمار زنده و کشته‌های جناح‌ها (روز)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- Dynamic Night Waking Queue Section (ONLY in Night phase) ---
        if (phase == "Night") {
            val nightQueue = remember(players, roles, phase) {
                com.example.data.model.buildNightQueue(players, roles)
            }
            var currentNightQueueIndex by remember { mutableStateOf(0) }
            
            // Handle if index gets out of bounds due to player death or count change
            if (currentNightQueueIndex >= nightQueue.size && nightQueue.isNotEmpty()) {
                currentNightQueueIndex = nightQueue.size - 1
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("night_queue_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🌙", fontSize = 18.sp)
                            Text(
                                text = "صف بیداری پویای شب ⚙️",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        
                        if (nightQueue.isNotEmpty()) {
                            val curItem = nightQueue.getOrNull(currentNightQueueIndex)
                            val priority = curItem?.ability?.nightPriority
                            Text(
                                text = "اولویت بیداری: ${priority ?: "-"}",
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(AccentGold.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))

                    if (nightQueue.isEmpty()) {
                        Text(
                            text = "تمامی نقش‌های بیدار بیدار شده‌اند یا نقشی برای امشب بیدار نمی‌شود.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val currentItem = nightQueue[currentNightQueueIndex]
                        
                        // Active Target Display (Required)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF140F22), RoundedCornerShape(12.dp))
                                .border(1.dp, AccentGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // UI title: [Player Name] - [Ability Name] (MANDATORY REQUIREMENT)
                            Text(
                                text = "${currentItem.player.name} - ${currentItem.ability.name}",
                                color = AccentGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.testTag("night_queue_title")
                            )
                            
                            Text(
                                text = currentItem.ability.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            
                            val roleName = currentItem.player.assignedRoleName ?: ""
                            Text(
                                text = "نقش بازی: $roleName",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Modular Ability Execution Switch-Case driven by ability.id
                        val caps = remember(currentItem.player.capabilitiesJson) {
                            try {
                                if (currentItem.player.capabilitiesJson.isNotBlank()) {
                                    Json.decodeFromString<List<RoleCapability>>(currentItem.player.capabilitiesJson)
                                } else emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }

                        when (currentItem.ability.id) {
                            "VETO" -> {
                                val deadPlayers = remember(players) {
                                    players.filter { it.isSelected && !it.isAlive }
                                }
                                var selectedVetoTargetId by remember { mutableStateOf<Int?>(null) }
                                var vetoAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1C1314), RoundedCornerShape(12.dp))
                                        .border(1.dp, AccentCrimson.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت لغو آرای روز و وتو 🗳️",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCrimson,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "یکی از بازیکنان حذف شده زیر را جهت احیا و وتو رای‌گیری روز گذشته انتخاب نمایید:",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )

                                    if (deadPlayers.isEmpty()) {
                                        Text(
                                            text = "هیچ بازیکن حذف‌شده‌ای در قبرستان وجود ندارد.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        // Dead players list selection
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 140.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            deadPlayers.forEach { deadPlayer ->
                                                val isSelected = selectedVetoTargetId == deadPlayer.id
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isSelected) AccentCrimson.copy(alpha = 0.15f) else Color(0xFF0F0F1A),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) AccentCrimson else BorderColor.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { selectedVetoTargetId = deadPlayer.id }
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = deadPlayer.name,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                        Text(
                                                            text = "نقش: ${deadPlayer.assignedRoleName ?: "شهروند ساده"}",
                                                            color = Color.Gray,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                    
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { selectedVetoTargetId = deadPlayer.id },
                                                        colors = RadioButtonDefaults.colors(
                                                            selectedColor = AccentCrimson,
                                                            unselectedColor = Color.Gray
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Button(
                                            onClick = {
                                                val targetId = selectedVetoTargetId
                                                if (targetId != null) {
                                                    val target = deadPlayers.find { it.id == targetId }
                                                    if (target != null) {
                                                        triggerConfirmation(
                                                            "تایید وتوی رای‌گیری ⚡",
                                                            "آیا مطمئن هستید که می‌خواهید رای‌گیری برای بازگشت بازیکن «${target.name}» را وتو کنید؟"
                                                        ) {
                                                            onExecuteVeto(currentItem.player.id, target.id)
                                                            vetoAlertMessage = "رایگیری وتو شد! بازیکن با موفقیت به بازی برگشت."
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = selectedVetoTargetId != null,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .testTag("veto_confirm_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "اجرای وتو و احیا 🩺",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Alert Confirmation logic
                                vetoAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "بیانیه وتو 📢",
                                        message = msg,
                                        onConfirm = { vetoAlertMessage = null },
                                        onDismiss = { vetoAlertMessage = null }
                                    )
                                }
                            }

                            "HACK" -> {
                                val alivePlayersExceptSelfByHacker = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                val selectedHackerTargets = remember { mutableStateListOf<Int>() }
                                var hackerAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "استعلام هکر 📡",
                                            fontWeight = FontWeight.Bold,
                                            color = AccentCitizen,
                                            fontSize = 12.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFFAD1F).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${selectedHackerTargets.size} از ۳ انتخاب شده",
                                                color = Color(0xFFFFAD1F),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    val hackCap = caps.find { it.name.contains("هکر") || it.name.contains("استعلام") }
                                    if (hackCap != null) {
                                        Text(
                                            text = "📡 تعداد استعلام‌های باقی‌مانده: ${hackCap.remainingCount} از ${hackCap.totalCount}",
                                            color = AccentCitizen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Reminder / Note
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2D)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFF5AB2FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "توجه: این قابلیت معمولاً فقط در شب دوم بازی استفاده می‌شود.",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Text(
                                        text = "۳ بازیکن متمایز انتخاب فرمایید:",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        if (alivePlayersExceptSelfByHacker.isEmpty()) {
                                            Text(
                                                text = "هیچ بازیکن زنده معتبری وجود ندارد.",
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        } else {
                                            alivePlayersExceptSelfByHacker.forEach { p ->
                                                val isChecked = selectedHackerTargets.contains(p.id)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (isChecked) Color(0xFF1B2A3A) else Color(0xFF161622),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isChecked) AccentCitizen.copy(alpha = 0.5f) else Color.Transparent,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            if (isChecked) {
                                                                selectedHackerTargets.remove(p.id)
                                                            } else {
                                                                if (selectedHackerTargets.size < 3) {
                                                                    selectedHackerTargets.add(p.id)
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            if (!checked) {
                                                                selectedHackerTargets.remove(p.id)
                                                            } else {
                                                                if (selectedHackerTargets.size < 3) {
                                                                    selectedHackerTargets.add(p.id)
                                                                }
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = AccentCitizen)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = p.name,
                                                        color = if (isChecked) Color.White else Color.LightGray,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = p.assignedRoleName ?: "بدون نقش",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val isHackerAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                    val isActionEnabled = selectedHackerTargets.size == 3 && isHackerAllowed && (hackCap == null || hackCap.remainingCount > 0)

                                    Button(
                                        onClick = {
                                            if (selectedHackerTargets.size == 3) {
                                                val targetNames = selectedHackerTargets.mapNotNull { tid ->
                                                    alivePlayersExceptSelfByHacker.find { it.id == tid }?.name
                                                }.joinToString("، ")
                                                
                                                triggerConfirmation(
                                                    "تایید استعلام هکر 📡",
                                                    "آیا از بررسی وضعیت بازیکنان [$targetNames] اطمینان دارید؟"
                                                ) {
                                                    viewModel.hackerScan(currentItem.player.id, selectedHackerTargets.toList()) { success, resultText ->
                                                        if (success) {
                                                            hackerAlertMessage = resultText
                                                        } else {
                                                            hackerAlertMessage = "خطا در استعلام: $resultText"
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentCitizen,
                                            disabledContainerColor = Color(0xFF1C1C2E)
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("hacker_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "ارسال استعلام 📡",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) Color.White else Color.Gray
                                        )
                                    }
                                }

                                hackerAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "نتیجه استعلام هکر 📡",
                                        message = msg,
                                        onConfirm = { hackerAlertMessage = null },
                                        onDismiss = { hackerAlertMessage = null }
                                    )
                                }
                            }

                            "INTOXICATE" -> {
                                val alivePlayers = remember(players) {
                                    players.filter { it.isSelected && it.isAlive }
                                }
                                var selectedTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isTargetMenuExpanded by remember { mutableStateOf(false) }
                                var sagiAlertMessage by remember { mutableStateOf<String?>(null) }
                                var intoxicateAlertMessage by remember { mutableStateOf<String?>(null) }

                                val isCooldownActive = currentRound == sagiCooldownNight

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCooldownActive) {
                                        Text(
                                            text = "قابلیت ساقی در این شب غیرفعال است (یک شب در میان).",
                                            color = AccentCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "انتخاب هدف برای مستی 🍷:",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            val currentSelectedPlayer = alivePlayers.find { it.id == selectedTargetId }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                                    .clickable { isTargetMenuExpanded = true }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = currentSelectedPlayer?.name ?: "لطفاً بازیکن مورد نظر را انتخاب کنید...",
                                                    color = if (currentSelectedPlayer != null) Color.White else Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "باز کردن لیست",
                                                    tint = Color.Gray
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = isTargetMenuExpanded,
                                                onDismissRequest = { isTargetMenuExpanded = false },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.9f)
                                                    .background(Color(0xFF13131F))
                                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            ) {
                                                alivePlayers.forEach { p ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(p.name, color = Color.White, fontSize = 11.sp)
                                                                if (sagiPastTargets.contains(p.id)) {
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Text("قبلاً انتخاب شده ⚠️", color = AccentCrimson, fontSize = 9.sp)
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            isTargetMenuExpanded = false
                                                            if (sagiPastTargets.contains(p.id)) {
                                                                sagiAlertMessage = "ساقی نمیتواند یک نفر را دوباره انتخاب کند!"
                                                            } else {
                                                                selectedTargetId = p.id
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        val isSagiAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                        val isActionEnabled = selectedTargetId != null && isSagiAllowed

                                        Button(
                                            onClick = {
                                                val selTarget = alivePlayers.find { it.id == selectedTargetId }
                                                if (selTarget != null) {
                                                    triggerConfirmation(
                                                        "تایید مستی ساقی 🍷",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${selTarget.name}» را امشب مست کنید؟"
                                                    ) {
                                                        viewModel.intoxicatePlayer(
                                                            currentItem.player.id,
                                                            selTarget.id,
                                                            currentRound
                                                        ) { success, resultText ->
                                                            intoxicateAlertMessage = resultText
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = isActionEnabled,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentGold,
                                                disabledContainerColor = Color(0xFF1C1C2E)
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("sagi_confirm_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "ثبت مست کردن شبانه 🍷",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isActionEnabled) BackgroundDark else Color.Gray
                                            )
                                        }
                                    }
                                }

                                sagiAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "خطای ساقی 🍷",
                                        message = msg,
                                        onConfirm = { sagiAlertMessage = null },
                                        onDismiss = { sagiAlertMessage = null }
                                    )
                                }

                                intoxicateAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "عملکرد ساقی 🍷",
                                        message = msg,
                                        onConfirm = { intoxicateAlertMessage = null },
                                        onDismiss = { intoxicateAlertMessage = null }
                                    )
                                }
                            }

                            "GRAVEDIG" -> {
                                var gravedigActionMessage by remember { mutableStateOf<String?>(null) }
                                val isBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "مدیریت نبش قبر (گورکن) 🪦",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = AccentGold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (isBlocked) {
                                        Text(
                                            text = "⚠️ قابلیت این نقش امشب توسط ماتادور بسته شده است و گورکن مسدود است.",
                                            color = AccentCrimson,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "گورکن می‌تواند امشب تصمیم بگیرد که فردا صبح نبش قبر کند تا نقش‌های تمامی کشته‌شدگان بازی تا این لحظه به طور عمومی افشا شوند.",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "فعالسازی نبش قبر برای فردا صبح 🪦:",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                            Switch(
                                                checked = isGravedigActive,
                                                onCheckedChange = { isChecked ->
                                                    triggerConfirmation(
                                                        "تایید قابلیت گورکن 🪦",
                                                        if (isChecked) {
                                                            "آیا مطمئن هستید که می‌خواهید قابلیت نبش قبر را برای فردا فعال کنید؟"
                                                        } else {
                                                            "آیا مایل به لغو نبش قبر گورکن هستید؟"
                                                        }
                                                    ) {
                                                        viewModel.setGravedigActive(isChecked)
                                                        gravedigActionMessage = if (isChecked) {
                                                            "نبش قبر گورکن برای فردا صبح فعال گردید."
                                                        } else {
                                                            "نبش قبر گورکن لغو گردید."
                                                        }
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = AccentGold,
                                                    checkedTrackColor = AccentGold.copy(alpha = 0.5f),
                                                    uncheckedThumbColor = Color.Gray,
                                                    uncheckedTrackColor = Color(0xFF1C1C2E)
                                                )
                                            )
                                        }

                                        // Status message
                                        Text(
                                            text = if (isGravedigActive) {
                                                "✅ وضعیت گورکن: فعال (نبش قبر فردا صبح اعمال خواهد شد)"
                                            } else {
                                                "❌ وضعیت گورکن: غیرفعال"
                                            },
                                            color = if (isGravedigActive) AccentCitizen else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                gravedigActionMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "عملکرد گورکن 🪦",
                                        message = msg,
                                        onConfirm = { gravedigActionMessage = null },
                                        onDismiss = { gravedigActionMessage = null }
                                    )
                                }
                            }

                            "SILENCE" -> {
                                val alivePlayersExceptSelfBySilence = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var silenceTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isSilenceTargetMenuExpanded by remember { mutableStateOf(false) }
                                var silenceAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت سکوت / سایلنت (روانپزشک) 🧠",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        fontSize = 12.sp
                                    )

                                    val silenceCap = caps.find { it.name.contains("سکوت") || it.name.contains("سایلنت") || it.name.contains("psychiatrist") }
                                    if (silenceCap != null) {
                                        Text(
                                            text = "🧠 تعداد سکوت‌های باقی‌مانده: ${silenceCap.remainingCount} از ${silenceCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isSilenceTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedSilenceTarget = alivePlayersExceptSelfBySilence.find { it.id == silenceTargetId }
                                            Text(
                                                text = selectedSilenceTarget?.name ?: "انتخاب بازیکن برای سکوت... 👥",
                                                color = if (selectedSilenceTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isSilenceTargetMenuExpanded,
                                            onDismissRequest = { isSilenceTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersExceptSelfBySilence.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isSilenceTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersExceptSelfBySilence.forEach { aliveP ->
                                                     DropdownMenuItem(
                                                         text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                         onClick = {
                                                             silenceTargetId = aliveP.id
                                                             isSilenceTargetMenuExpanded = false
                                                         }
                                                     )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingSilences = silenceCap == null || silenceCap.remainingCount > 0
                                    val isSilenceAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                    val isActionEnabled = silenceTargetId != null && isSilenceAllowed && hasRemainingSilences

                                    Button(
                                        onClick = {
                                            val targetId = silenceTargetId
                                            if (targetId != null) {
                                                val target = alivePlayersExceptSelfBySilence.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید سکوت بازیکن 🧠",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را امشب سایلنت کنید؟"
                                                    ) {
                                                        viewModel.silencePlayer(currentItem.player.id, target.id)
                                                        silenceAlertMessage = "بازیکن «${target.name}» با موفقیت برای فاز روز بعدی در حالت سکوت قرار گرفت."
                                                        silenceTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("psychiatrist_silence_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "سکوت کردن بازیکن 🧠",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) BackgroundDark else Color.Gray
                                        )
                                    }
                                }

                                silenceAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش سایلنت 🧠",
                                        message = msg,
                                        onConfirm = { silenceAlertMessage = null },
                                        onDismiss = { silenceAlertMessage = null }
                                    )
                                }
                            }

                            "UNSILENCE" -> {
                                val alivePlayersExceptSelfByUnsilence = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var unsilenceTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isUnsilenceTargetMenuExpanded by remember { mutableStateOf(false) }
                                var unsilenceAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت رفع سکوت (کشیش) ⛪",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCitizen,
                                        fontSize = 12.sp
                                    )

                                    val unsilenceCap = caps.find { it.name.contains("رفع سکوت") || it.name.contains("unsilence") }
                                    if (unsilenceCap != null) {
                                        Text(
                                            text = "⛪ تعداد رفع سکوت‌های باقی‌مانده: ${unsilenceCap.remainingCount} از ${unsilenceCap.totalCount}",
                                            color = AccentCitizen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isUnsilenceTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedUnsilenceTarget = alivePlayersExceptSelfByUnsilence.find { it.id == unsilenceTargetId }
                                            Text(
                                                text = selectedUnsilenceTarget?.name ?: "انتخاب بازیکن برای رفع سکوت... 👥",
                                                color = if (selectedUnsilenceTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentCitizen)
                                        }

                                        DropdownMenu(
                                            expanded = isUnsilenceTargetMenuExpanded,
                                            onDismissRequest = { isUnsilenceTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersExceptSelfByUnsilence.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isUnsilenceTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersExceptSelfByUnsilence.forEach { aliveP ->
                                                     DropdownMenuItem(
                                                         text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})${if (aliveP.isSilencedThisRound) " [سکوت]" else ""}", color = Color.White, fontSize = 11.sp) },
                                                         onClick = {
                                                             unsilenceTargetId = aliveP.id
                                                             isUnsilenceTargetMenuExpanded = false
                                                         }
                                                     )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingUnsilences = unsilenceCap == null || unsilenceCap.remainingCount > 0
                                    val isUnsilenceAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                    val isActionEnabled = unsilenceTargetId != null && isUnsilenceAllowed && hasRemainingUnsilences

                                    Button(
                                        onClick = {
                                            val targetId = unsilenceTargetId
                                            if (targetId != null) {
                                                val target = alivePlayersExceptSelfByUnsilence.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید رفع سکوت بازیکن ⛪",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را امشب رفع سکوت کنید؟"
                                                    ) {
                                                        viewModel.unsilencePlayer(currentItem.player.id, target.id)
                                                        unsilenceAlertMessage = "درخواست رفع سکوت برای بازیکن «${target.name}» با موفقیت ثبت شد."
                                                        unsilenceTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCitizen, disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("priest_unsilence_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "رفع سکوت بازیکن ⛪",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) Color.White else Color.Gray
                                        )
                                    }
                                }

                                unsilenceAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش رفع سکوت ⛪",
                                        message = msg,
                                        onConfirm = { unsilenceAlertMessage = null },
                                        onDismiss = { unsilenceAlertMessage = null }
                                    )
                                }
                            }

                             "INSURE" -> {
                                val alivePlayersForInsure = remember(players) {
                                    players.filter { it.isSelected && it.isAlive }
                                }
                                var insureTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isInsureTargetMenuExpanded by remember { mutableStateOf(false) }
                                var insureAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت بیمه کردن (بیمه‌کننده) 🛡️",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        fontSize = 12.sp
                                    )

                                    val insurerCap = caps.find { it.name.contains("بیمه") }
                                    if (insurerCap != null) {
                                        Text(
                                            text = "🛡️ تعداد بیمه‌های باقی‌مانده: ${insurerCap.remainingCount} از ${insurerCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isInsureTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedInsureTarget = alivePlayersForInsure.find { it.id == insureTargetId }
                                            Text(
                                                text = selectedInsureTarget?.name ?: "انتخاب بازیکن برای بیمه... 👥",
                                                color = if (selectedInsureTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isInsureTargetMenuExpanded,
                                            onDismissRequest = { isInsureTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersForInsure.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isInsureTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersForInsure.forEach { aliveP ->
                                                     DropdownMenuItem(
                                                         text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                         onClick = {
                                                             insureTargetId = aliveP.id
                                                             isInsureTargetMenuExpanded = false
                                                         }
                                                     )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingInsures = insurerCap == null || insurerCap.remainingCount > 0
                                    val isInsureAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                    val isActionEnabled = insureTargetId != null && isInsureAllowed && hasRemainingInsures

                                    Button(
                                        onClick = {
                                            val targetId = insureTargetId
                                            if (targetId != null) {
                                                val target = alivePlayersForInsure.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید بیمه کردن 🛡️",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را امشب بیمه کنید؟"
                                                    ) {
                                                        viewModel.insurePlayer(currentItem.player.id, target.id)
                                                        insureAlertMessage = "بازیکن «${target.name}» با موفقیت برای امشب تحت پوشش بیمه قرار گرفت."
                                                        insureTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("insurer_insure_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "بیمه کردن بازیکن 🛡️",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) BackgroundDark else Color.Gray
                                        )
                                    }
                                }

                                insureAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش بیمه 🛡️",
                                        message = msg,
                                        onConfirm = { insureAlertMessage = null },
                                        onDismiss = { insureAlertMessage = null }
                                    )
                                }
                            }

                            "BLOCK" -> {
                                val alivePlayersExceptSelfByBlock = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var blockTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isBlockTargetMenuExpanded by remember { mutableStateOf(false) }
                                var blockAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت مسدودسازی (ماتادور) 🧣",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        fontSize = 12.sp
                                    )

                                    val matadorCap = caps.find { it.name.contains("مسدود") }
                                    if (matadorCap != null) {
                                        Text(
                                            text = "🧣 تعداد مسدودسازی‌های باقی‌مانده: ${matadorCap.remainingCount} از ${matadorCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isBlockTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedBlockTarget = alivePlayersExceptSelfByBlock.find { it.id == blockTargetId }
                                            Text(
                                                text = selectedBlockTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                                color = if (selectedBlockTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isBlockTargetMenuExpanded,
                                            onDismissRequest = { isBlockTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersExceptSelfByBlock.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isBlockTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersExceptSelfByBlock.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            blockTargetId = aliveP.id
                                                            isBlockTargetMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingBlocks = matadorCap == null || matadorCap.remainingCount > 0
                                    val isBlockAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                    val isActionEnabled = blockTargetId != null && isBlockAllowed && hasRemainingBlocks

                                    Button(
                                        onClick = {
                                            val targetId = blockTargetId
                                            if (targetId != null) {
                                                val target = alivePlayersExceptSelfByBlock.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید مسدودسازی 🧣",
                                                        "آیا مطمئن هستید که می‌خواهید قابلیت‌های بازیکن «${target.name}» را مسدود کنید؟"
                                                    ) {
                                                        viewModel.matadorBlock(currentItem.player.id, target.id)
                                                        blockAlertMessage = "قابلیت‌های بازیکن «${target.name}» با موفقیت برای امشب مسدود گردید."
                                                        blockTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("matador_block_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "مسدود کردن بازیکن 🧣",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) BackgroundDark else Color.Gray
                                        )
                                    }
                                }

                                blockAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش مسدودسازی 🧣",
                                        message = msg,
                                        onConfirm = { blockAlertMessage = null },
                                        onDismiss = { blockAlertMessage = null }
                                    )
                                }
                            }

                            "HEAL" -> {
                                val healablePlayers = remember(players) {
                                    players.filter { it.isSelected && (it.isAlive || it.isShotThisNight) }
                                }
                                var healTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isHealTargetMenuExpanded by remember { mutableStateOf(false) }
                                var healAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت شفا و نجات (پزشک) 🩺",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp
                                    )

                                    val healCap = caps.find { it.name.contains("شفا") || it.name.contains("نجات") }
                                    if (healCap != null) {
                                        Text(
                                            text = "🩺 تعداد شفا / نجات باقی‌مانده: ${healCap.remainingCount} از ${healCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isHealTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedHealTarget = healablePlayers.find { it.id == healTargetId }
                                            Text(
                                                text = selectedHealTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                                color = if (selectedHealTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isHealTargetMenuExpanded,
                                            onDismissRequest = { isHealTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (healablePlayers.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isHealTargetMenuExpanded = false }
                                                )
                                            } else {
                                                healablePlayers.forEach { aliveP ->
                                                    val suffix = if (aliveP.id == currentItem.player.id) " (خودتان)" else ""
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})$suffix", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            healTargetId = aliveP.id
                                                            isHealTargetMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingHeals = healCap == null || healCap.remainingCount > 0
                                    val isDoctorBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
                                    val isDoctorSelfSaveLimitExceeded = (healTargetId == currentItem.player.id && currentItem.player.doctorSelfSavesCount >= 2)
                                    val isActionEnabled = healTargetId != null && !isDoctorBlocked && hasRemainingHeals && !isDoctorSelfSaveLimitExceeded

                                    if (isDoctorSelfSaveLimitExceeded) {
                                        Text(
                                            text = "⚠️ خطا: شما حداکثر ۲ مرتبه مجاز به شفای خود بوده‌اید که این حد به اتمام رسیده است.",
                                            color = AccentCrimson,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val targetId = healTargetId
                                            if (targetId != null) {
                                                val target = healablePlayers.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید شفا و نجات 🩺",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را امشب شفا بدهید؟"
                                                    ) {
                                                        viewModel.doctorHeal(currentItem.player.id, target.id)
                                                        healAlertMessage = "بازیکن «${target.name}» با موفقیت امشب نجات / شفا داده شد."
                                                        healTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("doctor_heal_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "اعمال شفا / نجات 🩺",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) Color.White else Color.Gray
                                        )
                                    }
                                }

                                healAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش شفا و نجات 🩺",
                                        message = msg,
                                        onConfirm = { healAlertMessage = null },
                                        onDismiss = { healAlertMessage = null }
                                    )
                                }
                            }

                            "SLAUGHTER" -> {
                                val isGodfather = currentItem.player.assignedRoleName?.contains("پدرخوانده") == true || currentItem.player.assignedRoleTeam == "Mafia"
                                val slaughterColor = Color(0xFFC026D3)
                                val titleText = "سلاخی هدفمند (پدرخوانده/حرفه‌ای) 🔪"

                                val alivePlayersExceptSelfBySlaughter = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var slaughterTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isSlaughterTargetMenuExpanded by remember { mutableStateOf(false) }
                                var slaughterAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = titleText,
                                        fontWeight = FontWeight.Bold,
                                        color = slaughterColor,
                                        fontSize = 12.sp
                                    )

                                    val shootCap = caps.find { it.name.contains("شلیک") || it.name.contains("سلاخ") || it.name.contains("سلاخی") }
                                    if (shootCap != null) {
                                        Text(
                                            text = "🔪 تعداد سلاخی/شلیک‌های باقی‌مانده: ${shootCap.remainingCount} از ${shootCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                       modifier = Modifier
                                           .fillMaxWidth()
                                           .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                           .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                           .clickable { isSlaughterTargetMenuExpanded = true }
                                           .padding(12.dp)
                                    ) {
                                       Row(
                                           modifier = Modifier.fillMaxWidth(),
                                           horizontalArrangement = Arrangement.SpaceBetween,
                                           verticalAlignment = Alignment.CenterVertically
                                       ) {
                                           val selectedSlaughterTarget = alivePlayersExceptSelfBySlaughter.find { it.id == slaughterTargetId }
                                           Text(
                                               text = selectedSlaughterTarget?.name ?: "انتخاب بازیکن برای سلاخی... 👥",
                                               color = if (selectedSlaughterTarget != null) Color.White else Color.Gray,
                                               fontSize = 12.sp
                                           )
                                           Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                       }

                                       DropdownMenu(
                                           expanded = isSlaughterTargetMenuExpanded,
                                           onDismissRequest = { isSlaughterTargetMenuExpanded = false },
                                           modifier = Modifier
                                               .fillMaxWidth(0.85f)
                                               .background(SurfaceDark)
                                               .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                       ) {
                                           if (alivePlayersExceptSelfBySlaughter.isEmpty()) {
                                               DropdownMenuItem(
                                                   text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                   onClick = { isSlaughterTargetMenuExpanded = false }
                                               )
                                           } else {
                                               alivePlayersExceptSelfBySlaughter.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            slaughterTargetId = aliveP.id
                                                            isSlaughterTargetMenuExpanded = false
                                                        }
                                                    )
                                               }
                                           }
                                       }
                                    }

                                    val hasRemainingSlaughters = shootCap == null || shootCap.remainingCount > 0
                                    val isSlaughterBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
                                    val isActionEnabled = slaughterTargetId != null && !isSlaughterBlocked && hasRemainingSlaughters

                                    Button(
                                       onClick = {
                                           val targetId = slaughterTargetId
                                           if (targetId != null) {
                                               val target = alivePlayersExceptSelfBySlaughter.find { it.id == targetId }
                                               if (target != null) {
                                                   triggerConfirmation(
                                                       "تایید سلاخی قطعی 🔪",
                                                       "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را سلاخی کنید؟ این اقدام نجات پزشک را نادیده می‌گیرد."
                                                   ) {
                                                       if (isGodfather) {
                                                           viewModel.godfatherSlaughter(currentItem.player.id, target.id)
                                                           slaughterAlertMessage = "سلاخی پدرخوانده بر روی بازیکن «${target.name}» با موفقیت اعمال گردید."
                                                       } else {
                                                           viewModel.professionalSlaughter(currentItem.player.id, target.id)
                                                           slaughterAlertMessage = "سلاخی حرفه‌ای بر روی بازیکن «${target.name}» با موفقیت اعمال گردید."
                                                       }
                                                       slaughterTargetId = null
                                                   }
                                               }
                                           }
                                       },
                                       enabled = isActionEnabled,
                                       colors = ButtonDefaults.buttonColors(containerColor = slaughterColor, disabledContainerColor = Color(0xFF151821)),
                                       modifier = Modifier.fillMaxWidth().height(38.dp).testTag("slaughter_confirm_button"),
                                       shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                           text = "اجرای سلاخی 🔪",
                                           fontWeight = FontWeight.Bold,
                                           fontSize = 11.sp,
                                           color = if (isActionEnabled) Color.White else Color.Gray
                                        )
                                    }
                                }

                                slaughterAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش سلاخی 🔪",
                                        message = msg,
                                        onConfirm = { slaughterAlertMessage = null },
                                        onDismiss = { slaughterAlertMessage = null }
                                    )
                                }
                            }

                            "SHOOT" -> {
                                val isGodfather = currentItem.player.assignedRoleName?.contains("پدرخوانده") == true || currentItem.player.assignedRoleTeam == "Mafia"
                                val bulletColor = if (isGodfather) AccentCrimson else Color(0xFF3B82F6)
                                val titleText = if (isGodfather) "شلیک شبانه پدرخوانده 💀" else "شلیک هدفمند حرفه‌ای 🎯"
                                
                                val alivePlayersExceptSelfByShoot = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var shootTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isShootTargetMenuExpanded by remember { mutableStateOf(false) }
                                var shootAlertMessage by remember { mutableStateOf<String?>(null) }

                                // Professional override dialog helper
                                var showOverrideDialogForTargetId by remember { mutableStateOf<Int?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = titleText,
                                        fontWeight = FontWeight.Bold,
                                        color = bulletColor,
                                        fontSize = 12.sp
                                    )

                                    val shootCap = caps.find { it.name.contains("شلیک") }
                                    if (shootCap != null) {
                                        Text(
                                            text = "🔫 تعداد شلیک‌های باقی‌مانده: ${shootCap.remainingCount} از ${shootCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isShootTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedShootTarget = alivePlayersExceptSelfByShoot.find { it.id == shootTargetId }
                                            Text(
                                                text = selectedShootTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                                color = if (selectedShootTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isShootTargetMenuExpanded,
                                            onDismissRequest = { isShootTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersExceptSelfByShoot.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isShootTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersExceptSelfByShoot.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            shootTargetId = aliveP.id
                                                            isShootTargetMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingShots = shootCap == null || shootCap.remainingCount > 0
                                    val isShootBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
                                    val isActionEnabled = shootTargetId != null && !isShootBlocked && hasRemainingShots

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                val targetId = shootTargetId
                                                if (targetId != null) {
                                                    val target = alivePlayersExceptSelfByShoot.find { it.id == targetId }
                                                    if (target != null) {
                                                        if (!isGodfather && (target.assignedRoleName?.contains("پدرخوانده") == true || target.assignedRoleName?.contains("چرچیل") == true)) {
                                                            // Professional shoots godfather or churchill -> open moderator override choice dialog (REQUIRED)
                                                            showOverrideDialogForTargetId = target.id
                                                        } else {
                                                            triggerConfirmation(
                                                                "تایید شلیک مستقیم 🔫",
                                                                "آیا مطمئن هستید که می‌خواهید به بازیکن «${target.name}» شلیک کنید؟"
                                                            ) {
                                                                if (isGodfather) {
                                                                    viewModel.godfatherShoot(currentItem.player.id, target.id)
                                                                    shootAlertMessage = "شلیک پدرخوانده با موفقیت به بازیکن «${target.name}» اعمال گردید."
                                                                } else {
                                                                    viewModel.professionalShoot(currentItem.player.id, target.id, null)
                                                                    shootAlertMessage = "شلیک حرفه‌ای با موفقیت به بازیکن «${target.name}» اعمال گردید."
                                                                }
                                                                shootTargetId = null
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = isActionEnabled,
                                            colors = ButtonDefaults.buttonColors(containerColor = bulletColor, disabledContainerColor = Color(0xFF151821)),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("shoot_confirm_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "شلیک عادی 🔫",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isActionEnabled) Color.White else Color.Gray
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val targetId = shootTargetId
                                                if (targetId != null) {
                                                    val target = alivePlayersExceptSelfByShoot.find { it.id == targetId }
                                                    if (target != null) {
                                                        triggerConfirmation(
                                                            "تایید سلاخی کامل ⚔️",
                                                            "آیا مطمئن هستید می‌خواهید بازیکن «${target.name}» را سلاخی کنید؟ (سلاخی نجات پزشک را کاملا لغو و بی‌اثر خواهد کرد)."
                                                        ) {
                                                            if (isGodfather) {
                                                                    viewModel.godfatherSlaughter(currentItem.player.id, target.id)
                                                                    shootAlertMessage = "سلاخی پدرخوانده بر روی بازیکن «${target.name}» با موفقیت ثبت گردید."
                                                            } else {
                                                                    viewModel.professionalSlaughter(currentItem.player.id, target.id)
                                                                    shootAlertMessage = "سلاخی حرفه‌ای بر روی بازیکن «${target.name}» با موفقیت ثبت گردید."
                                                            }
                                                            shootTargetId = null
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = isActionEnabled,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF151821)),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("slaughter_confirm_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "سلاخی 🔪",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isActionEnabled) Color.White else Color.Gray
                                            )
                                        }
                                    }
                                }

                                // Professional override dialog UI
                                showOverrideDialogForTargetId?.let { targetId ->
                                    val target = alivePlayersExceptSelfByShoot.find { it.id == targetId }
                                    StyledConfirmationDialog(
                                        title = "نتیجه لغو و تأیید گرداننده (شلیک حرفه‌ای) 🛡️",
                                        message = "حرفه‌ای به «${target?.name ?: "پدرخوانده/چرچیل"}» شلیک کرده است. آیا برای او تفنگ کُشنده (مرگ قطعی) ثبت شود یا به شکل صوری زنده بماند؟",
                                        onConfirm = {
                                            viewModel.professionalShoot(currentItem.player.id, targetId, true)
                                            shootAlertMessage = "شلیک حرفه‌ای با مرگ قطعی به بازیکن شلیک‌شونده ثبت شد."
                                            showOverrideDialogForTargetId = null
                                            shootTargetId = null
                                        },
                                        onDismiss = {
                                            viewModel.professionalShoot(currentItem.player.id, targetId, false)
                                            shootAlertMessage = "شلیک حرفه‌ای از سوی بازیکن شلیک‌شونده بی‌اثر اعلام شد."
                                            showOverrideDialogForTargetId = null
                                            shootTargetId = null
                                        }
                                    )
                                }

                                shootAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "گزارش شلیک / سلاخی 💀",
                                        message = msg,
                                        onConfirm = { shootAlertMessage = null },
                                        onDismiss = { shootAlertMessage = null }
                                    )
                                }
                            }

                            "RECRUIT" -> {
                                val alivePlayersExceptSelfByRecruit = remember(players) {
                                    players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
                                }
                                var recruitTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var isRecruitTargetMenuExpanded by remember { mutableStateOf(false) }
                                var recruitAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت خریداری / مذاکره گروه مافیا 🤝",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A),
                                        fontSize = 12.sp
                                    )

                                    val buyerCap = caps.find { it.name.contains("خریداری") }
                                    if (buyerCap != null) {
                                        Text(
                                            text = "🤝 تعداد مذاکرات / خریداری باقی‌مانده: ${buyerCap.remainingCount} از ${buyerCap.totalCount}",
                                            color = AccentGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { isRecruitTargetMenuExpanded = true }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedRecruitTarget = alivePlayersExceptSelfByRecruit.find { it.id == recruitTargetId }
                                            Text(
                                                text = selectedRecruitTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                                color = if (selectedRecruitTarget != null) Color.White else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                        }

                                        DropdownMenu(
                                            expanded = isRecruitTargetMenuExpanded,
                                            onDismissRequest = { isRecruitTargetMenuExpanded = false },
                                            modifier = Modifier
                                                .fillMaxWidth(0.85f)
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            if (alivePlayersExceptSelfByRecruit.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { isRecruitTargetMenuExpanded = false }
                                                )
                                            } else {
                                                alivePlayersExceptSelfByRecruit.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = {
                                                            recruitTargetId = aliveP.id
                                                            isRecruitTargetMenuExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val hasRemainingRecruits = buyerCap == null || buyerCap.remainingCount > 0
                                    val isRecruitBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
                                    val isActionEnabled = recruitTargetId != null && !isRecruitBlocked && hasRemainingRecruits

                                    Button(
                                        onClick = {
                                            val targetId = recruitTargetId
                                            if (targetId != null) {
                                                val target = alivePlayersExceptSelfByRecruit.find { it.id == targetId }
                                                if (target != null) {
                                                    triggerConfirmation(
                                                        "تایید خریداری / مذاکره 🤝",
                                                        "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را برای مذاکره / خریداری پیوستن به مافیا ثبت کنید؟"
                                                    ) {
                                                        val isBuyer = currentItem.player.assignedRoleName?.contains("خریدار") == true
                                                        if (isBuyer) {
                                                            viewModel.buyerRecruit(currentItem.player.id, target.id) { resultMsg ->
                                                                recruitAlertMessage = resultMsg
                                                            }
                                                        } else {
                                                            viewModel.godfatherRecruit(currentItem.player.id, target.id) { resultMsg ->
                                                                recruitAlertMessage = resultMsg
                                                            }
                                                        }
                                                        recruitTargetId = null
                                                    }
                                                }
                                            }
                                        },
                                        enabled = isActionEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A), disabledContainerColor = Color(0xFF1C1C2E)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("recruit_confirm_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "اعمال خریداری / مذاکره گروه مافیا 🤝",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isActionEnabled) Color.White else Color.Gray
                                        )
                                    }
                                }

                                recruitAlertMessage?.let { msg ->
                                    StyledConfirmationDialog(
                                        title = "بیانیه مذاکره / خریداری 📢",
                                        message = msg,
                                        onConfirm = { recruitAlertMessage = null },
                                        onDismiss = { recruitAlertMessage = null }
                                    )
                                }
                            }

                            "GIVE_GUN" -> {
                                val liveCap = caps.find { it.name.contains("جنگی") }
                                val blankCap = caps.find { it.name.contains("مشقی") }

                                var targetId1 by remember(currentItem) { mutableStateOf<Int?>(null) }
                                var targetId2 by remember(currentItem) { mutableStateOf<Int?>(null) }

                                var expanded1 by remember { mutableStateOf(false) }
                                var expanded2 by remember { mutableStateOf(false) }

                                val targetPlayer1 = remember(targetId1, players) { players.find { it.id == targetId1 } }
                                val targetPlayer2 = remember(targetId2, players) { players.find { it.id == targetId2 } }

                                val alivePlayersForGuns = remember(players) {
                                    players.filter { it.isSelected && it.isAlive }
                                }

                                val isActionAllowed = !currentItem.player.isBlocked && !currentItem.player.isBlockedThisNight
                                var gunAlertMessage by remember { mutableStateOf<String?>(null) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مدیریت تسلیح و توزیع سلاح (تفنگدار) 🪖",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold,
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        text = "⚔️ تفنگ جنگی باقی‌مانده: ${if (musketeerLiveGunExhausted) 0 else (liveCap?.remainingCount ?: 0)} | 🔫 تفنگ مشقی باقی‌مانده: ${blankCap?.remainingCount ?: "نامحدود"}",
                                        color = AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    // Target 1 Column
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF140F22), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text("🎯 بازیکن تفنگ‌دار اول:", color = Color.Gray, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF191928), RoundedCornerShape(6.dp))
                                                .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable { expanded1 = true }
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = targetPlayer1?.name ?: "انتخاب بازیکن هدف اول... 👥",
                                                    color = if (targetPlayer1 != null) Color.White else Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                            }
                                            DropdownMenu(
                                                expanded = expanded1,
                                                onDismissRequest = { expanded1 = false },
                                                modifier = Modifier.fillMaxWidth(0.8f).background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچکدام", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { targetId1 = null; expanded1 = false }
                                                )
                                                alivePlayersForGuns.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = { targetId1 = aliveP.id; expanded1 = false }
                                                    )
                                                }
                                            }
                                        }

                                        if (targetPlayer1 != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                val isBlankEnabled1 = isActionAllowed && (blankCap == null || blankCap.remainingCount > 0)
                                                Button(
                                                    onClick = {
                                                        viewModel.giveMusketeerGun(currentItem.player.id, targetPlayer1.id, false)
                                                        gunAlertMessage = "تیر مشقی با موفقیت به بازیکن «${targetPlayer1.name}» داده شد."
                                                        targetId1 = null
                                                    },
                                                    enabled = isBlankEnabled1,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), disabledContainerColor = Color(0xFF1E293B)),
                                                    modifier = Modifier.weight(1f).testTag("queue_musketeer_blank_gun_1"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("تیر مشقی 🔫", color = if (isBlankEnabled1) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                val anyLiveGunTonight1 = players.any { it.hasLiveGunThisRound }
                                                val isLiveEnabled1 = isActionAllowed && !musketeerLiveGunExhausted && !anyLiveGunTonight1 && (liveCap == null || liveCap.remainingCount > 0)
                                                Button(
                                                    onClick = {
                                                        viewModel.giveMusketeerGun(currentItem.player.id, targetPlayer1.id, true)
                                                        gunAlertMessage = "تیر جنگی با موفقیت به بازیکن «${targetPlayer1.name}» داده شد."
                                                        targetId1 = null
                                                    },
                                                    enabled = isLiveEnabled1,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF1E293B)),
                                                    modifier = Modifier.weight(1f).testTag("queue_musketeer_live_gun_1"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("تیر جنگی ⚔️", color = if (isLiveEnabled1) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Target 2 Column
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF140F22), RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text("🎯 بازیکن تفنگ‌دار دوم:", color = Color.Gray, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF191928), RoundedCornerShape(6.dp))
                                                .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable { expanded2 = true }
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = targetPlayer2?.name ?: "انتخاب بازیکن هدف دوم... 👥",
                                                    color = if (targetPlayer2 != null) Color.White else Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                            }
                                            DropdownMenu(
                                                expanded = expanded2,
                                                onDismissRequest = { expanded2 = false },
                                                modifier = Modifier.fillMaxWidth(0.8f).background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("هیچکدام", color = Color.Gray, fontSize = 11.sp) },
                                                    onClick = { targetId2 = null; expanded2 = false }
                                                )
                                                alivePlayersForGuns.filter { it.id != (targetId1 ?: -1) }.forEach { aliveP ->
                                                    DropdownMenuItem(
                                                        text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                        onClick = { targetId2 = aliveP.id; expanded2 = false }
                                                    )
                                                }
                                            }
                                        }

                                        if (targetPlayer2 != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                val isBlankEnabled2 = isActionAllowed && (blankCap == null || blankCap.remainingCount > 0)
                                                Button(
                                                    onClick = {
                                                        viewModel.giveMusketeerGun(currentItem.player.id, targetPlayer2.id, false)
                                                        gunAlertMessage = "تیر مشقی با موفقیت به بازیکن «${targetPlayer2.name}» داده شد."
                                                        targetId2 = null
                                                    },
                                                    enabled = isBlankEnabled2,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), disabledContainerColor = Color(0xFF1E293B)),
                                                    modifier = Modifier.weight(1f).testTag("queue_musketeer_blank_gun_2"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("تیر مشقی 🔫", color = if (isBlankEnabled2) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                val anyLiveGunTonight2 = players.any { it.hasLiveGunThisRound }
                                                val isLiveEnabled2 = isActionAllowed && !musketeerLiveGunExhausted && !anyLiveGunTonight2 && (liveCap == null || liveCap.remainingCount > 0)
                                                Button(
                                                    onClick = {
                                                        viewModel.giveMusketeerGun(currentItem.player.id, targetPlayer2.id, true)
                                                        gunAlertMessage = "تیر جنگی با موفقیت به بازیکن «${targetPlayer2.name}» داده شد."
                                                        targetId2 = null
                                                    },
                                                    enabled = isLiveEnabled2,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF1E293B)),
                                                    modifier = Modifier.weight(1f).testTag("queue_musketeer_live_gun_2"),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("تیر جنگی ⚔️", color = if (isLiveEnabled2) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "REVEAL_MAFIA" -> {
                                RevealActionContent(
                                    currentItem = currentItem,
                                    players = players,
                                    caps = caps,
                                    viewModel = viewModel
                                )
                            }

                            "REVIVE" -> {
                                ReviveActionContent(
                                    currentItem = currentItem,
                                    players = players,
                                    caps = caps,
                                    viewModel = viewModel,
                                    triggerConfirmation = triggerConfirmation
                                )
                            }

                            "NATO_GUESS" -> {
                                NatoGuessActionContent(
                                    currentItem = currentItem,
                                    players = players,
                                    caps = caps,
                                    viewModel = viewModel
                                )
                            }

                            "SABOTAGE" -> {
                                SabotageActionContent(
                                    currentItem = currentItem,
                                    players = players,
                                    caps = caps,
                                    viewModel = viewModel
                                )
                            }

                            "CHURCHILL_SHOOT" -> {
                                ChurchillShootActionContent(
                                    currentItem = currentItem,
                                    players = players,
                                    caps = caps,
                                    viewModel = viewModel,
                                    currentRound = currentRound,
                                    triggerConfirmation = triggerConfirmation
                                )
                            }
                        }

                        // Navigator buttons (Next / Prev)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentNightQueueIndex > 0) {
                                        currentNightQueueIndex--
                                    }
                                },
                                enabled = currentNightQueueIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("night_queue_prev_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("👉 قبلی", color = if (currentNightQueueIndex > 0) Color.White else Color.Gray, fontSize = 11.sp)
                            }
                            
                            Text(
                                text = "گام ${currentNightQueueIndex + 1} از ${nightQueue.size}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    if (currentNightQueueIndex < nightQueue.size - 1) {
                                        currentNightQueueIndex++
                                    }
                                },
                                enabled = currentNightQueueIndex < nightQueue.size - 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("night_queue_next_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    disabledContainerColor = Color(0xFF1C1C2E)
                                ),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("بعدی 👈", color = if (currentNightQueueIndex < nightQueue.size - 1) BackgroundDark else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live players ledger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "وضعیت زنده تمام بازیکنان 👥",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = "روی بازیکنی ضربه بزنید تا قابلیتها یا یادداشت را مدیریت کنید",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val activeHandheldPlayers = remember(players, phase) {
                val active = players.filter { it.isSelected }
                if (phase == "Night") {
                    val independent = active.filter { it.assignedRoleTeam == "Independent" }
                    val mafia = active.filter { it.assignedRoleTeam == "Mafia" }
                    val citizens = active.filter { it.assignedRoleTeam == "Citizen" }
                    val others = active.filter { it.assignedRoleTeam !in listOf("Independent", "Mafia", "Citizen") }
                    independent + mafia + citizens + others
                } else {
                    active
                }
            }

            if (activeHandheldPlayers.isEmpty()) {
                EmptyListTip(text = "هیچ بازیکن منتخبی وجود ندارد. دکمه بازنشانی را بفشارید.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activeHandheldPlayers) { player ->
                        PlayerLiveCard(
                            player = player,
                            phase = phase,
                            players = players,
                            isInDefense = player.id in playersInDefense,
                            onClick = { onPlayerClick(player) },
                            onRegisterEvent = { eventType ->
                                if (eventType == "KILL") {
                                    playerForKillerSelection = player
                                } else {
                                    onRegisterEvent(player.id, eventType)
                                }
                            },
                            onToggleBlock = { onToggleBlock(player.id) },
                            onToggleMute = { onToggleMute(player.id) },
                            onToggleLife = { onToggleLife(player.id) },
                            onUseLiveGun = { onUseLiveGun(player) }
                        )
                    }
                }
            }
        }

        // Bottom logs console stream
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                        Text(
                            text = "سوابق وقایع دیشب و امروز 📊",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }

                    Row {
                        IconButton(onClick = onClearLogs, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف لاگ", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { showLogsStream = !showLogsStream }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (showLogsStream) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showLogsStream) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.height(80.dp)) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "هیچ واقعه‌ای ثبت نشده است.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(logs) { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.message,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (log.phase == "Night") "شب 🌙" else "روز ☀️",
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 1. Day Phase Last Move Drawer & Game Over Triggers ---
        if (phase == "Day") {
            // Compact, highly professional Day Timer Open Button
            Button(
                onClick = { onShowTimerModalChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerIsRunning) AccentGold else Color(0xFF1E1E2F),
                    contentColor = if (timerIsRunning) BackgroundDark else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(44.dp),
                border = BorderStroke(1.dp, if (timerIsRunning) AccentGold else BorderColor)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (timerIsRunning) BackgroundDark else AccentGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val formattedTime = "${timerRemaining / 60}:${(timerRemaining % 60).toString().padStart(2, '0')}"
                val buttonText = if (timerIsRunning) {
                    "⏱️ تایمر روز فعال: $formattedTime (تنظیم یا مکث ⏳)"
                } else {
                    "⏱️ مدیریت و شروع تایمر سخنرانی روز (${timerSelectedTime / 60} دقیقه) ⏱️"
                }
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showLastMoveDrawDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("قرعه‌کشی کارت حرکت پایانی 🎲", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { showGameOverDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعلام اتمام بازی 🏁", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }

    // --- 2. Interactive Dialog Overlays ---

    if (showTimerModal) {
        DayTimerDialog(
            selectedTime = timerSelectedTime,
            onSelectedTimeChange = onTimerSelectedTimeChange,
            timeRemaining = timerRemaining,
            onTimeRemainingChange = onTimerRemainingChange,
            isRunning = timerIsRunning,
            onIsRunningChange = onTimerIsRunningChange,
            onDismiss = { onShowTimerModalChange(false) }
        )
    }

    if (showInquiryPromptDialog) {
        Dialog(
            onDismissRequest = { showInquiryPromptDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔍 استعلام وضعیت بازی (Inquiry)",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "آیا مایلید از یک استعلام (Inquiry) استفاده کنید تا آمار دقیق کشته‌شدگان هر جناح تا این لحظه نمایش داده شود؟",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعداد استعلام‌های باقی‌مانده:",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$remainingInquiries از $totalInquiries",
                                color = if (remainingInquiries > 0) AccentGold else AccentCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (remainingInquiries <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF3F1F24), RoundedCornerShape(8.dp))
                                .border(1.dp, AccentCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ استعلام‌ها کاملاً به پایان رسیده‌اند (Inquiries are completely depleted). قابلیت استعلام جدید مسدود است.",
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yes Button
                        Button(
                            onClick = {
                                if (remainingInquiries > 0) {
                                    onDecrementInquiry()
                                    onTogglePhase()
                                    showDayStatsDialog = true
                                    showInquiryPromptDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = BackgroundDark,
                                disabledContainerColor = Color(0xFF332715)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            enabled = remainingInquiries > 0
                        ) {
                            Text("بله (کاهش استعلام) 🔍", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // No Button
                        Button(
                            onClick = {
                                showInquiryPromptDialog = false
                                onTogglePhase() // Transitions to Day directly!
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCitizen, contentColor = BackgroundDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("خیر (شروع مستقیم روز) ☀️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    TextButton(onClick = { showInquiryPromptDialog = false }) {
                        Text("انصراف و بازگشت به فاز شب 🌙", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showNewNightSummaryDialog) {
        val shotDeadPlayers = remember(players) { players.filter { it.isSelected && it.isShotThisNight && !it.isAlive && !it.isSlaughtered } }
        val slaughteredPlayers = remember(players) { players.filter { it.isSelected && it.isSlaughtered } }
        val blockedPlayers = remember(players) { players.filter { it.isSelected && it.isBlockedThisNight } }
        val revealedMafiaPlayers = remember(players) { players.filter { it.isSelected && it.isRevealedMafia } }
        val revivedPlayers = remember(players) { players.filter { it.isSelected && it.isRevivedThisNight } }

        val listShotNames = if (shotDeadPlayers.isEmpty()) "هیچکس" else shotDeadPlayers.joinToString("، ") { it.name }
        val listSlaughterNames = if (slaughteredPlayers.isEmpty()) "هیچکس" else slaughteredPlayers.joinToString("، ") { it.name }
        val listBlockedNames = if (blockedPlayers.isEmpty()) "هیچکس" else blockedPlayers.joinToString("، ") { it.name }
        val listRevealedNames = if (revealedMafiaPlayers.isEmpty()) "هیچکس" else revealedMafiaPlayers.joinToString("، ") { it.name }
        val listRevivedNames = if (revivedPlayers.isEmpty()) "هیچکس" else revivedPlayers.joinToString("، ") { it.name }

        Dialog(
            onDismissRequest = {
                showNewNightSummaryDialog = false
                if (isGravedigActive) {
                    viewModel.setGravedigActive(false)
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "گزارش وقایع شب 🌙",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "کشته های دیشب : $listShotNames",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "سلاخی ها : $listSlaughterNames",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "ساکت شده/بلاک شده : $listBlockedNames",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "مافیای شناخته شده توسط همشهری کین : $listRevealedNames",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "زنده شده دیشب : $listRevivedNames",
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (isGravedigActive) {
                        val deadRoles = remember(players) {
                            players.filter { it.isSelected && !it.isAlive }.mapNotNull { it.assignedRoleName }.distinct()
                        }
                        val deadRolesText = if (deadRoles.isEmpty()) "هیچ نقشی هنوز خارج نشده است" else deadRoles.joinToString("، ")

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF331D2D)),
                            border = BorderStroke(1.dp, Color(0xFFE040FB)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🪦", fontSize = 16.sp)
                                    Text(
                                        text = "گورکن نبش قبر کرد!",
                                        color = Color(0xFFE040FB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = "نقش‌های خارج‌شده از بازی تا این لحظه:",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = deadRolesText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showNewNightSummaryDialog = false
                            showInquiryPromptDialog = true
                            if (isGravedigActive) {
                                viewModel.setGravedigActive(false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(
                            text = "تایید وقایع فاز شب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // 2.1 Night Report Summary Dialog
    if (showNightReportDialog) {
        val eliminatedPlayers = remember(players) { players.filter { it.isSelected && !it.isAlive } }
        val mafiaKilledCount = remember(eliminatedPlayers) {
            eliminatedPlayers.count { it.assignedRoleTeam?.equals("Mafia", ignoreCase = true) == true }
        }
        val citizenKilledCount = remember(eliminatedPlayers) {
            eliminatedPlayers.count { it.assignedRoleTeam?.equals("Citizen", ignoreCase = true) == true }
        }
        val independentKilledCount = remember(eliminatedPlayers) {
            eliminatedPlayers.count { it.assignedRoleTeam?.equals("Independent", ignoreCase = true) == true }
        }

        Dialog(
            onDismissRequest = { showNightReportDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔍 نتیجه استعلام وضعیت (Inquiry Result)",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "آمار دقیق اعضای کشته‌شده و حذف‌شده هر جناح از ابتدای بازی تا این لحظه به شرح زیر است:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    // Statistical Display Card (using exact requested Persian format)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Mafia Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعداد کشته های مافیا تا الان:",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = mafiaKilledCount.toString(),
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                            // Citizen Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعداد کشته های شهروند تا الان:",
                                    color = Color(0xFF42A5F5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = citizenKilledCount.toString(),
                                    color = Color(0xFF42A5F5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                            // Independent Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تعداد کشته های مستقل تا الان:",
                                    color = Color(0xFFAB47BC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = independentKilledCount.toString(),
                                    color = Color(0xFFAB47BC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showNightReportDialog = false
                            onTogglePhase() // Transition to Day phase!
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCitizen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = "تایید میشود ☀️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BackgroundDark
                        )
                    }
                }
            }
        }
    }

    // 2.2 Draw Last Move Card dialog
    if (showLastMoveDrawDialog) {
        SharedLastMoveDrawDialog(
            lastMoveCards = lastMoveCards,
            onBurnLastMoveCard = onBurnLastMoveCard,
            onDismiss = { showLastMoveDrawDialog = false }
        )
    }

    // 2.3 Game Over dialog
    if (showGameOverDialog) {
        Dialog(
            onDismissRequest = { showGameOverDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .imePadding()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏁 اعلام و ثبت پایان نهایی بازی سناریو",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Text("ساید و جبهه برنده کل بازی را مشخص کنید:", color = TextSecondary, fontSize = 10.sp)

                    // Winner Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Citizen" to "شهروندان 🕊️",
                            "Mafia" to "مافیا 🕶️",
                            "Independent" to "مستقل 🎭"
                        ).forEach { (teamVal, label) ->
                            val isSel = winnerTeamSelection == teamVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSel) Color(0xFF1E142F) else Color(0xFF13131F),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSel) AccentGold else BorderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { winnerTeamSelection = teamVal }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("علت دقیق خاتمه بازی (وصیت، رای‌گیری، تسلیم گروه و غیره):", color = TextSecondary, fontSize = 10.sp)

                    OutlinedTextField(
                        value = gameOverReasonInput,
                        onValueChange = { gameOverReasonInput = it },
                        placeholder = { Text("مثال: تصاحب شب و برقراری برتری شمارش مافیا...", fontSize = 11.sp, color = Color.Gray) },
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (gameOverReasonInput.isNotBlank()) {
                                    onSaveGameOverReport(winnerTeamSelection, gameOverReasonInput)
                                    showGameOverDialog = false
                                    onResetGame() // Go back to SETUP stage
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCitizen, contentColor = BackgroundDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            enabled = gameOverReasonInput.isNotBlank()
                        ) {
                            Text("ثبت و برگشت به خانه 💾", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showGameOverDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("انصراف", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // 2.4 Granular Killer Selection Popup
    if (playerForKillerSelection != null) {
        val target = playerForKillerSelection!!
        Dialog(onDismissRequest = { playerForKillerSelection = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "عوامل و دلایل واقعه کشف شات شب «${target.name}»:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp).fillMaxWidth()
                    ) {
                        item {
                            Button(
                                onClick = {
                                    onRegisterEvent(target.id, "KILL_BY_RULE")
                                    playerForKillerSelection = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1920)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, AccentCrimson.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("بدلیل استفاده اشتباه از نقش ⚠️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // List of roles active in this scenario
                        val activeRoles = players.filter { it.isSelected && it.id != target.id && it.assignedRoleName != null }
                            .map { it.assignedRoleName!! }
                            .distinct()

                        items(activeRoles) { roleName ->
                            Button(
                                onClick = {
                                    onRegisterEvent(target.id, "KILL_BY_ROLE_$roleName")
                                    playerForKillerSelection = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2F)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("توسط نقش «$roleName» 🗡️", color = Color.White, fontSize = 11.sp)
                            }
                        }

                        if (activeRoles.isEmpty()) {
                            item {
                                val fallbacks = listOf("تیم مافیا 🕶️", "تیرانداز 🔫", "تروریست 💣", "مستقل 🎭")
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    fallbacks.forEach { role ->
                                        Button(
                                            onClick = {
                                                onRegisterEvent(target.id, "KILL_BY_ROLE_$role")
                                                playerForKillerSelection = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2F)),
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.dp, BorderColor),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("توسط «$role» 🗡️", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { playerForKillerSelection = null },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انصراف", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showDayStatsDialog) {
        DayStatsDialog(
            players = players,
            onDismissRequest = { showDayStatsDialog = false }
        )
    }

    if (showEndDayConfirmationDialog) {
        Dialog(
            onDismissRequest = { showEndDayConfirmationDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.91f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌙 انتقال به فاز شب",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "آیا مطمئن هستید که میخواهید روز را پایان دهید و به فاز شب بروید؟",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Confirm/Yes Button
                        Button(
                            onClick = {
                                showEndDayConfirmationDialog = false
                                onTogglePhase()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("confirm_end_day_button")
                        ) {
                            Text(
                                text = "بله و خروج 🌙",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Cancel/No Button
                        Button(
                            onClick = { showEndDayConfirmationDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.LightGray
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("cancel_end_day_button")
                        ) {
                            Text(
                                text = "انصراف ☀️",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVotingDialog) {
        val alivePlayers = remember(players) { players.filter { it.isSelected && it.isAlive } }
        val threshold = kotlin.math.ceil(alivePlayers.size / 2.0).toInt()
        
        val tempVotes = remember { mutableStateMapOf<Int, Int>().apply { 
            alivePlayers.forEach { put(it.id, it.voteCount) } 
        } }

        Dialog(
            onDismissRequest = { showVotingDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth(0.91f)
                    .fillMaxHeight(0.85f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🗳️ مدیریت رأی‌گیری روز سناریو",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    )

                    val formattedThreshold = threshold.toString()
                    Text(
                        text = "تعداد زنده: ${alivePlayers.size} نفر | حد نصاب دفاعیه: $formattedThreshold رأی (نصف یا بیشتر زنده)",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                    // Player List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alivePlayers) { player ->
                            val currentCount = tempVotes[player.id] ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF161623), RoundedCornerShape(10.dp))
                                    .border(1.dp, BorderColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFF222235), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 11.sp)
                                    }
                                    Column {
                                        Text(
                                            text = player.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = player.assignedRoleName ?: "بدون نقش",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Decrement Button
                                    IconButton(
                                        onClick = { 
                                            val current = tempVotes[player.id] ?: 0
                                            if (current > 0) {
                                                tempVotes[player.id] = current - 1
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFF2C2424), RoundedCornerShape(6.dp))
                                    ) {
                                        Text("-", color = AccentCrimson, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = "$currentCount رأی",
                                        color = if (currentCount >= threshold) AccentCrimson else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.widthIn(min = 40.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    // Increment Button
                                    IconButton(
                                        onClick = { 
                                            val current = tempVotes[player.id] ?: 0
                                            tempVotes[player.id] = current + 1
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFF1E2F23), RoundedCornerShape(6.dp))
                                    ) {
                                        Text("+", color = AccentCitizen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                    // Dialog Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel/Dismiss Button
                        Button(
                            onClick = { showVotingDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.LightGray
                            ),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "انصراف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // End Voting / Submit Button
                        Button(
                            onClick = {
                                tempVotes.forEach { (pid, votes) ->
                                    onUpdatePlayerVoteDirectly(pid, votes)
                                }

                                val qualifiedIds = tempVotes.filter { it.value >= threshold }.keys.toList()
                                playersInDefense.clear()
                                playersInDefense.addAll(qualifiedIds)

                                val qualifiedMockList = alivePlayers.filter { it.id in qualifiedIds }
                                defenseEligibleNames = qualifiedMockList.map { "${it.name} (${tempVotes[it.id] ?: 0} رأی)" }

                                isVotingCompleted = true
                                showDefenseResultDialog = true
                                showVotingDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCitizen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "پایان رأی‌گیری 🗳️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDefenseResultDialog) {
        StyledConfirmationDialog(
            title = "🗳️ نتایج رأی‌گیری و لیست دفاعیه",
            message = if (defenseEligibleNames.isEmpty()) {
                "هیچ بازیکنی حد نصاب ورود به دفاعیه (نصف یا بیشتر آرا) را کسب نکرد."
            } else {
                "بازیکنان زیر با کسب حد نصاب آرا به مرحله دفاعیه راه یافتند:\n\n" + 
                defenseEligibleNames.joinToString("\n") { "⚖️ $it" }
            },
            onConfirm = { showDefenseResultDialog = false },
            onDismiss = { showDefenseResultDialog = false }
        )
    }
}

@Composable
fun PlayerLiveCard(
    player: PlayerEntity,
    phase: String,
    players: List<PlayerEntity>,
    isInDefense: Boolean = false,
    onClick: () -> Unit,
    onRegisterEvent: (String) -> Unit,
    onToggleBlock: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleLife: () -> Unit,
    onUseLiveGun: () -> Unit = {}
) {
    val isDead = !player.isAlive
    val aliveCount = remember(players) { players.filter { it.isSelected && it.isAlive }.size }
    val defenseThreshold = kotlin.math.ceil(aliveCount / 2.0).toInt()
    val isEligibleForDefense = player.isAlive && player.voteCount > 0 && player.voteCount >= defenseThreshold && aliveCount > 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDead) Color(0xFF141010) else if (player.isKilledToday) Color(0xFF241517) else SurfaceDark
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDead) Color(0xFF2B1D1D) else if (player.isKilledToday) AccentCrimson.copy(alpha = 0.6f) else {
                when (player.assignedRoleTeam) {
                    "Mafia" -> AccentCrimson.copy(alpha = 0.4f)
                    "Citizen" -> AccentCitizen.copy(alpha = 0.4f)
                    else -> AccentGold.copy(alpha = 0.4f)
                }
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Player metadata head line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar layout
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (isDead) Color.DarkGray else {
                                    when (player.assignedRoleTeam) {
                                        "Mafia" -> AccentCrimson.copy(alpha = 0.15f)
                                        "Citizen" -> AccentCitizen.copy(alpha = 0.15f)
                                        else -> AccentGold.copy(alpha = 0.15f)
                                    }
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDead) "🪦" else if (player.isKilledToday) "💀" else {
                                when (player.assignedRoleTeam) {
                                    "Mafia" -> "🕶️"
                                    "Citizen" -> "🕊️"
                                    else -> "🎭"
                                }
                            },
                            fontSize = 14.sp
                        )
                    }

                    Column {
                        val isNight = (phase == "Night")
                        val teamColor = when (player.assignedRoleTeam) {
                            "Mafia" -> AccentCrimson
                            "Citizen" -> AccentCitizen
                            else -> AccentGold
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isDead) Color.Gray else Color.White,
                                fontSize = 14.sp,
                                textDecoration = if (isDead) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (player.isSlaughtered) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("🩸🔪", fontSize = 14.sp)
                            }
                        }
                        
                        if (isNight && !isDead) {
                            Text(
                                text = "🎭 ${player.assignedRoleName ?: "بدون نقش"}",
                                color = teamColor,
                                fontSize = 15.sp, // Bigger size for night phase
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text(
                                text = if (isDead) "حذف شده از سناریو" else {
                                    "${player.assignedRoleName ?: "بدون نقش"} | ${
                                        when (player.assignedRoleTeam) {
                                            "Mafia" -> "تیم مافیا"
                                            "Citizen" -> "تیم شهروندان"
                                            else -> "مستقل"
                                        }
                                    }"
                                },
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Interactive Buff badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (player.isSlaughtered) {
                        BadgeLabel(text = "سلاخی شده 🩸🔪", bgColor = Color(0xFF4A1015), txtColor = Color(0xFFFF5252))
                    }
                    if (player.isBlocked) {
                        BadgeLabel(text = "مسدود 🚫", bgColor = Color(0xFF2A1F1F), txtColor = AccentCrimson)
                    }
                    if (player.isMuted) {
                        BadgeLabel(text = "سایلنت 🔇", bgColor = Color(0xFF2A2A1F), txtColor = AccentGold)
                    }
                    if (player.isSilencedThisRound) {
                        BadgeLabel(text = "سکوت 🧠", bgColor = Color(0xFF271A3C), txtColor = Color(0xFFD8B4FE))
                    }
                    if (player.isSaved) {
                        BadgeLabel(text = "امن 🩺", bgColor = Color(0xFF1F2A21), txtColor = AccentCitizen)
                    }
                    if (player.isKilledToday) {
                        BadgeLabel(text = "💀 کشته روز", bgColor = Color(0xFF3B1F23), txtColor = Color(0xFFEF5350))
                    }
                    if (player.isVoteRevoked) {
                        BadgeLabel(text = "بدون حق رأی ❌", bgColor = Color(0xFF3B1F2A), txtColor = Color(0xFFE57373))
                    }
                    if (isInDefense && phase == "Day") {
                        BadgeLabel(text = "دفاع ⚖️", bgColor = Color(0xFF4D1D16), txtColor = Color(0xFFFFB300))
                    }
                    if (phase == "Day" && (player.hasBlankGunThisRound || player.hasLiveGunThisRound)) {
                        BadgeLabel(text = "مسلح 🔫", bgColor = Color(0xFF1E293B), txtColor = Color(0xFF60A5FA))
                    }

                    // Direct toggle life state button
                    IconButton(onClick = onToggleLife) {
                        Icon(
                            imageVector = if (isDead) Icons.Default.Refresh else Icons.Default.Close,
                            contentDescription = "تغییر حیات",
                            tint = if (isDead) AccentCitizen else AccentCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Defense Threshold Banner
            if (isEligibleForDefense && phase == "Day") {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4A1521), RoundedCornerShape(6.dp))
                        .border(1.dp, AccentCrimson, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val formattedThreshold = defenseThreshold.toString()
                    Text(
                        text = "🚨 ورود به دفاعیه (رأی کافی: ${player.voteCount} از فرجه $formattedThreshold) ⚖️",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (phase == "Day" && player.hasLiveGunThisRound && !player.usedLiveGun && !isDead) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUseLiveGun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("use_live_gun_btn_${player.id}")
                ) {
                    Text(
                        text = "استفاده از تفنگ جنگی 💥",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Night panel actions shortcuts
            if (!isDead && phase == "Night") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 6.dp))
                Text(
                    text = "ثبت سریع واقعه شب بازیکن 🌙:",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onRegisterEvent("KILL") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F1919)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💀 کشته", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Check if there is an active slaughter-capable role in the game
                    val hasSlaughterer = remember(players) {
                        players.any { p ->
                            p.isSelected && p.isAlive && p.assignedRoleName != null &&
                            (p.assignedRoleName.contains("حرفه") || p.assignedRoleName.contains("پدرخوانده") || p.assignedRoleName.contains("چرچیل"))
                        }
                    }

                    Button(
                        onClick = { onRegisterEvent("SLAUGHTER") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSlaughterer) Color(0xFF5C101C) else Color(0xFF261014)
                        ),
                        enabled = hasSlaughterer,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (hasSlaughterer) "🔪 سلاخی" else "🔒 سلاخی",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSlaughterer) Color.White else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onRegisterEvent("MUTE") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F3A19)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🔇 سکوت", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { onRegisterEvent("BLOCK") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF192A3F)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🚫 بلاک", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { onRegisterEvent("SAVE") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3F19)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🩺 نجات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Quick Day actions
            if (!isDead && phase == "Day") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!player.isKilledToday) {
                            Button(
                                onClick = { onRegisterEvent("MARK_KILLED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E24)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("💀 نشان کشته روز", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { onRegisterEvent("UNMARK_KILLED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3822)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("💖 نجات / رفع مرگ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onRegisterEvent("DEC_VOTE") },
                            modifier = Modifier.size(26.dp).background(Color(0xFF2C2C3F), CircleShape)
                        ) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "${player.voteCount} رأی",
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { onRegisterEvent("INC_VOTE") },
                            modifier = Modifier.size(26.dp).background(Color(0xFF2C2C3F), CircleShape)
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Simple note preview
            if (player.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181829), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "📝 یادداشت: ${player.note}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeLabel(text: String, bgColor: Color, txtColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = txtColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------
// 4. DIALOGS & SHEET PRESETS
// ------------------------------------------
@Composable
fun PlayerSettingsDialog(
    player: PlayerEntity,
    capabilityTemplates: List<String>,
    phase: String,
    lastMoveCards: List<LastMoveCard>,
    onDismiss: () -> Unit,
    onUpdateNote: (Int, String) -> Unit,
    onUseCapability: (Int, String) -> Unit,
    onToggleLastMove: (Int) -> Unit,
    onBurnLastMoveCard: (Int) -> Unit,
    onToggleMute: (Int) -> Unit,
    onToggleVoteRevoked: (Int) -> Unit,
    onUpdateVotes: (Int, Int) -> Unit,
    onUpdateWarnings: (Int, Int) -> Unit,
    onEliminateWithReason: (Int, String) -> Unit,
    onReviveWithReason: (Int, String) -> Unit,
    players: List<PlayerEntity> = emptyList(),
    onProfessionalShoot: (Int, Int, Boolean?) -> Unit = { _, _, _ -> },
    onProfessionalSlaughter: (Int, Int) -> Unit = { _, _ -> },
    onDoctorHeal: (Int, Int) -> Unit = { _, _ -> },
    onGodfatherShoot: (Int, Int) -> Unit = { _, _ -> },
    onGodfatherSlaughter: (Int, Int) -> Unit = { _, _ -> },
    onGodfatherRecruit: (Int, Int, (String) -> Unit) -> Unit = { _, _, _ -> },
    onBuyerRecruit: (Int, Int, (String) -> Unit) -> Unit = { _, _, _ -> },
    onMatadorBlock: (Int, Int) -> Unit = { _, _ -> },
    onGeneralCheck: (Int, Int, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onConstantineRevive: (Int, Int) -> Unit = { _, _ -> },
    onCitizenKaneReveal: (Int, Int, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onGiveGun: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    musketeerLiveGunExhausted: Boolean = false
) {
    var noteText by remember { mutableStateOf(player.note) }
    var showOverrideDialog by remember { mutableStateOf(false) }
    var userSelectedCapabilityCount by remember { mutableStateOf("1") }

    val caps = remember(player.capabilitiesJson) {
        try {
            if (player.capabilitiesJson.isNotBlank()) {
                Json.decodeFromString<List<RoleCapability>>(player.capabilitiesJson)
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    var showLastMoveDrawDialog by remember { mutableStateOf(false) }
    var showGeneralResultDialog by remember { mutableStateOf<String?>(null) }

    val handleDismiss = {
        onUpdateNote(player.id, noteText)
        onDismiss()
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Player Info
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "تنظیمات پیشرفته «${player.name}» ⚙️",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "نقش: ${player.assignedRoleName ?: "نامشخص"} | جناح: ${
                                when (player.assignedRoleTeam) {
                                    "Mafia" -> "مافیا"
                                    "Citizen" -> "شهروند"
                                    else -> "مستقل"
                                }
                            }",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = BorderColor)
                }

                // Section 1: Note editor
                item {
                    Text(text = "یادداشت‌های ویژه راوی (گاد):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                        },
                        placeholder = { Text("مثلاً: استعلام منفی شد یا شلیکش خطا رفت...", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section 2: Interactive capability trigger (ONLY in Night phase)
                if (phase == "Night") {
                    if (player.assignedRoleName?.contains("دکتر") == true && player.assignedRoleName?.contains("لکتور") == false) {
                        item {
                            Text(
                                text = "🩺 مدیریت نجات و شفای پزشک:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && (it.isAlive || it.isShotThisNight) }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val docCap = caps.find { it.name.contains("شفا") || it.name.contains("نجات") }
                            if (docCap != null) {
                                Text(
                                    text = "⚡ تعداد شفا/نجات باقی‌مانده پزشک: ${docCap.remainingCount} از ${docCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Text(
                                text = "تعداد خود-نجاتی‌های پزشک: ${player.doctorSelfSavesCount} از ۲",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن برای نجات... 🩺",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    val subLabel = if (aliveP.id == player.id) " (خود پزشک)" else ""
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})$subLabel",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var showDoctorConfirmDialog by remember { mutableStateOf(false) }
                            var showDoctorWarningDialog by remember { mutableStateOf(false) }

                            val hasRemainingHeals = docCap == null || docCap.remainingCount > 0
                            val isActionEnabled = selectedTarget != null && !player.isBlocked && hasRemainingHeals

                            Button(
                                onClick = {
                                    if (selectedTarget != null) {
                                        if (selectedTarget.id == player.id && player.doctorSelfSavesCount >= 2) {
                                            showDoctorWarningDialog = true
                                        } else {
                                            showDoctorConfirmDialog = true
                                        }
                                    }
                                },
                                enabled = isActionEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGold,
                                    contentColor = BackgroundDark,
                                    disabledContainerColor = Color(0xFF2C2C35)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ثبت نجات/شفا 🩺", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showDoctorWarningDialog) {
                                StyledConfirmationDialog(
                                    title = "خطای خود-نجاتی ⚠️",
                                    message = "دکتر قبلاً ۲ بار خود را نجات داده است و دیگر نمیتواند.",
                                    onConfirm = {
                                        showDoctorWarningDialog = false
                                    },
                                    onDismiss = {
                                        showDoctorWarningDialog = false
                                    }
                                )
                            }

                            if (showDoctorConfirmDialog && selectedTarget != null) {
                                StyledConfirmationDialog(
                                    title = "تأیید نجات پزشک 🩺",
                                    message = "آیا مطمئن هستید که دکتر این بازیکن را نجات دهد؟",
                                    onConfirm = {
                                        onDoctorHeal(player.id, selectedTarget.id)
                                        targetPlayerId = null
                                        showDoctorConfirmDialog = false
                                        handleDismiss()
                                    },
                                    onDismiss = {
                                        showDoctorConfirmDialog = false
                                    }
                                )
                            }
                        }
                    } else if (player.assignedRoleName?.contains("پدرخوانده") == true || player.assignedRoleName?.contains("رئیس مافیا") == true) {
                        item {
                            Text(
                                text = "👑 مدیریت شلیک و سلاخی رئیس مافیا (پدرخوانده):",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val gfCap = caps.find { it.name.contains("شلیک") }
                            if (gfCap != null) {
                                Text(
                                    text = "⚡ تعداد شلیک/سلاخی باقی‌مانده رئیس مافیا: ${gfCap.remainingCount} از ${gfCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            
                            val gfRecruitCap = caps.find { it.name.contains("خریداری") }
                            if (gfRecruitCap != null) {
                                Text(
                                    text = "🤝 تعداد خریداری (مذاکره) باقی‌مانده رئیس مافیا: ${gfRecruitCap.remainingCount} از ${gfRecruitCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var showLocalConfirmDialog by remember { mutableStateOf(false) }
                                var showShootConfirmDialog by remember { mutableStateOf(false) }
                                var showRecruitConfirmDialog by remember { mutableStateOf(false) }
                                
                                val hasRemainingShots = gfCap == null || gfCap.remainingCount > 0
                                val isActionEnabled = selectedTarget != null && !player.isBlocked && hasRemainingShots
                                
                                val hasRemainingRecruits = gfRecruitCap == null || gfRecruitCap.remainingCount > 0
                                val isRecruitEnabled = selectedTarget != null && !player.isBlocked && !player.isBlockedThisNight && hasRemainingRecruits

                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showShootConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF19323F),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("شلیک 🔫", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showLocalConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5C101C),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("سلاخی 🔪", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showRecruitConfirmDialog = true
                                        }
                                    },
                                    enabled = isRecruitEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E3A8A),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("خریداری (مذاکره) 🤝", color = if (isRecruitEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showShootConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید شلیک شبانه 👑",
                                        message = "آیا مطمئن هستید که رئیس مافیا (پدرخوانده) به بازیکن «${selectedTarget.name}» شلیک کند؟",
                                        onConfirm = {
                                            onGodfatherShoot(player.id, selectedTarget.id)
                                            targetPlayerId = null
                                            showShootConfirmDialog = false
                                            handleDismiss()
                                        },
                                        onDismiss = {
                                            showShootConfirmDialog = false
                                        }
                                    )
                                }

                                if (showLocalConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید سلاخی شبانه 🩸",
                                        message = "آیا نقش قربانی به درستی حدس زده شده و اون بازیکن توسط پدرخوانده سلاخی شود؟",
                                        onConfirm = {
                                            onGodfatherSlaughter(player.id, selectedTarget.id)
                                            targetPlayerId = null
                                            showLocalConfirmDialog = false
                                            handleDismiss()
                                        },
                                        onDismiss = {
                                            showLocalConfirmDialog = false
                                        }
                                    )
                                }
                                
                                if (showRecruitConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید خریداری (مذاکره) بازیکن 🤝",
                                        message = "آیا مطمئن هستید که می‌خواهید برای خریداری (مذاکره) بازیکن «${selectedTarget.name}» اقدام کنید و حدس نقش بزنید؟",
                                        onConfirm = {
                                            onGodfatherRecruit(player.id, selectedTarget.id) { resultMsg ->
                                                showGeneralResultDialog = resultMsg
                                            }
                                            targetPlayerId = null
                                            showRecruitConfirmDialog = false
                                        },
                                        onDismiss = {
                                            showRecruitConfirmDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (player.assignedRoleName?.contains("حرفه") == true) {
                        item {
                            Text(
                                text = "🎯 مدیریت شلیک و سلاخی حرفه‌ای:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val profCap = caps.find { it.name.contains("شلیک") }
                            if (profCap != null) {
                                Text(
                                    text = "⚡ تعداد شلیک/سلاخی باقی‌مانده حرفه‌ای: ${profCap.remainingCount} از ${profCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var showLocalConfirmDialog by remember { mutableStateOf(false) }
                                var showShootConfirmDialog by remember { mutableStateOf(false) }
                                
                                val hasRemainingShots = profCap == null || profCap.remainingCount > 0
                                val isActionEnabled = selectedTarget != null && !player.isBlocked && hasRemainingShots

                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showShootConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF19323F),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("شلیک 🔫", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showLocalConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5C101C),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("سلاخی 🔪", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showShootConfirmDialog && selectedTarget != null) {
                                    val isTargetGodfather = selectedTarget.assignedRoleName?.contains("پدرخوانده") == true
                                    val isTargetChurchill = selectedTarget.assignedRoleName?.contains("چرچیل") == true
                                    
                                    if (isTargetGodfather || isTargetChurchill) {
                                        // Immediately pause automated logic and show moderator override choices
                                        LaunchedEffect(Unit) {
                                            showShootConfirmDialog = false
                                            showOverrideDialog = true
                                        }
                                    } else {
                                        StyledConfirmationDialog(
                                            title = "تأیید شلیک شبانه 🔫",
                                            message = "آیا مطمئن هستید که حرفه‌ای به بازیکن «${selectedTarget.name}» شلیک کند؟",
                                            onConfirm = {
                                                onProfessionalShoot(player.id, selectedTarget.id, null)
                                                targetPlayerId = null
                                                showShootConfirmDialog = false
                                                handleDismiss()
                                            },
                                            onDismiss = {
                                                showShootConfirmDialog = false
                                            }
                                        )
                                    }
                                }

                                if (showOverrideDialog && selectedTarget != null) {
                                    Dialog(onDismissRequest = { }) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                            border = BorderStroke(1.dp, BorderColor),
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(AccentGold.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = AccentGold,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Text(
                                                    text = "تأیید شلیک به نقش خاص ⚡",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    textAlign = TextAlign.Center
                                                )

                                                Text(
                                                    text = "این نقش [پدرخوانده / چرچیل] است. آیا کشته شود؟",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 18.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            onProfessionalShoot(player.id, selectedTarget.id, true)
                                                            targetPlayerId = null
                                                            showOverrideDialog = false
                                                            handleDismiss()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(44.dp),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "بله 👍",
                                                            color = BackgroundDark,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            onProfessionalShoot(player.id, selectedTarget.id, false)
                                                            targetPlayerId = null
                                                            showOverrideDialog = false
                                                            handleDismiss()
                                                        },
                                                        border = BorderStroke(1.dp, BorderColor),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(44.dp),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "خیر 👎",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showLocalConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید سلاخی شبانه 🩸",
                                        message = "آیا نقش قربانی به درستی حدس زده شده و اون بازیکن سلاخی شود؟",
                                        onConfirm = {
                                            onProfessionalSlaughter(player.id, selectedTarget.id)
                                            targetPlayerId = null
                                            showLocalConfirmDialog = false
                                            handleDismiss()
                                        },
                                        onDismiss = {
                                            showLocalConfirmDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (player.assignedRoleName?.contains("ماتادور") == true) {
                        item {
                            Text(
                                text = "🧣 مدیریت مسدود‌سازی ماتادور:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val matadorCap = caps.find { it.name.contains("مسدود") }
                            if (matadorCap != null) {
                                Text(
                                    text = "⚡ تعداد مسدود‌سازی باقی‌مانده: ${matadorCap.remainingCount} از ${matadorCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var showBlockConfirmDialog by remember { mutableStateOf(false) }
                                
                                val hasRemainingBlocks = matadorCap == null || matadorCap.remainingCount > 0
                                val isActionEnabled = selectedTarget != null && !player.isBlocked && hasRemainingBlocks

                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showBlockConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5C101C),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("مسدود کردن نقش 🧣", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showBlockConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید مسدود‌سازی شبانه 🧣",
                                        message = "آیا مطمئن هستید که می‌خواهید قابلیت‌های بازیکن «${selectedTarget.name}» را امشب مسدود کنید؟",
                                        onConfirm = {
                                            onMatadorBlock(player.id, selectedTarget.id)
                                            targetPlayerId = null
                                            showBlockConfirmDialog = false
                                            handleDismiss()
                                        },
                                        onDismiss = {
                                            showBlockConfirmDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (player.assignedRoleName?.contains("اوشن") == true || player.assignedRoleName?.contains("ژنرال") == true) {
                        item {
                            Text(
                                text = "🌊 تشخیص هویت اوشن - ژنرال:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val generalCap = caps.find { it.name.contains("تشخیص") || it.name.contains("اوشن") }
                            if (generalCap != null) {
                                Text(
                                    text = "⚡ تعداد تشخیص باقی‌مانده: ${generalCap.remainingCount} از ${generalCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var showCheckConfirmDialog by remember { mutableStateOf(false) }
                                
                                val hasRemainingChecks = generalCap == null || generalCap.remainingCount > 0
                                val isActionEnabled = selectedTarget != null && !player.isBlocked && !player.isBlockedThisNight && hasRemainingChecks

                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showCheckConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E3A8A),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("شروع معارفه و تشخیص هویت 🌊", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showCheckConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید تشخیص هویت ژنرال 🌊",
                                        message = "آیا مطمئن هستید که می‌خواهید هویت تفصیلی بازیکن «${selectedTarget.name}» را امشب بررسی و معارفه کنید؟",
                                        onConfirm = {
                                            showCheckConfirmDialog = false
                                            onGeneralCheck(player.id, selectedTarget.id) { isMafia ->
                                                if (isMafia) {
                                                    showGeneralResultDialog = "هدف مافیا بود! اوشن (ژنرال) کشته شد."
                                                } else {
                                                    showGeneralResultDialog = "هدف امن است. او را بیدار کنید تا ژنرال را بشناسد."
                                                }
                                            }
                                        },
                                        onDismiss = {
                                            showCheckConfirmDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (player.assignedRoleName?.contains("کنستانتین") == true) {
                        item {
                            Text(
                                text = "⚡ احیای کنستانتین:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val deadPlayers = remember(players) {
                                players.filter { it.isSelected && !it.isAlive }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val constantineCap = caps.find { it.name.contains("احیا") || it.name.contains("کنستانتین") }
                            if (constantineCap != null) {
                                Text(
                                    text = "⚡ تعداد احیای باقی‌مانده: ${constantineCap.remainingCount} از ${constantineCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن حذف‌شده... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (deadPlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن حذف‌شده‌ای یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        deadPlayers.forEach { deadP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${deadP.name} (${deadP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = deadP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var showReviveConfirmDialog by remember { mutableStateOf(false) }
                                
                                val hasRemainingRevives = constantineCap == null || constantineCap.remainingCount > 0
                                val isActionEnabled = selectedTarget != null && !player.isBlocked && !player.isBlockedThisNight && hasRemainingRevives

                                Button(
                                    onClick = {
                                        if (selectedTarget != null) {
                                            showReviveConfirmDialog = true
                                        }
                                    },
                                    enabled = isActionEnabled,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F766E),
                                        disabledContainerColor = Color(0xFF2C2C35)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("احیا و زنده کردن بازیکن ⚡", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showReviveConfirmDialog && selectedTarget != null) {
                                    StyledConfirmationDialog(
                                        title = "تأیید احیا با کنستانتین ⚡",
                                        message = "آیا مطمئن هستید که می‌خواهید بازیکن «${selectedTarget.name}» را امشب احیا کنید و به بازی برگردانید؟ این قابلیت تنها یک بار در کل بازی قابل استفاده است.",
                                        onConfirm = {
                                            onConstantineRevive(player.id, selectedTarget.id)
                                            targetPlayerId = null
                                            showReviveConfirmDialog = false
                                            handleDismiss()
                                        },
                                        onDismiss = {
                                            showReviveConfirmDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (player.assignedRoleName?.contains("همشهری کین") == true) {
                        item {
                            Text(
                                text = "📰 افشاگری همشهری کین:",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val kaneCap = caps.find { it.name.contains("افشاگری") || it.name.contains("کین") }
                            if (kaneCap != null) {
                                Text(
                                    text = "📰 تعداد افشاگری باقی‌مانده: ${kaneCap.remainingCount} از ${kaneCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val hasRemainingReveals = kaneCap == null || kaneCap.remainingCount > 0
                            val isActionEnabled = selectedTarget != null && !player.isBlocked && !player.isBlockedThisNight && hasRemainingReveals
                            
                            Button(
                                onClick = {
                                    if (selectedTarget != null) {
                                        onCitizenKaneReveal(player.id, selectedTarget.id) { isMafia ->
                                            if (isMafia) {
                                                showGeneralResultDialog = "هدف جزء جناح مافیا بود! او با موفقیت به عنوان مافیای افشا شده علامت‌گذاری شد و در گزارش صبح به همه اعلام خواهد شد."
                                            } else {
                                                showGeneralResultDialog = "هدف جزء جناح مافیا نبود! استعلام منفی شد."
                                            }
                                        }
                                        targetPlayerId = null
                                    }
                                },
                                enabled = isActionEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E3A8A),
                                    disabledContainerColor = Color(0xFF2C2C35)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بررسی و افشاگری کین 📰", color = if (isActionEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (player.assignedRoleName?.contains("خریدار") == true) {
                        item {
                            Text(
                                text = "🤝 خریدار (مذاکره کننده):",
                                fontWeight = FontWeight.Bold,
                                color = AccentGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var isTargetMenuExpanded by remember { mutableStateOf(false) }
                            var targetPlayerId by remember { mutableStateOf<Int?>(null) }
                            val alivePlayers = remember(players) {
                                players.filter { it.isSelected && it.isAlive && it.id != player.id }
                            }
                            val selectedTarget = remember(targetPlayerId, players) {
                                players.find { it.id == targetPlayerId }
                            }
                            
                            val buyerCap = caps.find { it.name.contains("خریداری") }
                            if (buyerCap != null) {
                                Text(
                                    text = "🤝 تعداد خریداری (مذاکره) باقی‌مانده: ${buyerCap.remainingCount} از ${buyerCap.totalCount}",
                                    color = AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { isTargetMenuExpanded = true }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                                        color = if (selectedTarget != null) Color.White else Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentGold
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = isTargetMenuExpanded,
                                    onDismissRequest = { isTargetMenuExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    if (alivePlayers.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                                            onClick = { isTargetMenuExpanded = false }
                                        )
                                    } else {
                                        alivePlayers.forEach { aliveP ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})",
                                                        color = Color.White,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                onClick = {
                                                    targetPlayerId = aliveP.id
                                                    isTargetMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            var showRecruitConfirmDialog by remember { mutableStateOf(false) }
                            
                            val hasRemainingRecruits = buyerCap == null || buyerCap.remainingCount > 0
                            val isRecruitEnabled = selectedTarget != null && !player.isBlocked && !player.isBlockedThisNight && hasRemainingRecruits

                            Button(
                                onClick = {
                                    if (selectedTarget != null) {
                                        showRecruitConfirmDialog = true
                                    }
                                },
                                enabled = isRecruitEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E3A8A),
                                    disabledContainerColor = Color(0xFF2C2C35)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                             ) {
                                 Text("خریداری (مذاکره) 🤝", color = if (isRecruitEnabled) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                             }
                             
                             if (showRecruitConfirmDialog && selectedTarget != null) {
                                 StyledConfirmationDialog(
                                     title = "تأیید خریداری (مذاکره) بازیکن 🤝",
                                     message = "آیا مطمئن هستید که می‌خواهید برای خریداری (مذاکره) بازیکن «${selectedTarget.name}» اقدام کنید و حدس نقش بزنید؟",
                                     onConfirm = {
                                         onBuyerRecruit(player.id, selectedTarget.id) { resultMsg ->
                                             showGeneralResultDialog = resultMsg
                                         }
                                         targetPlayerId = null
                                         showRecruitConfirmDialog = false
                                         handleDismiss()
                                     },
                                     onDismiss = {
                                         showRecruitConfirmDialog = false
                                     }
                                 )
                             }
                         }
                     } else if (player.assignedRoleName?.contains("تفنگدار") == true) {
                         item {
                             Text(
                                 text = "🪖 تفنگدار (مسلح کردن بازیکنان):",
                                 fontWeight = FontWeight.Bold,
                                 color = AccentGold,
                                 fontSize = 12.sp
                             )
                             Spacer(modifier = Modifier.height(6.dp))

                             val liveCap = caps.find { it.name.contains("جنگی") }
                             val blankCap = caps.find { it.name.contains("مشقی") }
                             if (liveCap != null || blankCap != null) {
                                 Text(
                                     text = "⚔️ تفنگ جنگی باقی‌مانده: ${if (musketeerLiveGunExhausted) 0 else (liveCap?.remainingCount ?: 0)} | 🔫 تفنگ مشقی باقی‌مانده: ${blankCap?.remainingCount ?: "نامحدود"}",
                                     color = AccentGold,
                                     fontSize = 11.sp,
                                     fontWeight = FontWeight.Bold,
                                     modifier = Modifier.padding(bottom = 6.dp)
                                 )
                             }

                             var targetId1 by remember { mutableStateOf<Int?>(null) }
                             var targetId2 by remember { mutableStateOf<Int?>(null) }

                             var expanded1 by remember { mutableStateOf(false) }
                             var expanded2 by remember { mutableStateOf(false) }

                             val targetPlayer1 = remember(targetId1, players) { players.find { it.id == targetId1 } }
                             val targetPlayer2 = remember(targetId2, players) { players.find { it.id == targetId2 } }

                             val alivePlayers = remember(players) {
                                 players.filter { it.isSelected && it.isAlive }
                             }

                             val isActionAllowed = !player.isBlocked && !player.isBlockedThisNight

                             Column(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .background(Color(0xFF13131F), RoundedCornerShape(8.dp))
                                     .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                     .padding(8.dp)
                             ) {
                                 Text("🎯 انتخاب بازیکن اول:", color = Color.Gray, fontSize = 11.sp)
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .background(Color(0xFF191928), RoundedCornerShape(6.dp))
                                         .clickable { expanded1 = true }
                                         .padding(10.dp)
                                 ) {
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                      ) {
                                         Text(
                                             text = targetPlayer1?.name ?: "انتخاب بازیکن هدف اول... 👥",
                                             color = if (targetPlayer1 != null) Color.White else Color.Gray,
                                             fontSize = 11.sp
                                         )
                                         Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                     }
                                     DropdownMenu(
                                         expanded = expanded1,
                                         onDismissRequest = { expanded1 = false },
                                         modifier = Modifier.fillMaxWidth(0.8f).background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                     ) {
                                         DropdownMenuItem(
                                             text = { Text("هیچکدام", color = Color.Gray, fontSize = 11.sp) },
                                             onClick = { targetId1 = null; expanded1 = false }
                                         )
                                         alivePlayers.forEach { aliveP ->
                                             DropdownMenuItem(
                                                 text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                 onClick = { targetId1 = aliveP.id; expanded1 = false }
                                             )
                                         }
                                     }
                                 }

                                 if (targetPlayer1 != null) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Row(
                                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         val isBlankEnabled1 = isActionAllowed && (blankCap == null || blankCap.remainingCount > 0)
                                         Button(
                                             onClick = {
                                                onGiveGun(player.id, targetPlayer1.id, false)
                                                targetId1 = null
                                             },
                                             enabled = isBlankEnabled1,
                                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), disabledContainerColor = Color(0xFF1E293B)),
                                             modifier = Modifier.weight(1f).testTag("musketeer_blank_gun_1"),
                                             shape = RoundedCornerShape(8.dp)
                                         ) {
                                             Text("اعطای تیر مشقی 🔫", color = if (isBlankEnabled1) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                         }

                                         val anyLiveGunTonight1 = players.any { it.hasLiveGunThisRound }
                                         val isLiveEnabled1 = isActionAllowed && !musketeerLiveGunExhausted && !anyLiveGunTonight1 && (liveCap == null || liveCap.remainingCount > 0)
                                         Button(
                                             onClick = {
                                                onGiveGun(player.id, targetPlayer1.id, true)
                                                targetId1 = null
                                             },
                                             enabled = isLiveEnabled1,
                                             colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF1E293B)),
                                             modifier = Modifier.weight(1f).testTag("musketeer_live_gun_1"),
                                             shape = RoundedCornerShape(8.dp)
                                         ) {
                                             Text("اعطای تیر جنگی ⚔️", color = if (isLiveEnabled1) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                         }
                                     }
                                 }
                             }

                             Spacer(modifier = Modifier.height(10.dp))

                             Column(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .background(Color(0xFF13131F), RoundedCornerShape(8.dp))
                                     .border(1.5.dp, BorderColor, RoundedCornerShape(8.dp))
                                     .padding(8.dp)
                             ) {
                                 Text("🎯 انتخاب بازیکن دوم:", color = Color.Gray, fontSize = 11.sp)
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .background(Color(0xFF191928), RoundedCornerShape(6.dp))
                                         .clickable { expanded2 = true }
                                         .padding(10.dp)
                                 ) {
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text(
                                             text = targetPlayer2?.name ?: "انتخاب بازیکن هدف دوم... 👥",
                                             color = if (targetPlayer2 != null) Color.White else Color.Gray,
                                             fontSize = 11.sp
                                         )
                                         Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                     }
                                     DropdownMenu(
                                         expanded = expanded2,
                                         onDismissRequest = { expanded2 = false },
                                         modifier = Modifier.fillMaxWidth(0.8f).background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                     ) {
                                         DropdownMenuItem(
                                             text = { Text("هیچکدام", color = Color.Gray, fontSize = 11.sp) },
                                             onClick = { targetId2 = null; expanded2 = false }
                                         )
                                         alivePlayers.filter { it.id != (targetId1 ?: -1) }.forEach { aliveP ->
                                             DropdownMenuItem(
                                                 text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                                                 onClick = { targetId2 = aliveP.id; expanded2 = false }
                                             )
                                         }
                                     }
                                 }

                                 if (targetPlayer2 != null) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Row(
                                         horizontalArrangement = Arrangement.spacedBy(8.dp),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         val isBlankEnabled2 = isActionAllowed && (blankCap == null || blankCap.remainingCount > 0)
                                         Button(
                                             onClick = {
                                                onGiveGun(player.id, targetPlayer2.id, false)
                                                targetId2 = null
                                             },
                                             enabled = isBlankEnabled2,
                                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), disabledContainerColor = Color(0xFF1E293B)),
                                             modifier = Modifier.weight(1f).testTag("musketeer_blank_gun_2"),
                                             shape = RoundedCornerShape(8.dp)
                                         ) {
                                             Text("اعطای تیر مشقی 🔫", color = if (isBlankEnabled2) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                         }

                                         val anyLiveGunTonight2 = players.any { it.hasLiveGunThisRound }
                                         val isLiveEnabled2 = isActionAllowed && !musketeerLiveGunExhausted && !anyLiveGunTonight2 && (liveCap == null || liveCap.remainingCount > 0)
                                         Button(
                                             onClick = {
                                                onGiveGun(player.id, targetPlayer2.id, true)
                                                targetId2 = null
                                             },
                                             enabled = isLiveEnabled2,
                                             colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson, disabledContainerColor = Color(0xFF1E293B)),
                                             modifier = Modifier.weight(1f).testTag("musketeer_live_gun_2"),
                                             shape = RoundedCornerShape(8.dp)
                                         ) {
                                             Text("اعطای تیر جنگی ⚔️", color = if (isLiveEnabled2) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                         }
                                     }
                                 }
                             }
                         }
                     } else {
                        item {
                            Text(text = "استفاده از قابلیت‌های نقش بازیکن (مخصوص فاز شب):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (caps.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1F1F2F), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "این نقش در حال حاضر هیچ قابلیتی ندارد🕊️", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    caps.forEach { cap ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = cap.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(text = "تعداد مجاز باقی مانده: ${cap.remainingCount} از ${cap.totalCount}", color = TextSecondary, fontSize = 10.sp)
                                            }

                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            Button(
                                                onClick = { 
                                                    if (player.isBlocked) {
                                                        android.widget.Toast.makeText(context, "این بازیکن مسدود/بلاک است و استفاده از قابلیت انجام نمی‌شود!", android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        onUseCapability(player.id, cap.name)
                                                    }
                                                },
                                                enabled = cap.remainingCount > 0,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (player.isBlocked) AccentCrimson else AccentGold,
                                                    contentColor = BackgroundDark,
                                                    disabledContainerColor = Color(0xFF2C2C35)
                                                ),
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Text(if (player.isBlocked) "مسدود 🚫" else "استفاده ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Interactive Votes Counter in Day
                item {
                    Text(text = "تعداد آرای مأخوذه بازیکن در روز جاری 🗳️:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUpdateVotes(player.id, player.voteCount - 1) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1F1F2F), CircleShape)
                        ) {
                            Text("−", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = "${player.voteCount} رأی کتبی/شَفاهی",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = { onUpdateVotes(player.id, player.voteCount + 1) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF1F1F2F), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "افزایش رأی", tint = Color.White)
                        }
                    }
                }

                // Section 4: Cards control
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (player.hasUsedLastMoveCard) Color(0xFF2C2C3E) else AccentGold.copy(alpha=0.1f), RoundedCornerShape(10.dp))
                            .clickable { 
                                if (!player.hasUsedLastMoveCard) {
                                    showLastMoveDrawDialog = true 
                                } else {
                                    onToggleLastMove(player.id)
                                }
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                            Text(text = if (player.hasUsedLastMoveCard) "کارت وصیت صادر شده 🃏" else "قرعه‌کشی کارت حرکت پایانی 🎲", color = if (player.hasUsedLastMoveCard) Color.Gray else AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (player.hasUsedLastMoveCard) {
                             IconButton(onClick = { onToggleLastMove(player.id) }, modifier = Modifier.size(24.dp)) {
                                 Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                             }
                        } else {
                             Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentGold)
                        }
                    }
                }

                // Section 5: Warnings & Disciplinary Points (up to 3)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "⚠️ تعداد اخطارهای منضبطی بازیکن (${player.warningsCount} از ۳):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (0..3).forEach { rating ->
                            FilterChip(
                                selected = player.warningsCount == rating,
                                onClick = { onUpdateWarnings(player.id, rating) },
                                label = { Text("$rating اخطار", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (rating > 0) AccentCrimson else Color(0xFF66BB6A),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1B1B2A),
                                    labelColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 5.5: Disciplinary Mute & Vote Revoke
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "🛡️ محرومیت‌های انضباطیِ موقت (فقط برای فردا/امروز):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (player.isMuted) Color(0xFF2C2C1F) else Color(0xFF1B1B2A), RoundedCornerShape(8.dp))
                                .border(1.dp, if (player.isMuted) AccentGold else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onToggleMute(player.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سکوت انضباطی 🔇", fontSize = 10.sp, color = if (player.isMuted) AccentGold else Color.LightGray)
                            Checkbox(
                                checked = player.isMuted,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = AccentGold),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (player.isVoteRevoked) Color(0xFF2C1C24) else Color(0xFF1B1B2A), RoundedCornerShape(8.dp))
                                .border(1.dp, if (player.isVoteRevoked) AccentCrimson else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { onToggleVoteRevoked(player.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سلب حق رأی ❌", fontSize = 10.sp, color = if (player.isVoteRevoked) AccentCrimson else Color.LightGray)
                            Checkbox(
                                checked = player.isVoteRevoked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = AccentCrimson),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Section 6: Kill & Revive with specific reasons
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "💀 مدیریت حیات و تعیین دقیق دلیل خروج/نجات:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (player.isAlive) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1F1315), RoundedCornerShape(10.dp))
                                .border(1.dp, AccentCrimson.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("انتخاب علت حذف بازیکن از سناریو (مرگ قطعی):", color = TextSecondary, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onEliminateWithReason(player.id, "VOTE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421E23)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("۱. رأی‌گیری ⚖️", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onEliminateWithReason(player.id, "DISCIPLINARY") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421E23)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("۲. انضباطی 🛑", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onEliminateWithReason(player.id, "LOGICAL") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421E23)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("۳. منطقی 🎯", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF121F17), RoundedCornerShape(10.dp))
                                .border(1.dp, AccentCitizen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text("انتخاب دلیل احیا و بازگرداندن بازیکن به بازی:", color = TextSecondary, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onReviveWithReason(player.id, "RETURN") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF183821)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("۱. برگشت به سناریو 🤝", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onReviveWithReason(player.id, "RESURRECT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF183821)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("۲. زنده شدن 🩺", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Dismiss Controls
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = handleDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بستن و ذخیره نهایی 💾", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLastMoveDrawDialog) {
        SharedLastMoveDrawDialog(
            lastMoveCards = lastMoveCards,
            onBurnLastMoveCard = { cardId ->
                onBurnLastMoveCard(cardId)
                if (!player.hasUsedLastMoveCard) {
                    onToggleLastMove(player.id)
                }
            },
            onDismiss = { showLastMoveDrawDialog = false }
        )
    }

    showGeneralResultDialog?.let { message ->
        StyledConfirmationDialog(
            title = "نتیجه تشخیص هویت ژنرال 🌊",
            message = message,
            onConfirm = {
                showGeneralResultDialog = null
                handleDismiss()
            },
            onDismiss = {
                showGeneralResultDialog = null
                handleDismiss()
            }
        )
    }
}

@Composable
fun CapabilitiesManagementDialog(
    templates: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReset: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "مدیریت قابلیت‌های پیش‌فرض ⚙️", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    IconButton(onClick = onReset) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "بازنشانی", tint = AccentGold)
                    }
                }

                Text(text = "لیست تمام الگوهای ذخیره شده در راوی بازی:", color = Color.Gray, fontSize = 11.sp)

                Box(modifier = Modifier.height(140.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                        items(templates) { template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF19192A), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = template, color = Color.White, fontSize = 12.sp)
                                IconButton(onClick = { onDelete(template) }, modifier = Modifier.size(28.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                Text(text = "افزودن الگوی جدید ➕:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("مثلاً: پکیج خرید تیر تفنگ... ", fontSize = 11.sp, color = Color.Gray) },
                        maxLines = 1,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onAdd(textInput)
                                textInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark)
                    ) {
                        Text("ثبت", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انصراف و اتمام")
                }
            }
        }
    }
}

@Composable
fun ExportImportDialog(
    onDismiss: () -> Unit,
    exportData: () -> String,
    onImport: (String) -> Boolean
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    val exported = remember { exportData() }

    // Launcher for exporting (creating a file)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
                outputStream?.use {
                    it.write(exported.toByteArray(Charsets.UTF_8))
                    it.flush()
                }
                Toast.makeText(context, "فایل تنظیمات با موفقیت ذخیره شد! ✅", Toast.LENGTH_LONG).show()
                resultText = "فایل تنظیمات سناریو با موفقیت صادر و ذخیره شد. ✅"
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ذخیره‌ی فایل: ${e.message}", Toast.LENGTH_LONG).show()
                resultText = "خطا در صادر کردن فایل ❌"
            }
        }
    }

    // Launcher for importing (opening a file)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (content != null) {
                    val ok = onImport(content)
                    if (ok) {
                        Toast.makeText(context, "پیکربندی با موفقیت از فایل وارد شد! 🎉", Toast.LENGTH_LONG).show()
                        resultText = "پیکربندی بازی از فایل وارد شد! ✅"
                    } else {
                        Toast.makeText(context, "محتوای فایل نامعتبر است ❌", Toast.LENGTH_LONG).show()
                        resultText = "پیکربندی داخل فایل نامعتبر است ❌"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن فایل: ${e.message}", Toast.LENGTH_LONG).show()
                resultText = "خطا در وارد کردن فایل ❌"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .padding(16.dp)
        ) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📂 ورود و خروج کاملاً حرفه‌ای سناریو",
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "از این بخش می‌توانید سناریوی طراحی‌شده‌ی خود را به صورت فایل ذخیره کرده یا از فایل‌های دیگر بازیابی کنید.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // METHOD 1: Pro Files
                Text(
                    text = "◄ روش اول: کار با فایل سناریو (نیازمند برنامه مدیریت فایل)",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Action: Export to File Click
                    Button(
                        onClick = {
                            try {
                                exportLauncher.launch("mafia_scenario_export.json")
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در اجرای ابزار فایل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A1B), contentColor = AccentCitizen),
                        border = BorderStroke(1.dp, AccentCitizen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentCitizen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ذخیره در فایل 📥", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Action: Import from File Click
                    Button(
                        onClick = {
                            try {
                                importLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در اجرای ابزار فایل: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1B2A), contentColor = AccentGold),
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بارگذاری فایل 📂", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                // METHOD 2: Clipboard
                Text(
                    text = "◄ روش دوم: اشتراک‌گذاری سریع با کلیپ‌بورد (فوق‌العاده مطمئن 🚀)",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Copy to Clipboard Button
                    Button(
                        onClick = {
                            try {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(exported))
                                Toast.makeText(context, "سناریو به حافظه موقت کپی شد! 📋", Toast.LENGTH_SHORT).show()
                                resultText = "کد سناریو جهت اشتراک‌گذاری متنی کپی شد. ✅"
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2430), contentColor = Color(0xFF80CBC4)),
                        border = BorderStroke(1.dp, Color(0xFF80CBC4).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("کپی سناریو 📋", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    // Paste & Apply from Clipboard Button
                    Button(
                        onClick = {
                            try {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    val ok = onImport(clipText)
                                    if (ok) {
                                        Toast.makeText(context, "سناریو با موفقیت اعمال شد! 🎉", Toast.LENGTH_SHORT).show()
                                        resultText = "سناریو با موفقیت از کلیپ‌بورد وارد و اعمال شد. ✅"
                                    } else {
                                        Toast.makeText(context, "محتوای کلیپ‌بورد نامعتبر است ❌", Toast.LENGTH_SHORT).show()
                                        resultText = "محتوای کلیپ‌بورد معتبر نیست. ❌"
                                    }
                                } else {
                                    Toast.makeText(context, "حافظه موقت خالی است!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در خواندن کلیپ‌بورد: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2030), contentColor = Color(0xFFCE93D8)),
                        border = BorderStroke(1.dp, Color(0xFFCE93D8).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(45.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("خوانش + اعمال 📥", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                if (resultText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161624), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2E2E3F), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = resultText, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Advanced / Toggle raw text mode
                TextButton(
                    onClick = { showAdvanced = !showAdvanced }
                ) {
                    Text(
                        text = if (showAdvanced) "▲ مخفی‌سازی کدهای خام" else "▼ نمایش فیلدهای ویرایش متنی متن خام سناریو",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                if (showAdvanced) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "کد پیکربندی سناریوی فعلی (کپی دستی):", color = Color.Gray, fontSize = 10.sp)
                        OutlinedTextField(
                            value = exported,
                            onValueChange = {},
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )

                        Text(text = "یا پیکربندی متنی قبلی را برای اعمال پیست کنید:", color = Color.Gray, fontSize = 10.sp)
                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("کد خام سناریو را اینجا پیست کنید...", fontSize = 10.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )

                        Button(
                            onClick = {
                                val ok = onImport(importText)
                                resultText = if (ok) "پیکربندی متنی با موفقیت اعمال شد! ✅" else "پیکربندی متنی نامعتبر است ❌"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            enabled = importText.isNotBlank(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("اعمال سناریوی متنی 💾", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Text("بستن و بازگشت ❌", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ------------------------------------------
// PERSISTENT AUXILIARY UTILS
// ------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun EmptyListTip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101019), RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun AddCustomRoleDialog(
    templates: List<String>,
    onDismiss: () -> Unit,
    onAddRole: (name: String, team: String, description: String, capabilitiesJson: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTeam by remember { mutableStateOf("Citizen") } // "Citizen", "Mafia", "Independent"
    var selectedCaps by remember { mutableStateOf(mapOf<String, Int>()) } // capabilityName -> count

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "افزودن نقش سفارشی جدید 🎭",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام نقش جدید") },
                        placeholder = { Text("مثلاً: بمب‌گذار، قهرمان...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = AccentGold,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "انتخاب جناح نقش:", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Citizen" to "شهروند 🕊️",
                            "Mafia" to "مافیا 🕶️",
                            "Independent" to "مستقل 🎭"
                        ).forEach { (teamKey, teamTitle) ->
                            val isSelected = selectedTeam == teamKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) {
                                            when (teamKey) {
                                                "Citizen" -> AccentCitizen.copy(alpha = 0.2f)
                                                "Mafia" -> AccentCrimson.copy(alpha = 0.2f)
                                                else -> AccentGold.copy(alpha = 0.2f)
                                            }
                                        } else Color(0xFF161626),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) {
                                            when (teamKey) {
                                                "Citizen" -> AccentCitizen
                                                "Mafia" -> AccentCrimson
                                                else -> AccentGold
                                            }
                                        } else BorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTeam = teamKey }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = teamTitle,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات و ساید نقش") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = AccentGold,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = BorderColor)

                    Text(
                        text = "انتخاب قابلیت‌های توانمندی این نقش ⚙️:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    templates.forEach { template ->
                        val capCount = selectedCaps[template] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = capCount > 0,
                                    onCheckedChange = { checked ->
                                        selectedCaps = if (checked) {
                                            selectedCaps + (template to 1)
                                        } else {
                                            selectedCaps - template
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AccentGold,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                Text(text = template, color = Color.White, fontSize = 11.sp)
                            }

                            if (capCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (capCount > 1) {
                                                selectedCaps = selectedCaps + (template to (capCount - 1))
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF1E1E2F), CircleShape)
                                    ) {
                                        Text("−", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = capCount.toString(),
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedCaps = selectedCaps + (template to (capCount + 1))
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF1E1E2F), CircleShape)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                // Fixed Footer Action buttons
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val capsList = selectedCaps.map { (capName, count) ->
                                RoleCapability(name = capName, totalCount = count, remainingCount = count)
                            }
                            val capsJson = Json.encodeToString(capsList)
                            onAddRole(name, selectedTeam, description, capsJson)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("ثبت و افزودن نقش 💾", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انصراف")
                }
            }
        }
    }
}

@Composable
fun RoleCapabilitiesConfigDialog(
    role: RoleEntity,
    templates: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (id: Int, capabilitiesJson: String) -> Unit
) {
    val initialCaps = remember(role) {
        try {
            if (role.capabilitiesJson.isNotBlank()) {
                Json.decodeFromString<List<RoleCapability>>(role.capabilitiesJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    var selectedCaps by remember(role) {
        mutableStateOf(initialCaps.associate { it.name to it.totalCount })
    }

    var customCapName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تنظیم قابلیت‌های ${role.name} 🎭",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    val teamLabel = when(role.team) {
                        "Citizen" -> "شهروند 🕊️"
                        "Mafia" -> "مافیا 🕶️"
                        else -> "مستقل 🎭"
                    }
                    val teamColor = when(role.team) {
                        "Citizen" -> AccentCitizen
                        "Mafia" -> AccentCrimson
                        else -> AccentGold
                    }
                    Text(
                        text = teamLabel,
                        color = teamColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(teamColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (role.description.isNotBlank()) {
                    Text(
                        text = role.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .background(Color(0xFF0F0F1A), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    )
                }

                HorizontalDivider(color = BorderColor)

                // Scrollable container for capabilities list (flex-grow: 1; overflow-y: auto)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "انتخاب قابلیت‌های توانمندی این نقش ⚙️:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // List existing template options
                    val allOptions = remember(templates, selectedCaps) {
                        (templates + selectedCaps.keys).distinct()
                    }

                    allOptions.forEach { capName ->
                        val capCount = selectedCaps[capName] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = capCount > 0,
                                    onCheckedChange = { checked ->
                                        selectedCaps = if (checked) {
                                            selectedCaps + (capName to 1)
                                        } else {
                                            selectedCaps - capName
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AccentGold,
                                        uncheckedColor = Color.Gray
                                    )
                                )
                                Text(text = capName, color = Color.White, fontSize = 11.sp)
                            }

                            if (capCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (capCount > 1) {
                                                selectedCaps = selectedCaps + (capName to (capCount - 1))
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF1E1E2F), CircleShape)
                                    ) {
                                        Text("−", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = capCount.toString(),
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(16.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedCaps = selectedCaps + (capName to (capCount + 1))
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(0xFF1E1E2F), CircleShape)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Add Custom capability section right here inside the dialog scroll area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCapName,
                            onValueChange = { customCapName = it },
                            placeholder = { Text("قابلیت سفارشی جدید...", fontSize = 11.sp, color = Color.Gray) },
                            maxLines = 1,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (customCapName.isNotBlank()) {
                                    selectedCaps = selectedCaps + (customCapName.trim() to 1)
                                    customCapName = ""
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(AccentGold, RoundedCornerShape(8.dp))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "اضافه کردن", tint = BackgroundDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                // Fixed footer container for action buttons (NEVER scroll)
                Button(
                    onClick = {
                        val capsList = selectedCaps.map { (capName, count) ->
                            RoleCapability(name = capName, totalCount = count, remainingCount = count)
                        }
                        val capsJson = Json.encodeToString(capsList)
                        onConfirm(role.id, capsJson)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = BackgroundDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ذخیره و اضافه کردن نقش به بازی 🎯", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انصراف")
                }
            }
        }
    }
}

@Composable
fun RoleAbilityManagerDialog(
    role: RoleEntity,
    onDismiss: () -> Unit,
    onSave: (roleId: Int, selectedAbilityIds: List<String>) -> Unit
) {
    val decodedInitialAbilities = remember(role) {
        try {
            if (role.abilitiesJson.isNotBlank()) {
                Json.decodeFromString<List<String>>(role.abilitiesJson)
            } else {
                com.example.data.model.getRoleAbilities(role.name)
            }
        } catch (e: Exception) {
            com.example.data.model.getRoleAbilities(role.name)
        }
    }

    var selectedAbilityIds by remember(role) {
        mutableStateOf(decodedInitialAbilities)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدیریت قابلیت‌های نقش: ${role.name} 🎭",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    val teamLabel = when(role.team) {
                        "Citizen" -> "شهروند 🕊️"
                        "Mafia" -> "مافیا 🕶️"
                        else -> "مستقل 🎭"
                    }
                    val teamColor = when(role.team) {
                        "Citizen" -> AccentCitizen
                        "Mafia" -> AccentCrimson
                        else -> AccentGold
                    }
                    
                    Text(
                        text = teamLabel,
                        color = teamColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .background(teamColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (role.description.isNotBlank()) {
                    Text(
                        text = role.description,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F1A), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    )
                }

                HorizontalDivider(color = BorderColor)

                Text(
                    text = "انتخاب توانمندی‌های اختصاصی این نقش برای بازی ⚙️:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.data.model.ABILITY_REGISTRY.values.forEach { ability ->
                        val isSelected = selectedAbilityIds.contains(ability.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFF1E142F) else Color(0xFF13131F),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AccentGold.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedAbilityIds = if (isSelected) {
                                        selectedAbilityIds - ability.id
                                    } else {
                                        selectedAbilityIds + ability.id
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ability.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = ability.description,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                                if (ability.nightPriority != null && ability.nightPriority > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اولویت بیداری شب: ${ability.nightPriority}",
                                        color = AccentGold.copy(alpha = 0.8f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedAbilityIds = if (checked) {
                                        selectedAbilityIds + ability.id
                                    } else {
                                        selectedAbilityIds - ability.id
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentGold,
                                    checkedTrackColor = AccentGold.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF1E1E2F)
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(role.id, selectedAbilityIds)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_abilities_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره 💾", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انصراف ❌", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SharedLastMoveDrawDialog(
    lastMoveCards: List<LastMoveCard>,
    onBurnLastMoveCard: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var drawnCardResult by remember { mutableStateOf<LastMoveCard?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲 سامانه قرعه‌کشی کارت حرکت پایانی",
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontSize = 13.sp
                )

                val selectedCards = lastMoveCards.filter { it.isSelected }

                if (drawnCardResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121D12)),
                        border = BorderStroke(1.dp, AccentCitizen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎉 کارت انتخاب شده:", color = Color.Gray, fontSize = 10.sp)
                            Text(drawnCardResult!!.name, color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(drawnCardResult!!.description, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCitizen, contentColor = BackgroundDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("بستن و اعمال (کارت سوزانده شد) 🔥", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                } else if (selectedCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2C161D), RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("هیچ کارت حرکت پایانی فعالی جهت کشش وجود ندارد! (در تنظیمات کارتی انتخاب نشده است)", color = AccentCrimson, fontSize = 11.sp)
                    }
                } else {
                    Text(
                        text = "یکی از تایل‌های زیر را برگزینید تا کارت مستقر در پشت آن رو گشته و بسوزد:",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )

                    val cardChunks = selectedCards.chunked(3)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        cardChunks.forEachIndexed { rowIdx, rowCards ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowCards.forEachIndexed { cardIdx, card ->
                                    val idx = rowIdx * 3 + cardIdx
                                    val isBurnt = card.isBurnt
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .background(
                                                if (isBurnt) Color(0xFF241517) else Color(0xFF1B1B2C),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                BorderStroke(
                                                    if (isBurnt) 1.dp else 2.dp,
                                                    if (isBurnt) Color(0xFF5E2F2F) else AccentGold
                                                ),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = !isBurnt) {
                                                drawnCardResult = card
                                                onBurnLastMoveCard(card.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            if (isBurnt) {
                                                Text(
                                                    text = "تایل ${idx + 1}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "🔥 سوخته",
                                                    color = AccentCrimson,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = card.name,
                                                    color = Color.Gray.copy(alpha = 0.8f),
                                                    fontSize = 8.sp,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "🃏",
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${idx + 1}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = "کارت بسته",
                                                    color = Color.LightGray.copy(alpha = 0.6f),
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                val remainingSlots = 3 - rowCards.size
                                if (remainingSlots > 0) {
                                    for (i in 0 until remainingSlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    if (selectedCards.all { it.isBurnt }) {
                        Text(
                            text = "همه کارت‌های حرکت آخر این بازی سوخته‌اند ❌",
                            color = AccentCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameHistoryDialog(
    history: List<com.example.data.model.GameHistoryEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📜 تاریخچه بازی‌های ذخیره شده",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161624), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هنوز هیچ بازی‌ای به اتمام نرسیده و ذخیره نشده است.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(history.reversed()) { prevGame ->
                                var isExpanded by remember { mutableStateOf(false) }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2A)),
                                    border = BorderStroke(1.dp, Color(0xFF2C2C3F)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val pDate = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(prevGame.timestamp))
                                            Text("📅 $pDate", color = Color.LightGray, fontSize = 11.sp)
                                            Text(
                                                "🏆 برنده: ${if (prevGame.winnerTeam == "Citizen") "شهروندان" else if (prevGame.winnerTeam == "Mafia") "مافیا" else prevGame.winnerTeam}",
                                                color = if (prevGame.winnerTeam == "Citizen") AccentCitizen else if (prevGame.winnerTeam == "Mafia") AccentCrimson else AccentGold,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("علت پایان: ${prevGame.reason}", color = TextSecondary, fontSize = 10.sp)

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "🎤 نام گرداننده/خدا: ${prevGame.moderatorName.ifBlank { "ثبت نشده" }}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("📝 خلاصه‌ی گزارشات بازی و وضعیت نهایی بازیکنان در پایان بازی:", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Decode players simple structure
                                            val playerSummary = remember(prevGame.playersJson) {
                                                try {
                                                    val players = Json.decodeFromString<List<PlayerEntity>>(prevGame.playersJson)
                                                    players.joinToString("\n") { 
                                                        val status = if (it.isSlaughtered) "🔪 سلاخی" else if (it.isAlive) "✅ زنده" else "💀 مرده"
                                                        "$status | ${it.name} (${it.assignedRoleName ?: "بدون نقش"})" 
                                                    }
                                                } catch (e: Exception) {
                                                    "خطا در بارگذاری لیست بازیکنان یا داده خراب است."
                                                }
                                            }

                                            Text(playerSummary, color = Color.White, fontSize = 11.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3F)),
                    modifier = Modifier.fillMaxWidth().height(45.dp)
                ) {
                    Text("بستن تاریخچه", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModeratorNamePromptDialog(
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var textState by remember { mutableStateOf(initialValue) }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎤 نام گرداننده / خدا",
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "لطفاً نام گرداننده (خدا) بازی را برای این دور وارد نمایید تا در تاریخچه بازی ثبت گردد.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        if (it.trim().isNotEmpty()) {
                            isError = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moderator_name_input"),
                    label = { Text("نام گرداننده/خدا") },
                    placeholder = { Text("مثال: امیرحسین") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161624),
                        unfocusedContainerColor = Color(0xFF161624),
                        disabledContainerColor = Color(0xFF161624),
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = AccentGold,
                        unfocusedLabelColor = Color.Gray
                    ),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("نام گرداننده اجباری است!", color = Color(0xFFFF5252)) }
                    } else null
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (textState.trim().isBlank()) {
                                isError = true
                            } else {
                                onConfirm(textState.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCitizen,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("confirm_moderator_name_button")
                    ) {
                        Text(
                            text = "شروع بازی 🃏",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.LightGray
                        ),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "انصراف",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayStatsDialog(
    players: List<PlayerEntity>,
    onDismissRequest: () -> Unit
) {
    val activePlayers = remember(players) { players.filter { it.isSelected } }

    val totalCitizens = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Citizen" } }
    val aliveCitizens = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Citizen" && it.isAlive } }
    val deadCitizens = remember(activePlayers) { totalCitizens - aliveCitizens }

    val totalMafia = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Mafia" } }
    val aliveMafia = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Mafia" && it.isAlive } }
    val deadMafia = remember(activePlayers) { totalMafia - aliveMafia }

    val totalIndependents = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Independent" } }
    val aliveIndependents = remember(activePlayers) { activePlayers.count { it.assignedRoleTeam == "Independent" && it.isAlive } }
    val deadIndependents = remember(activePlayers) { totalIndependents - aliveIndependents }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📊 آمار زنده‌ها و کشته‌های جناح‌ها",
                    fontWeight = FontWeight.Bold,
                    color = AccentGold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "لیست آمار تفکیکی بازیکنان فعال این سناریو به صورت زیر است:",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Citizen Row
                    StatRow(
                        title = "🕊️ جناح شهروندان",
                        aliveCount = aliveCitizens,
                        deadCount = deadCitizens,
                        totalCount = totalCitizens,
                        color = AccentCitizen
                    )

                    // Mafia Row
                    StatRow(
                        title = "🕶️ جناح مافیا",
                        aliveCount = aliveMafia,
                        deadCount = deadMafia,
                        totalCount = totalMafia,
                        color = AccentCrimson
                    )

                    // Independent Row
                    StatRow(
                        title = "🎭 جناح مستقل",
                        aliveCount = aliveIndependents,
                        deadCount = deadIndependents,
                        totalCount = totalIndependents,
                        color = AccentGold
                    )
                }

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E3F)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "بستن آمار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(
    title: String,
    aliveCount: Int,
    deadCount: Int,
    totalCount: Int,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161624)),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 13.sp
                )
                Text(
                    text = "کل: $totalCount نفر",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("✅ زنده:", fontSize = 11.sp, color = Color.Gray)
                    Text("$aliveCount نفر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("💀 کشته:", fontSize = 11.sp, color = Color.Gray)
                    Text("$deadCount نفر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCrimson)
                }
            }
        }
    }
}

@Composable
fun TerroristSelectionDialog(
    activeTerrorist: PlayerEntity,
    players: List<PlayerEntity>,
    onConfirmTerror: (PlayerEntity) -> Unit,
    onNormalElimination: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedVictim by remember { mutableStateOf<PlayerEntity?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentCrimson.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Terrorist Ability",
                        tint = AccentCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = "ترور در روز (تروریست) 💣",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "تروریست «${activeTerrorist.name}» در رای‌گیری حذف شد! چه کسی را ترور می‌کند؟",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val otherAlivePlayers = players.filter { it.isAlive && it.id != activeTerrorist.id }
                if (otherAlivePlayers.isEmpty()) {
                    Text(
                        text = "هیچ بازیکن زنده دیگری جهت ترور یافت نشد.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(otherAlivePlayers) { player ->
                            val isSelected = selectedVictim?.id == player.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) AccentCrimson.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentCrimson else BorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedVictim = player }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player.name,
                                    color = if (isSelected) AccentCrimson else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (player.assignedRoleName != null) {
                                    Text(
                                        text = "(${player.assignedRoleName})",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedVictim?.let { victim ->
                                onConfirmTerror(victim)
                            }
                        },
                        enabled = selectedVictim != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCrimson,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تایید ترور 💥", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Button(
                        onClick = onNormalElimination,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("خروج عادی 🗳️", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NatoGuessActionContent(
    currentItem: com.example.data.model.NightActionQueueItem,
    players: List<PlayerEntity>,
    caps: List<RoleCapability>,
    viewModel: com.example.data.viewmodel.MafiaViewModel
) {
    val alivePlayers = remember(players) {
        players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
    }
    var selectedTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
    var isTargetMenuExpanded by remember { mutableStateOf(false) }
    
    val citizenRoles = remember {
        listOf(
            "پزشک", "دکتر", "کشیش", "روانپزشک", "هکر", "گورکن", "فرمانده", "کارآگاه", 
            "حرفه‌ای", "تفنگدار", "زره‌پوش", "جان‌سخت", "اوشن - ژنرال", "کنستانتین", 
            "همشهری کین", "شهروند ساده"
        )
    }
    var selectedRole by remember(currentItem) { mutableStateOf<String?>(null) }
    var isRoleMenuExpanded by remember { mutableStateOf(false) }
    var natoGuessAlertMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "حدس نقش توسط ناتو (جناح مافیا) 🎯",
            fontWeight = FontWeight.Bold,
            color = AccentCrimson,
            fontSize = 12.sp
        )

        val natoCap = caps.find { it.name.contains("حدس") || it.name.contains("ناتو") }
        if (natoCap != null) {
            Text(
                text = "🎯 تعداد حدس‌های ناتو باقی‌مانده: ${natoCap.remainingCount} از ${natoCap.totalCount}",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Standard target player dropdown
        Text(
            text = "۱. انتخاب شهروند زنده جهت حدس:",
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            val currentSelectedPlayer = alivePlayers.find { it.id == selectedTargetId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { isTargetMenuExpanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSelectedPlayer?.name ?: "انتخاب بازیکن شهروند...",
                    color = if (currentSelectedPlayer != null) Color.White else Color.Gray,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = isTargetMenuExpanded,
                onDismissRequest = { isTargetMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                if (alivePlayers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("هیچ بازیکن زنده معتبری وجود ندارد.", color = Color.Gray, fontSize = 11.sp) },
                        onClick = { isTargetMenuExpanded = false }
                    )
                } else {
                    alivePlayers.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name, color = Color.White, fontSize = 11.sp) },
                            onClick = {
                                selectedTargetId = p.id
                                isTargetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Role select Dropdown Menu next to/below target
        Text(
            text = "۲. حدس نقش این بازیکن:",
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { isRoleMenuExpanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedRole ?: "انتخاب نقش حدس زده شده...",
                    color = if (selectedRole != null) Color.White else Color.Gray,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = isRoleMenuExpanded,
                onDismissRequest = { isRoleMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                citizenRoles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role, color = Color.White, fontSize = 11.sp) },
                        onClick = {
                            selectedRole = role
                            isRoleMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val isNatoBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
        val isConfirmEnabled = selectedTargetId != null && selectedRole != null && !isNatoBlocked

        Button(
            onClick = {
                val targetId = selectedTargetId
                val roleName = selectedRole
                if (targetId != null && roleName != null) {
                    val target = alivePlayers.find { it.id == targetId }
                    if (target != null) {
                        viewModel.executeNatoGuess(
                            currentItem.player.id,
                            target.id,
                            roleName
                        ) { isCorrect, wrongCount ->
                            if (isCorrect) {
                                natoGuessAlertMessage = "حدس درست بود! بازیکن «${target.name}» واقعاً «${target.assignedRoleName ?: "نقش شهروندی"}» است."
                            } else {
                                if (wrongCount >= 3) {
                                    natoGuessAlertMessage = "حدس اشتباه بود! تعداد حدس‌های اشتباه ناتو به ۳ رسید و ناتو («${currentItem.player.name}») از بازی حذف شد! 💀"
                                } else {
                                    natoGuessAlertMessage = "حدس اشتباه بود! تعداد حدس‌های اشتباه ناتو: $wrongCount/3"
                                }
                            }
                            selectedTargetId = null
                            selectedRole = null
                        }
                    }
                }
            },
            enabled = isConfirmEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCrimson,
                disabledContainerColor = Color(0xFF1E1E2D)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("nato_guess_confirm_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isNatoBlocked) "🚫 مسدود شده‌اید" else "تایید حدس نقش ناتو 💥",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isConfirmEnabled) Color.White else Color.Gray
            )
        }
    }

    natoGuessAlertMessage?.let { msg ->
        StyledConfirmationDialog(
            title = "نتیجه حدس نقش ناتو 🎯",
            message = msg,
            onConfirm = { natoGuessAlertMessage = null },
            onDismiss = { natoGuessAlertMessage = null }
        )
    }
}

@Composable
fun SabotageActionContent(
    currentItem: com.example.data.model.NightActionQueueItem,
    players: List<PlayerEntity>,
    caps: List<RoleCapability>,
    viewModel: com.example.data.viewmodel.MafiaViewModel
) {
    val alivePlayers = remember(players) {
        players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
    }
    var selectedTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
    var isTargetMenuExpanded by remember { mutableStateOf(false) }
    var sabotageAlertMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "خرابکاری تفنگ (خرابکار) 🔫",
            fontWeight = FontWeight.Bold,
            color = AccentCrimson,
            fontSize = 12.sp
        )

        val sabotageCap = caps.find { it.name.contains("خرابکاری") || it.name.contains("sabotage") }
        if (sabotageCap != null) {
            Text(
                text = "🔫 تعداد خرابکاری‌های باقی‌مانده: ${sabotageCap.remainingCount} از ${sabotageCap.totalCount}",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "انتخاب بازیکن جهت خرابکاری تفنگ:",
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            val currentSelectedPlayer = alivePlayers.find { it.id == selectedTargetId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .clickable { isTargetMenuExpanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentSelectedPlayer?.name ?: "انتخاب بازیکن...",
                    color = if (currentSelectedPlayer != null) Color.White else Color.Gray,
                    fontSize = 11.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = isTargetMenuExpanded,
                onDismissRequest = { isTargetMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                if (alivePlayers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("هیچ بازیکن زنده معتبری وجود ندارد.", color = Color.Gray, fontSize = 11.sp) },
                        onClick = { isTargetMenuExpanded = false }
                    )
                } else {
                    alivePlayers.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name, color = Color.White, fontSize = 11.sp) },
                            onClick = {
                                selectedTargetId = p.id
                                isTargetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val isSaboteurBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
        val hasRemainingSabotage = sabotageCap == null || sabotageCap.remainingCount > 0
        val isConfirmEnabled = selectedTargetId != null && !isSaboteurBlocked && hasRemainingSabotage

        Button(
            onClick = {
                val targetId = selectedTargetId
                if (targetId != null) {
                    val target = alivePlayers.find { it.id == targetId }
                    if (target != null) {
                        viewModel.executeSabotage(
                            currentItem.player.id,
                            target.id
                        ) { resultMessage ->
                            sabotageAlertMessage = resultMessage
                            selectedTargetId = null
                        }
                    }
                }
            },
            enabled = isConfirmEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCrimson,
                disabledContainerColor = Color(0xFF1E1E2D)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("sabotage_confirm_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = if (isSaboteurBlocked) "🚫 مسدود شده‌اید" else "تایید خرابکاری تفنگ 🔫",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isConfirmEnabled) Color.White else Color.Gray
            )
        }
    }

    sabotageAlertMessage?.let { msg ->
        StyledConfirmationDialog(
            title = "نتیجه خرابکاری تفنگ 🔫",
            message = msg,
            onConfirm = { sabotageAlertMessage = null },
            onDismiss = { sabotageAlertMessage = null }
        )
    }
}

@Composable
fun ChurchillShootActionContent(
    currentItem: com.example.data.model.NightActionQueueItem,
    players: List<PlayerEntity>,
    caps: List<RoleCapability>,
    viewModel: com.example.data.viewmodel.MafiaViewModel,
    currentRound: Int,
    triggerConfirmation: (String, String, () -> Unit) -> Unit
) {
    val alivePlayers = remember(players) {
        players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
    }
    var selectedTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
    var isTargetMenuExpanded by remember { mutableStateOf(false) }
    var churchillAlertMessage by remember { mutableStateOf<String?>(null) }
    
    val churchillLastShotNight by viewModel.churchillLastShotNight.collectAsStateWithLifecycle()
    val isChurchillResting = churchillLastShotNight > 0 && (currentRound - churchillLastShotNight) < 2

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "شلیک شبانه چرچیل ⚔️🤵",
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            fontSize = 12.sp
        )

        if (isChurchillResting) {
            Text(
                text = "چرچیل در این شب استراحت می‌کند (هر دو شب یک بار).",
                color = AccentCrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Text(
                text = "انتخاب بازیکن هدف برای شلیک:",
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                val currentSelectedPlayer = alivePlayers.find { it.id == selectedTargetId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F18), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .clickable { isTargetMenuExpanded = true }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentSelectedPlayer?.name ?: "انتخاب بازیکن...",
                        color = if (currentSelectedPlayer != null) Color.White else Color.Gray,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = isTargetMenuExpanded,
                    onDismissRequest = { isTargetMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(SurfaceDark)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    if (alivePlayers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("هیچ بازیکن زنده معتبری وجود ندارد.", color = Color.Gray, fontSize = 11.sp) },
                            onClick = { isTargetMenuExpanded = false }
                        )
                    } else {
                        alivePlayers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, color = Color.White, fontSize = 11.sp) },
                                onClick = {
                                    selectedTargetId = p.id
                                    isTargetMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val isChurchillBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
            val isConfirmEnabled = selectedTargetId != null && !isChurchillBlocked

            Button(
                onClick = {
                    val targetId = selectedTargetId
                    if (targetId != null) {
                        val target = alivePlayers.find { it.id == targetId }
                        if (target != null) {
                            val isBulletproof = target.isBulletproof || target.assignedRoleName?.contains("رویین") == true || target.assignedRoleName?.lowercase()?.contains("bulletproof") == true
                            val isProtected = target.isProtected || target.assignedRoleName?.contains("محافظ") == true || target.assignedRoleName?.lowercase()?.contains("guardian") == true
                            
                            if (isBulletproof || isProtected) {
                                triggerConfirmation(
                                    "تایید شلیک چرچیل ⚔️",
                                    "شلیک به بازیکن «${target.name}» به خاطر قابلیت‌های دفاعی او (رویین‌تن/محافظ) بی‌اثر است. آیا شلیک اعمال شود؟"
                                ) {
                                    viewModel.churchillShoot(currentItem.player.id, target.id, currentRound)
                                    churchillAlertMessage = "شلیک به این بازیکن بی‌اثر است (رویین‌تن/محافظ)."
                                    selectedTargetId = null
                                }
                            } else {
                                triggerConfirmation(
                                    "تایید شلیک چرچیل ⚔️",
                                    "آیا مطمئن هستید که می‌خواهید به بازیکن «${target.name}» شلیک کنید؟ این کشته توسط پزشک قابل شفا نیست."
                                ) {
                                    viewModel.churchillShoot(currentItem.player.id, target.id, currentRound)
                                    churchillAlertMessage = "شلیک با موفقیت ثبت گردید."
                                    selectedTargetId = null
                                }
                            }
                        }
                    }
                },
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCrimson,
                    disabledContainerColor = Color(0xFF1E1E2D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("churchill_confirm_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (isChurchillBlocked) "🚫 مسدود شده‌اید" else "شلیک و شات چرچیل ⚔️🤵",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isConfirmEnabled) Color.White else Color.Gray
                )
            }
        }
    }

    churchillAlertMessage?.let { msg ->
        StyledConfirmationDialog(
            title = "گزارش شلیک چرچیل ⚔️",
            message = msg,
            onConfirm = { churchillAlertMessage = null },
            onDismiss = { churchillAlertMessage = null }
        )
    }
}

@Composable
fun ReviveActionContent(
    currentItem: com.example.data.model.NightActionQueueItem,
    players: List<PlayerEntity>,
    caps: List<RoleCapability>,
    viewModel: com.example.data.viewmodel.MafiaViewModel,
    triggerConfirmation: (String, String, () -> Unit) -> Unit
) {
    val deadPlayersForRevive = remember(players) {
        players.filter { it.isSelected && !it.isAlive }
    }
    var reviveTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
    var isReviveTargetMenuExpanded by remember { mutableStateOf(false) }
    var reviveAlertMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "مدیریت احیای بازیکن (کنستانتین) 🟢",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            fontSize = 12.sp
        )

        val reviveCap = caps.find { it.name.contains("احیا") || it.name.contains("زنده") }
        if (reviveCap != null) {
            Text(
                text = "🟢 تعداد قابلیت زنده کردن باقی‌مانده: ${reviveCap.remainingCount} از ${reviveCap.totalCount}",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .clickable { isReviveTargetMenuExpanded = true }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedReviveTarget = deadPlayersForRevive.find { it.id == reviveTargetId }
                Text(
                    text = selectedReviveTarget?.name ?: "انتخاب بازیکن از گورستان... 👥",
                    color = if (selectedReviveTarget != null) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
            }

            DropdownMenu(
                expanded = isReviveTargetMenuExpanded,
                onDismissRequest = { isReviveTargetMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                if (deadPlayersForRevive.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("هیچ بازیکنی در قبرستان یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                        onClick = { isReviveTargetMenuExpanded = false }
                    )
                } else {
                    deadPlayersForRevive.forEach { deadP ->
                        DropdownMenuItem(
                            text = { Text("${deadP.name} (${deadP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                            onClick = {
                                reviveTargetId = deadP.id
                                isReviveTargetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        val hasRemainingRevives = reviveCap == null || reviveCap.remainingCount > 0
        val isReviveBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
        val isActionEnabled = reviveTargetId != null && !isReviveBlocked && hasRemainingRevives

        Button(
            onClick = {
                val targetId = reviveTargetId
                if (targetId != null) {
                    val target = deadPlayersForRevive.find { it.id == targetId }
                    if (target != null) {
                        triggerConfirmation(
                            "تایید احیا و زنده کردن 🟢",
                            "آیا مطمئن هستید که می‌خواهید بازیکن «${target.name}» را به بازی بازگردانید؟"
                        ) {
                            viewModel.constantineRevive(currentItem.player.id, target.id)
                            reviveAlertMessage = "بازیکن «${target.name}» با موفقیت توسط کنستانتین احیا شده و به سناریو بازگشت."
                            reviveTargetId = null
                        }
                    }
                }
            },
            enabled = isActionEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), disabledContainerColor = Color(0xFF1C1C2E)),
            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("constantine_revive_confirm_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "احیای بازیکن 🟢",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isActionEnabled) Color.White else Color.Gray
            )
        }
    }

    reviveAlertMessage?.let { msg ->
        StyledConfirmationDialog(
            title = "پیام زنده کردن 🟢",
            message = msg,
            onConfirm = { reviveAlertMessage = null },
            onDismiss = { reviveAlertMessage = null }
        )
    }
}

@Composable
fun RevealActionContent(
    currentItem: com.example.data.model.NightActionQueueItem,
    players: List<PlayerEntity>,
    caps: List<RoleCapability>,
    viewModel: com.example.data.viewmodel.MafiaViewModel
) {
    val alivePlayersExceptSelfByReveal = remember(players) {
        players.filter { it.isSelected && it.isAlive && it.id != currentItem.player.id }
    }
    var revealTargetId by remember(currentItem) { mutableStateOf<Int?>(null) }
    var isRevealTargetMenuExpanded by remember { mutableStateOf(false) }
    var revealAlertMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131F), RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "بررسی و افشاگری کین (کارآگاه کین) 📰",
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            fontSize = 12.sp
        )

        val kaneCap = caps.find { it.name.contains("افشاگری") || it.name.contains("تشخیص") }
        if (kaneCap != null) {
            Text(
                text = "📰 تعداد افشاگری‌های باقی‌مانده: ${kaneCap.remainingCount} از ${kaneCap.totalCount}",
                color = AccentGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF191928), RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .clickable { isRevealTargetMenuExpanded = true }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedRevealTarget = alivePlayersExceptSelfByReveal.find { it.id == revealTargetId }
                Text(
                    text = selectedRevealTarget?.name ?: "انتخاب بازیکن هدف... 👥",
                    color = if (selectedRevealTarget != null) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
            }

            DropdownMenu(
                expanded = isRevealTargetMenuExpanded,
                onDismissRequest = { isRevealTargetMenuExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(SurfaceDark)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                if (alivePlayersExceptSelfByReveal.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("هیچ بازیکن زنده معتبری یافت نشد", color = Color.Gray, fontSize = 11.sp) },
                        onClick = { isRevealTargetMenuExpanded = false }
                    )
                } else {
                    alivePlayersExceptSelfByReveal.forEach { aliveP ->
                        DropdownMenuItem(
                            text = { Text("${aliveP.name} (${aliveP.assignedRoleName ?: "بدون نقش"})", color = Color.White, fontSize = 11.sp) },
                            onClick = {
                                revealTargetId = aliveP.id
                                isRevealTargetMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        val hasRemainingReveals = kaneCap == null || kaneCap.remainingCount > 0
        val isRevealBlocked = currentItem.player.isBlocked || currentItem.player.isBlockedThisNight
        val isActionEnabled = revealTargetId != null && !isRevealBlocked && hasRemainingReveals

        Button(
            onClick = {
                val targetId = revealTargetId
                if (targetId != null) {
                    val target = alivePlayersExceptSelfByReveal.find { it.id == targetId }
                    if (target != null) {
                        viewModel.citizenKaneReveal(currentItem.player.id, target.id) { isMafia ->
                            if (isMafia) {
                                revealAlertMessage = "نتیجه افشاگری: هدف جزء جناح مافیا بود! او با موفقیت به عنوان مافیای افشا شده علامت‌گذاری شد و در گزارش صبح به همه اعلام خواهد شد."
                            } else {
                                revealAlertMessage = "نتیجه افشاگری: هدف جزء جناح مافیا نبود! استعلام منفی شد."
                            }
                        }
                        revealTargetId = null
                    }
                }
            },
            enabled = isActionEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8), disabledContainerColor = Color(0xFF1C1C2E)),
            modifier = Modifier.fillMaxWidth().height(38.dp).testTag("kane_reveal_confirm_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "اجرای افشاگری کین 📰",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isActionEnabled) Color.White else Color.Gray
            )
        }
    }

    revealAlertMessage?.let { msg ->
        StyledConfirmationDialog(
            title = "گزارش استعلام و افشاگری کین 📰",
            message = msg,
            onConfirm = { revealAlertMessage = null },
            onDismiss = { revealAlertMessage = null }
        )
    }
}

@Composable
fun HomeScreen(
    onStartNewGame: () -> Unit,
    onShowHistory: () -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 400.dp)
        ) {
            // App Title/Logo Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large stylish emoji/logo
                Text(
                    text = "🎭",
                    fontSize = 64.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "مدیریت بازی مافیا",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentCrimson,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.testTag("app_logo_title")
                )
                Text(
                    text = "دستیار هوشمند و پیشرفته مدیریت سناریوهای مافیا",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start New Game Button
            Button(
                onClick = onStartNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_new_game_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شروع بازی جدید",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Match History Button
            Button(
                onClick = onShowHistory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("match_history_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تاریخچه بازیها",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // About App Button
            OutlinedButton(
                onClick = { showAboutDialog = true },
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("about_app_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "درباره برنامه",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "درباره برنامه 🎭",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "برنامه مدیریت بازی مافیا",
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "نسخه: 1.0.0",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "این برنامه ابزاری پیشرفته و کاربردی برای گردانندگان (مادریتورها) بازی جذاب مافیا است. با استفاده از این برنامه می‌توانید به راحتی نقش‌ها را بین بازیکنان توزیع کرده، رویدادهای فاز شب و روز را ثبت نموده و جریان بازی را بدون نقص هدایت کنید.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "فهمیدم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            containerColor = SurfaceDark,
            modifier = Modifier.padding(16.dp)
        )
    }
}

