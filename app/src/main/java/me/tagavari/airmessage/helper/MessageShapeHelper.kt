package me.tagavari.airmessage.helper

import android.content.res.Resources
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import me.tagavari.airmessage.R

object MessageShapeHelper {
	/**
	 * Creates a shape appearance model for a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [ShapeAppearanceModel]
	 */
	@JvmStatic
	fun createRoundedMessageAppearance(resources: Resources, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean): ShapeAppearanceModel {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, if(anchoredTop) radiusAnchored else radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, if(anchoredBottom) radiusAnchored else radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.build()
		} else {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, if(anchoredTop) radiusAnchored else radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, if(anchoredBottom) radiusAnchored else radiusUnanchored)
					.build()
		}
	}
	
	/**
	 * Creates a shape appearance model for the top half of a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [ShapeAppearanceModel]
	 */
	fun createRoundedMessageAppearanceTop(resources: Resources, anchoredTop: Boolean, alignToRight: Boolean): ShapeAppearanceModel {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, if(anchoredTop) radiusAnchored else radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, 0F)
					.setBottomLeftCorner(CornerFamily.ROUNDED, 0F)
					.build()
		} else {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, if(anchoredTop) radiusAnchored else radiusUnanchored)
					.setTopRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomRightCorner(CornerFamily.ROUNDED, 0F)
					.setBottomLeftCorner(CornerFamily.ROUNDED, 0F)
					.build()
		}
	}
	
	/**
	 * Creates a shape appearance model for the bottom half of a message bubble to be used on any view
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [ShapeAppearanceModel]
	 */
	@JvmStatic
	fun createRoundedMessageAppearanceBottom(resources: Resources, anchoredBottom: Boolean, alignToRight: Boolean): ShapeAppearanceModel {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, 0F)
					.setTopRightCorner(CornerFamily.ROUNDED, 0F)
					.setBottomRightCorner(CornerFamily.ROUNDED, if(anchoredBottom) radiusAnchored else radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.build()
		} else {
			ShapeAppearanceModel.builder()
					.setTopLeftCorner(CornerFamily.ROUNDED, 0F)
					.setTopRightCorner(CornerFamily.ROUNDED, 0F)
					.setBottomRightCorner(CornerFamily.ROUNDED, radiusUnanchored)
					.setBottomLeftCorner(CornerFamily.ROUNDED, if(anchoredBottom) radiusAnchored else radiusUnanchored)
					.build()
		}
	}
	
	/**
	 * Creates an array of radii for use with paint views such as [me.tagavari.airmessage.view.InvisibleInkView]
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	@JvmStatic
	fun createStandardRadiusArray(resources: Resources, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean): FloatArray {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		val radiusTop = if(anchoredTop) radiusAnchored else radiusUnanchored
		val radiusBottom = if(anchoredBottom) radiusAnchored else radiusUnanchored
		return if(alignToRight) {
			floatArrayOf(
					radiusUnanchored, radiusUnanchored,
					radiusTop, radiusTop,
					radiusBottom, radiusBottom,
					radiusUnanchored, radiusUnanchored
					)
		} else {
			floatArrayOf(
					radiusTop, radiusTop,
					radiusUnanchored, radiusUnanchored,
					radiusUnanchored, radiusUnanchored,
					radiusBottom, radiusBottom
					)
		}
	}
	
	/**
	 * Creates an array of radii for use with upper paint views such as [me.tagavari.airmessage.view.InvisibleInkView]
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	@JvmStatic
	fun createStandardRadiusArrayTop(resources: Resources, anchoredTop: Boolean, alignToRight: Boolean): FloatArray {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		val radiusTop = if(anchoredTop) radiusAnchored else radiusUnanchored
		return if(alignToRight) {
			floatArrayOf(
					radiusUnanchored, radiusUnanchored,
					radiusTop, radiusTop, 0F, 0F, 0F, 0F)
		} else {
			floatArrayOf(
					radiusTop, radiusTop,
					radiusUnanchored, radiusUnanchored, 0F, 0F, 0F, 0F)
		}
	}
	
	/**
	 * Creates an array of radii for use lower with paint views such as [me.tagavari.airmessage.view.InvisibleInkView]
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return An array of radii values
	 */
	fun createStandardRadiusArrayBottom(resources: Resources, anchoredBottom: Boolean, alignToRight: Boolean): FloatArray {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		val radiusBottom = if(anchoredBottom) radiusAnchored else radiusUnanchored
		return if(alignToRight) {
			floatArrayOf(0F, 0F, 0F, 0F,
					radiusBottom, radiusBottom,
					radiusUnanchored, radiusUnanchored
					)
		} else {
			floatArrayOf(0F, 0F, 0F, 0F,
					radiusUnanchored, radiusUnanchored,
					radiusBottom, radiusBottom
					)
		}
	}
	
	/**
	 * Creates a rounded drawable for use with a message bubble
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [MaterialShapeDrawable]
	 */
	@JvmStatic
	fun createRoundedMessageDrawable(resources: Resources, anchoredTop: Boolean, anchoredBottom: Boolean, alignToRight: Boolean): MaterialShapeDrawable {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			createRoundedDrawable(
					radiusUnanchored,
					if(anchoredTop) radiusAnchored else radiusUnanchored,
					if(anchoredBottom) radiusAnchored else radiusUnanchored,
					radiusUnanchored
			)
		} else {
			createRoundedDrawable(
					if(anchoredTop) radiusAnchored else radiusUnanchored,
					radiusUnanchored,
					radiusUnanchored,
					if(anchoredBottom) radiusAnchored else radiusUnanchored
			)
		}
	}
	
	/**
	 * Creates a rounded drawable for use with a the top half of a message bubble
	 * @param resources The resources to use
	 * @param anchoredTop Whether this view is anchored to a message above
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [MaterialShapeDrawable]
	 */
	@JvmStatic
	fun createRoundedMessageDrawableTop(resources: Resources, anchoredTop: Boolean, alignToRight: Boolean): MaterialShapeDrawable {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			createRoundedDrawable(
					radiusUnanchored,
					if(anchoredTop) radiusAnchored else radiusUnanchored,
					0F,
					0F
			)
		} else {
			createRoundedDrawable(
					if(anchoredTop) radiusAnchored else radiusUnanchored,
					radiusUnanchored,
					0F,
					0F
			)
		}
	}
	
	/**
	 * Creates a rounded drawable for use with the bottom half of a message bubble
	 * @param resources The resources to use
	 * @param anchoredBottom Whether this view is anchored to a message below
	 * @param alignToRight Whether this view is aligned to the right edge of the screen
	 * @return A rounded [MaterialShapeDrawable]
	 */
	fun createRoundedMessageDrawableBottom(resources: Resources, anchoredBottom: Boolean, alignToRight: Boolean): MaterialShapeDrawable {
		val radiusUnanchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius).toFloat()
		val radiusAnchored = resources.getDimensionPixelSize(R.dimen.messagebubble_radius_anchored).toFloat()
		return if(alignToRight) {
			createRoundedDrawable(
					0F,
					0F,
					if(anchoredBottom) radiusAnchored else radiusUnanchored,
					radiusUnanchored
			)
		} else {
			createRoundedDrawable(
					0F,
					0F,
					radiusUnanchored,
					if(anchoredBottom) radiusAnchored else radiusUnanchored
			)
		}
	}
	
	/**
	 * Creates a rounded drawable
	 * @param radiusTopLeft The corner radius of the top-left corner
	 * @param radiusTopRight The corner radius of the top-right corner
	 * @param radiusBottomRight The corner radius of the bottom-right corner
	 * @param radiusBottomLeft The corner radius of the bottom-left corner
	 * @return A rounded [MaterialShapeDrawable]
	 */
	private fun createRoundedDrawable(radiusTopLeft: Float, radiusTopRight: Float, radiusBottomRight: Float, radiusBottomLeft: Float): MaterialShapeDrawable {
		val shapeAppearanceModel = ShapeAppearanceModel.builder()
				.setTopLeftCorner(CornerFamily.ROUNDED, radiusTopLeft)
				.setTopRightCorner(CornerFamily.ROUNDED, radiusTopRight)
				.setBottomRightCorner(CornerFamily.ROUNDED, radiusBottomRight)
				.setBottomLeftCorner(CornerFamily.ROUNDED, radiusBottomLeft)
				.build()
		return MaterialShapeDrawable(shapeAppearanceModel)
	}
}