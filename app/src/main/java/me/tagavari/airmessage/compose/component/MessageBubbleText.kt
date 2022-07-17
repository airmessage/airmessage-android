package me.tagavari.airmessage.compose.component

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import com.google.android.material.textview.MaterialTextView
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.LinkifyHelper
import me.tagavari.airmessage.util.CustomTabsLinkTransformationMethod
import me.tagavari.airmessage.util.MessagePartFlow

@Composable
fun MessageBubbleText(
	flow: MessagePartFlow,
	subject: String? = null,
	text: String? = null,
	tintRatio: Float = 0F
) {
	val colorScheme = MaterialTheme.colorScheme
	val (backgroundBubbleColor, bubbleOnBackgroundColor) = if(flow.isOutgoing) {
		val background = ColorUtils.blendARGB(
			colorScheme.secondary.toArgb(),
			colorScheme.primary.toArgb(),
			tintRatio
		).let { Color(it) }
		
		val foreground = ColorUtils.blendARGB(
			colorScheme.onSecondary.toArgb(),
			colorScheme.onPrimary.toArgb(),
			tintRatio
		).let { Color(it) }
		
		Pair(background, foreground)
	} else {
		Pair(colorScheme.surfaceVariant, colorScheme.onSurfaceVariant)
	}
	
	Surface(
		color = backgroundBubbleColor,
		shape = flow.bubbleShape
	) {
		Column(
			modifier = Modifier
				.heightIn(min = 40.dp)
				.padding(horizontal = 12.dp, vertical = 8.dp),
			verticalArrangement = Arrangement.Center
		) {
			val typography = MaterialTheme.typography.bodyLarge
			
			//Subject text
			if(subject != null) {
				Text(
					text = subject,
					style = typography.copy(fontWeight = FontWeight.Bold)
				)
				
				Spacer(modifier = Modifier.height(8.dp))
			}
			
			//Body text
			if(text != null) {
				val trimmedText = text.trim()
				
				val context = LocalContext.current
				val linkifiedText by produceState(initialValue = SpannableString(trimmedText), trimmedText) {
					value = LinkifyHelper.linkifyText(context, trimmedText)
				}
				
				//Use Android text view for Spannable support
				val textFont = MaterialTheme.typography.bodyLarge.fontSize
				assert(textFont.isSp) { "Text font is not in SP units!" }
				AndroidView(
					factory = { ctx ->
						MaterialTextView(ctx).apply {
							transformationMethod = CustomTabsLinkTransformationMethod()
							movementMethod = LinkMovementMethod.getInstance()
							textSize = textFont.value
						}
					},
					update = { view ->
						//Set view color
						val color = bubbleOnBackgroundColor.let { android.graphics.Color.rgb(it.red, it.green, it.blue) }
						view.setTextColor(color)
						view.setLinkTextColor(color)
						
						//Set linkified text
						view.text = linkifiedText
					}
				)
			}
		}
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleText() {
	AirMessageAndroidTheme {
		MessageBubbleText(
			flow = MessagePartFlow(
				isOutgoing = false,
				anchorBottom = false,
				anchorTop = false
			),
			text = "Cats are cool"
		)
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleSubject() {
	AirMessageAndroidTheme {
		MessageBubbleText(
			flow = MessagePartFlow(
				isOutgoing = false,
				anchorBottom = false,
				anchorTop = false
			),
			subject = "An important message",
			text = "Hello there friend!"
		)
	}
}
