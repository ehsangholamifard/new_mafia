package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RoleCapability
import com.example.data.model.RoleEntity
import com.example.data.viewmodel.MafiaViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val AVAILABLE_ICONS = listOf(
    "🕊️", "🕶️", "🎭", "🔍", "🩺", "🔫", "🪖", "🛡️", "💪", "👑", "💊", "🤝", "💣",
    "🧛", "🧟", "👼", "🕵️", "👮", "👩‍⚕️", "🧑‍⚖️", "🗡️", "👁️"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagerDialog(
    roles: List<RoleEntity>,
    viewModel: MafiaViewModel,
    templates: List<String>,
    onDismiss: () -> Unit
) {
    var editingRole by remember { mutableStateOf<RoleEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F0F1A),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1B2C))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مدیریت جامع نقش‌ها 🎭", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                if (editingRole != null || isAddingNew) {
                    EditAddRoleForm(
                        role = editingRole,
                        templates = templates,
                        onCancel = {
                            editingRole = null
                            isAddingNew = false
                        },
                        onSave = { name, team, desc, icon, capsJson ->
                            if (editingRole != null) {
                                viewModel.updateRoleFull(editingRole!!.id, name, team, desc, icon)
                            } else {
                                viewModel.addCustomRole(name, team, desc, icon, capsJson)
                            }
                            editingRole = null
                            isAddingNew = false
                        }
                    )
                } else {
                    // List
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Button(
                                    onClick = { isAddingNew = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("افزودن نقش سفارشی جدید", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(roles) { role ->
                                RoleManagerItem(
                                    role = role,
                                    onEdit = { editingRole = role },
                                    onDelete = { viewModel.deleteRole(role) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoleManagerItem(
    role: RoleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2C)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C3E))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2C2C3E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (role.iconName.isNotBlank()) role.iconName else (role.name.takeLast(1).let { if (it.isBlank()) "🎭" else it }),
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(role.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val teamTransl = when(role.team) {
                    "Citizen" -> "شهروند"
                    "Mafia" -> "مافیا"
                    else -> "مستقل"
                }
                Text(teamTransl, color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFD4AF37))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
fun EditAddRoleForm(
    role: RoleEntity?,
    templates: List<String>,
    onCancel: () -> Unit,
    onSave: (name: String, team: String, desc: String, icon: String, capsJson: String) -> Unit
) {
    var name by remember { mutableStateOf(role?.name ?: "") }
    var description by remember { mutableStateOf(role?.description ?: "") }
    var selectedTeam by remember { mutableStateOf(role?.team ?: "Citizen") }
    var iconName by remember { mutableStateOf(if (!role?.iconName.isNullOrBlank()) role!!.iconName else "") }
    
    // Parse capabilities
    var selectedCaps by remember {
        mutableStateOf(
            try {
                val caps = Json.decodeFromString<List<RoleCapability>>(role?.capabilitiesJson ?: "[]")
                caps.associate { it.name to it.totalCount }
            } catch (e: Exception) {
                mapOf<String, Int>()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("نام نقش") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFD4AF37), unfocusedBorderColor = Color(0xFF2C2C3E)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Teams
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Citizen" to "شهروند", "Mafia" to "مافیا", "Independent" to "مستقل").forEach { (key, title) ->
                val isSelected = selectedTeam == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.2f) else Color(0xFF1B1B2C),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFD4AF37) else Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                        .clickable { selectedTeam = key }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = if (isSelected) Color(0xFFD4AF37) else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("توضیحات نقش") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFD4AF37), unfocusedBorderColor = Color(0xFF2C2C3E)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Text("انتخاب آیکون نمایشی:", color = Color.White, fontSize = 14.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AVAILABLE_ICONS) { icon ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (iconName == icon) Color(0xFFD4AF37).copy(alpha = 0.3f) else Color(0xFF1B1B2C), CircleShape)
                        .border(1.dp, if (iconName == icon) Color(0xFFD4AF37) else Color.Transparent, CircleShape)
                        .clickable { iconName = icon },
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 24.sp)
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C3E))

        Text("انتخاب قابلیت‌های توانمندی این نقش:", color = Color.White, fontWeight = FontWeight.Bold)

        templates.forEach { template ->
            val capCount = selectedCaps[template] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = capCount > 0,
                        onCheckedChange = { checked ->
                            selectedCaps = if (checked) selectedCaps + (template to 1) else selectedCaps - template
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD4AF37), uncheckedColor = Color.Gray)
                    )
                    Text(template, color = Color.White, fontSize = 12.sp)
                }

                if (capCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { if (capCount > 1) selectedCaps = selectedCaps + (template to (capCount - 1)) },
                            modifier = Modifier.size(28.dp).background(Color(0xFF2C2C3E), CircleShape)
                        ) { Text("−", color = Color.White) }
                        Text(capCount.toString(), color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { selectedCaps = selectedCaps + (template to (capCount + 1)) },
                            modifier = Modifier.size(28.dp).background(Color(0xFF2C2C3E), CircleShape)
                        ) { Text("+", color = Color.White) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3E)),
                modifier = Modifier.weight(1f)
            ) {
                Text("انصراف", color = Color.White)
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val capsList = selectedCaps.map { RoleCapability(it.key, it.value, it.value) }
                        onSave(name, selectedTeam, description, iconName, Json.encodeToString(capsList))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                modifier = Modifier.weight(1f)
            ) {
                Text("ثبت", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
