package me.tagavari.airmessage.fragment

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

open class FragmentCommunication<A: Any> : Fragment {
	var communicationsCallback: A? = null
	
	constructor() : super()
	constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)
}