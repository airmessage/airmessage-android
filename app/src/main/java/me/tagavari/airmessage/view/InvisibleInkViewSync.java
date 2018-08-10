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
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.Random;

import me.tagavari.airmessage.R;

public class InvisibleInkViewSync extends View {
	//Creating the reference variables
	private static final int particleLifetime = 3 * 1000;
	private static final int pixelsPerParticle = 150;
	private static final float particleVelocity = 0.01F;
	private static final float particleRadius = 0.7F;
	//private static final int blurRadius = 25;
	
	private static final int timeRevealTransition = 500; //0.5 seconds
	private static final int timeRevealStay = 9 * 1000; //8 seconds
	
	//Creating the attribute values
	private int backgroundColor;
	
	//Creating the drawing values
	private final Paint backgroundPaint;
	private final Paint particlePaint;
	
	//Creating the rendering values (values used in the drawing process)
	private float[] viewRadii = new float[8];
	private int viewWidth;
	private int viewHeight;
	
	//Creating the threading values
	private boolean currentState = false;
	private volatile boolean viewRevealRequested = false;
	private volatile boolean viewRevealRunning = false;
	
	//Creating the other values
	private final Random random = new Random();
	private int visiblePixelCount;
	//private boolean[][] visibilityMap;
	private Particle[] particleList = null;
	private float particleVelocityPx;
	private float particleRadiusPx;
	private long lastTimeCheck = getTime();
	
	public InvisibleInkViewSync(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		
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
		//particlePaint.setShader(new RadialGradient(particleRadiusPx, particleRadiusPx, particleRadiusPx, 0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
	}
	
	private float[] drawParticlePositions = new float[2];
	private float drawParticleProgress;
	
	private Path clipPath = new Path();
	private RectF clipRect = new RectF();
	
	private int revealTime = 0;
	private int startAlpha = 0xFF;
	private int targetAlpha = startAlpha;
	
	@Override
	protected void onDraw(Canvas canvas) {
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
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		
		//Rounding out the view
		clipPath.reset();
		clipRect.set(0, 0, viewWidth, viewHeight);
		clipPath.addRoundRect(clipRect, viewRadii, Path.Direction.CW);
		canvas.clipPath(clipPath);
		
		//Drawing the background
		if(backgroundColor != Color.TRANSPARENT) {
			backgroundPaint.setColor(ColorUtils.setAlphaComponent(backgroundColor, targetAlpha));
			canvas.drawPaint(backgroundPaint);
		}
		
		//Drawing the particles
		if(particleList != null) {
			for(Particle particle : particleList) {
				//Getting the particle information
				drawParticleProgress = particle.calculateFrame(timeDiff, drawParticlePositions);
				
				//Configuring the paint
				//particlePaint.setShader(new RadialGradient(drawParticlePositions[0] * (float) viewWidth, drawParticlePositions[1] * (float) viewHeight, particleRadiusPx, Color.argb(calculateAlpha(drawParticleProgress), 255, 255, 255), 0x00FFFFFF, Shader.TileMode.CLAMP));
				particlePaint.setColor(Color.argb(Math.max(calculateAlpha(drawParticleProgress) - (0xFF - targetAlpha), 0), 0xFF, 0xFF, 0xFF));
				
				canvas.drawCircle(drawParticlePositions[0] * (float) viewWidth, drawParticlePositions[1] * (float) viewHeight, particleRadiusPx, particlePaint);
			}
		}
		
		//Invalidating the view to force another draw
		invalidate();
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
		viewWidth = width;
		viewHeight = height;
		
		//Calculating the visible pixel count
		visiblePixelCount = width * height;
		
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
		//Returning if the requested state matches the current state
		if(currentState == state) return;
		
		//Setting the state
		currentState = state;
		
		//Invalidating the view to start the animation process if needed
		if(state) invalidate();
	}
	
	public boolean reveal() {
		viewRevealRequested = true;
		return viewRevealRunning;
	}
	
	public void setRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
		viewRadii = new float[]{topLeft, topLeft,
				topRight, topRight,
				bottomRight, bottomRight,
				bottomLeft, bottomLeft};
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