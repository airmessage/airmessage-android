package me.tagavari.airmessage.util;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.AddressHelper;

//Represents a presentable and searchable user address
public class AddressInfo {
	@NonNull private final String address;
	@NonNull private final String normalizedAddress;
	@Nullable private final String addressLabel;
	
	/**
	 * Constructs a new address
	 * @param address The raw address
	 * @param addressLabel The address label
	 */
	public AddressInfo(@NonNull String address, @Nullable String addressLabel) {
		this.address = address;
		this.normalizedAddress = AddressHelper.normalizeAddress(address);
		this.addressLabel = addressLabel;
	}
	
	/**
	 * Gets the address as-is
	 */
	@NonNull
	public String getAddress() {
		return address;
	}
	
	/**
	 * Gets a normalized representation of the address
	 */
	@NonNull
	public String getNormalizedAddress() {
		return normalizedAddress;
	}
	
	/**
	 * Gets the label of the address
	 */
	@Nullable
	public String getAddressLabel() {
		return addressLabel;
	}
	
	/**
	 * Gets a user-presentable representation of this address with its label
	 */
	public String getDisplay(Resources resources) {
		if(addressLabel == null) return address;
		else return resources.getString(R.string.label_addressdetails, addressLabel, address);
	}
}