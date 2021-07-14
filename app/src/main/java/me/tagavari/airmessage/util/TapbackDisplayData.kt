package me.tagavari.airmessage.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * A class that holds display data required to render a tapback
 * @param iconResource The tapback's icon
 * @param color The tapback's color
 * @param label The tapback's title
 */
class TapbackDisplayData(
	@field:DrawableRes @get:DrawableRes @param:DrawableRes val iconResource: Int,
	@field:ColorRes @get:ColorRes @param:ColorRes val color: Int,
	@field:StringRes @get:StringRes @param:StringRes val label: Int
)