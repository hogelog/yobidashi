package org.hogel.yobidashi

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

object AudioOutputs {
    // Device identity stable across reconnects: AudioDeviceInfo.id is not.
    fun key(device: AudioDeviceInfo): String = "${device.type}|${device.productName}"

    fun label(key: String): String {
        val type = key.substringBefore('|').toIntOrNull() ?: -1
        val name = key.substringAfter('|')
        return "$name (${typeLabel(type)})"
    }

    fun connected(context: Context): List<String> =
        context.getSystemService(AudioManager::class.java)
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.type != AudioDeviceInfo.TYPE_TELEPHONY && it.type != AudioDeviceInfo.TYPE_REMOTE_SUBMIX }
            .map { key(it) }
            .distinct()

    fun autoPlayAllowed(context: Context, allowedOutputs: Set<String>): Boolean =
        allowedOutputs.isEmpty() || connected(context).any { it in allowedOutputs }

    private fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "earpiece"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "wired headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "wired headphones"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "bluetooth"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "bluetooth sco"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "usb headset"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "usb"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "ble headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "ble speaker"
        AudioDeviceInfo.TYPE_HEARING_AID -> "hearing aid"
        else -> "type $type"
    }
}
