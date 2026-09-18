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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkust.suggest.data.api.NkustApiClient
import com.nkust.suggest.data.imap.MailVerifier
import com.nkust.suggest.data.model.Constants
import com.nkust.suggest.data.model.SuggestionPayload
import com.nkust.suggest.data.storage.SecurePreferencesManager
import com.nkust.suggest.ui.theme.CtaPrimary
import com.nkust.suggest.ui.theme.CtaPrimaryHover
import com.nkust.suggest.ui.theme.DangerRed
import com.nkust.suggest.ui.theme.PrimaryBlue
import com.nkust.suggest.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun ComposeScreen(
    currentLang: String,
    prefsManager: SecurePreferencesManager,
    apiClient: NkustApiClient,
    onStatusChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var subject by remember { mutableStateOf("") }
    var selectedUnitId by remember { mutableStateOf(Constants.UNITS[0].id) }
    var contentText by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var submitProgressMsg by remember { mutableStateOf("") }
    var showManualVerifyDialog by remember { mutableStateOf(false) }
    var manualVerifyCode by remember { mutableStateOf("") }

    var unitDropdownExpanded by remember { mutableStateOf(false) }

    // 字數統計計算 (每行算2字元)
    val lineCount = contentText.count { it == '\n' }
    val calculatedCharCount = contentText.length + lineCount
    val isOverLimit = calculatedCharCount > 1000

    val currentUnit = Constants.UNITS.find { it.id == selectedUnitId } ?: Constants.UNITS[0]
    val unitDisplayName = when (currentLang) {
        "en" -> currentUnit.nameEn
        "ja" -> currentUnit.nameJa
        else -> currentUnit.nameZh
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 卡片 1: 建言基本資訊 (Rounded 16dp, Subtle Border, Icon Container)
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
                // 卡片標題 + 現代向量圖示徽章
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
                            imageVector = Icons.Outlined.Assignment,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = when (currentLang) {
                            "en" -> "Basic Information"
                            "ja" -> "提言基本情報"
                            else -> "建言基本資訊"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 主旨
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentLang) {
                                "en" -> "Subject (Required)"
                                "ja" -> "提言件名 (必須)"
                                else -> "建言主旨 (必填)"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (subject.length > 20) DangerRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${subject.length} / 20",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (subject.length > 20) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { if (it.length <= 20) subject = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = {
                            Text(
                                when (currentLang) {
                                    "en" -> "Brief summary of topic (max 20 chars)"
                                    "ja" -> "提言の核心主旨を簡潔に入力 (最大20文字)"
                                    else -> "請簡要敘述建言核心主旨 (最多20字)"
                                },
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // 受理單位選單
                Column {
                    Text(
                        text = when (currentLang) {
                            "en" -> "Recipient Department (Required)"
                            "ja" -> "受付窓口・部署 (必須)"
                            else -> "受理單位 (必選)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { unitDropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = unitDisplayName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "選擇受理單位",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            Constants.UNITS.forEach { unit ->
                                val name = when (currentLang) {
                                    "en" -> unit.nameEn
                                    "ja" -> unit.nameJa
                                    else -> unit.nameZh
                                }
                                DropdownMenuItem(
                                    text = { Text(name, fontSize = 14.sp) },
                                    onClick = {
                                        selectedUnitId = unit.id
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 卡片 2: 建言詳細內容
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = when (currentLang) {
                                "en" -> "Detailed Content"
                                "ja" -> "提言詳細内容"
                                else -> "建言詳細內容"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isOverLimit) DangerRed.copy(alpha = 0.15f)
                                else SuccessGreen.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = if (isOverLimit) "超出 ${calculatedCharCount - 1000} 字" else "剩餘 ${1000 - calculatedCharCount} 字",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimit) DangerRed else SuccessGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = {
                        Text(
                            when (currentLang) {
                                "en" -> "Please describe campus, people, events, time, and location..."
                                "ja" -> "キャンパス、関係者、事象、日時、場所などを具体的に記載してください..."
                                else -> "請完整說明反映事項之所屬校區、人、事、時、地、物，俾利承辦單位有效處理及回覆..."
                            },
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // 規範提示 (高雅 Info Notice Banner)
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
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = when (currentLang) {
                            "en" -> "Per campus rules: text is 1:1, only line breaks count as 2 characters."
                            "ja" -> "本校規定：文字は1:1計算（改行のみ2文字として計算されます）。"
                            else -> "依校務規範：中文字與英數字均為 1:1 計算，僅換行折算 2 字元。"
                        },
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 提交按鈕與狀態指示 (現代高質感科技藍漸層按鈕，告別廉價橘)
        Button(
            onClick = {
                val profile = prefsManager.loadUserProfile()
                if (profile.name.isBlank() || profile.email.isBlank() || profile.phone.isBlank()) {
                    Toast.makeText(context, "請先切換至「常駐設定」填寫姓名、信箱與電話！", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (subject.isBlank()) {
                    Toast.makeText(context, "請填寫建言主旨！", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (contentText.isBlank()) {
                    Toast.makeText(context, "請填寫建言內容！", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (isOverLimit) {
                    Toast.makeText(context, "建言內容超出 1000 字限制！", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSubmitting = true
                submitProgressMsg = "正在送出建言..."
                onStatusChange(submitProgressMsg)

                coroutineScope.launch {
                    val payload = SuggestionPayload(
                        subject = subject,
                        unitId = selectedUnitId,
                        content = contentText,
                        name = profile.name,
                        guestType = profile.guestType,
                        email = profile.email,
                        phone = profile.phone,
                        secrecyType = profile.secrecyType,
                        doneOpen = profile.doneOpen
                    )

                    val submitRes = apiClient.submitSuggestion(payload)
                    if (!submitRes.success) {
                        isSubmitting = false
                        submitProgressMsg = submitRes.message
                        onStatusChange("送出失敗")
                        Toast.makeText(context, submitRes.message, Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // 若使用者有設定 Google 授權碼，自動連線 IMAP 擷取確認信
                    if (profile.mailPassword.isNotBlank()) {
                        submitProgressMsg = "建言已送出！正在自動接收確認信..."
                        onStatusChange("正在接收確認信...")

                        try {
                            val verifier = MailVerifier(username = profile.email, passwordRaw = profile.mailPassword)
                            val confirmUrl = verifier.waitForConfirmationUrl { msg ->
                                submitProgressMsg = msg
                                onStatusChange(msg)
                            }

                            submitProgressMsg = "找到確認信，正在自動點擊確認..."
                            val clickRes = apiClient.clickConfirmationUrl(confirmUrl)
                            isSubmitting = false
                            submitProgressMsg = clickRes.message
                            onStatusChange("已送出並確認完成")
                            Toast.makeText(context, clickRes.message, Toast.LENGTH_LONG).show()

                            // 清空表單
                            subject = ""
                            contentText = ""
                        } catch (e: Exception) {
                            isSubmitting = false
                            submitProgressMsg = "自動確認逾時或失敗: ${e.message}"
                            onStatusChange("待手動確認")
                            Toast.makeText(context, submitProgressMsg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        isSubmitting = false
                        submitProgressMsg = "建言已送出！請至信箱點擊確認信完成送出。"
                        onStatusChange("已送出待確認")
                        Toast.makeText(context, submitProgressMsg, Toast.LENGTH_LONG).show()
                        subject = ""
                        contentText = ""
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CtaPrimary,
                disabledContainerColor = CtaPrimary.copy(alpha = 0.5f)
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = submitProgressMsg, color = Color.White, fontSize = 14.sp)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (currentLang) {
                            "en" -> "Submit Suggestion & Auto-Confirm"
                            "ja" -> "提言を送信して自動確認"
                            else -> "確認並送出建言"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        // 取消等待輔助按鈕：若自動搜尋過久，使用者可自由切換為手動確認模式
        if (isSubmitting) {
            TextButton(
                onClick = {
                    isSubmitting = false
                    submitProgressMsg = "已停止自動搜尋確認信，請自行至學校信箱點擊確認信。"
                    onStatusChange("待手動確認")
                    Toast.makeText(context, "已停止自動搜尋，請前往信箱點擊確認信！", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (currentLang) {
                        "en" -> "Stop Waiting & Check Mailbox Manually"
                        "ja" -> "待機を中止して手動でメールを確認する"
                        else -> "取消等待，改為自行前往信箱手動確認"
                    },
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // UX Persuasion: 安全背書承諾 (Trust Signal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = when (currentLang) {
                    "en" -> "Secure local memory · Direct connection to campus portal"
                    "ja" -> "端末内暗号化保存 · 公式ポータルへ直接送信"
                    else -> "本地端高強度安全記憶・資料直連校務系統"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }

        if (submitProgressMsg.isNotEmpty() && !isSubmitting) {
            Text(
                text = submitProgressMsg,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
