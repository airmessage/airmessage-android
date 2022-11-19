package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.AddressHelper

@Composable
fun AddressRowDirect(
	modifier: Modifier = Modifier,
	address: String,
	onClick: () -> Unit
) {
	val formattedAddress = remember(address) {
		AddressHelper.formatAddress(address)
	}
	
	Row(
		modifier = modifier
			.clickable(onClick = onClick)
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
			.height(56.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Image(
			modifier = Modifier
				.size(40.dp)
				.clip(CircleShape),
			painter = painterResource(id = R.drawable.user),
			contentDescription = null,
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary, BlendMode.Multiply)
		)
		
		Spacer(modifier = Modifier.width(16.dp))
		
		Text(
			text = stringResource(R.string.action_sendto, formattedAddress),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}

@Composable
@Preview
private fun PreviewAddressRowDirect() {
	AirMessageAndroidTheme {
		Surface {
			AddressRowDirect(
				address = "hello@airmessage.org",
				onClick = {}
			)
		}
	}
}
