package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ResourceHelper;

import java.util.ArrayList;
import java.util.List;

public class VisualizerView extends View {
	//Creating the reference values
	private static final long samplingInterval = 10; //50 ms
	private static final float amplitudeFallStep = 1000;
	private static final float amplitudeUpperDisplayBound = 10000;
	private static final float amplitudeMin = 250;
	private static final float amplitudeMax = amplitudeUpperDisplayBound + amplitudeFallStep * 3;
	
	//Creating the view values
	private int viewWidth;
	private int viewHeight;
	private int lineCount;
	
	//Creating the layout values
	private final int lineWidth;
	private final int lineSpacing;
	
	//Creating the data values
	private List<Float> amplitudeList;
	private MediaRecorder mediaRecorder = null;
	private final Handler timerHandler = new Handler();
	private final Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {
			addAmplitude(mediaRecorder.getMaxAmplitude());
			timerHandler.postDelayed(this, samplingInterval);
		}
	};
	
	//Creating the draw values
	private final Paint linePaint;
	
	public VisualizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		//Getting the attributes
		TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VisualizerView, 0, 0);
		
		int paintColor;
		try {
			paintColor = attributes.getColor(R.styleable.VisualizerView_renderColor, Color.WHITE);
			lineWidth = attributes.getDimensionPixelSize(R.styleable.VisualizerView_lineWidth, ResourceHelper.dpToPx(1));
			lineSpacing = attributes.getDimensionPixelSize(R.styleable.VisualizerView_lineSpacing, ResourceHelper.dpToPx(1));
		} finally {
			attributes.recycle();
		}
		
		//Creating the paint
		linePaint = new Paint();
		linePaint.setColor(paintColor);
		linePaint.setStrokeWidth(lineWidth);
	}
	
	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		//Setting the view size
		viewWidth = width;
		viewHeight = height;
		
		//Updating the size of the amplitude list
		lineCount = viewWidth / (lineWidth + lineSpacing);
		amplitudeList = new ArrayList<>(lineCount);
		fillList(amplitudeList, amplitudeMin, lineCount);
	}
	
	public void clear() {
		if(amplitudeList != null) fillList(amplitudeList, amplitudeMin, lineCount);
	}
	
	public void addAmplitude(float amplitude) {
		//Capping the amplitude
		if(amplitude > amplitudeMax) amplitude = amplitudeMax;
		else if(amplitude < amplitudeMin) amplitude = amplitudeMin;
		
		//Checking if the list is empty
		if(amplitudeList.isEmpty()) {
			//Adding the amplitude to the list
			amplitudeList.add(amplitude);
		} else {
			//Getting the last amplitude
			float lastAmplitude = amplitudeList.get(amplitudeList.size() - 1);
			
			//Fading out the amplitude
			if(lastAmplitude - amplitudeFallStep > amplitude) amplitude = lastAmplitude - amplitudeFallStep;
			
			//Adding the amplitude to the list
			amplitudeList.add(amplitude);
			
			//Removing the oldest amplitude if there is no more room for any more
			if(amplitudeList.size() * (lineWidth + lineSpacing) >= viewWidth) amplitudeList.remove(0);
		}
		
		//Updating the view
		invalidate();
	}
	
	public void attachMediaRecorder(MediaRecorder mediaRecorder) {
		//Setting the media recorder
		this.mediaRecorder = mediaRecorder;
		
		//Starting the update timer
		timerHandler.post(timerRunnable);
	}
	
	public void detachMediaRecorder() {
		//Stopping the update timer
		timerHandler.removeCallbacks(timerRunnable);
		
		//Invalidating the media recorder
		this.mediaRecorder = null;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		float offsetX = lineWidth;
		float centerY = viewHeight / 2;
		
		//Drawing the lines
		for(float power : amplitudeList) {
			float scaledHeight = power / amplitudeUpperDisplayBound * viewHeight;
			canvas.drawLine(offsetX, centerY + scaledHeight / 2, offsetX, centerY - scaledHeight / 2, linePaint);
			offsetX += lineWidth + lineSpacing;
		}
	}
	
	private <T> void fillList(List<T> list, T value, int count) {
		int listSize = list.size();
		for(int i = 0; i < listSize; i++) list.set(i, value);
		if(count > listSize) for(int i = listSize; i < count; i++) list.add(value);
	}
}