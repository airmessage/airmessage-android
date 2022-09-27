package me.tagavari.airmessage.compose.component

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.textview.MaterialTextView
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.LinkifyHelper
import me.tagavari.airmessage.helper.SendStyleHelper
import me.tagavari.airmessage.helper.StringHelper
import me.tagavari.airmessage.util.CustomTabsLinkTransformationMethod
import me.tagavari.airmessage.util.MessageFlowRadius
import me.tagavari.airmessage.util.MessagePartFlow

/**
 * A message bubble that displays a text message
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleText(
	flow: MessagePartFlow,
	subject: String? = null,
	text: String? = null,
	sendStyle: String? = null,
	onSetSelected: (Boolean) -> Unit
) {
	val haptic = LocalHapticFeedback.current
	val colors = flow.colors
	
	fun onClick() {
		//Deselect with a single tap
		if(flow.isSelected) {
			onSetSelected(false)
		}
	}
	
	fun onLongClick() {
		//Toggle selection
		onSetSelected(!flow.isSelected)
	}
	
	val isSingleEmoji = remember(subject, text) {
		subject == null && text != null && StringHelper.stringContainsOnlyEmoji(text)
	}
	
	val isInvisibleInk = sendStyle == SendStyleHelper.appleSendStyleBubbleInvisibleInk
	
	if(isSingleEmoji) {
		Text(
			modifier = if(flow.isSelected) {
				Modifier.background(colors.background, RoundedCornerShape(MessageFlowRadius.large))
			} else {
				Modifier
			}
				.combinedClickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = ::onClick,
					onLongClick = {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onLongClick()
					}
				),
			text = text!!,
			fontSize = 48.sp
		)
	} else {
		Surface(
			modifier = Modifier
				.width(IntrinsicSize.Min)
				.height(IntrinsicSize.Min),
			color = colors.background,
			shape = flow.bubbleShape,
			contentColor = colors.foreground
		) {
			val invisibleInkState = rememberInvisibleInkState(isInvisibleInk)
			
			Column(
				modifier = Modifier
					.heightIn(min = 40.dp)
					.padding(horizontal = 12.dp, vertical = 8.dp)
					.alpha(invisibleInkState.contentAlpha),
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
							val color = colors.foreground.toArgb()
							view.setTextColor(color)
							view.setLinkTextColor(color)
							
							//Set linkified text
							view.text = linkifiedText
							
							if(flow.isSelected) {
								view.setOnClickListener {
									onClick()
								}
							} else {
								view.isClickable = false
							}
							
							view.setOnLongClickListener {
								onLongClick()
								true
							}
						}
					)
				}
			}
			
			if(isInvisibleInk) {
				InvisibleInk(
					modifier = Modifier
						.alpha(1F - invisibleInkState.contentAlpha)
						.pointerInteropFilter { event ->
							if(event.action == MotionEvent.ACTION_DOWN) {
								invisibleInkState.reveal()
							}
							return@pointerInteropFilter false
						}
						.fillMaxSize()
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
				isSelected = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			text = "Cats are cool",
			onSetSelected = {}
		)
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleSubject() {
	AirMessageAndroidTheme {
		MessageBubbleText(
			flow = MessagePartFlow(
				isOutgoing = true,
				isSelected = true,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			subject = "An important message",
			text = "Hello there friend!",
			onSetSelected = {}
		)
	}
}

@Preview
@Composable
private fun PreviewMessageBubbleEmoji() {
	AirMessageAndroidTheme {
		MessageBubbleText(
			flow = MessagePartFlow(
				isOutgoing = true,
				isSelected = false,
				anchorBottom = false,
				anchorTop = false,
				tintRatio = 0F
			),
			subject = null,
			text = "\ud83d\udc08",
			onSetSelected = {}
		)
	}
}
