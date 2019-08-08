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
	private float[] radii = new float[8];
	
	private RectF rectF;
	private Path path = new Path();
	
	public RoundedFrameLayout(Context context) {
		super(context);
	}
	
	public RoundedFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundedView, 0, 0);
		
		try {
			float radius = typedArray.getDimensionPixelSize(R.styleable.RoundedView_radius, -1);
			if(radius != -1) {
				for(int i = 0; i < radii.length; i++) radii[i] = radius;
			} else {
				float radiusTop = typedArray.getDimensionPixelSize(R.styleable.RoundedView_radiusTop, 0);
				for(int i = 0; i < 4; i++) radii[i] = radiusTop;
				float radiusBottom = typedArray.getDimensionPixelSize(R.styleable.RoundedView_radiusBottom, 0);
				for(int i = 4; i < 8; i++) radii[i] = radiusBottom;
			}
		} finally {
			typedArray.recycle();
		}
	}
	
	public void setRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
		//Setting the radii
		radii = new float[]{topLeft, topLeft,
							topRight, topRight,
							bottomRight, bottomRight,
							bottomLeft, bottomLeft};
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
		path.addRoundRect(rectF, radii, Path.Direction.CW);
		path.close();
	}
}