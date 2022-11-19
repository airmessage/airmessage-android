package me.tagavari.airmessage.common.redux

import me.tagavari.airmessage.common.enums.ConnectionErrorCode
import me.tagavari.airmessage.common.enums.ConnectionState

abstract class ReduxEventConnection {
	@get:ConnectionState
	abstract val state: Int
	
	class Connected : ReduxEventConnection() {
		override val state = ConnectionState.connected
	}
	
	class Connecting : ReduxEventConnection() {
		override val state = ConnectionState.connecting
	}
	
	class Disconnected(
		@field:ConnectionErrorCode @get:ConnectionErrorCode
		@param:ConnectionErrorCode val code: Int
	) : ReduxEventConnection() {
		override val state = ConnectionState.disconnected
	}
}