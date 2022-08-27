package me.tagavari.airmessage.compose.component

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R

@Composable
fun MessageInputBarAudio(
	duration: Int
) {
	Row(
		modifier = Modifier
			.height(40.dp)
			.fillMaxWidth(),
		horizontalArrangement = Arrangement.End,
		verticalAlignment = Alignment.Bottom
	) {
		Surface(
			modifier = Modifier.fillMaxWidth(0.6F),
			shape = RoundedCornerShape(100),
			tonalElevation = 4.dp
		) {
			Row(
				modifier = Modifier.padding(4.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Box(
					modifier = Modifier
						.weight(1F)
						.height(4.dp)
						.background(Color.White)
				)
				
				Text(
					text = remember(duration) {
						DateUtils.formatElapsedTime(duration.toLong())
					}
				)
			}
		}
		Spacer(modifier = Modifier.width(8.dp))
		
		Surface(
			modifier = Modifier.wrapContentHeight(align = Alignment.Bottom, unbounded = true),
			shape = RoundedCornerShape(100),
			tonalElevation = 4.dp
		) {
			Column(modifier = Modifier.padding(8.dp)) {
				Icon(
					modifier = Modifier.size(48.dp),
					painter = painterResource(id = R.drawable.push_rounded),
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary
				)
				
				Spacer(modifier = Modifier.height(48.dp))
				
				Icon(
					modifier = Modifier.size(48.dp),
					painter = painterResource(id = R.drawable.play_circle_rounded),
					contentDescription = null,
					tint = MaterialTheme.colorScheme.primary
				)
			}
		}
	}
}
