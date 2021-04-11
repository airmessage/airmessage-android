package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import me.tagavari.airmessage.R;

public class InvisibleInkView extends TextureView implements Runnable {
	//Creating the reference variables
	private static final int particleLifetime = 3 * 1000;
	private static final int pixelsPerParticle = 150;
	private static final float particleVelocity = 0.01F;
	private static final float particleRadius = 0.7F;
	
	private static final int timeRevealTransition = 500; //0.5 seconds
	private static final int timeRevealStay = 9 * 1000; //8 seconds
	
	//Creating the attribute values
	private int backgroundColor;
	
	//Creating the drawing values
	private final Paint backgroundPaint;
	private final Paint particlePaint;
	
	//Creating the rendering values (values used in the drawing thread)
	private final Lock viewRadiiLock = new ReentrantLock();
	private float[] viewRadii = new float[8];
	private final AtomicInteger viewWidth = new AtomicInteger();
	private final AtomicInteger viewHeight = new AtomicInteger();
	
	//Creating the threading values
	private Thread viewThread = null;
	private boolean surfaceAvailable = false;
	private volatile boolean viewRequestedRunning = false;
	private volatile boolean viewRunning = false;
	private volatile boolean viewUpdateRequested = false;
	private volatile boolean viewRevealRequested = false;
	private volatile boolean viewRevealRunning = false;
	
	private boolean firstLayoutPassCompleted = false;
	private boolean firstStartRequestCompleted = false;
	
	//Creating the other values
	private final Random random = new Random();
	private int visiblePixelCount;
	//private boolean[][] visibilityMap;
	private Particle[] particleList = null;
	private float particleVelocityPx;
	private float particleRadiusPx;
	private long lastTimeCheck = getTime();
	
	public InvisibleInkView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		
		//Configuring the view
		setOpaque(false);
		
