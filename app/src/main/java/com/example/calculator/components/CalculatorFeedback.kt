package com.example.calculator.components

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

fun interface KeySoundPlayer {
    fun playKeyClick()
}

val LocalKeySoundPlayer = staticCompositionLocalOf<KeySoundPlayer?> { null }

class SystemKeySoundPlayer(
    private val view: View,
    context: Context = view.context,
) : KeySoundPlayer {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override fun playKeyClick() {
        val manager = audioManager
        if (manager != null) {
            if (manager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                manager.playSoundEffect(AudioManager.FX_KEY_CLICK, KEY_CLICK_VOLUME)
            }
        } else {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    private companion object {
        const val KEY_CLICK_VOLUME = 0.5f
    }
}

class CalculatorFeedbackController(
    private val view: View,
    private val keySoundPlayer: KeySoundPlayer,
    private val hapticEnabled: Boolean,
    private val soundEnabled: Boolean,
) {
    fun performKeyFeedback() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (soundEnabled) {
            keySoundPlayer.playKeyClick()
        }
    }
}

@Composable
fun rememberCalculatorFeedback(
    hapticEnabled: Boolean,
    soundEnabled: Boolean,
): CalculatorFeedbackController {
    val view = LocalView.current
    val overridePlayer = LocalKeySoundPlayer.current
    val soundPlayer = overridePlayer ?: remember(view) { SystemKeySoundPlayer(view) }
    return remember(view, soundPlayer, hapticEnabled, soundEnabled) {
        CalculatorFeedbackController(
            view = view,
            keySoundPlayer = soundPlayer,
            hapticEnabled = hapticEnabled,
            soundEnabled = soundEnabled
        )
    }
}
