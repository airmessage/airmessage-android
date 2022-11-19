package me.tagavari.airmessage;

import org.junit.Test;

import me.tagavari.airmessage.common.helper.AddressHelper;

import static com.google.common.truth.Truth.assertThat;

public class AddressTest {
	@Test
	public void testEmailValidation() {
		assertThat(AddressHelper.validateEmail("email@example.com")).isTrue();
		assertThat(AddressHelper.validateEmail("airmessagetest+ext@gmail.com")).isTrue();
		assertThat(AddressHelper.validateEmail("hello@airmessage.org")).isTrue();
		assertThat(AddressHelper.validateEmail("firstname.lastname@example-mail.com")).isTrue();
		
		assertThat(AddressHelper.validateEmail("name")).isFalse();
		assertThat(AddressHelper.validateEmail("@gmail.com")).isFalse();
		assertThat(AddressHelper.validateEmail("username@")).isFalse();
		assertThat(AddressHelper.validateEmail("mymail@gmail")).isFalse();
		assertThat(AddressHelper.validateEmail("mymail@email#example.com")).isFalse();
	}
	
	@Test
	public void testPhoneValidation() {
		assertThat(AddressHelper.validatePhoneNumber("123")).isTrue();
		assertThat(AddressHelper.validatePhoneNumber("1234567890")).isTrue();
		assertThat(AddressHelper.validatePhoneNumber("(123) 456-7890")).isTrue();
		assertThat(AddressHelper.validatePhoneNumber("(123 456-7890")).isTrue();
		
		assertThat(AddressHelper.validatePhoneNumber("1")).isFalse();
		assertThat(AddressHelper.validatePhoneNumber("90")).isFalse();
		assertThat(AddressHelper.validatePhoneNumber("(123a 456-7890")).isFalse();
	}
	
	@Test
	public void testStripPhoneNumber() {
		assertThat(AddressHelper.stripPhoneNumber("1234567890")).isEqualTo("1234567890");
		assertThat(AddressHelper.stripPhoneNumber("(123) 456-7890")).isEqualTo("1234567890");
		assertThat(AddressHelper.stripPhoneNumber("(123) 456-CLOUD7890")).isEqualTo("1234567890");
	}
}