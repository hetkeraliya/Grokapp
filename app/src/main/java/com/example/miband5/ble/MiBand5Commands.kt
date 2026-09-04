package com.example.miband5.ble

object MiBand5Commands {
    val HR_CONTROL_STOP = byteArrayOf(0x15, 0x01, 0x00)
    val HR_CONTROL_CONTINUOUS = byteArrayOf(0x15, 0x01, 0x01)
    val HR_CONTROL_MANUAL = byteArrayOf(0x15, 0x02, 0x01)
}
