package me.tagavari.airmessage.helper

sealed class ProgressState {
	object Indeterminate : ProgressState()
	data class Determinate(val progress: Float) : ProgressState()
}
