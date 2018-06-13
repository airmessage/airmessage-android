package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ScrollView;

import me.tagavari.airmessage.R;

public class RoundedScrollView extends ScrollView {
	//Creating the preference values
	private float[] radii = new float[8];
	
	public RoundedScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundedView, 0, 0);
		
		try {
			float radius = typedArray.getDimensionPixelSize(R.styleable.RoundedView_radii, -1);
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
	
	private Path clipPath = new Path();
	private RectF rect = new RectF();
	@Override
	protected void onDraw(Canvas canvas) {
		clipPath.reset();
		rect.set(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
		
		clipPath.addRoundRect(rect, radii, Path.Direction.CW);
		canvas.clipPath(clipPath);
		super.onDraw(canvas);
	}
}