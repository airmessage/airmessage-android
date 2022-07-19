package me.tagavari.airmessage.enums

import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(LocationState.loading, LocationState.permission, LocationState.failed, LocationState.unavailable, LocationState.resolvable, LocationState.ready)
annotation class LocationState {
	companion object {
		const val loading = 0 //Loading in progress
		const val permission = 1 //Permission required
		const val failed = 2 //Load error
		const val unavailable = 3 //Service unavailable
		const val resolvable = 4 //User action required
		const val ready = 5 //OK
	}
}