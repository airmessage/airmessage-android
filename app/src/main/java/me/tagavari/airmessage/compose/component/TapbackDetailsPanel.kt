package me.tagavari.airmessage.compose.component

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.remember.deriveUserInfo
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.ImmutableHolder
import me.tagavari.airmessage.compose.util.wrapImmutableHolder
import me.tagavari.airmessage.enums.TapbackType
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.messaging.TapbackInfo
import soup.compose.material.motion.MaterialSharedAxisX

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TapbackDetailsPanel(
	modifier: Modifier = Modifier,
	tapbacks: ImmutableHolder<Collection<TapbackInfo>>
) {
	var selectedTapbackType by remember { mutableStateOf<Int?>(null) }
	
	//Group tapbacks by type
	val sortedTapbacks = remember(tapbacks) {
		tapbacks.item.groupBy { it.code }
	}
	
	ElevatedCard(
		modifier = modifier
	) {
		//Convert group tapbacks to reaction groups
		val reactionGroupsMap = remember(sortedTapbacks) {
			sortedTapbacks.mapValues { (tapbackCode, tapbackList) ->
				ReactionGroup(
					code = tapbackCode,
					senders = tapbackList.map { it.sender }
				)
			}
		}
		
		//Resolve the selected reaction group
		val selectedReactionGroup = selectedTapbackType?.let { reactionGroupsMap[it] }
		
		MaterialSharedAxisX(
			targetState = selectedReactionGroup,
			forward = selectedReactionGroup != null
		) { reactionGroup ->
			if(reactionGroup == null) {
				val reactionGroupsList = remember(reactionGroupsMap) {
					reactionGroupsMap.values.wrapImmutableHolder()
				}
				
				TapbackDetailsMasterPanel(
					reactionGroups = reactionGroupsList,
					onClick = { selectedTapbackType = it }
				)
			} else {
				TapbackDetailsDetailPanel(
					reactionGroup = reactionGroup,
					onClose = { selectedTapbackType = null }
				)
			}
		}
	}
}

@Immutable
private data class ReactionGroup(
	@TapbackType val code: Int,
	val senders: Collection<String?>
)

@Composable
private fun TapbackDetailsMasterPanel(
	reactionGroups: ImmutableHolder<Collection<ReactionGroup>>,
	onClick: (Int) -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(76.dp)
			.horizontalScroll(state = rememberScrollState())
			.padding(horizontal = 32.dp, vertical = 4.dp),
		horizontalArrangement = Arrangement.spacedBy(space = 32.dp, alignment = Alignment.CenterHorizontally),
		verticalAlignment = Alignment.CenterVertically
	) {
		reactionGroups.item.forEach { reactionGroup ->
			Column(
				modifier = Modifier
					.clip(RoundedCornerShape(12.dp))
					.clickable { onClick(reactionGroup.code) }
					.padding(4.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				val userInfo by deriveUserInfo(reactionGroup.senders.first())
				
				MemberImage(
					modifier = Modifier.size(40.dp),
					color = MaterialTheme.colorScheme.primary,
					thumbnailURI = userInfo?.thumbnailURI.wrapImmutableHolder()
				)
				
				Spacer(modifier = Modifier.height(4.dp))
				
				Text(
					text = "${reactionGroup.senders.size} ${LanguageHelper.getTapbackEmoji(reactionGroup.code) ?: ""}",
					style = MaterialTheme.typography.labelSmall,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}

@Composable
private fun TapbackDetailsDetailPanel(
	reactionGroup: ReactionGroup,
	onClose: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(76.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		//Tapback type and count
		Text(
			modifier = Modifier.padding(start = 16.dp, end = 2.dp),
			text = "${reactionGroup.senders.size} ${LanguageHelper.getTapbackEmoji(reactionGroup.code) ?: ""}",
			style = MaterialTheme.typography.labelLarge
		)
		
		//User list
		Row(
			modifier = Modifier
				.horizontalScroll(state = rememberScrollState())
				.weight(1F),
			horizontalArrangement = Arrangement.Center,
			verticalAlignment = Alignment.CenterVertically
		) {
			reactionGroup.senders.forEach { sender ->
				Column(
					modifier = Modifier
						.width(80.dp)
						.padding(vertical = 4.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					val userInfo by deriveUserInfo(sender)
					
					MemberImage(
						modifier = Modifier.size(40.dp),
						color = MaterialTheme.colorScheme.primary,
						thumbnailURI = userInfo?.thumbnailURI.wrapImmutableHolder()
					)
					
					Spacer(modifier = Modifier.height(4.dp))
					
					Text(
						text = userInfo?.contactName ?: (sender ?: stringResource(id = R.string.part_you)),
						style = MaterialTheme.typography.labelSmall,
						textAlign = TextAlign.Center,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis
					)
				}
			}
		}
		
		//Close button
		IconButton(
			onClick = onClose
		) {
			Icon(
				imageVector = Icons.Default.Close,
				contentDescription = stringResource(R.string.action_close)
			)
		}
	}
}

@Composable
@Preview(widthDp = 300)
private fun PreviewTapbackDetailsPanel() {
	AirMessageAndroidTheme {
		TapbackDetailsPanel(
			modifier = Modifier.fillMaxWidth(),
			tapbacks = listOf(
				TapbackInfo(localID = 0, sender = null, code = TapbackType.like),
				TapbackInfo(localID = 1, sender = "person1@airmessage.org", code = TapbackType.like),
				TapbackInfo(localID = 2, sender = "person2@airmessage.org", code = TapbackType.heart)
			).wrapImmutableHolder()
		)
	}
}

@Composable
@Preview(widthDp = 300)
private fun PreviewTapbackDetailsDetailPanel() {
	AirMessageAndroidTheme {
		ElevatedCard {
			TapbackDetailsDetailPanel(
				reactionGroup = ReactionGroup(
					code = TapbackType.like,
					senders = listOf(null, "person1@airmessage.org", "person2@airmessage.org")
				),
				onClose = {}
			)
		}
	}
}
