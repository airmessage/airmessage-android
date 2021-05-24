package me.tagavari.airmessage.messaging.viewholder

import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.annotation.IdRes
import me.tagavari.airmessage.util.DisposableViewHolder

class VHMessageStructure(
	itemView: View,
	val labelTimeDivider: TextView,
	val labelSender: TextView,
	private var profileStub: ViewStub?,
	val containerMessagePart: ViewGroup,
	@field:IdRes @param:IdRes private val idProfileDefault: Int,
	@field:IdRes @param:IdRes private val idProfileImage: Int,
	val labelActivityStatus: TextSwitcher,
	val buttonSendEffectReplay: View,
	val buttonSendError: ImageButton
) : DisposableViewHolder(itemView) {
	var isProfileInflated = false; private set
	
	@JvmField var profileGroup: ViewGroup? = null
	@JvmField var profileDefault: ImageView? = null
	@JvmField var profileImage: ImageView? = null
	@JvmField val messageComponents: MutableList<VHMessageComponent> = mutableListOf()
	
	/**
	 * Inflates the profile view stub of this view
	 */
	fun inflateProfile() {
		if(isProfileInflated) return
		
		profileGroup = profileStub!!.inflate() as ViewGroup
		profileStub = null
		
		val profileGroup = profileGroup!!
		profileDefault = profileGroup.findViewById(idProfileDefault)
		profileImage = profileGroup.findViewById(idProfileImage)
		isProfileInflated = true
	}
}