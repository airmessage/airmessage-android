package me.tagavari.airmessage.compose.component

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.textview.MaterialTextView
import me.tagavari.airmessage.helper.LinkifyHelper
import me.tagavari.airmessage.util.CustomTabsLinkTransformationMethod
import me.tagavari.airmessage.util.MessagePartFlow

@Composable
fun MessageBubbleText(
	flow: MessagePartFlow,
	subject: String? = null,
	text: String? = null
) {
	val backgroundBubbleColor = if(flow.isOutgoing) {
		MaterialTheme.colorScheme.primary
	} else {
		MaterialTheme.colorScheme.surfaceVariant
	}
	
	Surface(
		color = backgroundBubbleColor,
		shape = flow.bubbleShape
	) {
		Column {
			//Subject text
			if(subject != null) {
				Text(
					text = subject,
					fontWeight = FontWeight.Bold
				)
			}
			
			//Body text
			if(text != null) {
				val context = LocalContext.current
				val linkifiedText by produceState(initialValue = SpannableString(text), text) {
					value = LinkifyHelper.linkifyText(context, text)
				}
				
				//Use Android text view for Spannable support
				AndroidView(
					factory = { ctx ->
						MaterialTextView(ctx).apply {
							transformationMethod = CustomTabsLinkTransformationMethod()
							movementMethod = LinkMovementMethod.getInstance()
						}
					},
					update = { view ->
						view.text = linkifiedText
					}
				)
			}
		}
	}
}