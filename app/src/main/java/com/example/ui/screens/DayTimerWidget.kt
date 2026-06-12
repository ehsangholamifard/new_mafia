package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun DayTimerDialog(
    selectedTime: Int,
    onSelectedTimeChange: (Int) -> Unit,
    timeRemaining: Int,
    onTimeRemainingChange: (Int) -> Unit,
    isRunning: Boolean,
    onIsRunningChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedTimes = listOf(30, 45, 60, 90, 120, 180, 240, 300) // seconds
    val isWarning = timeRemaining <= 10 && timeRemaining > 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2F)),
            border = BorderStroke(1.dp, Color(0xFF2C2C3E)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "⏱️ مدیریت زمان سخنرانی روز",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "زمان مناسب را انتخاب کرده و شمارش را آغاز کنید. تایمر در پس‌زمینه نیز فعال خواهد ماند.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Large Clock Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF131324), RoundedCornerShape(14.dp))
                        .border(BorderStroke(1.dp, if (isWarning) Color(0xFFEF5350) else Color(0xFF2C2C3E)), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                            color = if (timeRemaining == 0) Color(0xFFEF5350) else if (isWarning) Color(0xFFD4AF37) else Color.White,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp
                        )
                        if (isWarning) {
                            Text(
                                text = "⚠️ ثانیه‌های پایانی!",
                                color = Color(0xFFD4AF37),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isRunning) {
                            Text(
                                text = "⏳ در حال شمارش معکوس...",
                                color = Color(0xFF81C784),
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                text = "⏸️ متوقف شده",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Time selector
                Text(
                    text = "انتخاب زمان سخنرانی:",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )

                // Grid of 2 Rows of predefined times
                val chunks = predefinedTimes.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunks.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            chunk.forEach { timeSec ->
                                val isSelected = selectedTime == timeSec
                                Button(
                                    onClick = {
                                        onSelectedTimeChange(timeSec)
                                        onTimeRemainingChange(timeSec)
                                        onIsRunningChange(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFD4AF37) else Color(0xFF2C2C3E),
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val display = if (timeSec % 60 == 0) "${timeSec / 60}m" else "${timeSec}s"
                                    Text(display, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Start, Pause, Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onIsRunningChange(!isRunning) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFD4AF37) else Color(0xFF43A047),
                            contentColor = if (isRunning) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f).height(46.dp)
                    ) {
                        Text(if (isRunning) "⏸️ مکث" else "▶️ شروع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onTimeRemainingChange(selectedTime)
                            onIsRunningChange(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("🔄 بازنشانی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C3E))

                // Dismiss / Close
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3E), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("بستن پنجره و ادامه در پس‌زمینه ❌", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
