package me.tagavari.airmessage.compose.component

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
	val (backgroundBubbleColor, bubbleOnBackgroundColor) = if(flow.isOutgoing) {
		Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
	} else {
		Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
	}
	
	Surface(
		color = backgroundBubbleColor,
		shape = flow.bubbleShape
	) {
		Column(
			modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
		) {
			//Subject text
			if(subject != null) {
				Text(
					text = subject,
					fontWeight = FontWeight.Bold
				)
				
				Spacer(modifier = Modifier.height(8.dp))
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
						view.setTextColor(
							bubbleOnBackgroundColor.let { android.graphics.Color.rgb(it.red, it.green, it.blue) }
						)
						view.text = linkifiedText
					}
				)
			}
		}
	}
}