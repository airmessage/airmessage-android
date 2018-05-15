package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/**
 * Handles Apple's iMessage screen effects
 */
public class AppleEffectView extends View {
	//Creating the view values
	private int viewWidth, viewHeight;
	
	//Creating the other values
	private EffectRenderer renderer = null;
	private Runnable finishListener = null;
	private final Random random = new Random();
	
	public AppleEffectView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//Checking if there is a renderer
		if(renderer != null) {
			//Drawing the renderer
			renderer.draw(canvas);
			
			//Invalidating the view to draw the next frame
			invalidate();
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if(isInEditMode()) return;
		
		viewWidth = w;
		viewHeight = h;
		
	}
	
	public void playEcho(View target) {
		//Getting the target's view as a bitmap
		target.buildDrawingCache();
		Bitmap bitmap = target.getDrawingCache();
		if(bitmap == null) {
			target.destroyDrawingCache();
			return;
		}
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
		target.destroyDrawingCache();
		
		//Setting the renderer
		renderer = new EchoRenderer(bitmap);
		
		//Invalidating the view
		invalidate();
	}
	
	private abstract class EffectRenderer {
		abstract void draw(Canvas canvas);
	}
	
	private class EchoRenderer extends EffectRenderer {
		//Creating the reference values
		private static final int lifetime = 3 * 1000; //3 seconds
		private static final float dpSquaredToBubbleCountRatio = 100F / 300441F; //100 bubbles on Nexus 5X
		
		//Creating the renderer values
		private final Bitmap image;
		private final int imageWidth, imageHeight;
		
		//Creating the other values
		private final long startTime;
		private int lastTimeLived = 0;
		private final Bubble[] bubbleList;
		
		EchoRenderer(Bitmap image) {
			//Setting the values
			this.image = image;
			imageWidth = image.getWidth();
			imageHeight = image.getHeight();
			
			startTime = getTime();
			
			//Determining the amount of bubbles
			int bubbleCount = (int) (pxToDp(viewWidth) * pxToDp(viewHeight) * dpSquaredToBubbleCountRatio);
			bubbleList = new Bubble[bubbleCount];
			for(int i = 0; i < bubbleCount; i++) bubbleList[i] = new Bubble();
		}
		
		private final RectF renderRectOut = new RectF();
		@Override
		void draw(Canvas canvas) {
			//Calculating the time
			int timeLived = (int) (getTime() - startTime);
			int timeDiff = timeLived - lastTimeLived;
			lastTimeLived = timeLived;
			
			//Checking if the time to live is up
			if(timeLived >= lifetime) {
				//Removing the renderer
				resetRenderer();
				return;
			}
			
			//Iterating over the bubbles
			for(int i = 0; i < Math.min(lerpInt(0, bubbleList.length, (float) timeLived / (float) (lifetime - Bubble.lifetimeTotal)), bubbleList.length); i++) {
				//Getting the bubble
				Bubble bubble = bubbleList[i];
				
				//Calculating the frame
				float progress = bubble.calculateFrame(timeDiff, renderRectOut);
				
				//Skipping the remainder of the iteration if the particle isn't running
				if(progress == 0) continue;
				
				//Creating the rectangle
				canvas.drawBitmap(image, null, renderRectOut, null);
			}
		}
		
		private class Bubble {
			//Creating the reference values
			private static final int lifetimeBoost = 100;
			private static final int lifetimeTotal = 1000;
			private final float horizontalDistance = dpToPx(50);
			private final float verticalDistance = dpToPx(60);
			private static final float scaleTarget = 1.2F;
			
			//Creating the other values
			float posX, posY;
			int startLifetime;
			int timeLived;
			
			Bubble() {
				generateState();
			}
			
			void generateState() {
				//Picking a location
				posX = random.nextFloat();
				posY = random.nextFloat();
				
				//Setting the time
				timeLived = startLifetime = random.nextInt(lifetimeBoost + 1);
			}
			
			/* private void cycleParticle(int timePassed) {
				//Adding to the cycle time
				int newTime = timeLived + timeDiff;
				if(newTime >= lifetimeTotal) {
					//Regenerating the state
					generateState();
					
					
				} else timeLived = newTime;
			} */
			
			float calculateFrame(int timeDiff, RectF rectOut) {
				//Adding to the time
				if((timeLived += timeDiff) >= lifetimeTotal) return 0;
				
				//Calculating the progresses
				float relativeProgress = (float) timeLived / (float) lifetimeTotal; //Progress including the time offset
				float absoluteProgress = (float) (timeLived - startLifetime) / (float) (lifetimeTotal - startLifetime); //Progress without the time offset
				
				//Calculating the position
				float posXPx = posX * viewWidth + horizontalDistance * interpolateX(relativeProgress);
				float posYPx = posY * viewHeight + verticalDistance * interpolateY(relativeProgress);
				
				//Calculating the view scale
				float scaleProgress = calcViewScaleProgress(absoluteProgress);
				float scale = lerpFloat(0, scaleTarget, 1F - (1F - scaleProgress) * (1F - scaleProgress));
				//float scale = lerpFloat(0, scaleTarget, scaleProgress);
				
				rectOut.left = posXPx - imageWidth * scale / 2F;
				rectOut.right = posXPx + imageWidth * scale / 2F;
				rectOut.top = posYPx - imageHeight * scale / 2F;
				rectOut.bottom = posYPx + imageHeight * scale / 2F;
				
				//Returning the view's alpha target
				return absoluteProgress;
			}
			
			/**
			 * Calculates the X position of the view based on the input
			 * @param input The progress, ranging from 0.0 to 1.0
			 * @return The relative X position of the view, ranging from -1.0 to 1.0
			 */
			private float interpolateX(float input) {
				if(input < 0.5F) return (float) Math.sin(Math.PI * -input); //0.0 -> -1.0
				else {
					input = (input - 0.5F) * 2F; //Fixing the percentage (0.5-1.0 -> 0.0-1.0)
					return input * input * 2F - 1F; //-1.0 -> 1.0
				}
			}
			
			private float interpolateY(float input) {
				return -(input * input);
			}
			
			private float calcViewScaleProgress(float progress) {
				if(progress < 0.25F) return progress * 4;
				else return 1F - (progress - 0.25F) * (1F / 0.75F);
			}
		}
	}
	
	void resetRenderer() {
		//Invalidating the renderer
		renderer = null;
		if(finishListener != null) finishListener.run();
	}
	
	private static long getTime() {
		return SystemClock.uptimeMillis();
	}
	
	private static int lerpInt(int start, int end, float progress) {
		return start + (int) ((end - start) * progress);
	}
	
	private static float lerpFloat(float start, float end, float progress) {
		return start + ((end - start) * progress);
	}
	
	private static float dpToPx(float dp) {
		return dp * Resources.getSystem().getDisplayMetrics().density;
	}
	
	private static float pxToDp(float px) {
		return px / Resources.getSystem().getDisplayMetrics().density;
	}
	
	public void setFinishListener(Runnable listener) {
		finishListener = listener;
	}
}