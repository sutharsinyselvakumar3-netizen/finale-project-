package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AgroCardSurface
import com.example.ui.theme.AgroOutline
import com.example.ui.theme.AgroTextMuted
import com.example.ui.theme.AgroTextPrimary
import com.example.ui.theme.StatusEmergencyRed
import com.example.ui.theme.StatusSafeGreen
import com.example.ui.theme.StatusWarningOrange

@Composable
fun HardwareLinkBar(
    mainEspOnline: Boolean,
    secondaryEspOnline: Boolean,
    uartConnected: Boolean,
    camConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionBadge(
                label = "ESP #1 (MAIN)",
                isOnline = mainEspOnline,
                onlineText = "ONLINE",
                offlineText = "OFFLINE"
            )
            ConnectionBadge(
                label = "ESP #2 (HW)",
                isOnline = secondaryEspOnline,
                onlineText = "ONLINE",
                offlineText = "OFFLINE"
            )
            ConnectionBadge(
                label = "UART LINK",
                isOnline = uartConnected,
                onlineText = "CONNECTED",
                offlineText = "ERROR"
            )
            ConnectionBadge(
                label = "ESP32-CAM",
                isOnline = camConnected,
                onlineText = "STREAM",
                offlineText = "OFFLINE"
            )
        }
    }
}

@Composable
fun ConnectionBadge(
    label: String,
    isOnline: Boolean,
    onlineText: String = "ONLINE",
    offlineText: String = "OFFLINE"
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = AgroTextMuted
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isOnline) Color(0xFF14532D) else Color(0xFF450A0A))
                .border(1.dp, if (isOnline) StatusSafeGreen else StatusEmergencyRed, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) StatusSafeGreen else StatusEmergencyRed)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOnline) onlineText else offlineText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun TelemetryItemCard(
    title: String,
    value: String,
    unit: String = "",
    icon: ImageVector,
    statusColor: Color = StatusSafeGreen,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AgroCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AgroOutline))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = AgroTextMuted,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroTextPrimary
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        color = AgroTextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LevelGaugeBar(
    label: String,
    currentPercent: Int?,
    minThresholdPercent: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val pct = currentPercent ?: 0
    val isLow = pct < minThresholdPercent

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (currentPercent != null) "$pct%" else "--",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLow) StatusWarningOrange else accentColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF263329))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLow) StatusWarningOrange else accentColor)
            )
        }
    }
}
