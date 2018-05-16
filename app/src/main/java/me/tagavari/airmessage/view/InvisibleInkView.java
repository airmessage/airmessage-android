package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;

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
	private static final int blurRadius = 50;
	//private static final int blurRadius = 25;
	
	private static final int timeRevealTransition = 500; //0.5 seconds
	private static final int timeRevealStay = 9 * 1000; //8 seconds
	
	//Creating the attribute values
	private final int targetViewID;
	private View targetView = null;
	
	//Creating the drawing values
	private final Paint voidPaint;
	private final Paint backgroundPaint;
	private final Paint particlePaint;
	
	//Creating the rendering values (values used in the drawing thread)
	private final Lock viewRadiiLock= new ReentrantLock();
	private float[] viewRadii = new float[8];
	private final AtomicInteger viewWidth = new AtomicInteger();
	private final AtomicInteger viewHeight = new AtomicInteger();
	private Bitmap backgroundBitmap = null;
	
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
	private int backgroundColor = Color.WHITE;
	
	public InvisibleInkView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		
		//Configuring the view
		setOpaque(false);
		
		//Getting the attributes
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.InvisibleInkView, 0, 0);
		
		try {
			targetViewID = attributes.getResourceId(R.styleable.InvisibleInkView_target, -1);
			if(targetViewID == -1) throw new IllegalArgumentException("Target view not provided");
		} finally {
			attributes.recycle();
		}
		
		//Converting the units
		particleVelocityPx = dpToPx(particleVelocity);
		particleRadiusPx = dpToPx(particleRadius);
		
		//Setting up the paint
		voidPaint = new Paint();
		voidPaint.setStyle(Paint.Style.FILL);
		voidPaint.setColor(Color.BLACK);
		
		backgroundPaint = new Paint();
		backgroundPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
		
		particlePaint = new Paint();
		particlePaint.setAntiAlias(true);
		particlePaint.setStyle(Paint.Style.FILL);
		//particlePaint.setShader(new RadialGradient(particleRadiusPx, particleRadiusPx, particleRadiusPx, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
		
		//Setting the surface listener
		setSurfaceTextureListener(new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				//Setting the drawing surface as available
				surfaceAvailable = true;
				
				//Drawing a black box
				Canvas canvas = lockCanvas();
				try {
					Path clipPath = new Path();
					RectF clipRect = new RectF();
					
					clipPath.reset();
					clipRect.set(0, 0, viewWidth.get(), viewHeight.get());
					
					viewRadiiLock.lock();
					try {
						clipPath.addRoundRect(clipRect, viewRadii, Path.Direction.CW);
					} finally {
						viewRadiiLock.unlock();
					}
					
					canvas.clipPath(clipPath);
					
					canvas.drawPaint(voidPaint);
				} finally {
					unlockCanvasAndPost(canvas);
				}
				
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
					/* //Drawing a black box
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
					
					canvas.drawPaint(voidPaint); */
						
						//Updating the view
						while(!processTargetView()) {
							try {
								Thread.sleep(100);
							} catch(InterruptedException exception) {
								exception.printStackTrace();
							}
						}
						
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
						if(revealTime > timeRevealTransition + timeRevealStay) //Fade out stage
							targetAlpha = lerpInt(0x00, startAlpha, (revealTime - (timeRevealTransition + timeRevealStay)) / (float) timeRevealTransition);
						else if(revealTime > timeRevealTransition) //Stay stage
							targetAlpha = 0x00;
						else //Fade in stage
							targetAlpha = lerpInt(0xFF, 0x00, revealTime / (float) timeRevealTransition);
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
					if(backgroundBitmap == null) {
						backgroundPaint.setColor(ColorUtils.setAlphaComponent(backgroundColor, targetAlpha));
						canvas.drawPaint(backgroundPaint);
					} else {
						backgroundPaint.setColor(Color.argb(targetAlpha, 0xFF, 0xFF, 0xFF));
						canvas.drawBitmap(backgroundBitmap, 0, 0, backgroundPaint);
					}
					
					//Drawing the particles
					if(particleList == null) continue;
					for(Particle particle : particleList) {
						//Getting the particle information
						drawParticleProgress = particle.calculateFrame(timeDiff, drawParticlePositions);
						
						//Configuring the paint
						//particlePaint.setShader(new RadialGradient(drawParticlePositions[0] * (float) viewWidth, drawParticlePositions[1] * (float) viewHeight, particleRadiusPx, Color.argb(calculateAlpha(drawParticleProgress), 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP));
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
			Crashlytics.logException(exception);
		} finally {
			stop();
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		targetView = ((ViewGroup) getParent()).findViewById(targetViewID);
		if(targetView == null) throw new IllegalArgumentException("Target view not found (id " + targetViewID + ")");
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
	
	/* @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		//Calculating the time difference
		long currentTime = getTime();
		int timeDiff = (int) (currentTime - lastTimeCheck);
		lastTimeCheck = currentTime;
		
		//Drawing the particles
		for(Particle particle : particleList) {
			drawParticleProgress = particle.calculateFrame(timeDiff, drawParticlePositions);
			//particlePaint.setShader(new RadialGradient(drawParticlePositions[0] * (float) viewWidth, drawParticlePositions[1] * (float) viewHeight, particleRadiusPx, Color.argb(255 - (int) (drawParticleProgress * 255), 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP));
			canvas.drawCircle(drawParticlePositions[0] * (float) viewWidth, drawParticlePositions[1] * (float) viewHeight, particleRadiusPx, particlePaint);
		}
		
		//Invalidating the view to draw the next frame
		invalidate();
	} */
	
	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		if(isInEditMode()) return;
		
		//Setting the view sizes
		viewWidth.set(width);
		viewHeight.set(height);
		
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
	
	boolean processTargetView() {
		//Rebuilding the visibility map
		if(!calculateVisibilityMap()) return false;
		
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
		
		return true;
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
	
	private boolean calculateVisibilityMap() {
		//Resetting the visible pixel count
		visiblePixelCount = 0;
		
		//Requesting the target view's bitmap
		targetView.buildDrawingCache();
		
		//Getting the bitmap
		if(targetView.getDrawingCache() == null) {
			targetView.destroyDrawingCache();
			return false;
		}
		backgroundBitmap = targetView.getDrawingCache().copy(Bitmap.Config.ARGB_8888, false);
		
		//Mapping the bitmap's opacity
		/* visibilityMap = new boolean[backgroundBitmap.getWidth()][backgroundBitmap.getHeight()];
		for(int x = 0; x < backgroundBitmap.getWidth(); x++) {
			for(int y = 0; y < backgroundBitmap.getHeight(); y++) {
				boolean isVisible = Color.alpha(backgroundBitmap.getPixel(x, y)) > 255 / 2;
				visibilityMap[x][y] = isVisible;
				if(isVisible) visiblePixelCount++;
			}
		} */
		visiblePixelCount = backgroundBitmap.getWidth() * backgroundBitmap.getHeight();
		
		//Destroying the drawing cache
		targetView.destroyDrawingCache();
		
		//Checking if the target view is an image view
		if(targetView instanceof ImageView) {
			//Blurring the bitmap
			Bitmap targetBitmap = ((BitmapDrawable) ((ImageView) targetView).getDrawable()).getBitmap();
			if(targetBitmap == null) backgroundBitmap = null;
			else backgroundBitmap = stackBlur(targetBitmap, (float) viewWidth.get() / (float) targetBitmap.getWidth(), blurRadius);
			//else backgroundBitmap = renderScriptBlur(getContext(), targetBitmap, viewWidth.get(), viewHeight.get(), blurRadius);
		} else {
			//Invalidating the bitmap
			backgroundBitmap = null;
		}
		
		return true;
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
	
	/**
	 * Stack Blur v1.0 from
	 * http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
	 * Java Author: Mario Klingemann <mario at quasimondo.com>
	 * http://incubator.quasimondo.com
	 *
	 * created Feburary 29, 2004
	 * Android port : Yahel Bouaziz <yahel at kayenko.com>
	 * http://www.kayenko.com
	 * ported april 5th, 2012
	 *
	 * This is a compromise between Gaussian Blur and Box blur
	 * It creates much better looking blurs than Box Blur, but is
	 * 7x faster than my Gaussian Blur implementation.
	 *
	 * I called it Stack Blur because this describes best how this
	 * filter works internally: it creates a kind of moving stack
	 * of colors whilst scanning through the image. Thereby it
	 * just has to add one new block of color to the right side
	 * of the stack and remove the leftmost color. The remaining
	 * colors on the topmost layer of the stack are either added on
	 * or reduced by one, depending on if they are on the right or
	 * on the left side of the stack.
	 *
	 * If you are using this algorithm in your code please add
	 * the following line:
	 * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
	 */
	
	private static Bitmap stackBlur(Bitmap sentBitmap, float scale, int radius) {
		int width = Math.round(sentBitmap.getWidth() * scale);
		int height = Math.round(sentBitmap.getHeight() * scale);
		sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);
		
		Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
		
		if (radius < 1) {
			return (null);
		}
		
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		
		int[] pix = new int[w * h];
		bitmap.getPixels(pix, 0, w, 0, 0, w, h);
		
		int wm = w - 1;
		int hm = h - 1;
		int wh = w * h;
		int div = radius + radius + 1;
		
		int r[] = new int[wh];
		int g[] = new int[wh];
		int b[] = new int[wh];
		int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
		int vmin[] = new int[Math.max(w, h)];
		
		int divsum = (div + 1) >> 1;
		divsum *= divsum;
		int dv[] = new int[256 * divsum];
		for (i = 0; i < 256 * divsum; i++) {
			dv[i] = (i / divsum);
		}
		
		yw = yi = 0;
		
		int[][] stack = new int[div][3];
		int stackpointer;
		int stackstart;
		int[] sir;
		int rbs;
		int r1 = radius + 1;
		int routsum, goutsum, boutsum;
		int rinsum, ginsum, binsum;
		
		for (y = 0; y < h; y++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			for (i = -radius; i <= radius; i++) {
				p = pix[yi + Math.min(wm, Math.max(i, 0))];
				sir = stack[i + radius];
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				rbs = r1 - Math.abs(i);
				rsum += sir[0] * rbs;
				gsum += sir[1] * rbs;
				bsum += sir[2] * rbs;
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
			}
			stackpointer = radius;
			
			for (x = 0; x < w; x++) {
				
				r[yi] = dv[rsum];
				g[yi] = dv[gsum];
				b[yi] = dv[bsum];
				
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				
				if (y == 0) {
					vmin[x] = Math.min(x + radius + 1, wm);
				}
				p = pix[yw + vmin[x]];
				
				sir[0] = (p & 0xff0000) >> 16;
				sir[1] = (p & 0x00ff00) >> 8;
				sir[2] = (p & 0x0000ff);
				
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];
				
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;
				
				stackpointer = (stackpointer + 1) % div;
				sir = stack[(stackpointer) % div];
				
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];
				
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];
				
				yi++;
			}
			yw += w;
		}
		for (x = 0; x < w; x++) {
			rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
			yp = -radius * w;
			for (i = -radius; i <= radius; i++) {
				yi = Math.max(0, yp) + x;
				
				sir = stack[i + radius];
				
				sir[0] = r[yi];
				sir[1] = g[yi];
				sir[2] = b[yi];
				
				rbs = r1 - Math.abs(i);
				
				rsum += r[yi] * rbs;
				gsum += g[yi] * rbs;
				bsum += b[yi] * rbs;
				
				if (i > 0) {
					rinsum += sir[0];
					ginsum += sir[1];
					binsum += sir[2];
				} else {
					routsum += sir[0];
					goutsum += sir[1];
					boutsum += sir[2];
				}
				
				if (i < hm) {
					yp += w;
				}
			}
			yi = x;
			stackpointer = radius;
			for (y = 0; y < h; y++) {
				// Preserve alpha channel: ( 0xff000000 & pix[yi] )
				pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];
				
				rsum -= routsum;
				gsum -= goutsum;
				bsum -= boutsum;
				
				stackstart = stackpointer - radius + div;
				sir = stack[stackstart % div];
				
				routsum -= sir[0];
				goutsum -= sir[1];
				boutsum -= sir[2];
				
				if (x == 0) {
					vmin[y] = Math.min(y + r1, hm) * w;
				}
				p = x + vmin[y];
				
				sir[0] = r[p];
				sir[1] = g[p];
				sir[2] = b[p];
				
				rinsum += sir[0];
				ginsum += sir[1];
				binsum += sir[2];
				
				rsum += rinsum;
				gsum += ginsum;
				bsum += binsum;
				
				stackpointer = (stackpointer + 1) % div;
				sir = stack[stackpointer];
				
				routsum += sir[0];
				goutsum += sir[1];
				boutsum += sir[2];
				
				rinsum -= sir[0];
				ginsum -= sir[1];
				binsum -= sir[2];
				
				yi += w;
			}
		}
		
		bitmap.setPixels(pix, 0, w, 0, 0, w, h);
		
		return (bitmap);
	}
	
	/* public static Bitmap renderScriptBlur(Context context, Bitmap image, int imageWidth, int imageHeight, float blurRadius) {
		Bitmap inputBitmap = Bitmap.createScaledBitmap(image, imageWidth, imageHeight, false);
		Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
		
		RenderScript rs = RenderScript.create(context);
		
		ScriptIntrinsicBlur intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
		Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
		Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
		
		intrinsicBlur.setRadius(blurRadius);
		intrinsicBlur.setInput(tmpIn);
		intrinsicBlur.forEach(tmpOut);
		tmpOut.copyTo(outputBitmap);
		
		return outputBitmap;
	} */
	
	@Override
	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
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