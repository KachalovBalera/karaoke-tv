package com.karaoke.tv.soundcore

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class AudioOutputStatus(
    val isBluetoothConnected: Boolean,
    val deviceName: String,
    val isSoundcoreDetected: Boolean,
    val description: String
)

class SoundcoreManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun checkAudioOutput(): AudioOutputStatus {
        if (audioManager == null) {
            return AudioOutputStatus(
                isBluetoothConnected = false,
                deviceName = "Неизвестно",
                isSoundcoreDetected = false,
                description = "Аудиоменеджер недоступен"
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val btDevice = devices.find {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }

            if (btDevice != null) {
                val name = btDevice.productName?.toString() ?: "Bluetooth Аудиоустройство"
                val isSoundcore = name.contains("soundcore", ignoreCase = true) ||
                        name.contains("rave", ignoreCase = true)

                val desc = if (isSoundcore) {
                    "Подключена караоке-система Soundcore Rave 3S через Bluetooth A2DP. Звук выводится без потерь качества."
                } else {
                    "Подключено Bluetooth-устройство: $name. Если это Soundcore, микрофоны работают автономно."
                }

                return AudioOutputStatus(
                    isBluetoothConnected = true,
                    deviceName = name,
                    isSoundcoreDetected = isSoundcore,
                    description = desc
                )
            }
        }

        return AudioOutputStatus(
            isBluetoothConnected = false,
            deviceName = "HDMI / Встроенные динамики ТВ",
            isSoundcoreDetected = false,
            description = "Bluetooth-колонка не подключена. Музыка выводится через HDMI на телевизор."
        )
    }

    companion object {
        const val PREF_KEY_LATENCY_OFFSET = "soundcore_video_latency_offset_ms"
        const val DEFAULT_LATENCY_OFFSET = 0 // ms

        val CONNECTION_GUIDE = listOf(
            "1. Включите караоке-колонку Soundcore Rave 3S нажатием кнопки питания на панели управления.",
            "2. Активируйте режим сопряжения Bluetooth на колонке (кнопка с логотипом Bluetooth начнет мигать синим).",
            "3. В настройках вашей Android TV-приставки откройте: «Пульты и аксессуары» → «Добавить устройство».",
            "4. Выберите «Soundcore Rave 3S» из списка найденных устройств и подтвердите сопряжение.",
            "5. Включите оба штатных беспроводных микрофона Soundcore. Они подключаются напрямую к колонке по радиоканалу 2.4 ГГц.",
            "6. Отрегулируйте громкость микрофонов и эхо регуляторами на корпусе колонки. Наше приложение автоматически направляет звук минусовок через системный Bluetooth."
        )
    }
}
