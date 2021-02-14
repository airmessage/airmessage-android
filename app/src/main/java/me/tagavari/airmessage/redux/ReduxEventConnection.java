package me.tagavari.airmessage.redux;

import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.enums.ConnectionState;

public abstract class ReduxEventConnection {
	@ConnectionState
	public abstract int getState();
	
	public static class Connected extends ReduxEventConnection {
		@Override
		public int getState() {
			return ConnectionState.connected;
		}
	}
	
	public static class Connecting extends ReduxEventConnection {
		@Override
		public int getState() {
			return ConnectionState.connecting;
		}
	}
	
	public static class Disconnected extends ReduxEventConnection {
		@ConnectionErrorCode int code;
		
		public Disconnected(@ConnectionErrorCode int code) {
			this.code = code;
		}
		
		@Override
		public int getState() {
			return ConnectionState.disconnected;
		}
		
		@ConnectionErrorCode
		public int getCode() {
			return code;
		}
	}
}