package me.tagavari.airmessage.fragment

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import me.tagavari.airmessage.R
import me.tagavari.airmessage.data.SharedPreferencesManager
import me.tagavari.airmessage.extension.FragmentCommunicationFaceTime
import java.nio.charset.StandardCharsets

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
		webView.webViewClient = object : WebViewClient() {
			override fun onPageFinished(view: WebView, url: String) {
				//Inject function JavaScript
				val rawJS = resources.openRawResource(R.raw.facetime_inject_javascript).bufferedReader().use { it.readText() }
				val rawCSS = resources.openRawResource(R.raw.facetime_inject_css).bufferedReader().use { it.readText() }
				
				val userName = escapeStringJS(SharedPreferencesManager.getServerUserName(requireContext()))
				val cssBase64 = Base64.encodeToString(rawCSS.encodeToByteArray(), Base64.NO_WRAP)
				webView.evaluateJavascript(String.format(rawJS, userName, cssBase64), null)
			}
		}
		webView.webChromeClient = object : WebChromeClient() {
			override fun onPermissionRequest(request: PermissionRequest) {
				//Grant all permissions
				request.grant(request.resources)
			}
		}
		webView.loadUrl(faceTimeLink)
	}
	
	companion object {
		const val PARAM_LINK = "link"
		
		private fun escapeStringJS(string: String): String {
			return string
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\b", "\\b")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t")
		}
	}
}