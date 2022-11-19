package me.tagavari.airmessage.common.helper

import android.app.Activity
import android.content.*
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.rxjava3.disposables.Disposable
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.common.connection.ConnectionManager
import me.tagavari.airmessage.common.data.SharedPreferencesManager
import me.tagavari.airmessage.common.data.SharedPreferencesManager.getProxyType
import me.tagavari.airmessage.common.data.SharedPreferencesManager.isConnectionConfigured
import me.tagavari.airmessage.common.enums.ProxyType
import me.tagavari.airmessage.common.helper.ConnectionServiceLaunchHelper.launchPersistent
import me.tagavari.airmessage.service.ConnectionService
import me.tagavari.airmessage.service.ConnectionService.ConnectionBinder

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

typealias ConnectionManagerReceiver = (ConnectionManager) -> Unit

/**
 * A class that manages the link between the connection manager and an activity
 */
class ConnectionServiceLink(activity: ComponentActivity) {
	/**
	 * The instance of the currently bound connection manager, or null if there is none
	 */
	var connectionManager: ConnectionManager? = null
		private set
	private val connectionManagerReceiverList = mutableListOf<ConnectionManagerReceiver>()
	
	private val serviceConnection: ServiceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			//Set the service
			val binder = service as ConnectionBinder
			binder.connectionManager
			connectionManager = binder.connectionManager
			binder.connectionManager.connect()
			
			//Notify pending receivers
			for(receiver in connectionManagerReceiverList) {
				receiver(binder.connectionManager)
			}
			connectionManagerReceiverList.clear()
		}
		
		override fun onServiceDisconnected(name: ComponentName) {
			connectionManager = null
		}
	}
	
	/**
	 * Schedules a receiver to be run when the service connection is made,
	 * or runs it immediately if the service is already connected
	 */
	fun onServiceConnection(receiver: ConnectionManagerReceiver) {
		connectionManager?.also { receiver(it) }
			?: connectionManagerReceiverList.add(receiver)
	}
	
	init {
		if(activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
			activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
				override fun onCreate(owner: LifecycleOwner) {
					//Ignoring if the connection isn't configured
					if(!isConnectionConfigured(activity)) return
					
					//Starting the persistent connection service if required
					if(isConnectionConfigured(activity) && getProxyType(activity) == ProxyType.direct) {
						launchPersistent(activity)
					}
					
					//Binding to the connection service
					activity.bindService(
						Intent(activity, ConnectionService::class.java),
						serviceConnection,
						Context.BIND_AUTO_CREATE
					)
				}
				
				override fun onDestroy(owner: LifecycleOwner) {
					//Unbinding from the connection service
					if(connectionManager != null) {
						activity.unbindService(serviceConnection)
					}
				}
			})
		}
	}
}