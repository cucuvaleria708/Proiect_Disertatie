package com.alex.monitorsanatate.domain.model

data class DeviceInfo(
    val name: String,
    val address: String,
    val method: ConnectionMethod,
    val rssi: Int? = null
)
