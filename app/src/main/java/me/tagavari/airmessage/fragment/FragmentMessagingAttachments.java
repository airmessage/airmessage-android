package me.tagavari.airmessage.fragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Rect;
import android.location.Location;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.math.MathUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.property.RawProperty;
import ezvcard.property.Url;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.activity.LocationPicker;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.constants.FileNameConstants;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.helper.*;
import me.tagavari.airmessage.messaging.FileDisplayMetadata;
import me.tagavari.airmessage.messaging.FileLinked;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentLinked;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContent;
import me.tagavari.airmessage.messaging.viewholder.VHAttachmentTileContentMedia;
import me.tagavari.airmessage.task.FileQueueTask;
import me.tagavari.airmessage.util.DisposableViewHolder;
import me.tagavari.airmessage.util.Union;
import me.tagavari.airmessage.view.VisualizerView;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class FragmentMessagingAttachments extends FragmentCommunication<FragmentMessagingAttachments.FragmentCommunicationQueue> {
	private static final int attachmentsTileCount = 24;
	
	//Gallery views
	private ViewGroup viewGroupGallery;
	private View viewGalleryPermission;
	private View viewGalleryError;
	private RecyclerView recyclerViewGallery;
	private MaterialCardView cardViewGalleryPicker;
	
	//Location views
	private ViewGroup viewGroupLocation;
	private ViewGroup viewGroupLocationAction;
	private TextView labelLocationAction;
	private ViewGroup viewGroupLocationContent;
	
	//Audio views
	private ViewGroup viewGroupAudio;
	private View viewAudioPermission;
	private ViewGroup viewAudioContent;
	private MaterialCardView cardViewAudioPicker;
	
	private View viewRecordingGate;
	private ViewGroup viewGroupRecordingActive;
	private TextView labelRecordingTime;
	private VisualizerView visualizerRecording;
	
	//Adapters
	private AttachmentsGalleryRecyclerAdapter galleryAdapter;
	
	//Fragment values
	private FragmentViewModel viewModel;
	
	//Parameter values
	private boolean supportsAppleContent = false;
	private boolean lowResContent = false;
	private int primaryColor = 0;
	
	//Composite disposable
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();

	//Activity callbacks
	private final ActivityResultLauncher<String> requestStoragePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
		if(granted) {
			//Load gallery images
			viewModel.loadGallery();
		}
	});
	private final ActivityResultLauncher<String> requestAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
		if(granted) {
			//Update the recording section
			updateViewAudio(false);
		}
	});
	private final ActivityResultLauncher<String[]> requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
		//Check if all permissions are granted
		if(permissions.values().stream().allMatch((granted) -> granted)) {
			//Loading the location
			viewModel.loadLocation();
		}
	});

	private final ActivityResultCallback<Boolean> cameraResultCallback = captured -> {
		if(!captured || viewModel.targetFileIntent == null) return;

		//Queuing the file
		getCommunicationsCallback().queueFile(new FileLinked(Union.ofA(viewModel.targetFileIntent), viewModel.targetFileIntent.getName(), viewModel.targetFileIntent.length(), FileHelper.getMimeType(viewModel.targetFileIntent)));
	};
	private final ActivityResultLauncher<Uri> cameraPictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), cameraResultCallback);
	private final ActivityResultLauncher<Uri> cameraVideoLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo(), cameraResultCallback);
	private final ActivityResultLauncher<Uri> cameraVideoLowResLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo() {
		@NonNull
		@Override
		public Intent createIntent(@NonNull Context context, @NonNull Uri input) {
			return super.createIntent(context, input)
					.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
		}
	}, cameraResultCallback);
	private final ActivityResultLauncher<Intent> mediaSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
		if(result.getResultCode() != Activity.RESULT_OK) return;

		//Getting the content
		Intent intent = result.getData();
		if(intent.getData() != null) {
			//Queuing the content
			queueURIs(new Uri[]{intent.getData()});
		} else if(intent.getClipData() != null) {
			Uri[] list = new Uri[intent.getClipData().getItemCount()];
			for(int i = 0; i < intent.getClipData().getItemCount(); i++)
				list[i] = intent.getClipData().getItemAt(i).getUri();
			queueURIs(list);
		}
	});
	private final ActivityResultLauncher<Location> locationPickerLauncher = registerForActivityResult(new LocationPicker.ResultContract(), result -> {
		if(result == null) return;

		//Checking if this is an iMessage conversation
		if(supportsAppleContent) {
			//Writing the file and creating the attachment data
			queueLocation(result.getLocation(), result.getAddress(), result.getName());
		} else {
			//Creating the query string
			String query;
			if(result.getAddress() != null) {
				query = result.getAddress();
			} else {
				query = result.getLocation().latitude + "," + result.getLocation().longitude;
			}

			//Building the Google Maps URL
			Uri mapsUri = new Uri.Builder()
					.scheme("https")
					.authority("www.google.com")
					.appendPath("maps")
					.appendPath("search")
					.appendPath("")
					.appendQueryParameter("api", "1")
					.appendQueryParameter("query", query)
					.build();

			//Appending the generated URL to the text box
			getCommunicationsCallback().queueText(mapsUri.toString());
		}
	});

	private final ActivityResultLauncher<IntentSenderRequest> resolveLocationServicesLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
		//Updating the attachment section
		if(result.getResultCode() == Activity.RESULT_OK) {
			viewModel.loadLocation();
		}
	});
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Getting the view model
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_attachments, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//Getting the views
		viewGroupGallery = view.findViewById(R.id.viewgroup_attachment_gallery);
		viewGalleryPermission = viewGroupGallery.findViewById(R.id.button_attachment_gallery_permission);
		viewGalleryError = viewGroupGallery.findViewById(R.id.label_attachment_gallery_failed);
		recyclerViewGallery = viewGroupGallery.findViewById(R.id.list_attachment_gallery);
		cardViewGalleryPicker = viewGroupGallery.findViewById(R.id.button_attachment_gallery_systempicker);
		
		viewGroupLocation = view.findViewById(R.id.viewgroup_attachment_location);
		viewGroupLocationAction = viewGroupLocation.findViewById(R.id.button_attachment_location_action);
		labelLocationAction = viewGroupLocationAction.findViewById(R.id.button_attachment_location_action_label);
		viewGroupLocationContent = viewGroupLocation.findViewById(R.id.frame_attachment_location_content);
		
		viewGroupAudio = view.findViewById(R.id.viewgroup_attachment_audio);
		viewAudioPermission = viewGroupAudio.findViewById(R.id.button_attachment_audio_permission);
		viewAudioContent = viewGroupAudio.findViewById(R.id.frame_attachment_audio_content);
		cardViewAudioPicker = viewGroupAudio.findViewById(R.id.button_attachment_audio_systempicker);
		
		viewRecordingGate = viewAudioContent.findViewById(R.id.frame_attachment_audio_gate);
		viewGroupRecordingActive = viewAudioContent.findViewById(R.id.frame_attachment_audio_recording);
		labelRecordingTime = viewAudioContent.findViewById(R.id.label_attachment_audio_recording);
		visualizerRecording = viewAudioContent.findViewById(R.id.visualizer_attachment_audio_recording);
		
		//Coloring the views
		if(primaryColor != 0) ViewHelper.colorTaggedUI(getResources(), (ViewGroup) view, primaryColor);
		if(ThemeHelper.shouldUseAMOLED(requireContext())) view.setBackgroundColor(ColorConstants.colorAMOLED);
		
		//Setting up the gallery section
		setupViewGallery();
		viewModel.galleryStateLD.observe(getViewLifecycleOwner(), this::updateViewGallery);
		
		//Setting up the recording section
		setupViewAudio();
		updateViewAudio(requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED);
		viewModel.isRecording.observe(getViewLifecycleOwner(), value -> {
			//Returning if the value is recording (already handled, since the activity has to initiate it)
			if(value) return;
			
			//Concealing the recording view
			concealRecordingView();
			
			//Queuing the target file (if it is available)
			if(viewModel.targetFileRecording != null) {
				getCommunicationsCallback().queueFile(new FileLinked(Union.ofA(viewModel.targetFileRecording), viewModel.targetFileRecording.getName(), viewModel.targetFileRecording.length(), "audio/amr"));
				viewModel.targetFileRecording = null;
			}
		});
		viewModel.recordingDuration.observe(getViewLifecycleOwner(), value -> labelRecordingTime.setText(DateUtils.formatElapsedTime(value)));
		
		//Setting up the location section
		viewModel.locationStateLD.observe(getViewLifecycleOwner(), this::updateViewLocation);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		compositeDisposable.clear();
	}

	/**
	 * Sets whether to provide content that is only available in iMessage conversations
	 */
	public void setSupportsAppleContent(boolean supportsAppleContent) {
		this.supportsAppleContent = supportsAppleContent;
	}
	
	/**
	 * Sets whether to request low-resolution content (ex. for MMS)
	 */
	public void setLowResContent(boolean lowResContent) {
		this.lowResContent = lowResContent;
	}
	
	/**
	 * Sets the primary tint color for the view
	 */
	public void setPrimaryColor(int primaryColor) {
		this.primaryColor = primaryColor;
		
		//Updating the UI color
		View view = getView();
		if(view != null) ViewHelper.colorTaggedUI(getResources(), (ViewGroup) view, primaryColor);
	}
	
	/**
	 * Updates this fragment's view with the queue of a file
	 * @param mediaStoreID The media store ID of the file
	 */
	public void onFileQueued(long mediaStoreID) {
		//Ignoring if attachments are not loaded
		if(viewModel.galleryStateLD.getValue() != AttachmentsState.ready) return;
		
		//Finding the item in the list
		int matchedIndex = IntStream.range(0, viewModel.galleryFileList.size()).filter(i -> viewModel.galleryFileList.get(i).getMediaStoreData().getMediaStoreID() == mediaStoreID).findAny().orElse(-1);
		if(matchedIndex == -1) return;
		
		//Updating the item
		galleryAdapter.notifyItemChanged(galleryAdapter.mapAttachmentIndex(matchedIndex), AttachmentsGalleryRecyclerAdapter.payloadUpdateSelection);
	}
	
	/**
	 * Updates this fragment's view with the dequeue of a file
	 * @param mediaStoreID The media store ID of the file
	 */
	public void onFileDequeued(long mediaStoreID) {
		//Ignoring if attachments are not loaded
		if(viewModel.galleryStateLD.getValue() != AttachmentsState.ready) return;
		
		{
			//Finding the item in the list
			int matchedIndex = IntStream.range(0, viewModel.galleryFileList.size()).filter(i -> viewModel.galleryFileList.get(i).getMediaStoreData().getMediaStoreID() == mediaStoreID).findAny().orElse(-1);
			if(matchedIndex == -1) return;
			
			//Updating the item
			galleryAdapter.notifyItemChanged(galleryAdapter.mapAttachmentIndex(matchedIndex), AttachmentsGalleryRecyclerAdapter.payloadUpdateSelection);
		}
		
		//Updating selection indices
		Arrays.stream(getCommunicationsCallback().getMediaStoreIDs())
				//Map each queued file to its index in the gallery file list
				.mapToObj(id -> IntStream.range(0, viewModel.galleryFileList.size()).filter(i -> viewModel.galleryFileList.get(i).getMediaStoreData().getMediaStoreID() == id).findAny())
				//Filter out non-matches
				.filter(OptionalInt::isPresent)
				//Request an index update
				.forEach(matchedIndex -> galleryAdapter.notifyItemChanged(galleryAdapter.mapAttachmentIndex(matchedIndex.getAsInt()), AttachmentsGalleryRecyclerAdapter.payloadUpdateIndex));
	}
	
	/**
	 * Assigns listeners and adapters for the gallery section
	 */
	private void setupViewGallery() {
		//Setting the picker button click listener
		cardViewGalleryPicker.setOnClickListener(view -> requestGalleryFile());
		
		//Configuring the layout manager
		GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2, LinearLayoutManager.HORIZONTAL, false);
		layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				int itemCount = viewModel.galleryStateLD.getValue() == AttachmentsState.ready ? viewModel.galleryFileList.size() : attachmentsTileCount;
				return (position == 0 || position == itemCount + 1) ? 2 : 1;
			}
		});
		recyclerViewGallery.setLayoutManager(layoutManager);
		
		//Collapse the picker button on scroll
		recyclerViewGallery.addOnScrollListener(new PickerCardScrollListener(cardViewGalleryPicker, getResources().getDimensionPixelSize(R.dimen.contenttile_size_double)));
		
		//Add spacing between vertical entries
		recyclerViewGallery.addItemDecoration(new AttachmentsDoubleSpacingDecoration());
	}
	
	/**
	 * Updates the gallery section in response to a change in state
	 */
	private void updateViewGallery(@AttachmentsState int state) {
		//Checking if the state is failed
		if(state == AttachmentsState.failed) {
			//Showing the failed text
			viewGalleryError.setVisibility(View.VISIBLE);
			
			//Hiding the permission request button and the list
			viewGalleryPermission.setVisibility(View.GONE);
			recyclerViewGallery.setVisibility(View.GONE);
		}
		//Checking if the permission has not been granted
		else if(state == AttachmentsState.permission) {
			//Hiding the list and failed text
			recyclerViewGallery.setVisibility(View.GONE);
			viewGalleryError.setVisibility(View.GONE);
			
			//Setting up the permission request button
			viewGalleryPermission.setVisibility(View.VISIBLE);
			viewGalleryPermission.setOnClickListener(view -> requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE));
		} else {
			//Hiding the permission request button and the failed text
			viewGalleryPermission.setVisibility(View.GONE);
			viewGalleryError.setVisibility(View.GONE);
			
			//Setting up the list
			recyclerViewGallery.setVisibility(View.VISIBLE);
			
			//Checking if the files are loaded
			if(state == AttachmentsState.ready) {
				//Setting the list adapter
				recyclerViewGallery.setAdapter(galleryAdapter = new AttachmentsGalleryRecyclerAdapter(viewModel.galleryFileList));
			} else {
				//Setting the list adapter
				List<FileLinked> itemList = new ArrayList<>();
				for(int i = 0; i < attachmentsTileCount; i++) itemList.add(null);
				recyclerViewGallery.setAdapter(galleryAdapter = new AttachmentsGalleryRecyclerAdapter(itemList));
			}
		}
	}
	
	/**
	 * Assigns listeners to the audio section
	 */
	private void setupViewAudio() {
		//Setting the click listeners
		cardViewAudioPicker.setOnClickListener(view -> requestAudioFile());
		viewRecordingGate.setOnTouchListener((view, event) -> {
			//Performing the click
			view.performClick();
			
			//Checking if the input state is content and the action is a down touch
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				//Playing a sound
				SoundHelper.playSound(viewModel.soundPool, viewModel.soundIDRecordingStart);
				
				//Telling the view model to start recording
				boolean result = viewModel.startRecording();
				if(!result) return false;
				
				//Revealing the recording view
				revealRecordingView(event.getX(), event.getY());
				
				return true;
			} else if(event.getAction() == MotionEvent.ACTION_UP) {
				//Telling the view model to stop recording
				viewModel.stopRecording(true, false);
				
				return true;
			}
			
			//Returning false
			return false;
		});
		
		//Restoring the recording view
		if(viewModel.isRecording()) {
			restoreRecordingView();
		}
	}
	
	/**
	 * Updates the audio section based on its permission
	 */
	private void updateViewAudio(boolean needsPermission) {
		//Checking if the permission has not been granted
		if(needsPermission) {
			//Setting up the permission request button
			viewAudioPermission.setVisibility(View.VISIBLE);
			viewAudioPermission.setOnClickListener(view -> requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO));
			
			//Hiding the recording view
			viewAudioContent.setVisibility(View.GONE);
		} else {
			//Swapping to the content view
			viewAudioPermission.setVisibility(View.GONE);
			viewAudioContent.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Restores the recording view to make it up-to-date with the viewmodel's recording state
	 */
	private void restoreRecordingView() {
		viewGroupRecordingActive.setAlpha(1);
		viewGroupRecordingActive.setVisibility(View.VISIBLE);
		visualizerRecording.attachMediaRecorder(viewModel.mediaRecorder);
	}
	
	/**
	 * Reveals the recording view and starts the visualizer
	 * @param touchX The X position to start the reveal animation
	 * @param touchY The Y position to start the reveal animation
	 */
	private void revealRecordingView(float touchX, float touchY) {
		//Returning if the view is invalid
		if(viewGroupRecordingActive == null) return;
		
		//Calculating the radius
		float greaterX = Math.max(touchX, viewGroupRecordingActive.getWidth() - touchX);
		float greaterY = Math.max(touchY, viewGroupRecordingActive.getHeight() - touchY);
		float endRadius = (float) Math.hypot(greaterX, greaterY);
		
		//Revealing the recording view
		Animator animator = ViewAnimationUtils.createCircularReveal(viewGroupRecordingActive, (int) touchX, (int) touchY, 0, endRadius);
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				viewGroupRecordingActive.setAlpha(1);
				viewGroupRecordingActive.setVisibility(View.VISIBLE);
				visualizerRecording.clear();
				visualizerRecording.attachMediaRecorder(viewModel.mediaRecorder);
			}
		});
		animator.start();
		
		//Resetting the time label
		labelRecordingTime.setText(DateUtils.formatElapsedTime(0));
	}
	
	/**
	 * Conceals the recording view for when recording is complete
	 */
	private void concealRecordingView() {
		//Returning if the view is invalid
		if(viewGroupRecordingActive == null) return;
		
		//Fading the view
		viewGroupRecordingActive.animate().alpha(0).withEndAction(() -> {
			if(viewModel.isRecording()) return;
			viewGroupRecordingActive.setVisibility(View.GONE);
			visualizerRecording.detachMediaRecorder();
		});
	}
	
	/**
	 * Updates the location view in response to a change in state
	 */
	private void updateViewLocation(@LocationState int state) {
		//Checking if the state is OK
		if(state == LocationState.ready) {
			//Swapping to the content view
			viewGroupLocationAction.setVisibility(View.GONE);
			viewGroupLocationContent.setVisibility(View.VISIBLE);
			
			//Configuring the map
			SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_attachment_location_map);
			mapFragment.getMapAsync(googleMap -> {
				googleMap.setBuildingsEnabled(true);
				googleMap.getUiSettings().setMapToolbarEnabled(false);
				googleMap.getUiSettings().setAllGesturesEnabled(false);
				googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(viewModel.attachmentsLocationResult.getLatitude(), viewModel.attachmentsLocationResult.getLongitude()), 15));
				googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), ThemeHelper.isNightMode(getResources()) ? R.raw.map_plaindark : R.raw.map_plainlight));
			});
			
			viewGroupLocationContent.findViewById(R.id.frame_attachment_location_click).setOnClickListener(view -> locationPickerLauncher.launch(viewModel.attachmentsLocationResult));
		} else {
			//Showing the action view
			viewGroupLocationAction.setVisibility(View.VISIBLE);
			viewGroupLocationContent.setVisibility(View.GONE);
			
			String buttonText;
			View.OnClickListener buttonClickListener;
			switch(state) {
				case LocationState.loading:
					buttonText = getResources().getString(R.string.message_generalloading);
					buttonClickListener = null;
					break;
				case LocationState.permission:
					buttonText = getResources().getString(R.string.imperative_permission_location);
					buttonClickListener = view -> requestLocationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
					break;
				case LocationState.failed:
					buttonText = getResources().getString(R.string.message_loaderror_location);
					buttonClickListener = null;
					break;
				case LocationState.unavailable:
					buttonText = getResources().getString(R.string.message_notsupported);
					buttonClickListener = null;
					break;
				case LocationState.resolvable:
					buttonText = getResources().getString(R.string.imperative_enablelocationservices);
					buttonClickListener = view -> {
						resolveLocationServicesLauncher.launch(new IntentSenderRequest.Builder(viewModel.attachmentsLocationResolvable.getResolution().getIntentSender()).build());
					};
					break;
				default:
					throw new IllegalArgumentException("Invalid attachment location state " + state + " provided");
			}
			
			//Setting the details
			labelLocationAction.setText(buttonText);
			if(buttonClickListener != null) {
				viewGroupLocationAction.setOnClickListener(buttonClickListener);
				viewGroupLocationAction.setClickable(true);
			} else {
				viewGroupLocationAction.setClickable(false);
			}
		}
	}
	
	/**
	 * A scroll listener that manages the bubble state of a picker card
	 */
	private class PickerCardScrollListener extends RecyclerView.OnScrollListener {
		//Creating the parameter values
		private final CardView pickerView;
		private final int fullHeight;
		
		//Creating the dimension values
		private final float systemPickerBubbleStateRadiusTile = getResources().getDimensionPixelSize(R.dimen.contenttile_radius);
		private final float systemPickerBubbleStateElevationBubble = ResourceHelper.dpToPx(4);
		
		//Creating the state values
		boolean buttonIsBubble = false;
		
		PickerCardScrollListener(CardView pickerView, int fullHeight) {
			this.pickerView = pickerView;
			this.fullHeight = fullHeight;
		}
		
		@Override
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			//boolean isAtStart = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0;
			boolean isAtStart = !recyclerView.canScrollHorizontally(-1);
			if(buttonIsBubble == isAtStart) {
				buttonIsBubble = !isAtStart;
				setSystemPickerBubbleState(pickerView, buttonIsBubble, fullHeight);
			}
		}
		
		/**
		 * Animates a {@link CardView} to either a rectangle or floating bubble
		 * @param view The view to animate
		 * @param bubble Whether to make this view into a bubble
		 * @param tileSize The size of the card as a rectangle
		 */
		private void setSystemPickerBubbleState(CardView view, boolean bubble, int tileSize) {
			//Establishing the target values
			float sizeStart = view.getHeight();
			float radiusStart = view.getRadius();
			float elevationStart = view.getCardElevation();
			float sizeTarget, radiusTarget, elevationTarget;
			if(bubble) {
				sizeTarget = view.getWidth();
				radiusTarget = sizeTarget / 2F;
				elevationTarget = systemPickerBubbleStateElevationBubble;
			} else {
				sizeTarget = tileSize;
				radiusTarget = systemPickerBubbleStateRadiusTile;
				elevationTarget = 0;
			}
			
			//Setting the value animator
			ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
			valueAnimator.setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
			valueAnimator.addUpdateListener(animation -> {
				float value = (float) animation.getAnimatedValue();
				view.getLayoutParams().height = (int) MathUtils.lerp(sizeStart, sizeTarget, value);
				view.setRadius(MathUtils.lerp(radiusStart, radiusTarget, value));
				view.setCardElevation(MathUtils.lerp(elevationStart, elevationTarget, value));
				view.requestLayout();
			});
			valueAnimator.start();
		}
	}
	
	/**
	 * Launches an intent to capture a picture or a video with the camera
	 * @param video Whether to capture a video instead of a picture
	 */
	private void requestCamera(boolean video) {
		//Finding a free file
		File targetFile = AttachmentStorageHelper.prepareContentFile(requireContext(), AttachmentStorageHelper.dirNameDraftPrepare, video ? FileNameConstants.videoName : FileNameConstants.pictureName);
		viewModel.targetFileIntent = targetFile;
		Uri targetUri = FileProvider.getUriForFile(requireContext(), AttachmentStorageHelper.getFileAuthority(requireContext()), viewModel.targetFileIntent);

		try {
			if(video) {
				//Asking for low-quality video if the user is using MMS
				if(lowResContent) {
					cameraVideoLowResLauncher.launch(targetUri);
				} else {
					cameraVideoLauncher.launch(targetUri);
				}
			} else {
				cameraPictureLauncher.launch(targetUri);
			}
		} catch(ActivityNotFoundException exception) {
			exception.printStackTrace();

			//Telling the user via a toast
			Toast.makeText(requireContext(), R.string.message_intenterror_camera, Toast.LENGTH_SHORT).show();

			//Cleaning up
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, targetFile);
		}
	}
	
	/**
	 * Launches the file picker to select an image or video file
	 */
	private void requestGalleryFile() {
		launchPickerIntent(new String[]{"image/*", "video/*"});
	}
	
	/**
	 * Launches the file picker to select an audio file
	 */
	private void requestAudioFile() {
		launchPickerIntent(new String[]{"audio/*"});
	}
	
	/**
	 * Launches the picker intent to select files
	 * @param mimeTypes An array of MIME types to filter the results to
	 */
	private void launchPickerIntent(String[] mimeTypes) {
		Intent intent = new Intent();
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		intent.setAction(Intent.ACTION_GET_CONTENT);
		mediaSelectorLauncher.launch(Intent.createChooser(intent, getResources().getString(R.string.imperative_selectfile)));
	}
	
	/**
	 * Adds content URIs from arbitrary sources to the queue
	 */
	private void queueURIs(Uri[] uris) {
		compositeDisposable.add(Observable.fromArray(uris).observeOn(Schedulers.io())
				//Map each URI to a FileLinked
				.map(uri -> FileQueueTask.uriToFileLinkedSync(requireContext(), uri)).observeOn(AndroidSchedulers.mainThread()).subscribe(file -> getCommunicationsCallback().queueFile(file)));
	}
	
	/**
	 * Adds a location card to the queue
	 * @param targetLocation The selected location
	 * @param locationAddress The address of the selected location
	 * @param locationName The name of the selected location
	 */
	private void queueLocation(@NonNull LatLng targetLocation, @Nullable String locationAddress, @Nullable String locationName) {
		//Selecting a file to write to
		File file = AttachmentStorageHelper.prepareContentFile(requireContext(), AttachmentStorageHelper.dirNameDraftPrepare, locationName != null ? FileHelper.cleanFileName(locationName) + ".loc.vcf" : FileNameConstants.locationName);
		
		compositeDisposable.add(Single.create((SingleEmitter<FileLinked> emitter) -> {
			//Creating the vCard
			VCard vcard = new VCard();
			vcard.setProductId("-//" + Build.MANUFACTURER + "//" + "Android" + " " + Build.VERSION.RELEASE + "//" + Locale.getDefault().getLanguage().toUpperCase());
			vcard.setFormattedName(locationName);
			
			//Constructing the URL
			String stringLoc = targetLocation.latitude + "," + targetLocation.longitude;
			Uri.Builder uriBuilder = new Uri.Builder()
					.scheme("https")
					.authority("maps.apple.com")
					.appendQueryParameter("ll", stringLoc)
					.appendQueryParameter("q", locationAddress != null ? locationName : stringLoc);
			if(locationAddress != null) uriBuilder.appendQueryParameter("address", locationAddress);
			Url url = new Url(uriBuilder.build().toString());
			
			//Adding the URL
			url.setGroup("item1");
			vcard.addUrl(url);
			
			//Adding the type identifier
			RawProperty typeProperty = vcard.addExtendedProperty("X-ABLabel", "map url");
			typeProperty.setGroup("item1");
			
			//Writing the vCard
			try(VCardWriter writer = new VCardWriter(file, VCardVersion.V3_0)) {
				writer.write(vcard);
			} catch(IOException exception) {
				exception.printStackTrace();
				emitter.onError(exception);
			}
			
			//Returning the attachment data
			emitter.onSuccess(new FileLinked(Union.ofA(file), file.getName(), file.length(), MIMEConstants.mimeTypeVLocation));
		}).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread()).subscribe(
						linkedFile -> getCommunicationsCallback().queueFile(linkedFile),
						error -> AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, file)
				));
	}
	
	private abstract class AttachmentsRecyclerAdapter<VH extends VHAttachmentTileContent> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		//Creating the reference values
		private static final int itemTypeActionButton = 0;
		private static final int itemTypeContent = 1;
		private static final int itemTypeOverflowButton = 2;
		
		static final int payloadUpdateIndex = 0;
		static final int payloadUpdateSelection = 1;
		
		//Creating the list values
		private List<FileLinked> fileList;
		
		AttachmentsRecyclerAdapter(List<FileLinked> list) {
			fileList = list;
		}
		
		/**
		 * Gets whether this recycler view has an action button
		 */
		abstract boolean usesActionButton();
		
		/**
		 * Inflates this recycler view's action button
		 */
		abstract View inflateActionButton(@NonNull ViewGroup parent);
		
		/**
		 * Handles presses on this recycler view's overflow button
		 */
		abstract void onOverflowButtonClick();
		
		@Override
		public int getItemCount() {
			int customButtonCount = usesActionButton() ? 2 : 1;
			return fileList == null ? customButtonCount : fileList.size() + customButtonCount;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(usesActionButton() && position == 0) return itemTypeActionButton;
			else {
				if(position + 1 == getItemCount()) return itemTypeOverflowButton;
				else return itemTypeContent;
			}
		}
		
		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch(viewType) {
				case itemTypeActionButton: {
					return new ViewHolderImpl(inflateActionButton(parent));
				}
				case itemTypeOverflowButton: {
					View overflowButton = getLayoutInflater().inflate(R.layout.listitem_attachment_overflow, parent, false);
					overflowButton.setOnClickListener(view -> onOverflowButtonClick());
					return new ViewHolderImpl(overflowButton);
				}
				case itemTypeContent: {
					//Inflating the layout
					ViewGroup layout = (ViewGroup) getLayoutInflater().inflate(R.layout.listitem_attachment_linktile, parent, false);
					
					VH contentViewHolder = createContentViewHolder(layout.findViewById(R.id.container));
					return new VHAttachmentLinked(layout, layout.findViewById(R.id.viewstub_selection), contentViewHolder);
				}
				default:
					throw new IllegalArgumentException("Invalid view type received: " + viewType);
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
			//Filtering out non-content items
			if(getItemViewType(position) != itemTypeContent) return;
			
			//Getting the item
			FileLinked item = getItemAt(position);
			
			//Checking if the item is invalid
			if(item == null) {
				//Removing the click listener
				viewHolder.itemView.setOnClickListener(null);
				
				//Returning
				return;
			}
			
			//Binding the content view
			VHAttachmentLinked attachmentViewHolder = (VHAttachmentLinked) viewHolder;
			attachmentViewHolder.bind(requireContext(), item.getFile(), item.getFileName(), item.getFileType(), item.getFileSize(), item.getMediaStoreData().getModificationDate(), -1);
			if(item.getMetadata() != null) ((VHAttachmentTileContentMedia) attachmentViewHolder.getContent()).applyMetadata((FileDisplayMetadata.Media) item.getMetadata());
			
			//Setting the selection state
			int queueIndex = getCommunicationsCallback().getQueueIndex(item.getMediaStoreData().getMediaStoreID());
			if(queueIndex != -1) attachmentViewHolder.setSelected(getResources(), false, queueIndex + 1);
			else attachmentViewHolder.setDeselected(getResources(), false);
			
			//Assigning the click listener
			assignItemClickListener((VHAttachmentLinked) viewHolder, item, queueIndex);
		}
		
		abstract VH createContentViewHolder(@NonNull ViewGroup parent);
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
			//Filtering out non-content items
			if(getItemViewType(position) != itemTypeContent) return;
			
			//Ignoring the request if the payloads are empty
			if(payloads.isEmpty()) {
				super.onBindViewHolder(holder, position, payloads);
				return;
			}
			
			for(Object objectPayload : payloads) {
				int payload = (int) objectPayload;
				
				switch(payload) {
					case payloadUpdateIndex: {
						if(!(holder instanceof VHAttachmentLinked)) break;
						
						VHAttachmentLinked tileHolder = (VHAttachmentLinked) holder;
						
						//Getting the item information
						FileLinked item = getItemAt(position);
						int itemIndex;
						if(item != null && (itemIndex = getCommunicationsCallback().getQueueIndex(item.getMediaStoreData().getMediaStoreID())) != -1) {
							//Setting the index
							tileHolder.setSelectionIndex(getResources(), itemIndex + 1);
						}
						
						break;
					}
					case payloadUpdateSelection: {
						if(!(holder instanceof VHAttachmentLinked)) break;
						
						VHAttachmentLinked tileHolder = (VHAttachmentLinked) holder;
						
						//Getting the item information
						FileLinked item = getItemAt(position);
						
						//Setting the selection
						int queueIndex = getCommunicationsCallback().getQueueIndex(item.getMediaStoreData().getMediaStoreID());
						if(queueIndex != -1) tileHolder.setSelected(getResources(), true, queueIndex + 1);
						else tileHolder.setDeselected(getResources(), true);
						
						//Updating the click listener
						assignItemClickListener(tileHolder, item, queueIndex);
						
						break;
					}
				}
			}
		}
		
		@Override
		public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
			if(getItemViewType(holder.getAdapterPosition()) == itemTypeContent) {
				((DisposableViewHolder) holder).getCompositeDisposable().clear();
			}
		}
		
		/**
		 * Gets the {@link FileLinked} at the specified recycler index, taking into account action buttons
		 */
		private FileLinked getItemAt(int index) {
			if(usesActionButton()) index--;
			if(index < 0 || index >= fileList.size()) return null;
			return fileList.get(index);
		}
		
		/**
		 * Assigns the click listener for a tile view
		 * @param viewHolder The view's view holder
		 * @param item The file
		 * @param queueIndex The queue index of the view
		 */
		private void assignItemClickListener(VHAttachmentLinked viewHolder, FileLinked item, int queueIndex) {
			if(queueIndex == -1) viewHolder.itemView.setOnClickListener(view -> getCommunicationsCallback().queueFile(item));
			else viewHolder.itemView.setOnClickListener(view -> getCommunicationsCallback().dequeueFile(item));
		}
		
		private class ViewHolderImpl extends RecyclerView.ViewHolder {
			ViewHolderImpl(View itemView) {
				super(itemView);
			}
		}
		
		/**
		 * Maps a source array index to a recycler index
		 * @param index The source array index to map
		 * @return The index of the item's recycler view
		 */
		public int mapAttachmentIndex(int index) {
			if(usesActionButton()) return index + 1;
			else return index;
		}
	}
	
	private class AttachmentsGalleryRecyclerAdapter extends AttachmentsRecyclerAdapter<VHAttachmentTileContentMedia> {
		AttachmentsGalleryRecyclerAdapter(List<FileLinked> list) {
			super(list);
		}
		
		@Override
		boolean usesActionButton() {
			return true;
		}
		
		@Override
		View inflateActionButton(@NonNull ViewGroup parent) {
			View actionButton = getLayoutInflater().inflate(R.layout.listitem_attachment_actiontile_double, parent, false);
			{
				ViewGroup group = actionButton.findViewById(R.id.group_1);
				TextView label = group.findViewById(R.id.label_1);
				label.setText(R.string.action_short_picture);
				label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.camera_outlined, 0, 0);
				group.setOnClickListener(view -> requestCamera(false));
			}
			{
				ViewGroup group = actionButton.findViewById(R.id.group_2);
				TextView label = group.findViewById(R.id.label_2);
				label.setText(R.string.action_short_video);
				label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.videocam_outlined, 0, 0);
				group.setOnClickListener(view -> requestCamera(true));
			}
			return actionButton;
		}
		
		@Override
		void onOverflowButtonClick() {
			requestGalleryFile();
		}
		
		@Override
		VHAttachmentTileContentMedia createContentViewHolder(@NonNull ViewGroup parent) {
			View view = getLayoutInflater().inflate(R.layout.listitem_attachment_mediatile, parent, true);
			return new VHAttachmentTileContentMedia(
					view.findViewById(R.id.image),
					view.findViewById(R.id.image_flag_gif),
					view.findViewById(R.id.group_flag_video),
					view.findViewById(R.id.label_flag_video)
			);
		}
	}
	
	private class AttachmentsDoubleSpacingDecoration extends RecyclerView.ItemDecoration {
		@Override
		public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
			//Getting the item count
			int itemCount = viewModel.galleryStateLD.getValue() == AttachmentsState.ready ? viewModel.galleryFileList.size() : attachmentsTileCount;
			
			//Adding top margin for items on the bottom row
			int position = parent.getChildLayoutPosition(view);
			if(position != 0 && position != itemCount + 1 && parent.getChildLayoutPosition(view) % 2 == 0) {
				outRect.top = getResources().getDimensionPixelSize(R.dimen.contenttile_margin) / 2;
			}
		}
	}
	
	public static class FragmentViewModel extends AndroidViewModel {
		final MutableLiveData<Integer> galleryStateLD = new MutableLiveData<>();
		final List<FileLinked> galleryFileList = new ArrayList<>();
		
		private MediaRecorder mediaRecorder = null;
		final MutableLiveData<Boolean> isRecording = new MutableLiveData<>();
		final MutableLiveData<Integer> recordingDuration = new MutableLiveData<>();
		private final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
		private final Runnable recordingTimerHandlerRunnable = new Runnable() {
			@Override
			public void run() {
				//Adding a second
				recordingDuration.setValue(recordingDuration.getValue() + 1);
				
				//Scheduling the next run
				recordingTimerHandler.postDelayed(this, 1000);
			}
		};
		private final Handler mediaRecorderHandler = new Handler(Looper.getMainLooper());
		private final Runnable mediaRecorderRunnable = () -> {
			//Starting the recording timer
			startRecordingTimer();
			
			//Starting the media recorder
			mediaRecorder.start();
		};
		
		final SoundPool soundPool = SoundHelper.getSoundPool();
		final int soundIDRecordingStart = soundPool.load(getApplication(), R.raw.recording_start, 1);
		final int soundIDRecordingEnd = soundPool.load(getApplication(), R.raw.recording_end, 1);
		
		final MutableLiveData<Integer> locationStateLD = new MutableLiveData<>();
		private Location attachmentsLocationResult = null;
		private ResolvableApiException attachmentsLocationResolvable = null;
		
		//Creating the attachment values
		File targetFileIntent = null;
		File targetFileRecording = null;
		
		//Creating the task values
		private final CompositeDisposable compositeDisposable = new CompositeDisposable();
		
		public FragmentViewModel(@NonNull Application application) {
			super(application);
			
			//Initializing the sections
			loadGallery();
			loadLocation();
		}
		
		@Override
		protected void onCleared() {
			//Releasing the sound pool
			soundPool.release();
			
			//Cleaning up the media recorder
			if(isRecording()) stopRecording(true, true);
			if(mediaRecorder != null) mediaRecorder.release();
			
			//Unsubscribing from tasks
			compositeDisposable.clear();
		}
		
		/**
		 * Updates the gallery state and attempts to load files from the gallery
		 */
		public void loadGallery() {
			//Checking if we don't have permission
			if(getApplication().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				//Setting the state to permission requested
				galleryStateLD.setValue(AttachmentsState.permission);
				
				return;
			}
			
			//Setting the state to loading
			galleryStateLD.setValue(AttachmentsState.loading);
			
			//Loading the files
			compositeDisposable.add(indexAttachmentsFromMediaStore(MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO).toList().subscribe((files) -> {
				//Adding the files and updating the state
				galleryFileList.addAll(files);
				galleryStateLD.setValue(AttachmentsState.ready);
			}, (error) -> {
				//Updating the state
				galleryStateLD.setValue(AttachmentsState.failed);
			}));
		}
		
		/**
		 * Indexes files from Android's media library, returning them as {@link FileLinked} in an observable
		 * @param msQuerySelection Passed to the SQL selection parameter
		 * @return An observable to handle the response
		 */
		@CheckReturnValue
		private Observable<FileLinked> indexAttachmentsFromMediaStore(String msQuerySelection) {
			return Observable.create((ObservableEmitter<FileLinked> emitter) -> {
				try {
					//Querying the media files
					Uri queryUri = MediaStore.Files.getContentUri("external");
					String[] queryProjection = new String[]{MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MIME_TYPE, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATE_MODIFIED};
					Cursor cursor;
					if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						Bundle queryArgs = new Bundle();
						queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, attachmentsTileCount);
						queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, msQuerySelection);
						queryArgs.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Files.FileColumns.DATE_ADDED});
						queryArgs.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
						queryArgs.putStringArray(ContentResolver.EXTRA_HONORED_ARGS, new String[]{ContentResolver.QUERY_ARG_SORT_COLUMNS, ContentResolver.QUERY_ARG_SORT_DIRECTION});
						
						cursor = getApplication().getContentResolver().query(queryUri, queryProjection, queryArgs, null);
					} else {
						cursor = getApplication().getContentResolver().query(queryUri, queryProjection, msQuerySelection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC" + ' ' + "LIMIT " + attachmentsTileCount);
					}
					
					if(cursor == null) {
						emitter.onError(new NullPointerException("Content resolver returned null cursor"));
						return;
					}
					
					try {
						//int indexData = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
						int iLocalID = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
						int iType = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE);
						int iDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
						int iSize = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
						int iModificationDate = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
						
						while(cursor.moveToNext()) {
							
							//Getting the file information
							long mediaStoreID = cursor.getLong(iLocalID);
							Uri uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), mediaStoreID);
							String fileType = cursor.getString(iType);
							if(fileType == null) fileType = "application/octet-stream";
							String fileName;
							{
								String originalFileName = cursor.getString(iDisplayName);
								fileName = originalFileName == null ? "file" : FileHelper.cleanFileName(originalFileName);
							}
							long fileSize = cursor.getLong(iSize);
							long modificationDate = cursor.getLong(iModificationDate);
							
							//Creating the union file
							Union<File, Uri> unionFile = Union.ofB(uri);
							
							//Loading the file metadata
							FileDisplayMetadata metadata;
							if(FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeAudio) || FileHelper.compareMimeTypes(fileType, MIMEConstants.mimeTypeVideo)) {
								metadata = new FileDisplayMetadata.Media(getApplication(), unionFile);
							} else {
								metadata = null;
							}
							
							//Submitting the file
							emitter.onNext(new FileLinked(unionFile, fileName, fileSize, fileType, metadata, new FileLinked.MediaStore(mediaStoreID, modificationDate)));
						}
					} finally {
						cursor.close();
					}
					
					//Returning the list
					emitter.onComplete();
				} catch(SQLiteException exception) {
					//Logging the exception
					exception.printStackTrace();
					FirebaseCrashlytics.getInstance().recordException(exception);
					
					//Failing
					emitter.onError(exception);
				}
			}).subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread());
		}
		
		/**
		 * Starts the current recording session
		 */
		boolean startRecording() {
			//Setting up the media recorder
			setupMediaRecorder();
			
			//Finding a target file
			targetFileRecording = AttachmentStorageHelper.prepareContentFile(getApplication(), AttachmentStorageHelper.dirNameDraftPrepare, FileNameConstants.recordingName);
			
			//Setting the media recorder file
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) mediaRecorder.setOutputFile(targetFileRecording);
			else mediaRecorder.setOutputFile(targetFileRecording.getAbsolutePath());
			
			try {
				//Preparing the media recorder
				mediaRecorder.prepare();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning
				return false;
			}
			
			//Updating the state
			isRecording.setValue(true);
			
			//Queueing a delay for the audio recorder
			mediaRecorderHandler.postDelayed(mediaRecorderRunnable, 70);
			
			//Returning true
			return true;
		}
		
		/**
		 * Stops the current recording session
		 *
		 * @param cleanup whether or not to clean up the media recorder (usually wanted, unless the media recorder encountered an error)
		 * @param discard whether or not to discard the recorded file
		 * @return the file's availability (to be able to use or send)
		 */
		private boolean stopRecording(boolean cleanup, boolean discard) {
			//Returning if the input state is not recording
			if(!isRecording()) return true;
			
			//Removing the timer callback
			mediaRecorderHandler.removeCallbacks(mediaRecorderRunnable);
			
			try {
				//Stopping the timer
				stopRecordingTimer();
				
				if(cleanup) {
					try {
						//Stopping the media recorder
						mediaRecorder.stop();
					} catch(RuntimeException stopException) { //The media recorder couldn't capture any media
						//Showing a toast
						Toast.makeText(MainApplication.getInstance(), R.string.imperative_recording_instructions, Toast.LENGTH_LONG).show();
						
						//Invalidating the recording file reference
						targetFileRecording = null;
						
						//Returning false
						return false;
					}
				}
				
				//Checking if the recording was under a second
				if(recordingDuration.getValue() < 1) {
					//Showing a toast
					Toast.makeText(MainApplication.getInstance(), R.string.imperative_recording_instructions, Toast.LENGTH_LONG).show();
					
					//Discarding the file
					discard = true;
				}
				
				//Checking if the recording should be discarded
				if(discard) {
					//Deleting the file and invalidating its reference
					AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameDraftPrepare, targetFileRecording);
					targetFileRecording = null;
					
					//Returning false
					return false;
				}
				
				//Playing a sound
				SoundHelper.playSound(soundPool, soundIDRecordingEnd);
				
				//Returning true
				return true;
			} finally {
				//Updating the state
				isRecording.setValue(false);
			}
		}
		
		/**
		 * Initializes or reconfigures the media recorder for recording
		 */
		private void setupMediaRecorder() {
			//Setting the media recorder
			if(mediaRecorder == null) mediaRecorder = new MediaRecorder();
			else mediaRecorder.reset();
			
			//Configuring the media recorder
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mediaRecorder.setMaxDuration(10 * 60 * 1000); //10 minutes
			
			mediaRecorder.setOnInfoListener((recorder, what, extra) -> {
				if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					//Stopping recording
					stopRecording(false, false);
				}
			});
			mediaRecorder.setOnErrorListener((recorder, what, extra) -> {
				stopRecording(false, true);
				mediaRecorder.release();
				mediaRecorder = null;
			});
		}
		
		/**
		 * Starts the 1-second repeating recording timer
		 */
		private void startRecordingTimer() {
			recordingDuration.setValue(0);
			recordingTimerHandler.postDelayed(recordingTimerHandlerRunnable, 1000);
		}
		
		/**
		 * Stops the recording timer
		 */
		private void stopRecordingTimer() {
			recordingTimerHandler.removeCallbacks(recordingTimerHandlerRunnable);
		}
		
		/**
		 * Gets if a recording is currently in progress
		 */
		boolean isRecording() {
			return Boolean.TRUE.equals(isRecording.getValue());
		}
		
		/**
		 * Updates the location state and attempts to initialize location services
		 */
		void loadLocation() {
			//Checking if we don't have permission
			if(getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				//Setting the state to permission requested
				locationStateLD.setValue(LocationState.permission);
				
				return;
			}
			
			//Setting the state to loading
			locationStateLD.setValue(LocationState.loading);
			
			FusedLocationProviderClient locationProvider = LocationServices.getFusedLocationProviderClient(getApplication());
			LocationRequest locationRequest = LocationRequest.create();
			//Requesting location services status
			LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
			Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(getApplication()).checkLocationSettings(builder.build());
			task.addOnCompleteListener(taskResult -> {
				//Restoring the loading state
				try {
					//Getting the result
					taskResult.getResult(ApiException.class); //Forces exception to be thrown if needed
					
					//Getting user's location
					locationProvider.getLastLocation().addOnSuccessListener(location -> {
						if(location == null) {
							//Pulling an update from location services
							locationProvider.requestLocationUpdates(locationRequest, new LocationCallback() {
								@Override
								public void onLocationResult(LocationResult locationResult) {
									//Ignoring if there is no result (and waiting for another update)
									if(locationResult == null) return;
									
									//Removing the updater
									locationProvider.removeLocationUpdates(this);
									
									//Setting the location
									attachmentsLocationResult = locationResult.getLastLocation();
									locationStateLD.setValue(LocationState.ready);
								}
							}, null);
						} else {
							//Setting the location
							attachmentsLocationResult = location;
							locationStateLD.setValue(LocationState.ready);
						}
					});
				} catch(ApiException exception) {
					switch(exception.getStatusCode()) {
						case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
							//Setting the resolvable
							attachmentsLocationResolvable = (ResolvableApiException) exception;
							locationStateLD.setValue(LocationState.resolvable);
							break;
						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
							locationStateLD.setValue(LocationState.unavailable);
							break;
						default:
							locationStateLD.setValue(LocationState.failed);
							break;
					}
				}
			});
		}
	}
	
	public static abstract class FragmentCommunicationQueue {
		/**
		 * Gets an array of the media store IDs of queued files (ignoring ones that don't have media store metadata)
		 */
		public abstract long[] getMediaStoreIDs();
		
		/**
		 * Gets the index of a queued linked file from its media store ID, or -1 if the file is not queued
		 */
		public abstract int getQueueIndex(long mediaStoreID);
		
		/**
		 * Adds a file to the queue
		 */
		public abstract void queueFile(FileLinked file);
		
		/**
		 * Removes a file from the queue
		 */
		public abstract void dequeueFile(FileLinked file);
		
		/**
		 * Adds text content to the input field
		 */
		public abstract void queueText(String text);
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({AttachmentsState.loading, AttachmentsState.permission, AttachmentsState.failed, AttachmentsState.ready})
	private @interface AttachmentsState {
		int loading = 0; //Loading in progress
		int permission = 1; //Permission required
		int failed = 2; //Load error
		int ready = 3; //OK
	}
	
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({LocationState.loading, LocationState.permission, LocationState.failed, LocationState.unavailable, LocationState.resolvable, LocationState.ready})
	private @interface LocationState {
		int loading = 0; //Loading in progress
		int permission = 1; //Permission required
		int failed = 2; //Load error
		int unavailable = 3; //Service unavailable
		int resolvable = 4; //User action required
		int ready = 5; //OK
	}
}