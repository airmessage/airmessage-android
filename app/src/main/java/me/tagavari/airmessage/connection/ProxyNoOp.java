package me.tagavari.airmessage.connection;

import android.content.Context;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.util.DirectConnectionParams;

public final class ProxyNoOp<Packet> extends DataProxy<Packet> {
	@Override
	public void start(Context context, @Nullable Object override) {
		notifyClose(ConnectionErrorCode.badRequest);
	}
	
	@Override
	public void stop(int code) {
	
	}
	
	@Override
	public boolean send(Packet packet) {
		return false;
	}
}