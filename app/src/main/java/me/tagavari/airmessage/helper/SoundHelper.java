package me.tagavari.airmessage.helper;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import androidx.annotation.RawRes;

public class SoundHelper {
	/**
	 * A constant value for how loud this app should play sounds
	 */
	public static final float soundVolume = 0.15F;
	
	/**
	 * Gets a {@link SoundPool} instance to use to play sounds
	 */
	public static SoundPool getSoundPool() {
		return new SoundPool.Builder().setAudioAttributes(new AudioAttributes.Builder()
				.setLegacyStreamType(AudioManager.STREAM_SYSTEM)
				.build())
				.setMaxStreams(2)
				.build();
	}
	
	public static void playSound(SoundPool soundPool, @RawRes int soundID) {
		soundPool.play(soundID, soundVolume, soundVolume, 0, 0, 1);
	}
}