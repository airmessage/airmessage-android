package me.tagavari.airmessage.fragment

import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import me.tagavari.airmessage.R
import me.tagavari.airmessage.extension.FragmentCommunicationFaceTime

class FragmentCallActive : FragmentCommunication<FragmentCommunicationFaceTime>(R.layout.fragment_callactive) {
	//Parameters
	private lateinit var faceTimeLink: String
	
	//Views
	private lateinit var webView: WebView
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		faceTimeLink = requireArguments().getString(PARAM_LINK)!!
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		//Get the views
		webView = view.findViewById(R.id.webview)
		webView.settings.javaScriptEnabled = true
		webView.webChromeClient = object : WebChromeClient() {
			override fun onPermissionRequest(request: PermissionRequest) {
				request.grant(request.resources)
			}
		}
		webView.loadUrl(faceTimeLink)
	}
	
	companion object {
		const val PARAM_LINK = "link"
	}
}