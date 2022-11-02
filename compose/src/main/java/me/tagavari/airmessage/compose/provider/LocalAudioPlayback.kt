package me.tagavari.airmessage.compose.provider

import androidx.compose.runtime.compositionLocalOf
import me.tagavari.airmessage.compose.remember.AudioPlaybackControls
import me.tagavari.airmessage.compose.remember.AudioPlaybackControlsEmpty

val LocalAudioPlayback = compositionLocalOf<AudioPlaybackControls> {
	AudioPlaybackControlsEmpty()
}
