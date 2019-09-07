package me.tagavari.airmessage.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.extension.MediaSharedElementCallback;
import me.tagavari.airmessage.messaging.ConversationAttachmentList;
import me.tagavari.airmessage.view.RoundedFrameLayout;

public class MediaViewer extends AppCompatActivity {
	//Creating the constants
	public static final String PARAM_INDEX = "index";
	public static final String PARAM_DATALIST = "data_list";
	public static final String PARAM_RADIIRAW = "radii_raw";
	public static final String PARAM_SELECTEDID = "selected_id";
	
	private static final int permissionRequestExportFile = 0;
	
	private static final int activityResultCreateFileSAF = 0;
	
	private ViewPager2 viewPager;
	private Toolbar toolbar;
	private View scrimTop;
	private View scrimBottom;
	
	private final List<PlayerView> playerViewList = new ArrayList<>();
	private Player currentPlayer = null;
	private boolean uiVisible = true;
	
	private boolean activityExiting = false;
	
	public static long selectedID = -1;
	private ConversationAttachmentList.Item selectedItem;
	
	private File targetExportFile = null;
	
	private Rect systemInsetsRect = new Rect();
	private final List<Consumer<Rect>> systemInsetsRectUpdateListeners = new ArrayList<>();
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Configuring the shared element transition
		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
		getWindow().setSharedElementEnterTransition(new ChangeBounds().setDuration(150));
		setEnterSharedElementCallback(new MediaSharedElementCallback());
		
		//Configuring the system UI
		getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		
		//Setting the layout
		setContentView(R.layout.activity_mediaviewer);
		
		//Getting the views
		RoundedFrameLayout frameRounder = findViewById(R.id.frame_round);
		
		viewPager = frameRounder.findViewById(R.id.viewpager);
		toolbar = findViewById(R.id.toolbar);
		scrimTop = findViewById(R.id.scrim_top);
		scrimBottom = findViewById(R.id.scrim_bottom);
		
		//Setting and configuring the app bar
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
		
		//Getting the activity parameters
		int selectionIndex = getIntent().getIntExtra(PARAM_INDEX, 0);
		List<ConversationAttachmentList.Item> itemList = getIntent().getParcelableArrayListExtra(PARAM_DATALIST);
		float[] radiiRaw = getIntent().getFloatArrayExtra(PARAM_RADIIRAW);
		
