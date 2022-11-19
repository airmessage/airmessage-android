package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.remember.deriveUserInfo
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.wrapImmutableHolder
import me.tagavari.airmessage.data.UserCacheHelper
import me.tagavari.airmessage.messaging.MemberInfo

/**
 * A row for details about a specific member
 */
@Composable
fun ConversationDetailsMember(
	modifier: Modifier = Modifier,
	member: MemberInfo,
	onClick: (MemberInfo, UserCacheHelper.UserInfo?) -> Unit,
) {
	//Load the message contact
	val userInfo by deriveUserInfo(member.address)
	
	Row(
		modifier = modifier
			.clickable(onClick = { onClick(member, userInfo) })
			.padding(horizontal = 16.dp)
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		MemberImage(
			modifier = Modifier.size(40.dp),
			color = Color(member.color),
			thumbnailURI = userInfo?.thumbnailURI.wrapImmutableHolder()
		)
		
		Spacer(modifier = Modifier.width(16.dp))
		
		Text(userInfo?.contactName ?: member.address)
	}
}

@Preview(widthDp = 384)
@Composable
fun ConversationDetailsMemberPreview() {
	AirMessageAndroidTheme {
		Surface {
			ConversationDetailsMember(
				member = MemberInfo("example@airmessage.org", 0xFFFF1744.toInt()),
				onClick = { _, _ -> }
			)
		}
	}
}