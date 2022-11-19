import androidx.test.filters.SmallTest;

import org.junit.Test;

import me.tagavari.airmessage.common.helper.AddressHelper;

import static com.google.common.truth.Truth.assertThat;

@SmallTest
public class AndroidUnitTest {
	@Test
	public void testAddressNormalization() {
		final String[] inputs = new String[]{
				"example@example.com",
				"an_example.te24@example.com",
				"1234567890", //(123) 456-7890
				"+12223334444", //+1 (222) 333-4444
				"+1 222 333 4444", //+1 (222) 333-4444
				"+10 (222) 333-4444", //+1 (222) 333-4444
				"+10 222.333.4444", //+1 (222) 333-4444
				"+10 222.333.4444", //+1 (222) 333-4444
				"+10 222.333.4444" //+1 (222) 333-4444
		};
		
		final String[] expected = new String[]{
				"example@example.com",
				"an_example.te24@example.com",
				"1234567890",
				"+12223334444",
				"+12223334444",
				"+102223334444",
				"+102223334444",
				"+102223334444",
				"+102223334444"
		};
		
		for(int i = 0; i < inputs.length; i++) {
			assertThat(AddressHelper.normalizeAddress(inputs[i])).isEqualTo(expected[i]);
		}
		
		{
			String[] inputArray = inputs.clone();
			AddressHelper.normalizeAddresses(inputArray);
			assertThat(inputArray).isEqualTo(expected);
		}
	}
}