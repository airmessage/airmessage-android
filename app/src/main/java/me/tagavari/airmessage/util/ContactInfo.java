package me.tagavari.airmessage.util;

import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ContactInfo {
	private final long identifier;
	@Nullable private final String name;
	@NonNull private final List<AddressInfo> addresses;
	
	public ContactInfo(long identifier, @Nullable String name, @NonNull List<AddressInfo> addresses) {
		this.identifier = identifier;
		this.name = name;
		this.addresses = addresses;
	}
	
	public long getIdentifier() {
		return identifier;
	}
	
	@Nullable
	public String getName() {
		return name;
	}
	
	public void addAddress(AddressInfo address) {
		addresses.add(address);
	}
	
	@NonNull
	public List<AddressInfo> getAddresses() {
		return addresses;
	}
	
	public String[] getAddressDisplayArray(Resources resources) {
		String[] displayArray = new String[addresses.size()];
		for(ListIterator<AddressInfo> iterator = addresses.listIterator(); iterator.hasNext();) {
			displayArray[iterator.nextIndex()] = iterator.next().getDisplay(resources);
		}
		return displayArray;
	}
	
	@NonNull
	@Override
	protected ContactInfo clone() {
		return new ContactInfo(identifier, name, new ArrayList<>(addresses));
	}
}