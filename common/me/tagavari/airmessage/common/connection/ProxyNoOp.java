package me.tagavari.airmessage.common.connection;

import android.content.Context;
import androidx.annotation.Nullable;
import me.tagavari.airmessage.common.enums.ConnectionErrorCode;
import me.tagavari.airmessage.common.util.ConnectionParams;

public final class ProxyNoOp<Packet> extends DataProxy<Packet> {
	@Override
	public void start(Context context, @Nullable ConnectionParams override) {
		notifyClose(ConnectionErrorCode.badRequest);
	}
	
	@Override
	public void stop(int code) {
	
	}
	
	@Override
	public boolean send(Packet packet) {
		return false;
	}
	
	@Override
	public boolean isUsingFallback() {
		return false;
	}
}