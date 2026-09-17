package com.nkust.suggest.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkust.suggest.data.model.Constants
import com.nkust.suggest.data.model.UserProfile
import com.nkust.suggest.data.storage.SecurePreferencesManager
import com.nkust.suggest.ui.theme.PrimaryBlue

@Composable
fun SettingsScreen(
    currentLang: String,
    prefsManager: SecurePreferencesManager
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val initialProfile = remember { prefsManager.loadUserProfile() }

    var name by remember { mutableStateOf(initialProfile.name) }
    var guestType by remember { mutableStateOf(initialProfile.guestType) }
    var email by remember { mutableStateOf(initialProfile.email) }
    var phone by remember { mutableStateOf(initialProfile.phone) }
    var secrecyType by remember { mutableStateOf(initialProfile.secrecyType) }
    var doneOpen by remember { mutableStateOf(initialProfile.doneOpen) }
    var mailPassword by remember { mutableStateOf(initialProfile.mailPassword) }
    var showPassword by remember { mutableStateOf(false) }

    var guestDropdownExpanded by remember { mutableStateOf(false) }
    var secrecyDropdownExpanded by remember { mutableStateOf(false) }
    var doneOpenDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 卡片 1: 建言人常駐個資 (Rounded 16dp, Subtle Border, Icon Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = when (currentLang) {
                            "en" -> "Resident User Profile (Encrypted)"
                            "ja" -> "基本個人情報 (端末暗号化保存)"
                            else -> "建言人常駐個資 (本地安全記憶)"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 姓名
                Column {
                    Text("姓名 (必填)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: 王小明", fontSize = 13.sp) },
                        singleLine = true
                    )
                }

                // 身分別
                Column {
                    Text("身分別 (必填)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentGuest = Constants.GUEST_TYPES.find { it.key == guestType } ?: Constants.GUEST_TYPES[0]
                    val guestName = when (currentLang) {
                        "en" -> currentGuest.labelEn
                        "ja" -> currentGuest.labelJa
                        else -> currentGuest.labelZh
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { guestDropdownExpanded = true }
                            .padding(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(guestName, fontSize = 14.sp)
                            Text("▼", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = guestDropdownExpanded, onDismissRequest = { guestDropdownExpanded = false }) {
                            Constants.GUEST_TYPES.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(if (currentLang == "en") item.labelEn else if (currentLang == "ja") item.labelJa else item.labelZh) },
                                    onClick = {
                                        guestType = item.key
                                        guestDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 信箱
                Column {
                    Text("接收確認信之信箱 (必填)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: C110123456@nkust.edu.tw", fontSize = 13.sp) },
                        singleLine = true
                    )
                }

                // 電話
                Column {
                    Text("聯絡電話 (必填)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("例如: 0912345678", fontSize = 13.sp) },
                        singleLine = true
                    )
                }

                // 保密設定
                Column {
                    Text("個資是否保密", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentSecrecy = Constants.SECRECY_TYPES.find { it.key == secrecyType } ?: Constants.SECRECY_TYPES[0]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { secrecyDropdownExpanded = true }
                            .padding(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (currentLang == "en") currentSecrecy.labelEn else if (currentLang == "ja") currentSecrecy.labelJa else currentSecrecy.labelZh, fontSize = 14.sp)
                            Text("▼", fontSize = 11.sp)
                        }
                        DropdownMenu(expanded = secrecyDropdownExpanded, onDismissRequest = { secrecyDropdownExpanded = false }) {
                            Constants.SECRECY_TYPES.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(if (currentLang == "en") item.labelEn else if (currentLang == "ja") item.labelJa else item.labelZh) },
                                    onClick = {
                                        secrecyType = item.key
                                        secrecyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 卡片 2: Google 信箱自動驗證授權 (Rounded 16dp, Subtle Border, Icon Container)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = when (currentLang) {
                            "en" -> "Google Mail Auto-Auth"
                            "ja" -> "学生Googleメール自動認証設定"
                            else -> "Google 學生信箱自動驗證授權"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Google 應用程式密碼 (16碼 App Password)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = mailPassword,
                    onValueChange = { mailPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = {
                        Text(
                            "例如: abcd efgh ijkl mnop",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = if (showPassword) "隱藏" else "顯示",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showPassword = !showPassword }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            color = PrimaryBlue
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 密碼安全提示 Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "密碼由 Android 系統硬體金鑰模組（EncryptedSharedPreferences）加密保護，僅用於本機收取確認信，絕不上傳任何第三方伺服器。",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 儲存設定按鈕 (科技藍 50dp，向量圖標)
        Button(
            onClick = {
                prefsManager.saveUserProfile(
                    UserProfile(
                        name = name.trim(),
                        guestType = guestType,
                        email = email.trim(),
                        phone = phone.trim(),
                        secrecyType = secrecyType,
                        doneOpen = doneOpen,
                        mailPassword = mailPassword.trim()
                    )
                )
                Toast.makeText(context, "設定已安全加密儲存！", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (currentLang) {
                        "en" -> "Save Settings Locally"
                        "ja" -> "設定を端末に保存"
                        else -> "儲存常駐設定至本機"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
