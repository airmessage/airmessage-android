package me.tagavari.airmessage.connection.request;

import android.os.Handler;
import android.os.Looper;
import androidx.core.util.Consumer;

public abstract class ChatCreationResponseManager {
	private static final long timeoutDelay = 10 * 1000; //10-second delay
	
	private Consumer<ChatCreationResponseManager> deregistrationListener;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable timeoutRunnable = () -> {
		//Calling the fail method
		//onFail(Constants.messageErrorCodeLocalExpired, null);
		onFail();
		
		//Removing the item
		deregistrationListener.accept(this);
		
		//Getting the connection service
		/* ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager == null) return;
		
		for(int i = 0; i < connectionManager.getChatCreationRequests().size(); i++) {
			if(!connectionManager.getChatCreationRequests().valueAt(i).equals(ChatCreationResponseManager.this)) continue;
			connectionManager.getChatCreationRequests().removeAt(i);
			break;
		} */
	};
	
	public ChatCreationResponseManager(Consumer<ChatCreationResponseManager> deregistrationListener) {
		this.deregistrationListener = deregistrationListener;
	}
	
	public abstract void onSuccess(String chatGUID);
	
	//abstract void onFail(int resultCode, String details);
	public abstract void onFail();
	
	public void startTimer() {
		handler.postDelayed(timeoutRunnable, timeoutDelay);
	}
	
	public void stopTimer() {
		handler.removeCallbacks(timeoutRunnable);
	}
}
