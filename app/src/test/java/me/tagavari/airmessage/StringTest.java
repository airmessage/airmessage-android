package me.tagavari.airmessage;

import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import me.tagavari.airmessage.common.helper.StandardCompressionHelper;
import me.tagavari.airmessage.common.helper.StringHelper;

import static com.google.common.truth.Truth.assertThat;

public class StringTest {
	@Test
	public void testStringNullify() {
		assertThat(StringHelper.nullifyEmptyString(null)).isNull();
		assertThat(StringHelper.nullifyEmptyString("")).isNull();
		assertThat(StringHelper.nullifyEmptyString("hi")).isNotNull();
	}
	
	@Test
	public void testStringFallback() {
		assertThat(StringHelper.defaultEmptyString(null, "fallback")).isEqualTo("fallback");
		assertThat(StringHelper.defaultEmptyString("", "fallback")).isEqualTo("fallback");
		assertThat(StringHelper.defaultEmptyString("hi", "fallback")).isNotEqualTo("fallback");
	}
	
	@Test
	public void testEmojiCheck() {
		assertThat(StringHelper.stringContainsOnlyEmoji("❤❤️️")).isTrue();
		assertThat(StringHelper.stringContainsOnlyEmoji("\uD83C\uDF55")).isTrue();
		
		assertThat(StringHelper.stringContainsOnlyEmoji("Hi, how are you?")).isFalse();
		assertThat(StringHelper.stringContainsOnlyEmoji("Do you want to eat some \uD83C\uDF55?")).isFalse();
		assertThat(StringHelper.stringContainsOnlyEmoji("❤text❤️️")).isFalse();
	}
}