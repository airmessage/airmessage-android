package me.tagavari.airmessage.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import androidx.core.view.MotionEventCompat;

public class OverScrollScrollView extends ScrollView {
	private OverScrollListener overScrollListener = null;
	
	public OverScrollScrollView(Context context) {
		super(context);
	}
	
	public OverScrollScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
	}
	
	float oldY;
	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			//Recording the position to calculate the delta
			oldY = motionEvent.getY();
		} else if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
			//Calculating the deltas and reassigning the old value
			float deltaY = motionEvent.getY() - oldY;
			oldY = motionEvent.getY();
			
			//Checking if the list has reached its scroll limit
			if((!canScrollVertically(deltaY < 0 ? 1 : -1))) {
				if(overScrollListener != null) overScrollListener.onOverScroll(deltaY);
			}
		}
		
		return super.onTouchEvent(motionEvent);
	}
	
	boolean spoofVertical = false;
	@Override
	public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
		spoofVertical = true; //The scroll view will ignore all touch events if it can't scroll, which we don't want
		return super.onInterceptTouchEvent(motionEvent);
	}
	
	@Override
	public boolean canScrollVertically(int direction) {
		if(spoofVertical) {
			spoofVertical = false;
			return true;
		}
		return super.canScrollVertically(direction);
	}
	
	public void setOverScrollListener(OverScrollListener overScrollListener) {
		this.overScrollListener = overScrollListener;
	}
	
	public static abstract class OverScrollListener {
		public abstract void onOverScroll(float scrollY);
	}
}