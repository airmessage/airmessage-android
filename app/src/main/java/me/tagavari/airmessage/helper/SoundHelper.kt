package me.tagavari.airmessage.helper

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes

object SoundHelper {
	/**
	 * A constant value for how loud this app should play sounds
	 */
	private const val soundVolume = 0.15F
	
	/**
	 * Gets a [SoundPool] instance to use to play sounds
	 */
	@JvmStatic
	fun getSoundPool(): SoundPool {
		return SoundPool.Builder().apply {
			setAudioAttributes(AudioAttributes.Builder().apply {
				setLegacyStreamType(AudioManager.STREAM_SYSTEM)
			}.build())
			
			setMaxStreams(2)
		}.build()
	}
	
	@JvmStatic
	fun playSound(soundPool: SoundPool, @RawRes soundID: Int) {
		soundPool.play(soundID, soundVolume, soundVolume, 0, 0, 1F)
	}
}