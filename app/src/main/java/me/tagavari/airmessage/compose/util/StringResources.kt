package me.tagavari.airmessage.compose.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.*
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@Composable
fun annotatedStringResource(@StringRes id: Int): AnnotatedString {
	val resources = LocalContext.current.resources
	val density = LocalDensity.current
	return remember(id) {
		val text = resources.getText(id)
		spannableStringToAnnotatedString(text, density)
	}
}

private fun spannableStringToAnnotatedString(
	text: CharSequence,
	density: Density
): AnnotatedString {
	return if(text is Spanned) {
		with(density) {
			buildAnnotatedString {
				append(text.toString())
				
				text.getSpans(0, text.length, Any::class.java).forEach { span ->
					val start = text.getSpanStart(span)
					val end = text.getSpanEnd(span)
					when(span) {
						is StyleSpan -> when(span.style) {
							Typeface.NORMAL -> addStyle(
								SpanStyle(
									fontWeight = FontWeight.Normal,
									fontStyle = FontStyle.Normal
								),
								start,
								end
							)
							Typeface.BOLD -> addStyle(
								SpanStyle(
									fontWeight = FontWeight.Bold,
									fontStyle = FontStyle.Normal
								),
								start,
								end
							)
							Typeface.ITALIC -> addStyle(
								SpanStyle(
									fontWeight = FontWeight.Normal,
									fontStyle = FontStyle.Italic
								),
								start,
								end
							)
							Typeface.BOLD_ITALIC -> addStyle(
								SpanStyle(
									fontWeight = FontWeight.Bold,
									fontStyle = FontStyle.Italic
								),
								start,
								end
							)
						}
						is TypefaceSpan -> addStyle(
							SpanStyle(
								fontFamily = when(span.family) {
									FontFamily.SansSerif.name -> FontFamily.SansSerif
									FontFamily.Serif.name -> FontFamily.Serif
									FontFamily.Monospace.name -> FontFamily.Monospace
									FontFamily.Cursive.name -> FontFamily.Cursive
									else -> FontFamily.Default
								}
							),
							start,
							end
						)
						is BulletSpan -> {
							Log.d("StringResources", "BulletSpan not supported yet")
							addStyle(SpanStyle(), start, end)
						}
						is AbsoluteSizeSpan -> addStyle(
							SpanStyle(fontSize = if(span.dip) span.size.dp.toSp() else span.size.toSp()),
							start,
							end
						)
						is RelativeSizeSpan -> addStyle(
							SpanStyle(fontSize = span.sizeChange.em),
							start,
							end
						)
						is StrikethroughSpan -> addStyle(
							SpanStyle(textDecoration = TextDecoration.LineThrough),
							start,
							end
						)
						is UnderlineSpan -> addStyle(
							SpanStyle(textDecoration = TextDecoration.Underline),
							start,
							end
						)
						is SuperscriptSpan -> addStyle(
							SpanStyle(baselineShift = BaselineShift.Superscript),
							start,
							end
						)
						is SubscriptSpan -> addStyle(
							SpanStyle(baselineShift = BaselineShift.Subscript),
							start,
							end
						)
						is ForegroundColorSpan -> addStyle(
							SpanStyle(color = Color(span.foregroundColor)),
							start,
							end
						)
						else -> addStyle(SpanStyle(), start, end)
					}
				}
			}
		}
	} else {
		AnnotatedString(text.toString())
	}
}