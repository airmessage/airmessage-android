package me.tagavari.airmessage.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class RoundedImageView extends AppCompatImageView {
	private float[] radii = new float[8];
	
	public RoundedImageView(Context context) {
		super(context);
	}
	
	public RoundedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
		//float radius = 36.0f;
		clipPath.reset();
		rect.set(0, 0, this.getWidth(), this.getHeight());
		
		clipPath.addRoundRect(rect, radii, Path.Direction.CW);
		canvas.clipPath(clipPath);
		super.onDraw(canvas);
	}
}