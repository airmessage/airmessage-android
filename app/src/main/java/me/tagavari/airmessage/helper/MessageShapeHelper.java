package me.tagavari.airmessage.helper;

import android.content.res.Resources;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

import me.tagavari.airmessage.R;

public class MessageShapeHelper {
	/**
	 * Creates a shape appearance model for a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link ShapeAppearanceModel}
	 */
	public static ShapeAppearanceModel createRoundedMessageAppearance(Resources resources, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, anchoredTop ? radiusAnchored : radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, anchoredBottom ? radiusAnchored : radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.build();
		} else {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, anchoredTop ? radiusAnchored : radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, anchoredBottom ? radiusAnchored : radiusUnanchored)
					.build();
		}
	}
	
	/**
	 * Creates a shape appearance model for the top half of a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link ShapeAppearanceModel}
	 */
	public static ShapeAppearanceModel createRoundedMessageAppearanceTop(Resources resources, boolean anchoredTop, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, anchoredTop ? radiusAnchored : radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, 0)
					.setBottomLeftCorner(CornerFamily.ROUNDED, 0)
					.build();
		} else {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, anchoredTop ? radiusAnchored : radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, 0)
					.setBottomLeftCorner(CornerFamily.ROUNDED, 0)
					.build();
		}
	}
	
	/**
	 * Creates a shape appearance model for the bottom half of a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link ShapeAppearanceModel}
	 */
	public static ShapeAppearanceModel createRoundedMessageAppearanceBottom(Resources resources, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, 0)
					.setTopRightCorner(CornerFamily.ROUNDED, 0)
					.setBottomRightCorner(CornerFamily.ROUNDED, anchoredBottom ? radiusAnchored : radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.build();
		} else {
			return ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, 0)
					.setTopRightCorner(CornerFamily.ROUNDED, 0)
					.setBottomRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, anchoredBottom ? radiusAnchored : radiusUnanchored)
					.build();
		}
	}
	
	/**
	 * Creates an array of radii for use with paint views such as {@link me.tagavari.airmessage.view.InvisibleInkView}
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	public static float[] createStandardRadiusArray(Resources resources, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		int radiusTop = anchoredTop ? radiusAnchored : radiusUnanchored;
		int radiusBottom = anchoredBottom ? radiusAnchored : radiusUnanchored;
		
		if(alignToRight) {
			return new float[]{
					radiusUnanchored, radiusUnanchored,
					radiusTop, radiusTop,
					radiusBottom, radiusBottom,
					radiusUnanchored, radiusUnanchored
			};
		} else {
			return new float[]{
					radiusTop, radiusTop,
					radiusUnanchored, radiusUnanchored,
					radiusUnanchored, radiusUnanchored,
					radiusBottom, radiusBottom
			};
		}
	}
	
	/**
	 * Creates an array of radii for use with upper paint views such as {@link me.tagavari.airmessage.view.InvisibleInkView}
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	public static float[] createStandardRadiusArrayTop(Resources resources, boolean anchoredTop, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		int radiusTop = anchoredTop ? radiusAnchored : radiusUnanchored;
		
		if(alignToRight) {
			return new float[]{
					radiusUnanchored, radiusUnanchored,
					radiusTop, radiusTop,
					0, 0,
					0, 0
			};
		} else {
			return new float[]{
					radiusTop, radiusTop,
					radiusUnanchored, radiusUnanchored,
					0, 0,
					0, 0
			};
		}
	}
	
	/**
	 * Creates an array of radii for use lower with paint views such as {@link me.tagavari.airmessage.view.InvisibleInkView}
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	public static float[] createStandardRadiusArrayBottom(Resources resources, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		int radiusBottom = anchoredBottom ? radiusAnchored : radiusUnanchored;
		
		if(alignToRight) {
			return new float[]{
					0, 0,
					0, 0,
					radiusBottom, radiusBottom,
					radiusUnanchored, radiusUnanchored
			};
		} else {
			return new float[]{
					0, 0,
					0, 0,
					radiusUnanchored, radiusUnanchored,
					radiusBottom, radiusBottom
			};
		}
	}
	
	/**
	 * Creates a rounded drawable for use with a message bubble
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link MaterialShapeDrawable}
	 */
	public static MaterialShapeDrawable createRoundedMessageDrawable(Resources resources, boolean anchoredTop, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return createRoundedDrawable(
					radiusUnanchored,
					anchoredTop ? radiusAnchored : radiusUnanchored,
					anchoredBottom ? radiusAnchored : radiusUnanchored,
					radiusUnanchored
			);
		} else {
			return createRoundedDrawable(
					anchoredTop ? radiusAnchored : radiusUnanchored,
					radiusUnanchored,
					radiusUnanchored,
					anchoredBottom ? radiusAnchored : radiusUnanchored
			);
		}
	}
	
	/**
	 * Creates a rounded drawable for use with a the top half of a message bubble
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link MaterialShapeDrawable}
	 */
	public static MaterialShapeDrawable createRoundedMessageDrawableTop(Resources resources, boolean anchoredTop, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return createRoundedDrawable(
					radiusUnanchored,
					anchoredTop ? radiusAnchored : radiusUnanchored,
					0,
					0
			);
		} else {
			return createRoundedDrawable(
					anchoredTop ? radiusAnchored : radiusUnanchored,
					radiusUnanchored,
					0,
					0
			);
		}
	}
	
	/**
	 * Creates a rounded drawable for use with the bottom half of a message bubble
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded {@link MaterialShapeDrawable}
	 */
	public static MaterialShapeDrawable createRoundedMessageDrawableBottom(Resources resources, boolean anchoredBottom, boolean alignToRight) {
		int radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius);
		int radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored);
		
		if(alignToRight) {
			return createRoundedDrawable(
					0,
					0,
					anchoredBottom ? radiusAnchored : radiusUnanchored,
					radiusUnanchored
			);
		} else {
			return createRoundedDrawable(
					0,
					0,
					radiusUnanchored,
					anchoredBottom ? radiusAnchored : radiusUnanchored
			);
		}
	}
	
	/**
	 * Creates a rounded drawable
	 * @param radiusTopLeft The corner radius of the top-left corner
	 * @param radiusTopRight The corner radius of the top-right corner
	 * @param radiusBottomRight The corner radius of the bottom-right corner
	 * @param radiusBottomLeft The corner radius of the bottom-left corner
	 * @return A rounded {@link MaterialShapeDrawable}
	 */
	public static MaterialShapeDrawable createRoundedDrawable(int radiusTopLeft, int radiusTopRight, int radiusBottomRight, int radiusBottomLeft) {
		ShapeAppearanceModel shapeAppearanceModel = ShapeAppearanceModel.builder()
				.setTopLeftCorner(CornerFamily.ROUNDED, radiusTopLeft)
				.setTopRightCorner(CornerFamily.ROUNDED, radiusTopRight)
				.setBottomRightCorner(CornerFamily.ROUNDED, radiusBottomRight)
				.setBottomLeftCorner(CornerFamily.ROUNDED, radiusBottomLeft)
				.build();
		return new MaterialShapeDrawable(shapeAppearanceModel);
	}
}