package me.tagavari.airmessage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import me.tagavari.airmessage.R;

@Deprecated
public class RoundedImageView extends AppCompatImageView {
	private float[] radii = new float[8];
	
	public RoundedImageView(Context context) {
		super(context);
	}
	
	public RoundedImageView(Context context, AttributeSet attrs) {
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
	
	public float[] getRadiiRaw() {
		return radii;
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