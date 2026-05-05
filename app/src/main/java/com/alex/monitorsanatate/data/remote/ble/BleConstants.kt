package com.alex.monitorsanatate.data.remote.ble

import java.util.UUID

object BleConstants {
    val HEALTH_SERVICE_UUID: UUID =
        UUID.fromString("0000aa00-0000-1000-8000-00805f9b34fb")
    val BPM_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb")
    val ECG_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("0000aa02-0000-1000-8000-00805f9b34fb")
    val COMMAND_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("0000aa03-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
