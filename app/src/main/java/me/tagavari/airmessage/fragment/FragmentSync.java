package me.tagavari.airmessage.fragment;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.common.connection.ConnectionManager;
import me.tagavari.airmessage.common.connection.MassRetrievalParams;
import me.tagavari.airmessage.common.helper.MessagesDataHelper;
import me.tagavari.airmessage.common.data.SharedPreferencesManager;
import me.tagavari.airmessage.common.helper.ResourceHelper;
import me.tagavari.airmessage.common.helper.WindowHelper;
import me.tagavari.airmessage.common.redux.ReduxEmitterNetwork;
import me.tagavari.airmessage.common.redux.ReduxEventConnection;
import me.tagavari.airmessage.service.ConnectionService;

public class FragmentSync extends BottomSheetDialogFragment {
	private static final String TAG = BottomSheetDialogFragment.class.getSimpleName();
	
	private static final String savedInstanceKeyServerName = "serverName";
	private static final String savedInstanceKeyInstallationID = "installationID";
	private static final String savedInstanceKeyDeleteMessages = "deleteMessages";
	
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	private String serverName;
	private String serverInstallationID;
	private boolean deleteMessages;
	
	public FragmentSync() {
	}
	
	public FragmentSync(String serverName, String serverInstallationID, boolean deleteMessages) {
		this.serverName = serverName;
		this.serverInstallationID = serverInstallationID;
		this.deleteMessages = deleteMessages;
	}
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Restoring the parameters
		if(savedInstanceState != null) {
			serverName = savedInstanceState.getString(savedInstanceKeyServerName);
			serverInstallationID = savedInstanceState.getString(savedInstanceKeyInstallationID);
			deleteMessages = savedInstanceState.getBoolean(savedInstanceKeyDeleteMessages);
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
		
		//Updating the secondary action section
		Button buttonSecondary = view.findViewById(R.id.button_secondary);
		if(deleteMessages) {
			//Showing the description
			view.findViewById(R.id.label_delete).setVisibility(View.VISIBLE);
			
			//Setting the button to "delete messages"
			buttonSecondary.setText(R.string.action_deletemessages);
			buttonSecondary.setOnClickListener(this::deleteMessages);
		} else {
			//Hiding the description
			view.findViewById(R.id.label_delete).setVisibility(View.GONE);
			
			//Setting the button text to "skip"
			buttonSecondary.setText(R.string.action_skip);
			buttonSecondary.setOnClickListener(this::skip);
		}
		
		//Adding bottom padding to compensate for system bars
		ViewCompat.setOnApplyWindowInsetsListener(view, (applyView, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
			applyView.setPadding(applyView.getPaddingLeft(), applyView.getPaddingTop(), applyView.getPaddingRight(), insets.bottom);
			return WindowInsetsCompat.CONSUMED;
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
		
		//Subscribing to connection state updates
		compositeDisposable.add(ReduxEmitterNetwork.getConnectionStateSubject().subscribe(this::onConnectionStateUpdate));
		
		//Configuring the header images
		if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			requireActivity().getWindow().getDecorView().post(() -> {
				//Getting the window height
				float windowHeight = ResourceHelper.pxToDp(WindowHelper.getWindowHeight(requireActivity()));
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
		
		//Unsubscribing from updates
		compositeDisposable.clear();
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString(savedInstanceKeyServerName, serverName);
		outState.putString(savedInstanceKeyInstallationID, serverInstallationID);
		outState.putBoolean(savedInstanceKeyDeleteMessages, deleteMessages);
	}
	
	private void onConnectionStateUpdate(ReduxEventConnection event) {
		//Dismissing the dialog if the connection was lost
		if(event instanceof ReduxEventConnection.Disconnected) dismiss();
	}
	
	private void syncMessages(View view) {
		//Updating the saved installation ID
		updateInstallationID();
		
		//Deleting the messages
		MessagesDataHelper.deleteAMBMessages(getContext()).subscribeOn(Schedulers.single()).subscribe(() -> {
			//Requesting a re-sync
			ConnectionManager connectionManager = ConnectionService.getConnectionManager();
			if(connectionManager != null) {
				connectionManager.fetchMassConversationData(new MassRetrievalParams())
						.doOnError(error -> Log.i(TAG, "Failed to sync messages", error))
						.onErrorComplete().subscribe();
			}
		});
		
		//Closing the dialog
		dismiss();
	}
	
	private void deleteMessages(View view) {
		//Updating the saved installation ID
		updateInstallationID();
		
		//Deleting the messages
		MessagesDataHelper.deleteAMBMessages(getContext()).subscribeOn(Schedulers.single()).subscribe();
		
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
		if(serverInstallationID != null) {
			SharedPreferencesManager.setLastSyncInstallationID(getContext(), serverInstallationID);
		}
		
		//Clearing the state
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		if(connectionManager != null) connectionManager.clearPendingSync();
	}
}