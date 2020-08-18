package me.tagavari.airmessage.fragment;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.ConversationsBase;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.MassRetrievalParams;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.ConversationUtils;

public class FragmentSync extends BottomSheetDialogFragment {
	private static final String savedInstanceKeyServerName = "serverName";
	private static final String savedInstanceKeyInstallationID = "installationID";
	
	private final BroadcastReceiver clientConnectionResultBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Dismissing the dialog if the connection was lost
			int state = intent.getIntExtra(Constants.intentParamState, -1);
			if(state == ConnectionManager.stateDisconnected) {
				dismiss();
			}
		}
	};
	
	private String serverName;
	private String severInstallationID;
	
	public FragmentSync() {
	}
	
	public FragmentSync(String serverName, String severInstallationID) {
		this.serverName = serverName;
		this.severInstallationID = severInstallationID;
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Restoring the parameters
		if(savedInstanceState != null) {
			serverName = savedInstanceState.getString(savedInstanceKeyServerName);
			severInstallationID = savedInstanceState.getString(savedInstanceKeyInstallationID);
		}
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_serversync, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//Setting the sync text
		((TextView) view.findViewById(R.id.label_sync)).setText(getResources().getString(R.string.message_setup_sync_download_description, serverName));
		
		//Setting the sync button click listener
		view.findViewById(R.id.button_sync).setOnClickListener(this::syncMessages);
		
		//Updating the "delete messages" section
		Button buttonDelete = view.findViewById(R.id.button_delete);
		if(hasServerConversations()) {
			//Showing the description
			view.findViewById(R.id.label_delete).setVisibility(View.VISIBLE);
			
			//Setting the button to "delete messages"
			buttonDelete.setText(R.string.action_deletemessages);
			buttonDelete.setOnClickListener(this::deleteMessages);
		} else {
			//Hiding the description
			view.findViewById(R.id.label_delete).setVisibility(View.GONE);
			
			//Setting the button text to "skip"
			buttonDelete.setText(R.string.action_skip);
			buttonDelete.setOnClickListener(this::skip);
		}
		
		//Adding bottom padding to compensate for system bars
		view.setOnApplyWindowInsetsListener((applyView, windowInsets) -> {
			int paddingBottom;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				paddingBottom = windowInsets.getInsets(WindowInsets.Type.systemBars()).bottom;
			} else {
				paddingBottom = windowInsets.getSystemWindowInsetBottom();
			}
			
			applyView.setPadding(applyView.getPaddingLeft(), applyView.getPaddingTop(), applyView.getPaddingRight(), paddingBottom);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return WindowInsets.CONSUMED;
			else return windowInsets.consumeSystemWindowInsets();
		});
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog superDialog = super.onCreateDialog(savedInstanceState);
		
		superDialog.setOnShowListener(dialog -> {
			BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
			FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
			BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
			
			//Expanding and blocking drags
			behavior.setPeekHeight(0);
			behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
				@Override
				public void onStateChanged(@NonNull View bottomSheet, int newState) {
					if(newState == BottomSheetBehavior.STATE_DRAGGING) behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
				}
				
				@Override
				public void onSlide(@NonNull View bottomSheet, float slideOffset) {
				
				}
			});
		});
		
		return superDialog;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		//Adding the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(requireContext());
		localBroadcastManager.registerReceiver(clientConnectionResultBroadcastReceiver, new IntentFilter(ConnectionManager.localBCStateUpdate));
		
		//Configuring the header images
		if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			requireActivity().getWindow().getDecorView().post(() -> {
				//Getting the window height
				float windowHeight = Constants.pxToDp(Constants.getWindowHeight(requireActivity()));
				ImageView imageHeader = requireView().findViewById(R.id.image_header);
				if(windowHeight < 577) {
					//Short banner
					imageHeader.setImageResource(R.drawable.onboarding_download_short);
				} else if(windowHeight < 772) {
					//Medium banner
					imageHeader.setImageResource(R.drawable.onboarding_download_medium);
				} else {
					//Tall banner
					imageHeader.setImageResource(R.drawable.onboarding_download_tall);
				}
			});
		}
		
		if(getDialog() != null && getDialog().getWindow() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Window window = getDialog().getWindow();
			window.findViewById(com.google.android.material.R.id.container).setFitsSystemWindows(false);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		//Removing the broadcast listeners
		LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(requireContext());
		localBroadcastManager.unregisterReceiver(clientConnectionResultBroadcastReceiver);
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString(savedInstanceKeyServerName, serverName);
		outState.putString(savedInstanceKeyInstallationID, severInstallationID);
	}
	
	private boolean hasServerConversations() {
		//Getting the conversations
		List<ConversationInfo> conversations = ConversationUtils.getConversations();
		
		//Returning false if there are no conversations
		if(conversations == null || conversations.isEmpty()) return false;
		
		//Returning true if any conversation relies on AM bridge
		for(ConversationInfo conversation : conversations) {
			if(conversation.getServiceHandler() == ConversationInfo.serviceHandlerAMBridge) {
				return true;
			}
		}
		
		return false;
	}
	
	private void syncMessages(View view) {
		//Updating the saved installation ID
		updateInstallationID();
		
		//Syncing the messages
		new ConversationsBase.SyncMessagesTask(MainApplication.getInstance(), null, new MassRetrievalParams()).execute();
		
		//Closing the dialog
		dismiss();
	}
	
	private void deleteMessages(View view) {
		//Updating the saved installation ID
		updateInstallationID();
		
		//Deleting the messages
		new ConversationsBase.DeleteAMMessagesTask(MainApplication.getInstance()).execute();
		
		//Closing the dialog
		dismiss();
	}
	
	private void skip(View view) {
		//Updating the saved installation ID
		updateInstallationID();
		
		//Closing the dialog
		dismiss();
	}
	
	private void updateInstallationID() {
		//Writing the new installation ID
		if(severInstallationID != null) {
			MainApplication.getInstance().getConnectivitySharedPrefs().edit()
					.putString(MainApplication.sharedPreferencesConnectivityKeyLastSyncInstallationID, severInstallationID)
					.apply();
		}
		
		//Clearing the state
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager != null) connectionManager.clearServerSyncNeeded();
	}
}