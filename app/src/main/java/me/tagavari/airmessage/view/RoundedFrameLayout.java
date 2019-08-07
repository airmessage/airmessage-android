package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.FrameLayout;

import me.tagavari.airmessage.Constants;
import me.tagavari.airmessage.R;

public class RoundedFrameLayout extends FrameLayout {
	private static final float defaultRadius = 4F;
	private float cornerRadius;
	
	private RectF rectF;
	private Path path = new Path();
	
	public RoundedFrameLayout(Context context) {
		super(context);
		init(context, null, 0);
	}
	
	public RoundedFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {
		//Setting the default radius
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		float radius = defaultRadius;
		
		if(attrs != null) {
			TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundedViewSimple, 0, 0);
			
			try {
				radius = typedArray.getDimension(R.styleable.RoundedViewSimple_radius, radius);
			} finally {
				typedArray.recycle();
			}
		}
		cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, defaultRadius, metrics);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		rectF = new RectF(0, 0, w, h);
		resetPath();
	}
	
	@Override
	public void draw(Canvas canvas) {
		int save = canvas.save();
		canvas.clipPath(path);
		super.draw(canvas);
		canvas.restoreToCount(save);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		int save = canvas.save();
		canvas.clipPath(path);
		super.dispatchDraw(canvas);
		canvas.restoreToCount(save);
	}
	
	private void resetPath() {
		path.reset();
		path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
		path.close();
	}
}