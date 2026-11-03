package com.karaoke.tv.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karaoke.tv.presentation.components.QrCodeView
import com.karaoke.tv.presentation.components.TvButton
import com.karaoke.tv.presentation.theme.AccentPink
import com.karaoke.tv.presentation.theme.BackgroundDark
import com.karaoke.tv.presentation.theme.GoldStar
import com.karaoke.tv.presentation.theme.PrimaryPurple
import com.karaoke.tv.presentation.theme.PrimaryPurpleGlow
import com.karaoke.tv.presentation.theme.SecondaryCyan
import com.karaoke.tv.presentation.theme.SuccessGreen
import com.karaoke.tv.presentation.theme.SurfaceCard
import com.karaoke.tv.presentation.theme.TextMuted
import com.karaoke.tv.presentation.theme.TextPrimary
import com.karaoke.tv.presentation.theme.TextSecondary
import com.karaoke.tv.server.ConnectedPhoneClient

@Composable
fun PhoneConnectScreen(
    qrCodeUrl: String,
    localIpAddress: String,
    port: Int,
    adminPin: String,
    connectedPhonesCount: Int,
    connectedClients: List<ConnectedPhoneClient> = emptyList(),
    onRefreshQrCode: () -> Unit = {},
    onDisconnectAll: () -> Unit = {},
    onDisconnectClient: (String) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Large Sharp QR Code for Smartphone Camera & Actions
        Column(
            modifier = Modifier.width(320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(3.5.dp, PrimaryPurpleGlow, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeView(
                    data = qrCodeUrl,
                    sizePx = 280,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvButton(
                    text = "Обновить QR",
                    onClick = onRefreshQrCode,
                    isPrimary = true,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                if (connectedPhonesCount > 0) {
                    TvButton(
                        text = "Отключить",
                        onClick = onDisconnectAll,
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(40.dp))

        // Right Column: Connection Instructions, Status, and Active Devices
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "УПРАВЛЕНИЕ С ТЕЛЕФОНА",
                        color = PrimaryPurple,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Status Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F2E1E))
                        .border(1.dp, SuccessGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Сервер активен (порт $port)",
                            color = SuccessGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Управляйте караоке, ищите песни на RUTUBE и добавляйте треки в очередь прямо со смартфона без установки приложений!",
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            // Direct Link and PIN row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Direct Address Card
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(1.dp, Color(0xFF33334D), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "Адрес в браузере (Wi-Fi):", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "http://$localIpAddress:$port",
                            color = SecondaryCyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Connected Phones Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (connectedPhonesCount > 0) Color(0xFF132A1C) else SurfaceCard)
                        .border(1.dp, if (connectedPhonesCount > 0) SuccessGreen else Color(0xFF33334D), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "Подключено:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$connectedPhonesCount телефон(ов)",
                            color = if (connectedPhonesCount > 0) SuccessGreen else TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // PIN
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2E1065))
                        .border(1.dp, PrimaryPurpleGlow, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "PIN ведущего:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = adminPin,
                            color = GoldStar,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Connected Guests List
            if (connectedClients.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(1.dp, Color(0xFF33334D), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Подключённые участники:",
                        color = SecondaryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(80.dp)
                    ) {
                        items(connectedClients) { client ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1A38))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = client.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "(${client.ip})", color = TextMuted, fontSize = 11.sp)
                                }
                                Text(text = client.connectedAt, color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                // Steps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "1. Подключите телефон к домашней сети Wi-Fi", color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "2. Наведите камеру телефона на QR-код слева", color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Smartphone, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "3. В браузере откроется пульт: ищите караоке RUTUBE и пойте!", color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TvButton(
                text = "Вернуться на главную",
                onClick = onBack,
                isPrimary = false
            )
        }
    }
}
