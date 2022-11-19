package me.tagavari.airmessage.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.common.constants.MIMEConstants;
import me.tagavari.airmessage.contract.ContractCreateDynamicDocument;
import me.tagavari.airmessage.common.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.common.helper.ExternalStorageHelper;
import me.tagavari.airmessage.common.helper.FileHelper;
import me.tagavari.airmessage.common.messaging.AttachmentInfo;

public class MediaViewer extends AppCompatActivity {
	//Creating the constants
	public static final String intentParamIndex = "index";
	public static final String intentParamDataList = "dataList";
	
	private static final String INSTANCEPARAM_RESTORE = "restore";
	
	private RecyclerAdapter recyclerAdapter;
	private Toolbar toolbar;
	private View scrimTop;
	private View scrimBottom;
	
	//Creating the state values
	private boolean uiVisible = true;
	private ValueAnimator activeUIAnimator = null;
	
	private AttachmentInfo selectedItem;
	
	private boolean autoPlay = false;
	
	private File targetExportFile = null;
	
	//Creating the callbacks
	private final ActivityResultLauncher<ContractCreateDynamicDocument.Params> saveFileLauncher = registerForActivityResult(new ContractCreateDynamicDocument(), uri -> {
		if(uri == null) return;
		ExternalStorageHelper.exportFile(this, targetExportFile, uri);
	});
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content))
				.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH);
		
		//Setting the layout
		setContentView(R.layout.activity_mediaviewer);
		
		//Creating the view values
		ViewPager2 viewPager = findViewById(R.id.viewpager);
		AppBarLayout appBar = findViewById(R.id.appbar);
		toolbar = findViewById(R.id.toolbar);
		scrimTop = findViewById(R.id.scrim_top);
		scrimBottom = findViewById(R.id.scrim_bottom);
		
		//Setting and configuring the app bar
		setSupportActionBar(toolbar);
		ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeAsUpIndicator(R.drawable.arrow_back);
		
		//Restoring the activity data
		if(savedInstanceState == null) {
			autoPlay = true;
		} else {
			autoPlay = savedInstanceState.getBoolean(INSTANCEPARAM_RESTORE, true);
		}
		
		//Getting the activity parameters
		int selectionIndex = getIntent().getIntExtra(intentParamIndex, 0);
		List<AttachmentInfo> itemList = getIntent().getParcelableArrayListExtra(intentParamDataList);
		
		ViewCompat.setOnApplyWindowInsetsListener(appBar, (view, windowInsets) -> {
			Insets insets = windowInsets.getInsetsIgnoringVisibility(
					WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
			);
			
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
			layoutParams.leftMargin = insets.left;
			layoutParams.rightMargin = insets.right;
			layoutParams.topMargin = insets.top;
			
			return WindowInsetsCompat.CONSUMED;
		});
		
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
			Insets insets = windowInsets.getInsetsIgnoringVisibility(
					WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
			);
			
			//Updating the scrims
			toolbar.post(() -> {
				scrimTop.getLayoutParams().height = insets.top + toolbar.getHeight();
				scrimBottom.getLayoutParams().height = insets.bottom * 2;
			});
			
			return WindowInsetsCompat.CONSUMED;
		});
		
		//Initializing the view pager
		viewPager.setAdapter(recyclerAdapter = new RecyclerAdapter(itemList));
		viewPager.setCurrentItem(selectionIndex, false);
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				selectedItem = itemList.get(position);
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if(positionOffsetPixels != 0) recyclerAdapter.pausePlayer();
			}
		});
		selectedItem = itemList.get(selectionIndex);
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(INSTANCEPARAM_RESTORE, true);
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
		
		//Pausing the current video player
		recyclerAdapter.pausePlayer();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Releasing the recycler view data
		recyclerAdapter.release();
	}
	
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if(itemId == android.R.id.home) {
			super.onBackPressed();
			return true;
		} else if(itemId == R.id.action_share) {
			shareItem(selectedItem);
			return true;
		} else if(itemId == R.id.action_save) {
			saveItem(selectedItem);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void toggleUI() {
		if(uiVisible) hideUI();
		else showUI();
	}
	
	private void hideUI() {
		if(!uiVisible) return;
		uiVisible = false;
		
		if(activeUIAnimator != null) {
			activeUIAnimator.cancel();
		}
		
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
		
		activeUIAnimator = valueAnimator;
		
		//Enabling immersive mode
		WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content))
				.hide(WindowInsetsCompat.Type.systemBars());
		
		//Hiding the recycler adapter views' UI
		recyclerAdapter.hideLocalUI();
	}
	
	private void showUI() {
		if(uiVisible) return;
		uiVisible = true;
		
		if(activeUIAnimator != null) {
			activeUIAnimator.cancel();
		}
		
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
		
		activeUIAnimator = valueAnimator;
		
		//Disabling immersive mode
		WindowCompat.getInsetsController(getWindow(), findViewById(android.R.id.content))
				.show(WindowInsetsCompat.Type.systemBars());
		
		//Show the recycler adapter views' UI
		recyclerAdapter.showLocalUI();
	}
	
	private void shareItem(AttachmentInfo attachment) {
		//Creating a new intent
		Intent intent = new Intent();
		
		//Setting the intent action
		intent.setAction(Intent.ACTION_SEND);
		
		//Creating a content URI
		Uri content = FileProvider.getUriForFile(
				this,
				AttachmentStorageHelper.getFileAuthority(this),
				Objects.requireNonNull(attachment.getFile())
		);
		
		//Setting the intent file
		intent.putExtra(Intent.EXTRA_STREAM, content);
		
		//Setting the clip data
		intent.setClipData(ClipData.newUri(getContentResolver(), attachment.getComputedFileName(), content));
		
		//Setting the type
		intent.setType(attachment.getComputedContentType());
		
		//Enabling read access
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		//Starting the activity
		startActivity(Intent.createChooser(intent, getResources().getText(R.string.action_sharemessage)));
	}
	
	private void saveItem(AttachmentInfo attachment) {
		targetExportFile = attachment.getFile();
		
		saveFileLauncher.launch(new ContractCreateDynamicDocument.Params(
				attachment.getComputedFileName(),
				attachment.getComputedContentType()
		));
	}
	
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int viewTypeImage = 0;
		private static final int viewTypeVideo = 1;
		
		private Player currentPlayer = null;
		private final List<AttachmentInfo> itemList;
		private final List<ExoPlayer> playerList = new ArrayList<>();
		private final List<VideoViewHolder> playerViewHolderList = new ArrayList<>();
		
		RecyclerAdapter(List<AttachmentInfo> itemList) {
			this.itemList = itemList;
			
			setHasStableIds(true);
		}
		
		public void pausePlayer() {
			if(currentPlayer != null) currentPlayer.setPlayWhenReady(false);
		}
		
		public void release() {
			for(ExoPlayer player : playerList) player.release();
		}
		
		public void hideLocalUI() {
			//Updating the video players
			for(VideoViewHolder viewHolder : playerViewHolderList) {
				viewHolder.setPlayerUIVisibility(false);
			}
		}
		
		public void showLocalUI() {
			//Updating the video players
			for(VideoViewHolder viewHolder : playerViewHolderList) {
				viewHolder.setPlayerUIVisibility(true);
			}
		}
		
		@Override
		public int getItemCount() {
			return itemList.size();
		}
		
		@Override
		public long getItemId(int position) {
			return itemList.get(position).getLocalID();
		}
		
		@Override
		public int getItemViewType(int position) {
			AttachmentInfo item = itemList.get(position);
			if(FileHelper.compareMimeTypes(item.getComputedContentType(), MIMEConstants.mimeTypeImage)) return viewTypeImage;
			else if(FileHelper.compareMimeTypes(item.getComputedContentType(), MIMEConstants.mimeTypeVideo)) return viewTypeVideo;
			else throw new IllegalArgumentException("Unsupported item type provided: " + item.getComputedContentType());
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
			AttachmentInfo item = itemList.get(position);
			
			boolean shouldAutoPlay = autoPlay && selectedItem == item;
			if(shouldAutoPlay) autoPlay = false;
			
			switch(getItemViewType(position)) {
				case viewTypeImage: {
					//Loading the image
					ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
					imageViewHolder.loadImage(item);
					break;
				}
				case viewTypeVideo: {
					VideoViewHolder videoViewHolder = (VideoViewHolder) holder;
					videoViewHolder.playVideo(item.getFile(), shouldAutoPlay);
					if(shouldAutoPlay) hideUI();
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
			
			void loadImage(AttachmentInfo attachment) {
				//Loading the image file with Glide
				Glide.with(MediaViewer.this)
						.load(attachment.getFile())
						.transition(DrawableTransitionOptions.withCrossFade(100))
						.signature(new ObjectKey(attachment.getLocalID()))
						.listener(new RequestListener<>() {
							@Override
							public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
								return false;
							}
							
							@Override
							public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
								if(resource instanceof GifDrawable) {
									resource.setVisible(true, true);
								}
								
								return false;
							}
						})
						.into(imageView);
			}
		}
		
		class VideoViewHolder extends RecyclerView.ViewHolder {
			private static final long updateFrequency = 1000 / 30;
			
			private final ExoPlayer player;
			private final StyledPlayerView playerView;
			private final ViewGroup playerControlsGroup;
			private final ImageButton playButton, pauseButton;
			private final TextView progressLabel, durationLabel;
			private final Slider progressSlider;
			
			VideoViewHolder(@NonNull View itemView) {
				super(itemView);
				
				//Creating the player instance
				player = new ExoPlayer.Builder(MediaViewer.this).build();
				
				//Adding the player to the list
				playerList.add(player);
				
				//Resolving views
				playerView = itemView.findViewById(R.id.playerview);
				playerControlsGroup = itemView.findViewById(R.id.player_controls);
				playButton = playerControlsGroup.findViewById(R.id.player_play);
				pauseButton = playerControlsGroup.findViewById(R.id.player_pause);
				ViewGroup bottomBar = playerControlsGroup.findViewById(R.id.player_bottom_bar);
				progressLabel = bottomBar.findViewById(R.id.player_progress);
				durationLabel = bottomBar.findViewById(R.id.player_duration);
				progressSlider = bottomBar.findViewById(R.id.player_slider);
				
				//Linking the player to the player view
				playerView.setPlayer(player);
				
				//Adding the view holder to the list
				playerViewHolderList.add(this);
				
				//Keeping the video player's UI in line with the activity's UI
				playerView.setOnClickListener((view) -> toggleUI());
				
				//Adding a listener to the player in order to keep track of which player is active
				player.addListener(new Player.Listener() {
					@Override
					public void onPlaybackStateChanged(int playbackState) {
						if(playbackState == ExoPlayer.STATE_READY) {
							//Update the bottom bar UI as soon as we have video information
							updateBottomBarUI();
						}
					}
					
					@Override
					public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
						if(playWhenReady) { //When the "play" button is pressed
							if(currentPlayer != null && currentPlayer != player) currentPlayer.seekTo(0); //Resetting the previous player
							currentPlayer = player; //Setting the current player
						}
					}
					
					@Override
					public void onIsPlayingChanged(boolean isPlaying) {
						//Update the play button
						updatePlayButton();
						
						//Update the playback state
						updatePlaybackState();
					}
				});
				
				updatePlayButton();
				updatePlaybackState();
				
				ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (view, windowInsets) -> {
					Insets insets = windowInsets.getInsetsIgnoringVisibility(
							WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
					);
					
					ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
					layoutParams.leftMargin = insets.left;
					layoutParams.rightMargin = insets.right;
					layoutParams.bottomMargin = insets.bottom;
					
					return WindowInsetsCompat.CONSUMED;
				});
				
				//Set the listeners
				playButton.setOnClickListener((view) -> {
					if(player.getPlaybackState() == ExoPlayer.STATE_ENDED) {
						player.seekTo(0);
					}
					
					player.play();
				});
				pauseButton.setOnClickListener((view) -> player.pause());
				
				progressSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
					boolean wasPlaying;
					
					@Override
					public void onStartTrackingTouch(@NonNull Slider slider) {
						wasPlaying = player.getPlayWhenReady();
						player.setPlayWhenReady(false);
					}
					
					@Override
					public void onStopTrackingTouch(@NonNull Slider slider) {
						player.seekTo((long) (slider.getValue() * player.getDuration()));
						player.setPlayWhenReady(wasPlaying);
					}
				});
			}
			
			/**
			 * Shows the correct play button for the current player state
			 */
			void updatePlayButton() {
				if(player.isPlaying()) {
					playButton.setVisibility(View.GONE);
					pauseButton.setVisibility(View.VISIBLE);
				} else {
					playButton.setVisibility(View.VISIBLE);
					pauseButton.setVisibility(View.GONE);
				}
			}
			
			/**
			 * Updates the playback position and duration
			 */
			void updatePlaybackState() {
				//Ignore if we're no longer playing
				if(!player.isPlaying()) return;
				
				//Update the bottom bar UI
				updateBottomBarUI();
				
				//Schedule the next update
				playerView.postDelayed(this::updatePlaybackState, updateFrequency);
			}
			
			void updateBottomBarUI() {
				//Get the playback position in milliseconds
				long playbackPosition = player.getCurrentPosition();
				long playbackDuration = player.getDuration();
				
				//Update the labels
				String progressText = DateUtils.formatElapsedTime(playbackPosition / 1000);
				progressLabel.setText(progressText);
				
				String durationText = DateUtils.formatElapsedTime(playbackDuration / 1000);
				durationLabel.setText(durationText);
				
				//Update the slider
				float progress = (float) playbackPosition / playbackDuration;
				progress = Math.max(progress, 0);
				progress = Math.min(progress, 1);
				progressSlider.setValue(progress);
			}
			
			/**
			 * Sets the UI visibility of this player instance
			 * @param visible Whether to show the player UI
			 */
			void setPlayerUIVisibility(boolean visible) {
				if(visible) {
					playerControlsGroup.animate()
							.alpha(1)
							.withStartAction(() -> playerControlsGroup.setVisibility(View.VISIBLE));
				} else {
					playerControlsGroup.animate()
							.alpha(0)
							.withEndAction(() -> playerControlsGroup.setVisibility(View.INVISIBLE));
				}
			}
			
			/**
			 * Sets the video file to play
			 * @param file The video file to play
			 * @param autoPlay Whether the player should be set to play automatically
			 */
			void playVideo(File file, boolean autoPlay) {
				player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
				player.prepare();
				player.setPlayWhenReady(autoPlay);
			}
		}
	}
}
