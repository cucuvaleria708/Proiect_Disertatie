package com.alex.monitorsanatate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.ui.theme.SuccessGreen
import com.alex.monitorsanatate.ui.theme.WarningAmber
import com.alex.monitorsanatate.ui.theme.LiveRed

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (connectionState) {
        is ConnectionState.Connected   -> "Conectat"      to SuccessGreen
        is ConnectionState.Connecting  -> "Conectare..."  to WarningAmber
        is ConnectionState.Scanning    -> "Scanare..."    to WarningAmber
        is ConnectionState.Disconnected -> "Deconectat"   to LiveRed
        is ConnectionState.Error       -> "Eroare"        to LiveRed
    }

    val icon = if (connectionState is ConnectionState.Connected &&
        connectionState.device.method == ConnectionMethod.BLE)
        Icons.Filled.Bluetooth else Icons.Filled.Wifi

    Row(
        modifier = modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Icons.Filled.Circle, contentDescription = null,
            tint = color, modifier = Modifier.size(7.dp))
        Icon(icon, contentDescription = null,
            tint = color, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