		//Configuring the shared element transition
		getWindow().getSharedElementEnterTransition().addListener(new Transition.TransitionListener() {
					@Override
					public void onTransitionStart(Transition transition) {
						ValueAnimator animator = activityExiting ? ValueAnimator.ofFloat(1, 0) : ValueAnimator.ofFloat(0, 1);
						animator.addUpdateListener(valueAnimator -> {
							float fraction = (float) valueAnimator.getAnimatedValue();
							float[] radii = new float[8];
							for(int i = 0; i < radii.length; i++) radii[i] = Constants.lerp(fraction, radiiRaw[i], 0);
							frameRounder.setRadiiRaw(radii);
						});
						animator.setDuration(250);
						animator.start();
					}
					
					@Override
					public void onTransitionEnd(Transition transition) {}
					
					@Override
					public void onTransitionCancel(Transition transition) {}
					
					@Override
					public void onTransitionPause(Transition transition) {}
					
					@Override
					public void onTransitionResume(Transition transition) {}
				});
		/* setEnterSharedElementCallback(new SharedElementCallback() {
			@Override
			public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
				sharedElements.put(names.get(0), viewPager);
			}
		}); */
		
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
			//Updating the toolbar
			toolbar.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), toolbar.getPaddingBottom());
			
			//Updating the scrims
			toolbar.post(() -> {
				scrimTop.getLayoutParams().height = insets.getSystemWindowInsetTop() + toolbar.getHeight();
			});
			scrimBottom.getLayoutParams().height = insets.getSystemWindowInsetBottom() * 2;
			
			//Updating the rectangle
			systemInsetsRect.left = insets.getSystemWindowInsetLeft();
			systemInsetsRect.top = insets.getSystemWindowInsetTop();
			systemInsetsRect.right = insets.getSystemWindowInsetRight();
			systemInsetsRect.bottom = insets.getSystemWindowInsetBottom();
			for(Consumer<Rect> listener : systemInsetsRectUpdateListeners) listener.accept(systemInsetsRect);
			
			return insets.consumeSystemWindowInsets();
		});
		
		//Initializing the view pager
		viewPager.setAdapter(new RecyclerAdapter(itemList));
		viewPager.setCurrentItem(selectionIndex, false);
		//viewPager.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		//new RVPagerSnapHelperListenable().attachToRecyclerView(recyclerView, new PagerListener());
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedItem = itemList.get(position);
				selectedID = selectedItem.localID;
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if(currentPlayer != null) currentPlayer.setPlayWhenReady(false);
			}
		});
		selectedItem = itemList.get(selectionIndex);
		selectedID = selectedItem.localID;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//Inflating the menu resource
		getMenuInflater().inflate(R.menu.menu_mediaviewer, menu);
		
		//Returning true
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if(currentPlayer != null) currentPlayer.setPlayWhenReady(false);
	}
	
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				super.onBackPressed();
				return true;
			case R.id.action_share:
				shareItem(selectedItem.file);
				return true;
			case R.id.action_save:
				saveItem(selectedItem.file);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == activityResultCreateFileSAF) {
			if(resultCode == RESULT_OK) Constants.exportUri(this, targetExportFile, data.getData());
		}
	}
	
	@Override
	public void onBackPressed() {
		activityExiting = true;
		super.onBackPressed();
	}
	
	@Override
	public void finishAfterTransition() {
		Intent data = new Intent();
		data.putExtra(PARAM_SELECTEDID, selectedID);
		setResult(RESULT_OK, data);
		
		super.finishAfterTransition();
	}
	
	/* @Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if(requestCode == permissionRequestExportFile) {
			if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Constants.exportFile(this, targetExportFile);
			}
		}
	} */
	
	private void toggleUI() {
		if(uiVisible) hideUI();
		else showUI();
	}
	
	private void hideUI() {
		if(!uiVisible) return;
		uiVisible = false;
		
		//Hiding the views
		ValueAnimator valueAnimator = ValueAnimator.ofFloat(1, 0);
		valueAnimator.addUpdateListener(animator -> {
			float value = (float) animator.getAnimatedValue();
			toolbar.setAlpha(value);
			scrimTop.setAlpha(value);
			scrimBottom.setAlpha(value);
		});
		valueAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				toolbar.setVisibility(View.GONE);
				scrimTop.setVisibility(View.GONE);
				scrimBottom.setVisibility(View.GONE);
			}
		});
		valueAnimator.start();
		
		//Enabling immersive mode
		getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		
		//Updating the video players
		for(PlayerView playerView : playerViewList) playerView.hideController();
	}
	
	private void showUI() {
		if(uiVisible) return;
		uiVisible = true;
		
		//Hiding the views
		ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
		valueAnimator.addUpdateListener(animator -> {
			float value = (float) animator.getAnimatedValue();
			toolbar.setAlpha(value);
			scrimTop.setAlpha(value);
			scrimBottom.setAlpha(value);
		});
		valueAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				toolbar.setVisibility(View.VISIBLE);
				scrimTop.setVisibility(View.VISIBLE);
				scrimBottom.setVisibility(View.VISIBLE);
			}
		});
		valueAnimator.start();
		
		//Disabling immersive mode
		getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION));
		
		//Updating the video players
		for(PlayerView playerView : playerViewList) playerView.showController();
	}
	
	private boolean shareItem(File file) {
		//Getting the file mime type
		String fileName = file.getName();
		int substringStart = file.getName().lastIndexOf(".") + 1;
		
		//Returning if the file cannot be substringed
		if(fileName.length() <= substringStart) return false;
		
		//Creating a new intent
		Intent intent = new Intent();
		
		//Setting the intent action
		intent.setAction(Intent.ACTION_SEND);
		
		//Creating a content URI
		Uri content = FileProvider.getUriForFile(this, MainApplication.fileAuthority, file);
		
		//Setting the intent file
		intent.putExtra(Intent.EXTRA_STREAM, content);
		
		//Getting the mime type
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substring(substringStart));
		
		//Setting the type
		intent.setType(mimeType);
		
		//Starting the activity
		startActivity(Intent.createChooser(intent, getResources().getText(R.string.action_sharemessage)));
		
		//Returning true
		return true;
	}
	
	private boolean saveItem(File file) {
		targetExportFile = file;
		Constants.createFileSAF(this, activityResultCreateFileSAF, Constants.getMimeType(file), file.getName());
		
		//Returning true
		return true;
	}
	
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int viewTypeImage = 0;
		private static final int viewTypeVideo = 1;
		
		private final List<ConversationAttachmentList.Item> itemList;
		
		RecyclerAdapter(List<ConversationAttachmentList.Item> itemList) {
			this.itemList = itemList;
			
			setHasStableIds(true);
		}
		
		@Override
		public int getItemCount() {
			return itemList.size();
		}
		
		@Override
		public long getItemId(int position) {
			return itemList.get(position).localID;
		}
		
		@Override
		public int getItemViewType(int position) {
			ConversationAttachmentList.Item item = itemList.get(position);
			if(Constants.compareMimeTypes(item.type, "image/*")) return viewTypeImage;
			else if(Constants.compareMimeTypes(item.type, "video/*")) return viewTypeVideo;
			else throw new IllegalArgumentException("Unsupported item type provided: " + item.type);
		}
		
		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			switch(viewType) {
				case viewTypeImage:
					return new ImageViewHolder(getLayoutInflater().inflate(R.layout.listitem_mediaviewer_image, parent, false));
				case viewTypeVideo:
					return new VideoViewHolder(getLayoutInflater().inflate(R.layout.listitem_mediaviewer_video, parent, false));
				default:
					throw new IllegalArgumentException("Unknown view type provided: " + viewType);
			}
		}
		
		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			//Getting the item
			ConversationAttachmentList.Item item = itemList.get(position);
			
			switch(getItemViewType(position)) {
				case viewTypeImage: {
					//Loading the image
					ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
					imageViewHolder.loadImage(item.file);
					break;
				}
				case viewTypeVideo: {
					VideoViewHolder videoViewHolder = (VideoViewHolder) holder;
					videoViewHolder.playVideo(item.file);
					break;
				}
			}
		}
		
		class ImageViewHolder extends RecyclerView.ViewHolder {
			final PhotoView imageView;
			
			ImageViewHolder(@NonNull View itemView) {
				super(itemView);
				imageView = (PhotoView) itemView;
				
				imageView.setOnPhotoTapListener((view, x, y) -> toggleUI());
			}
			
			void loadImage(File file) {
				//Loading the image file with Glide
				Glide.with(MediaViewer.this)
						.load(file)
						.transition(DrawableTransitionOptions.withCrossFade())
						.into(imageView);
				
				//Setting the image listener
				imageView.setOnClickListener(view -> {
				
				});
			}
		}
		
		class VideoViewHolder extends RecyclerView.ViewHolder {
			final SimpleExoPlayer player;
			
			VideoViewHolder(@NonNull View itemView) {
				super(itemView);
				
				//Creating the player instance
				player = ExoPlayerFactory.newSimpleInstance(MediaViewer.this);
				
				//Linking the player to the player view
				PlayerView playerView = itemView.findViewById(R.id.playerview);
				playerView.setPlayer(player);
				
				playerViewList.add(playerView);
				
				//Keeping the video player's UI in line with the activity's UI
				playerView.setControllerVisibilityListener(visibility -> {
					if(visibility == View.VISIBLE) showUI();
					else hideUI();
				});
				
				//Ensuring that the player control bar has appropriate margins, not to fall behind the system bars
				ViewGroup controlBar = playerView.findViewById(R.id.group_controlbar);
				Consumer<Rect> insetsUpdateListener = rect -> {
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) controlBar.getLayoutParams();
					layoutParams.leftMargin = systemInsetsRect.left;
					layoutParams.rightMargin = systemInsetsRect.right;
					layoutParams.bottomMargin = systemInsetsRect.bottom;
				};
				insetsUpdateListener.accept(systemInsetsRect); //Updating immediately
				systemInsetsRectUpdateListeners.add(insetsUpdateListener); //For future changes
				
				//Adding a listener to the player in order to keep track of which player is active
				player.addListener(new Player.EventListener() {
					@Override
					public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
						if(playWhenReady) { //When the "play" button is pressed
							if(currentPlayer != null && currentPlayer != player) currentPlayer.seekTo(0); //Resetting the previous player
							currentPlayer = player; //Setting the current player
						}
					}
				});
			}
			
			void playVideo(File file) {
				DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(MediaViewer.this, Util.getUserAgent(MediaViewer.this, getResources().getString(R.string.app_name)));
				MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(file));
				player.prepare(videoSource);
				
				//if(currentPlayer != null) currentPlayer.seekTo(0);
				/* {
					int index = playerList.indexOfValue(player);
					if(index != -1) playerList.removeAt(index);
					
					playerList.put(position, player);
				} */
			}
		}
	}
}