package me.tagavari.airmessage.connection.request;

import android.os.Handler;
import android.os.Looper;

import androidx.core.util.Consumer;

import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;

public abstract class MessageResponseManager {
	private static final long timeoutDelay = 20 * 1000; //20-second delay
	private final Handler handler = new Handler(Looper.getMainLooper());
	private Consumer<MessageResponseManager> deregistrationListener;
	private final Runnable timeoutRunnable = () -> {
		//Calling the fail method
		onFail(Constants.messageErrorCodeLocalExpired, null);
		
		//Removing the item
		deregistrationListener.accept(this);
		
		//Getting the connection service
		/* ConnectionService connectionService = ConnectionService.getInstance();
		if(connectionService == null) return;
		
		for(int i = 0; i < connectionService.messageSendRequests.size(); i++) {
			if(!connectionService.messageSendRequests.valueAt(i).equals(MessageResponseManager.this)) continue;
			connectionService.messageSendRequests.removeAt(i);
			break;
		} */
	};
	
	public MessageResponseManager(Consumer<MessageResponseManager> deregistrationListener) {
		this.deregistrationListener = deregistrationListener;
	}
	
	public abstract void onSuccess();
	
	public abstract void onFail(int resultCode, String details);
	
	public void startTimer() {
		handler.postDelayed(timeoutRunnable, timeoutDelay);
	}
	
	public void stopTimer(boolean restart) {
		handler.removeCallbacks(timeoutRunnable);
		if(restart) handler.postDelayed(timeoutRunnable, timeoutDelay);
	}
}
