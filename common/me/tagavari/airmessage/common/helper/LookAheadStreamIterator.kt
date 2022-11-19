package me.tagavari.airmessage.common.helper

import java.io.IOException
import java.io.InputStream

/**
 * A utility class that accepts an input stream to read in chunks,
 * while also reading ahead to see if data will be available on the next read
 * @param bufferLength The length of the buffer used to read the stream
 * @param inputStream The stream to read
 */
class LookAheadStreamIterator(bufferLength: Int, private val inputStream: InputStream) {
	//Creating the current and future buffer
	private var bufferCurrent: ByteArray
	private var bufferFuture: ByteArray
	private lateinit var bufferSwap: ByteArray
	private var lengthCurrent: Int
	
	/**
	 * Gets if data is available for another read
	 */
	operator fun hasNext(): Boolean {
		return lengthCurrent != -1
	}
	
	/**
	 * Reads further into the stream and returns the stream data
	 */
	@Throws(IOException::class)
	operator fun next(): ForwardsStreamData {
		//Read the future data
		val lengthFuture = inputStream.read(bufferFuture)
		
		//Create the stream data
		val data = ForwardsStreamData(bufferCurrent, lengthCurrent, lengthFuture == -1)
		
		//Swap the current and future buffers (so that the current buffer will be returned, and the future buffer will be overwritten)
		bufferSwap = bufferCurrent
		bufferCurrent = bufferFuture
		bufferFuture = bufferSwap
		
		//Update the length
		lengthCurrent = lengthFuture
		
		//Return the stream data
		return data
	}
	
	data class ForwardsStreamData(val data: ByteArray, val length: Int, val isLast: Boolean)
	
	init {
		//Initializing the buffers
		bufferCurrent = ByteArray(bufferLength)
		bufferFuture = ByteArray(bufferLength)
		
		//Read the initial current data
		lengthCurrent = inputStream.read(bufferCurrent)
	}
}