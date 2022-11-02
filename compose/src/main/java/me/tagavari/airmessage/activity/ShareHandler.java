package me.tagavari.airmessage.activity;

import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.compose.R;
import me.tagavari.airmessage.compose.ConversationsCompose;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.data.SharedPreferencesManager;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.ShortcutHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.viewbinder.VBConversation;
import me.tagavari.airmessage.messaging.viewholder.VHConversationBase;
import me.tagavari.airmessage.util.DisposableViewHolder;

public class ShareHandler extends AppCompatCompositeActivity {
	//Creating the view model value
	private ActivityViewModel viewModel;
	
	//Creating the view values
	private RecyclerView listConversations;
	private View viewLoading;
	private View viewBlank;
	private View viewError;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Getting the view model
		viewModel = new ViewModelProvider(this).get(ActivityViewModel.class);
		
		//Checking if the request came from direct share
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && getIntent().hasExtra(Intent.EXTRA_SHORTCUT_ID)) {
			long conversationID = ShortcutHelper.INSTANCE.shortcutIDToConversationID(getIntent().getStringExtra(Intent.EXTRA_SHORTCUT_ID));
			if(conversationID != -1) {
				//Launching the activity
				launchMessaging(conversationID);
			}
			
			//Finishing the activity
			finish();
			return;
		}
		
		//Setting the window flags
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		
		//Setting the content
		setContentView(R.layout.activity_share);
		
		//Configuring the window
		DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
		getWindow().setLayout(Math.min(displayMetrics.widthPixels, getResources().getDimensionPixelSize(R.dimen.dialogwidth_max)), ViewGroup.LayoutParams.MATCH_PARENT);
		
		//Getting the views
		listConversations = findViewById(R.id.list);
		viewLoading = findViewById(R.id.loading_text);
		viewBlank = findViewById(R.id.no_conversations);
		viewError = findViewById(R.id.errorview);
		viewError.findViewById(R.id.button_retry).setOnClickListener(view -> viewModel.loadConversations());
		
		//Setting the listeners
		viewModel.stateLD.observe(this, this::updateState);
		
		//Preventing the activity from finishing if the user touches outside of its bounds
		setFinishOnTouchOutside(false);
		
		//Checking if the server is not configured
		if(!SharedPreferencesManager.isConnectionConfigured(this)) {
			Button button = findViewById(R.id.button_new);
			button.setEnabled(false);
			button.setAlpha(ResourceHelper.resolveFloatAttr(this, android.R.attr.disabledAlpha));
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(listConversations != null) listConversations.setAdapter(null);
	}
	
	/**
	 * Updates the activity UI in response to a state change
	 */
	public void updateState(@ActivityState int state) {
		switch(state) {
			case ActivityState.loading:
				listConversations.setVisibility(View.GONE);
				viewLoading.setVisibility(View.VISIBLE);
				viewBlank.setVisibility(View.GONE);
				viewError.setVisibility(View.GONE);
				break;
			case ActivityState.ready:
				viewLoading.setVisibility(View.GONE);
				viewError.setVisibility(View.GONE);
				
				if(viewModel.conversationList.isEmpty()) {
					listConversations.setVisibility(View.GONE);
					viewBlank.setVisibility(View.VISIBLE);
				} else {
					listConversations.setVisibility(View.VISIBLE);
					viewBlank.setVisibility(View.GONE);
					
					listConversations.setAdapter(new RecyclerAdapter(viewModel.conversationList));
				}
				break;
			case ActivityState.error:
				listConversations.setVisibility(View.GONE);
				viewLoading.setVisibility(View.GONE);
				viewBlank.setVisibility(View.GONE);
				viewError.setVisibility(View.VISIBLE);
				break;
		}
	}
	
	/**
	 * Finishes the activity
	 */
	public void closeDialog(View view) {
		finish();
	}
	
	/**
	 * Launches the 'create new conversation' activity with the share data
	 */
	public void createNewConversation(View view) {
		//Copy the intent
		Intent intent = new Intent(getIntent());
		intent.setClass(ShareHandler.this, ConversationsCompose.class);
		
		//Launch a single activity, and copy URI permissions
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		//Set the target to create a new conversation
		intent.putExtra(ConversationsCompose.INTENT_TARGET_NEW, true);
		
		startActivity(intent);
	}
	
	/**
	 * Launches the messaging activity with the share data
	 * @param conversationID The ID of the conversation to launch
	 */
	private void launchMessaging(long conversationID) {
		//Copy the intent
		Intent intent = new Intent(getIntent());
		intent.setClass(ShareHandler.this, ConversationsCompose.class);
		
		//Launch a single activity, and copy URI permissions
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		//Set the target conversation
		intent.putExtra(ConversationsCompose.INTENT_TARGET_ID, conversationID);
		
		startActivity(intent);
	}
	
	private class RecyclerAdapter extends RecyclerView.Adapter<VHConversationBase> {
		//Creating the list values
		private final List<ConversationInfo> conversationList;
		
		RecyclerAdapter(List<ConversationInfo> items) {
			//Setting the items
			conversationList = items;
			
			//Enabling stable IDs
			setHasStableIds(true);
		}
		
		@NonNull
		@Override
		public VHConversationBase onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(ShareHandler.this).inflate(R.layout.listitem_conversation_simple, parent, false);
			return new VHConversationBase(view, view.findViewById(R.id.conversationicon), view.findViewById(R.id.title));
		}
		
		@Override
		public void onBindViewHolder(@NonNull VHConversationBase holder, int position) {
			//Getting the conversation info
			ConversationInfo conversationInfo = conversationList.get(position);
			
			//Binding the view details
			holder.getCompositeDisposable().addAll(
					VBConversation.bindTitle(ShareHandler.this, holder.getConversationTitle(), conversationInfo).subscribe(),
					VBConversation.bindUsers(ShareHandler.this, holder.getIconGroup(), conversationInfo).subscribe()
			);
			
			//Setting the view's click listeners
			holder.itemView.setOnClickListener(view -> launchMessaging(conversationInfo.getLocalID()));
		}
		
		@Override
		public void onViewRecycled(@NonNull VHConversationBase holder) {
			holder.getCompositeDisposable().clear();
		}
		
		@Override
		public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
			//Cancelling all view holder tasks
			LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
			int firstVisibleIndex = layoutManager.findFirstVisibleItemPosition();
			int lastVisibleIndex = layoutManager.findLastVisibleItemPosition();
			
			for(int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
				DisposableViewHolder holder = (DisposableViewHolder) recyclerView.findViewHolderForLayoutPosition(i);
				if(holder != null) holder.getCompositeDisposable().clear();
			}
		}
		
		@Override
		public int getItemCount() {
			return conversationList.size();
		}
		
		@Override
		public long getItemId(int position) {
			return conversationList.get(position).getLocalID();
		}
	}
	
	public static class ActivityViewModel extends AndroidViewModel {
		private final CompositeDisposable compositeDisposable = new CompositeDisposable();
		
		public List<ConversationInfo> conversationList;
		public MutableLiveData<Integer> stateLD = new MutableLiveData<>(-1);
		
		public ActivityViewModel(@NonNull Application application) {
			super(application);
			
			//Loading conversations
			loadConversations();
		}
		
		@Override
		protected void onCleared() {
			//Clearing task subscriptions
			compositeDisposable.clear();
		}
		
		/**
		 * Loads conversations from the database
		 */
		private void loadConversations() {
			//Updating the state
			stateLD.setValue(ActivityState.loading);
			
			//Loading the conversations
			compositeDisposable.add(
					//Loading the conversations
					Single.fromCallable(() -> DatabaseManager.getInstance().fetchSummaryConversations(getApplication(), false))
							.subscribeOn(Schedulers.single())
							.observeOn(AndroidSchedulers.mainThread())
							.subscribe((conversations) -> {
								//Setting the conversation list
								conversationList = conversations;
								
								//Updating the state
								stateLD.setValue(ActivityState.ready);
							}, (error) -> {
								stateLD.setValue(ActivityState.error);
							})
			);
		}
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ActivityState.loading, ActivityState.error, ActivityState.ready})
	private @interface ActivityState {
		int loading = 0; //Loading in progress
		int error = 1; //An error occurred
		int ready = 2; //OK
	}
}