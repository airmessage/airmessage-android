package me.tagavari.airmessage.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
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
import com.google.android.exoplayer2.MediaItem;
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
import java.util.stream.Collectors;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.constants.MIMEConstants;
import me.tagavari.airmessage.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.helper.ExternalStorageHelper;
import me.tagavari.airmessage.helper.FileHelper;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.view.RoundedFrameLayout;

public class MediaViewer extends AppCompatActivity {
	//Creating the constants
	public static final String intentParamIndex = "index";
	public static final String intentParamDataList = "dataList";
	
	private static final String INSTANCEPARAM_RESTORE = "restore";
	
	private static final int activityResultCreateFileSAF = 0;
	
	private ViewPager2 viewPager;
	private RecyclerAdapter recyclerAdapter;
	private Toolbar toolbar;
	private View scrimTop;
	private View scrimBottom;
	
	private boolean uiVisible = true;
	
	private AttachmentInfo selectedItem;
	
	private boolean autoPlay = false;
	
	private File targetExportFile = null;
	
	private final Rect systemInsetsRect = new Rect();
	private final List<Consumer<Rect>> systemInsetsRectUpdateListeners = new ArrayList<>();
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
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
		
		//Restoring the activity data
		if(savedInstanceState == null) {
			autoPlay = true;
		} else {
			autoPlay = savedInstanceState.getBoolean(INSTANCEPARAM_RESTORE, true);
		}
		
		//Getting the activity parameters
		int selectionIndex = getIntent().getIntExtra(intentParamIndex, 0);
		List<AttachmentInfo> itemList = getIntent().getParcelableArrayListExtra(intentParamDataList);
		
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == activityResultCreateFileSAF) {
			if(resultCode == RESULT_OK) ExternalStorageHelper.exportFile(this, targetExportFile, data.getData());
		}
	}
	
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
		
		//Hiding the recycler adapter views' UI
		recyclerAdapter.hideLocalUI();
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
		
		//Show the recycler adapter views' UI
		recyclerAdapter.showLocalUI();
	}
	
	private boolean shareItem(AttachmentInfo attachment) {
		//Getting the file mime type
		String fileName = attachment.getFile().getName();
		int substringStart = attachment.getFile().getName().lastIndexOf(".") + 1;
		
		//Returning if the file cannot be substringed
		if(fileName.length() <= substringStart) return false;
		
		//Creating a new intent
		Intent intent = new Intent();
		
		//Setting the intent action
		intent.setAction(Intent.ACTION_SEND);
		
		//Creating a content URI
		Uri content = FileProvider.getUriForFile(this, AttachmentStorageHelper.getFileAuthority(this), attachment.getFile());
		
		//Setting the intent file
		intent.putExtra(Intent.EXTRA_STREAM, content);
		
		//Setting the type
		intent.setType(attachment.getContentType());
		
		//Starting the activity
		startActivity(Intent.createChooser(intent, getResources().getText(R.string.action_sharemessage)));
		
		//Returning true
		return true;
	}
	
	private boolean saveItem(AttachmentInfo attachment) {
		targetExportFile = attachment.getFile();
		ExternalStorageHelper.createFileSAF(this, activityResultCreateFileSAF, attachment.getContentType(), attachment.getFile().getName());
		
		//Returning true
		return true;
	}
	
	private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		private static final int viewTypeImage = 0;
		private static final int viewTypeVideo = 1;
		
		private Player currentPlayer = null;
		private final List<AttachmentInfo> itemList;
		private final List<SimpleExoPlayer> playerList = new ArrayList<>();
		private final List<PlayerView> playerViewList = new ArrayList<>();
		
		RecyclerAdapter(List<AttachmentInfo> itemList) {
			this.itemList = itemList;
			
			setHasStableIds(true);
		}
		
		public void pausePlayer() {
			if(currentPlayer != null) currentPlayer.setPlayWhenReady(false);
		}
		
		public void release() {
			for(SimpleExoPlayer player : playerList) player.release();
		}
		
		public void hideLocalUI() {
			//Updating the video players
			for(PlayerView playerView : playerViewList) playerView.hideController();
		}
		
		public void showLocalUI() {
			//Updating the video players
			for(PlayerView playerView : playerViewList) playerView.showController();
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
			if(FileHelper.compareMimeTypes(item.getContentType(), MIMEConstants.mimeTypeImage)) return viewTypeImage;
			else if(FileHelper.compareMimeTypes(item.getContentType(), MIMEConstants.mimeTypeVideo)) return viewTypeVideo;
			else throw new IllegalArgumentException("Unsupported item type provided: " + item.getContentType());
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
						.listener(new RequestListener<Drawable>() {
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
			final SimpleExoPlayer player;
			
			VideoViewHolder(@NonNull View itemView) {
				super(itemView);
				
				//Creating the player instance
				player = new SimpleExoPlayer.Builder(MediaViewer.this).build();
				
				//Adding the player to the list
				playerList.add(player);
				
				//Linking the player to the player view
				PlayerView playerView = itemView.findViewById(R.id.playerview);
				playerView.setPlayer(player);
				
				//Adding the player view to the list
				playerViewList.add(playerView);
				
				//Keeping the video player's UI in line with the activity's UI
				playerView.setControllerVisibilityListener(visibility -> {
					if(visibility == View.VISIBLE) showUI();
					else hideUI();
				});
				
				//Ensuring that the player control bar has appropriate margins, as not to fall behind the system bars
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
			
			void playVideo(File file, boolean autoPlay) {
				DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(MediaViewer.this, Util.getUserAgent(MediaViewer.this, getResources().getString(R.string.app_name)));
				MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(Uri.fromFile(file)));
				player.setMediaSource(videoSource);
				player.prepare();
				player.setPlayWhenReady(autoPlay);
			}
		}
	}
}