package me.tagavari.airmessage.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.rxjava3.disposables.Disposable

fun Disposable.bindUntilStop(lifecycleOwner: LifecycleOwner) {
	if(lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
		//Ignore
		return
	}
	
	lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
		override fun onStop(owner: LifecycleOwner) {
			dispose()
		}
	})
}

fun Disposable.bindUntilDestroy(lifecycleOwner: LifecycleOwner) {
	if(lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
		//Ignore
		return
	}
	
	lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
		override fun onDestroy(owner: LifecycleOwner) {
			dispose()
		}
	})
}