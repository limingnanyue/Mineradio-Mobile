package com.mineradio.player.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mineradio.player.data.api.dto.LoginStatus
import com.mineradio.player.ui.component.GlassPanel
import com.mineradio.player.ui.theme.MineradioColors
import kotlinx.coroutines.delay

/**
 * 登录面板 —— 复刻桌面版 #login-modal（index.html:2470-2503）。
 *
 * 双平台：网易云（二维码登录）/ QQ 音乐（cookie 登录）。
 * 二维码：key → create → 轮询 check（2 秒一次）。
 * QQ：cookie 文本框 + 保存。
 */
@Composable
fun LoginPanel(
    provider: String,
    onProviderChange: (String) -> Unit,
    qrImgUrl: String?,
    qrStatus: String,
    qqCookieInput: String,
    onQqCookieChange: (String) -> Unit,
    onRefreshQr: () -> Unit,
    onSubmitQqCookie: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MineradioColors.FcBg.copy(alpha = 0.78f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 470.dp)
                .clickable(enabled = false) {},
            cornerRadius = 24,
        ) {
            Column(Modifier.padding(24.dp)) {
                // 平台切换 tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF)),
                ) {
                    listOf("netease" to "网易云", "qq" to "QQ 音乐").forEach { (key, label) ->
                        val active = provider == key
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) MineradioColors.FcAccent else Color.Transparent)
                                .clickable { onProviderChange(key) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (active) MineradioColors.ChillInk else MineradioColors.FcInk2,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Mineradio",
                    color = MineradioColors.Champagne,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (provider == "netease") "扫码登录网易云音乐" else "QQ 音乐 Cookie 登录",
                    color = MineradioColors.FcInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (provider == "netease") "用网易云 App 扫描下方二维码完成登录"
                    else "在 QQ 音乐网页版复制 cookie 后粘贴到下方",
                    color = MineradioColors.FcMuted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(18.dp))

                if (provider == "netease") {
                    // 二维码展示
                    Box(
                        Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!qrImgUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = qrImgUrl,
                                contentDescription = "登录二维码",
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text("生成中…", color = Color.Black, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        qrStatus,
                        color = MineradioColors.FcAccent,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRefreshQr,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MineradioColors.FcAccent,
                            contentColor = MineradioColors.ChillInk,
                        ),
                    ) { Text("刷新二维码") }
                } else {
                    // QQ cookie 输入
                    Text("Cookie", color = MineradioColors.FcMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MineradioColors.GlassDark)
                            .padding(12.dp),
                    ) {
                        BasicTextField(
                            value = qqCookieInput,
                            onValueChange = onQqCookieChange,
                            textStyle = TextStyle(
                                color = MineradioColors.FcInk,
                                fontSize = 11.sp,
                            ),
                            cursorBrush = SolidColor(MineradioColors.FcAccent),
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (qqCookieInput.isEmpty()) {
                            Text(
                                "uin=...; qqmusic_key=...; qm_keyst=...",
                                color = MineradioColors.FcMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onSubmitQqCookie,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MineradioColors.FcAccent,
                            contentColor = MineradioColors.ChillInk,
                        ),
                    ) { Text("保存") }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消", color = MineradioColors.FcMuted) }
                }
            }
        }
    }
}

/**
 * 账户面板 —— 复刻桌面版 #user-modal（index.html:2505-2530）。
 * 显示当前登录态 + 退出按钮。
 */
@Composable
fun AccountPanel(
    neteaseLogin: LoginStatus?,
    qqLogin: LoginStatus?,
    activeProvider: String,
    onSwitchProvider: (String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = if (activeProvider == "qq") qqLogin else neteaseLogin
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MineradioColors.FcBg.copy(alpha = 0.78f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 420.dp)
                .clickable(enabled = false) {},
            cornerRadius = 24,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("账户", color = MineradioColors.FcInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                // 头像
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MineradioColors.GlassDark),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatar = active?.avatarUrl
                    if (!avatar.isNullOrEmpty()) {
                        AsyncImage(model = avatar, contentDescription = "头像", modifier = Modifier.fillMaxSize())
                    } else {
                        Text("?", color = MineradioColors.FcMuted, fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    active?.nickname ?: "未登录",
                    color = MineradioColors.FcInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (active?.userId != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("UID: ${active.userId}", color = MineradioColors.FcMuted, fontSize = 11.sp)
                }
                // VIP 标签（对应桌面版 #user-modal-vip，index.html:2518-2522）
                val vipLabel = active?.displayVipLabel
                if (!vipLabel.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MineradioColors.Champagne.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(vipLabel, color = MineradioColors.Champagne, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(20.dp))
                // 平台切换
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("netease" to "网易云", "qq" to "QQ 音乐").forEach { (key, label) ->
                        val sel = activeProvider == key
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) MineradioColors.FcAccent else Color(0x1AFFFFFF))
                                .clickable { onSwitchProvider(key) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(label, color = if (sel) MineradioColors.ChillInk else MineradioColors.FcInk2, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MineradioColors.Danger,
                        contentColor = Color.White,
                    ),
                    enabled = active?.loggedIn == true,
                ) { Text("退出 ${if (activeProvider == "qq") "QQ 音乐" else "网易云"}") }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onDismiss) { Text("关闭", color = MineradioColors.FcMuted) }
            }
        }
    }
}
