package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.messaging.MemberInfo

/**
 * An icon to represent a group of users
 */
@Composable
fun UserIconGroup(
	modifier: Modifier = Modifier,
	members: List<MemberInfo>,
	highRes: Boolean = false
) {
	Box(modifier.size(40.dp, 40.dp)) {
		when(members.size) {
			0 -> {}
			1 -> {
				MemberImage(
					member = members[0],
					modifier = Modifier.fillMaxSize(),
					highRes = highRes
				)
			}
			2 -> {
				val size = 0.575F
				
				MemberImage(
					member = members[0],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.BottomStart),
					highRes = highRes
				)
				MemberImage(
					member = members[1],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.TopEnd),
					highRes = highRes
				)
			}
			3 -> {
				val size = 0.475F
				
				MemberImage(
					member = members[0],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.TopCenter),
					highRes = highRes
				)
				MemberImage(
					member = members[1],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.BottomStart),
					highRes = highRes
				)
				MemberImage(
					member = members[2],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.BottomEnd),
					highRes = highRes
				)
			}
			else -> {
				val size = 0.4875F
				
				MemberImage(
					member = members[0],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.TopStart),
					highRes = highRes
				)
				MemberImage(
					member = members[1],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.TopEnd),
					highRes = highRes
				)
				MemberImage(
					member = members[2],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.BottomStart),
					highRes = highRes
				)
				MemberImage(
					member = members[3],
					modifier = Modifier
						.fillMaxSize(size)
						.align(Alignment.BottomEnd),
					highRes = highRes
				)
			}
		}
	}
}

@Preview
@Composable
private fun Preview1Member() {
	UserIconGroup(members = listOf(
		MemberInfo("1", 0xFFFF1744.toInt())
	))
}

@Preview
@Composable
private fun Preview2Members() {
	UserIconGroup(members = listOf(
		MemberInfo("1", 0xFFFF1744.toInt()),
		MemberInfo("2", 0xFFF50057.toInt())
	))
}

@Preview
@Composable
private fun Preview3Members() {
	UserIconGroup(members = listOf(
		MemberInfo("1", 0xFFFF1744.toInt()),
		MemberInfo("2", 0xFFF50057.toInt()),
		MemberInfo("3", 0xFFB317CF.toInt())
	))
}

@Preview
@Composable
private fun Preview4Members() {
	UserIconGroup(members = listOf(
		MemberInfo("1", 0xFFFF1744.toInt()),
		MemberInfo("2", 0xFFF50057.toInt()),
		MemberInfo("3", 0xFFB317CF.toInt()),
		MemberInfo("4", 0xFF703BE3.toInt())
	))
}
