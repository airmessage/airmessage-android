package me.tagavari.airmessage.common.compose.component

import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.R
import me.tagavari.airmessage.common.compose.util.ImmutableHolder
import me.tagavari.airmessage.common.compose.util.wrapImmutableHolder

/**
 * A pane that displays all libraries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesPane(
	onBack: () -> Unit
) {
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
	
	Scaffold(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		topBar = {
			TopAppBar(
				title = {
					Text(text = stringResource(id = R.string.screen_licenses))
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(
							imageVector = Icons.Filled.ArrowBack,
							contentDescription = stringResource(id = R.string.action_back)
						)
					}
				},
				scrollBehavior = scrollBehavior
			)
		},
		content = { innerPadding ->
			val context = LocalContext.current
			val libs by produceState<Libs?>(null) {
				value = withContext(Dispatchers.IO) {
					Libs.Builder().withContext(context).build()
				}
			}
			
			libs?.let { localLibs ->
				LibrariesList(
					modifier = Modifier.fillMaxSize(),
					contentPadding = innerPadding,
					libs = localLibs.wrapImmutableHolder()
				)
			}
		}
	)
}

/**
 * Displays all provided libraries in a simple list.
 */
@Composable
fun LibrariesList(
	modifier: Modifier = Modifier,
	lazyListState: LazyListState = rememberLazyListState(),
	contentPadding: PaddingValues = PaddingValues(0.dp),
	libs: ImmutableHolder<Libs>
) {
	var openDialog by rememberSaveable { mutableStateOf<Library?>(null) }
	
	LazyColumn(
		modifier = modifier,
		state = lazyListState,
		contentPadding = contentPadding
	) {
		items(
			items = libs.item.libraries,
			key = { it.uniqueId }
		) { library ->
			Row(
				modifier = Modifier
					.clickable {
						if(!library.licenses.firstOrNull()?.licenseContent.isNullOrBlank()) {
							openDialog = library
						}
					}
					.fillMaxWidth()
					.height(48.dp)
					.padding(horizontal = 16.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = library.name,
					modifier = Modifier.weight(1F),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
	}
	
	openDialog?.let { library ->
		LicenseDialog(
			library = library.wrapImmutableHolder(),
			onDismiss = { openDialog = null }
		)
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LicenseDialog(
	library: ImmutableHolder<Library>,
	onDismiss: () -> Unit,
) {
	@Suppress("NAME_SHADOWING")
	val library by library
	
	val license = library.licenses.firstOrNull()
	
	val scrollState = rememberScrollState()
	AlertDialog(
		title = license?.name?.let { { Text(it) } },
		onDismissRequest = {
			onDismiss()
		},
		confirmButton = {
			TextButton(onClick = { onDismiss() }) {
				Text(stringResource(id = android.R.string.ok))
			}
		},
		text = {
			Column(
				modifier = Modifier.verticalScroll(scrollState),
			) {
				HTMLText(
					html = license?.licenseContent?.replace("\n", "<br />").orEmpty()
				)
			}
		},
		modifier = Modifier.padding(16.dp),
		properties = DialogProperties(usePlatformDefaultWidth = false)
	)
}

@Composable
private fun HTMLText(
	html: String,
	modifier: Modifier = Modifier,
	color: Color = MaterialTheme.colorScheme.onSurface
) {
	AndroidView(
		modifier = modifier,
		factory = { context -> TextView(context) },
		update = { textView ->
			textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
			textView.setTextColor(color.toArgb()) }
	)
}
