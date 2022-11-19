package me.tagavari.airmessage.common.helper

sealed class ProgressState {
	object Indeterminate : ProgressState()
	data class Determinate(val progress: Float) : ProgressState()
}
