package me.tagavari.airmessage.common.compose.provider

import androidx.compose.runtime.compositionLocalOf
import me.tagavari.airmessage.common.compose.remember.AudioPlaybackControls
import me.tagavari.airmessage.common.compose.remember.AudioPlaybackControlsEmpty

val LocalAudioPlayback = compositionLocalOf<AudioPlaybackControls> {
	AudioPlaybackControlsEmpty()
}
