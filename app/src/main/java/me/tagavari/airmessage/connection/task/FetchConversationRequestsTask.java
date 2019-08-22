package me.tagavari.airmessage.connection.task;

import android.content.Context;
import android.os.AsyncTask;

import androidx.core.util.Consumer;

import java.lang.ref.WeakReference;
import java.util.List;

import me.tagavari.airmessage.connection.request.ConversationInfoRequest;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.service.ConnectionService;

public class FetchConversationRequestsTask extends AsyncTask<Void, Void, List<ConversationInfo>> {
	private final WeakReference<Context> contextReference;
	private final Consumer<List<ConversationInfo>> finishListener;
	
	public FetchConversationRequestsTask(Context context, Consumer<List<ConversationInfo>> finishListener) {
		//Setting the context reference
		contextReference = new WeakReference<>(context);
		
		//Setting the finish listener
		this.finishListener = finishListener;
	}
	
	@Override
	protected List<ConversationInfo> doInBackground(Void... parameters) {
		Context context = contextReference.get();
		if(context == null) return null;
		
		//Fetching the incomplete conversations
		return DatabaseManager.getInstance().fetchConversationsWithState(context, ConversationInfo.ConversationState.INCOMPLETE_SERVER);
	}
	
	@Override
	protected void onPostExecute(List<ConversationInfo> conversations) {
		//Calling the finish listener
		finishListener.accept(conversations);
	}
}
