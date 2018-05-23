package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

import me.tagavari.airmessage.ColorHelper;

/**
 * Handles Apple's iMessage screen effects
 */
public class AppleEffectView extends View {
	//Creating the reference values
	private static final int[] effectColors = {
			0xFFFCE18A, //Yellow
			0xFFFF726D, //Orange
			0xFFB48DEF, //Purple
			0xFFF4306D, //Pink
			0xFF42A5F5, //Blue
			0xFF7986CB //Indigo
	};
	
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
	
	public void playBalloons() {
		//Setting the renderer
		renderer = new BalloonRenderer();
		
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
	
	private class BalloonRenderer extends EffectRenderer {
		//Creating the reference values
		private static final int balloonCountMin = 4;
		private static final int balloonCountMax = 8;
		
		//Creating the other values
		private final Random random = new Random();
		private int balloonCount ;
		private final Balloon[] balloonList;
		private final long startTime;
		private long lastCheckTime;
		
		BalloonRenderer() {
			//Recording the start time
			startTime = lastCheckTime = getTime();
			
			//Adding the balloons
			balloonCount = balloonCountMin + random.nextInt(balloonCountMax - balloonCountMin + 1);
			balloonList = new Balloon[balloonCount];
			for(int i = 0; i < balloonCount; i++) balloonList[i] = new Balloon();
		}
		
		@Override
		void draw(Canvas canvas) {
			//Calculating the times
			int timeLived = (int) (getTime() - startTime);
			long currentTime = getTime();
			int deltaTime = (int) (currentTime - lastCheckTime);
			lastCheckTime = currentTime;
			
			//Drawing the balloons (releasing them over a 1000-millisecond period)
			for(int i = 0; i < Math.min(lerpInt(0, balloonList.length, (float) timeLived / 2000), balloonList.length); i++) {
				balloonList[i].draw(canvas, deltaTime);
			}
			
			//Checking if there are no more balloons
			if(balloonCount == 0) {
				//Removing the renderer
				resetRenderer();
			}
		}
		
		private class Balloon {
			//Creating the reference values
			private final float verticalSpeed = dpToPx(0.00006F);
			private final float balloonHeadWidth = dpToPx(150 / 2);
			private final float balloonHeadHeight = dpToPx(180 / 2);
			private final float balloonTieSize = dpToPx(10);
			private final float balloonStringLength = dpToPx(50);
			
			//Creating the size values
			private float sVerticalSpeed;
			private float sBalloonHeadWidth;
			private float sBalloonHeadHeight;
			private float sBalloonTieSize;
			private float sBalloonStringLength;
			
			//Creating the paint values
			private final Paint colorPaint;
			private final Paint stemPaint;
			
			//Creating the state values
			boolean isActive = true;
			private final float posX;
			int timeLived = 0;
			
			Balloon() {
				//Picking a location
				posX = random.nextFloat() * viewWidth;
				
				//Scaling the values
				float scale = 0.5F + random.nextFloat(); //0.5 to 1.5
				sVerticalSpeed = verticalSpeed * scale;
				sBalloonHeadWidth = balloonHeadWidth * scale;
				sBalloonHeadHeight = balloonHeadHeight * scale;
				sBalloonTieSize = balloonTieSize * scale;
				sBalloonStringLength = balloonStringLength * scale;
				
				//Picking a color
				int color = effectColors[random.nextInt(effectColors.length)];
				
				//Creating the paints
				colorPaint = new Paint();
				colorPaint.setAntiAlias(true);
				colorPaint.setStyle(Paint.Style.FILL);
				colorPaint.setShader(new LinearGradient(0, 0, sBalloonHeadWidth, sBalloonHeadHeight + sBalloonTieSize, ColorHelper.modifyColorMultiply(color, 1.2F), color, Shader.TileMode.CLAMP));
				
				stemPaint = new Paint();
				stemPaint.setAntiAlias(true);
				stemPaint.setShader(new LinearGradient(0, sBalloonHeadHeight + sBalloonTieSize, 0, sBalloonStringLength + sBalloonTieSize, 0xFFB0B0B0, 0xFF979797, Shader.TileMode.CLAMP));
			}
			
			void draw(Canvas canvas, int deltaTime) {
				//Returning if the balloon is not isActive
				if(!isActive) return;
				
				//Adding to the time
				timeLived += deltaTime;
				
				//Calculating the position
				float posY = (viewHeight + sBalloonHeadHeight / 2F) - (timeLived * timeLived * sVerticalSpeed);
				
				if(posY + sBalloonHeadHeight / 2F + sBalloonTieSize + sBalloonStringLength < 0) {
					//Deactivating the balloon
					isActive = false;
					BalloonRenderer.this.balloonCount--;
					return;
				}
				
				//Positioning the canvas
				canvas.save();
				canvas.translate(posX, posY);
				
				//Drawing the balloon head
				canvas.drawOval(-sBalloonHeadWidth / 2F, -sBalloonHeadHeight / 2F, sBalloonHeadWidth / 2F, sBalloonHeadHeight / 2F, colorPaint);
				{
					Path path = new Path();
					path.moveTo(0, sBalloonHeadHeight / 2F);
					path.lineTo(sBalloonTieSize / 2F, sBalloonHeadHeight / 2F + sBalloonTieSize);
					path.lineTo(-sBalloonTieSize / 2F, sBalloonHeadHeight / 2F + sBalloonTieSize);
					path.close();
					canvas.drawPath(path, colorPaint);
				}
				
				//Drawing the balloon stem
				canvas.drawLine(0, sBalloonHeadHeight / 2F + sBalloonTieSize, 0, sBalloonHeadHeight / 2F + sBalloonTieSize + sBalloonStringLength, stemPaint);
				
				//Restoring the canvas
				canvas.restore();
			}
			
			//Calls moveTo() on the path with the provided X and Y positions, taking into account the balloon's rotation
			/* private void applyPath(Path path, float pointX, float pointY, boolean moveTo) {
				//float pointXRotated = pointX;
				//float pointYRotated = pointY;
				float pointXRotated = (pointX) * (float) Math.cos(rotation) - (pointY) * (float) Math.sin(rotation);
				float pointYRotated = (pointX) * (float) Math.sin(rotation) + (pointY) * (float) Math.cos(rotation);
				if(moveTo) path.moveTo(pointXRotated, pointYRotated);
				else path.lineTo(pointXRotated, pointYRotated);
			} */
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