		//Getting the attributes
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.InvisibleInkView, 0, 0);
		
		try {
			backgroundColor = attributes.getColor(R.styleable.InvisibleInkView_backgroundColor, Color.TRANSPARENT);
		} finally {
			attributes.recycle();
		}
		
		//Converting the units
		particleVelocityPx = dpToPx(particleVelocity);
		particleRadiusPx = dpToPx(particleRadius);
		
		//Setting up the paints
		backgroundPaint = new Paint();
		backgroundPaint.setStyle(Paint.Style.FILL);
		backgroundPaint.setColor(backgroundColor);
		
		particlePaint = new Paint();
		particlePaint.setAntiAlias(true);
		particlePaint.setStyle(Paint.Style.FILL);
		
		//Setting the surface listener
		setSurfaceTextureListener(new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				//Setting the drawing surface as available
				surfaceAvailable = true;
				
				//Starting the drawing thread
				if(viewRunning && firstLayoutPassCompleted && viewThread == null) startThread();
			}
			
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			
			}
			
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				surfaceAvailable = false;
				onPause();
				return true;
			}
			
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			
			}
		});
	}
	
	@Override
	public void run() {
		Canvas canvas;
		float[] drawParticlePositions = new float[2];
		float drawParticleProgress;
		
		Path clipPath = new Path();
		RectF clipRect = new RectF();
		
		int revealTime = 0;
		int startAlpha = 0xFF;
		int targetAlpha = startAlpha;
		
		try {
			while(viewRunning) {
				//Returning if the view isn't valid
				if(!isAttachedToWindow() || !isAvailable()) return;
				
				//Getting the canvas
				canvas = lockCanvas();
				try {
					if(canvas == null) return;
					
					//Checking if the view needs to be updated
					if(viewUpdateRequested) {
						processTargetView();
						viewUpdateRequested = false;
					}
					
					//Returning if the view isn't valid
					if(!isAttachedToWindow() || !isAvailable()) return;
					
					//Calculating the time difference
					long currentTime = getTime();
					int timeDiff = (int) (currentTime - lastTimeCheck);
					lastTimeCheck = currentTime;
					
					//Advancing the reveal time
					if(revealTime > 0) {
						revealTime = Math.max(revealTime - timeDiff, 0);
						if(revealTime > timeRevealTransition + timeRevealStay) targetAlpha = lerpInt(0x00, startAlpha, (revealTime - (timeRevealTransition + timeRevealStay)) / (float) timeRevealTransition); //Fade out stage
						else if(revealTime > timeRevealTransition) targetAlpha = 0x00; //Stay stage
						else targetAlpha = lerpInt(0xFF, 0x00, revealTime / (float) timeRevealTransition); //Fade in stage
					} else viewRevealRunning = false;
					
					//Checking if a reveal had been requested
					if(viewRevealRequested) {
						//Setting the reveal time
						revealTime = timeRevealTransition * 2 + timeRevealStay;
						startAlpha = targetAlpha;
						
						viewRevealRequested = false;
						viewRevealRunning = true;
					}
					
					//Clearing the canvas
					if(!viewRunning) return;
					canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
					
					//Rounding out the view
					clipPath.reset();
					clipRect.set(0, 0, viewWidth.get(), viewHeight.get());
					
					viewRadiiLock.lock();
					try {
						clipPath.addRoundRect(clipRect, viewRadii, Path.Direction.CW);
					} finally {
						viewRadiiLock.unlock();
					}
					
					if(!viewRunning) return;
					canvas.clipPath(clipPath);
					
					//Drawing the background
					if(!viewRunning) return;
					if(backgroundColor != Color.TRANSPARENT) {
						backgroundPaint.setColor(ColorUtils.setAlphaComponent(backgroundColor, targetAlpha));
						canvas.drawPaint(backgroundPaint);
					}
					
					//Drawing the particles
					if(particleList == null) continue;
					for(Particle particle : particleList) {
						//Getting the particle information
						drawParticleProgress = particle.calculateFrame(timeDiff, drawParticlePositions);
						
						//Configuring the paint
						particlePaint.setColor(Color.argb(Math.max(calculateAlpha(drawParticleProgress) - (0xFF - targetAlpha), 0), 0xFF, 0xFF, 0xFF));
						
						if(!viewRunning) return;
						canvas.drawCircle(drawParticlePositions[0] * (float) viewWidth.get(), drawParticlePositions[1] * (float) viewHeight.get(), particleRadiusPx, particlePaint);
					}
				} finally {
					//Updating the canvas changes
					unlockCanvasAndPost(canvas);
				}
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			FirebaseCrashlytics.getInstance().recordException(exception);
		} finally {
			stop();
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		onResume();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		onPause();
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		
		if(visibility == VISIBLE) {
			if(viewRequestedRunning) start();
		} else stop();
	}
	
	private static int calculateAlpha(float progress) {
		if(progress < 0.1F) return (int) (progress * 10F * 255F);
		else return 255 - (int) ((progress - 0.1F) * (10F / 9F) * 255F);
	}
	
	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		if(isInEditMode()) return;
		
		//Setting the view sizes
		viewWidth.set(width);
		viewHeight.set(height);
		
		//Calculating the visible pixel count
		visiblePixelCount = width * height;
		
		//Setting the layout pass as completed
		boolean isFirst = !firstLayoutPassCompleted;
		firstLayoutPassCompleted = true;
		
		//Returning if the isn't running
		if(!viewRunning) return;
		
		//Requesting a view update
		viewUpdateRequested = true;
		
		//Starting the thread if start() was called before this layout pass (and was unable to start because of a lack of layout data)
		if(isFirst && surfaceAvailable && firstStartRequestCompleted) startThread();
	}
	
	void processTargetView() {
		//Rebuilding the particle list
		int targetListSize = visiblePixelCount / pixelsPerParticle;
		if(particleList == null) {
			//Populating the particle list
			particleList = new Particle[targetListSize];
			for(int i = 0; i < targetListSize; i++) particleList[i] = new Particle();
		} else {
			int listSize = particleList.length;
			if(listSize != targetListSize) {
				//Resizing the array
				particleList = Arrays.copyOf(particleList, targetListSize);
				
				//Filling in the blank values
				for(int i = listSize; i < targetListSize; i++) particleList[i] = new Particle();
			}
		}
	}
	
	public void setState(boolean state) {
		if(state) start();
		else stop();
		viewRequestedRunning = state;
	}
	
	private void start() {
		//Returning if the view is already running
		if(viewRunning) return;
		
		//Setting the view as running
		viewRunning = true;
		
		//Requesting a view update
		viewUpdateRequested = true;
		
		//Setting the start request as completed
		firstStartRequestCompleted = true;
		
		//Starting the thread if there has already been a layout pass
		if(firstLayoutPassCompleted && surfaceAvailable) startThread();
	}
	
	private void stop() {
		//Returning if the view isn't running
		if(!viewRunning) return;
		
		//Setting the view as stopped
		viewRunning = false;
		
		try {
			if(viewThread != null) viewThread.join();
		} catch(InterruptedException exception) {
			exception.printStackTrace();
		} finally {
			viewThread = null;
		}
	}
	
	private void startThread() {
		viewThread = new Thread(this);
		viewThread.start();
	}
	
	public void onPause() {
		stop();
	}
	
	public void onResume() {
		if(viewRequestedRunning) start();
	}
	
	public boolean reveal() {
		viewRevealRequested = true;
		return viewRevealRunning;
	}
	
	public void setRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
		//Setting the radii
		viewRadiiLock.lock();
		try {
			viewRadii = new float[]{topLeft, topLeft,
					topRight, topRight,
					bottomRight, bottomRight,
					bottomLeft, bottomLeft};
		} finally {
			viewRadiiLock.unlock();
		}
	}
	
	public void setRadii(float[] radii) {
		//Setting the radii
		viewRadiiLock.lock();
		try {
			viewRadii = radii;
		} finally {
			viewRadiiLock.unlock();
		}
	}
	
	public void setBackgroundColor(int color) {
		//Setting the background color
		backgroundColor = color;
	}
	
	/**
	 * A class that represents a particle of the invisible ink
	 * The coordinates represent values 0.0 to 1.0, mapped to the view's size
	 * The creation date is used for timeLived
	 */
	private class Particle {
		float x, y;
		float velX, velY;
		int cycleTime;
		
		Particle() {
			//Prewarming
			generateState();
			cycleTime = random.nextInt(particleLifetime);
		}
		
		void generateState() {
			//Picking a random location
			x = random.nextFloat();
			y = random.nextFloat();
			
			//Picking a new velocity
			float direction = random.nextFloat() * (float) Math.PI * 2F;
			velX = (float) Math.cos(direction) - (float) Math.sin(direction);
			velY = (float) Math.sin(direction) + (float) Math.cos(direction);
		}
		
		private void cycleParticle(int timeDiff) {
			//Adding to the cycle time
			int newTime = cycleTime + timeDiff;
			if(newTime >= particleLifetime) {
				//Cycling the time
				cycleTime = newTime % particleLifetime;
				
				//Regenerating the state
				generateState();
				
				
			} else cycleTime = newTime;
		}
		
		/**
		 * Calculates the values required for the frame, recycling the particle if it has exceeded its timeLived
		 * @param locOut The X and the Y location of the particle
		 * @return The progress of the particle (timeLived)
		 */
		float calculateFrame(int timeDiff, float[] locOut) {
			//Cycling the particle (making a new particle with the current instance) if needed
			cycleParticle(timeDiff);
			
			//Calculating the progress (from life to death - 0.0 to 1.0)
			float progress = (float) cycleTime / (float) particleLifetime;
			
			//Calculating the position
			locOut[0] = x + velX * particleVelocityPx * progress;
			locOut[1] = y + velY * particleVelocityPx * progress;
			
			//Returning the progress
			return progress;
		}
	}
	
	private static long getTime() {
		return SystemClock.uptimeMillis();
	}
	
	private static float dpToPx(float dp) {
		return dp * Resources.getSystem().getDisplayMetrics().density;
	}
	
	private static float pxToDp(float px) {
		return px / Resources.getSystem().getDisplayMetrics().density;
	}
	
	private static int lerpInt(int start, int end, float progress) {
		return start + (int) ((end - start) * progress);
	}
}