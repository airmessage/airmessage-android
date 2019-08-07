package me.tagavari.airmessage;

import androidx.appcompat.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BlockedAddresses extends AppCompatActivity {
	//Creating the view values
	private ListAdapter blockedListAdapter;
	
	//Creating the retained fragment values
	private static final String retainedFragmentTag = RetainedFragment.class.getName();
	private RetainedFragment retainedFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_blockedaddresses);
		
		//Preparing the retained fragment
		prepareRetainedFragment();
		
		//Setting up the views
		blockedListAdapter = new ListAdapter(this, R.layout.activity_blockedaddresses_listitem, retainedFragment.blockedList);
		((ListView) findViewById(R.id.list)).setAdapter(blockedListAdapter);
	}
	
	private void prepareRetainedFragment() {
		//Getting the retained fragment
		FragmentManager fragmentManager = getFragmentManager();
		retainedFragment = (RetainedFragment) fragmentManager.findFragmentByTag(retainedFragmentTag);
		
		//Checking if the fragment is invalid
		if(retainedFragment == null) { //If the fragment is valid, it has been retrieved from across a config change
			//Creating and adding the fragment
			retainedFragment = new RetainedFragment();
			fragmentManager.beginTransaction().add(retainedFragment, retainedFragmentTag).commit();
		}
	}
	
	void updateBlockedState(int oldState, int newState) {
		switch(oldState) {
			case -1:
				break;
			case RetainedFragment.blockedStateLoading:
				findViewById(R.id.loading_text).setVisibility(View.GONE);
				break;
			case RetainedFragment.blockedStateLoaded:
				findViewById(R.id.list).setVisibility(View.GONE);
				break;
			case RetainedFragment.blockedStateFailed:
				findViewById(R.id.label_error).setVisibility(View.GONE);
				break;
		}
		
		switch(newState) {
			case RetainedFragment.blockedStateLoading:
				findViewById(R.id.loading_text).setVisibility(View.VISIBLE);
				break;
			case RetainedFragment.blockedStateLoaded:
				findViewById(R.id.list).setVisibility(View.VISIBLE);
				break;
			case RetainedFragment.blockedStateFailed:
				findViewById(R.id.label_error).setVisibility(View.VISIBLE);
				break;
		}
	}
	
	void updateBlockedList() {
		blockedListAdapter.notifyDataSetChanged();
	}
	
	void removeBlockedAddress(String normalizedAddress) {
		//Removing the first matching instance
		Iterator<BlockedAddress> iterator = retainedFragment.blockedList.iterator();
		while(iterator.hasNext()) {
			if(iterator.next().normalizedAddress.equals(normalizedAddress)) {
				iterator.remove();
				break;
			}
		}
	}
	
	static class BlockedAddress {
		final String address;
		final String normalizedAddress;
		final int blockCount;
		
		BlockedAddress(String address, String normalizedAddress, int blockCount) {
			this.address = address;
			this.normalizedAddress = normalizedAddress;
			this.blockCount = blockCount;
		}
	}
	
	private class ListAdapter extends ArrayAdapter<BlockedAddress> {
		ListAdapter(Context context, int resource, ArrayList<BlockedAddress> items) {
			//Calling the super method
			super(context, resource, items);
			
			//Setting the data
			//filteredData = (ArrayList<ConversationManager.ConversationInfo>) items.clone();
		}
		
		@Override
		@NonNull
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			//Inflating the view if one wasn't provided
			if(convertView == null) convertView = getLayoutInflater().inflate(R.layout.activity_blockedaddresses_listitem, null);
			
			//Getting the item
			BlockedAddress blockedAddress = getItem(position);
			
			//Checking if the item is valid
			if(blockedAddress != null) {
				//Filling in the view text
				((TextView) convertView.findViewById(R.id.label_address)).setText(blockedAddress.address);
				((TextView) convertView.findViewById(R.id.label_count)).setText(getResources().getQuantityString(R.plurals.message_blockedmessagecount, blockedAddress.blockCount, blockedAddress.blockCount));
				
				//Setting the listeners
				convertView.findViewById(R.id.button_remove).setOnClickListener(view -> {
					//Showing a dialog
					new MaterialAlertDialogBuilder(BlockedAddresses.this)
							.setMessage(R.string.message_confirm_unblock)
							.setPositiveButton(R.string.action_unblock, (dialog, which) -> removeBlockedAddress(blockedAddress.normalizedAddress))
							.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
							.create().show();
				});
			}
			
			//Returning the view
			return convertView;
		}
	}
	
	public static class RetainedFragment extends Fragment {
		//Creating the task values
		private BlockedAddresses parentActivity;
		
		//Creating the state values
		private static final int blockedStateIdle = 0;
		private static final int blockedStateLoading = 1;
		private static final int blockedStateLoaded = 2;
		private static final int blockedStateFailed = 3;
		private int blockedState = blockedStateIdle;
		
		//Creating the other values
		private ArrayList<BlockedAddress> originalBlockedList = new ArrayList<>();
		private ArrayList<BlockedAddress> blockedList = new ArrayList<>();
		
		/**
		 * Hold a reference to the parent Activity so we can report the
		 * task's current progress and results. The Android framework
		 * will pass us a reference to the newly created Activity after
		 * each configuration change.
		 */
		@Override
		public void onAttach(Context context) {
			//Calling the super method
			super.onAttach(context);
			
			//Getting the parent activity
			parentActivity = (BlockedAddresses) context;
		}
		
		/**
		 * This method will only be called once when the retained
		 * Fragment is first created.
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			//Calling the super method
			super.onCreate(savedInstanceState);
			
			//Retain this fragment across configuration changes
			setRetainInstance(true);
		}
		
		/**
		 * Set the callback to null so we don't accidentally leak the
		 * Activity instance.
		 */
		@Override
		public void onDetach() {
			super.onDetach();
			parentActivity = null;
		}
		
		void updateState(int state) {
			//Returning if the requested state matches the existing state
			if(blockedState == state) return;
			
			//Updating the activity
			if(parentActivity != null) parentActivity.updateBlockedState(blockedState, state);
			
			//Setting the new state
			blockedState = state;
		}
		
		void loadBlocked(Context appContext) {
			//Returning if the state is not ready to load blocked users
			if(blockedState == blockedStateLoading || blockedState == blockedStateLoaded) return;
			
			//Starting the task
			new LoadBlockedAsyncTask(appContext, this).execute();
			
			//Setting the state
			updateState(blockedStateLoaded);
		}
		
		public static class LoadBlockedAsyncTask extends AsyncTask<Void, Void, ArrayList<BlockedAddress>> {
			//Creating the reference values
			private final WeakReference<Context> contextReference;
			private final WeakReference<RetainedFragment> fragmentReference;
			
			LoadBlockedAsyncTask(Context context, RetainedFragment fragment) {
				//Setting the references
				contextReference = new WeakReference<>(context);
				fragmentReference = new WeakReference<>(fragment);
			}
			
			@Override
			protected ArrayList<BlockedAddress> doInBackground(Void... parameters) {
				//Getting the context
				Context context = contextReference.get();
				if(context == null) return null;
				
				//Querying the database
				//return DatabaseManager.fetchBlockedAddresses(DatabaseManager.getReadableDatabase(context));
				return null;
			}
			
			@Override
			protected void onPostExecute(ArrayList<BlockedAddress> blocked) {
				//Getting the fragment
				RetainedFragment fragment = fragmentReference.get();
				if(fragment == null) return;
				
				//Checking if the result is invalid
				if(blocked == null) {
					//Updating to a failed state
					fragment.updateState(blockedStateFailed);
				} else {
					//Saving the data
					fragment.blockedList.addAll(blocked);
					fragment.originalBlockedList.addAll(fragment.blockedList);
					
					//Updating the list
					if(fragment.getActivity() != null) fragment.parentActivity.updateBlockedList();
					
					//Updating to a completed state
					fragment.updateState(blockedStateLoaded);
				}
			}
		}
	}
}
