package com.nkust.suggest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkust.suggest.data.api.NkustApiClient
import com.nkust.suggest.data.storage.SecurePreferencesManager
import com.nkust.suggest.ui.components.AppHeader
import com.nkust.suggest.ui.screens.ComposeScreen
import com.nkust.suggest.ui.screens.FeedbackScreen
import com.nkust.suggest.ui.screens.SettingsScreen
import com.nkust.suggest.ui.theme.NKUSTSuggestTheme
import com.nkust.suggest.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: SecurePreferencesManager
    private val apiClient = NkustApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = SecurePreferencesManager(this)

        setContent {
            var isDark by remember { mutableStateOf(prefsManager.loadDarkMode()) }
            var currentLang by remember { mutableStateOf(prefsManager.loadAppLang()) }
            var selectedTab by remember { mutableStateOf(0) }

            val langCycle = listOf("zh", "en", "ja")

            NKUSTSuggestTheme(darkTheme = isDark) {
                Scaffold(
                    topBar = {
                        AppHeader(
                            currentLang = currentLang,
                            onLangToggle = {
                                val nextIndex = (langCycle.indexOf(currentLang) + 1) % langCycle.size
                                currentLang = langCycle[nextIndex]
                                prefsManager.saveAppLang(currentLang)
                            },
                            isDark = isDark,
                            onThemeToggle = {
                                isDark = !isDark
                                prefsManager.saveDarkMode(isDark)
                            }
                        )
                    },
                    bottomBar = {
                        // 浮動膠囊導航欄 (Floating Capsule Bottom Bar)
                        FloatingCapsuleBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            currentLang = currentLang
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (selectedTab) {
                            0 -> ComposeScreen(
                                currentLang = currentLang,
                                prefsManager = prefsManager,
                                apiClient = apiClient
                            )
                            1 -> SettingsScreen(
                                currentLang = currentLang,
                                prefsManager = prefsManager
                            )
                            2 -> FeedbackScreen(
                                currentLang = currentLang
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 現代化極致圓角膠囊導航欄 (Capsule Floating Island)
 * 靈感來自 Google Gemini / iOS Dynamic Island 膠囊外觀
 */
@Composable
fun FloatingCapsuleBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentLang: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // 浮動大膠囊主體
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.25f),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 分頁 1: 撰寫建言
            CapsuleNavItem(
                isSelected = selectedTab == 0,
                icon = Icons.Outlined.Edit,
                label = when (currentLang) {
                    "en" -> "Compose"
                    "ja" -> "提言作成"
                    else -> "撰寫建言"
                },
                onClick = { onTabSelected(0) }
            )

            // 分頁 2: 常駐設定
            CapsuleNavItem(
                isSelected = selectedTab == 1,
                icon = Icons.Outlined.Tune,
                label = when (currentLang) {
                    "en" -> "Settings"
                    "ja" -> "登録設定"
                    else -> "常駐設定"
                },
                onClick = { onTabSelected(1) }
            )

            // 分頁 3: 高科iAI問題回饋
            CapsuleNavItem(
                isSelected = selectedTab == 2,
                icon = Icons.Outlined.ChatBubbleOutline,
                label = when (currentLang) {
                    "en" -> "Feedback"
                    "ja" -> "問題報告"
                    else -> "高科iAI問題回饋"
                },
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
private fun CapsuleNavItem(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "nav_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "nav_content"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
