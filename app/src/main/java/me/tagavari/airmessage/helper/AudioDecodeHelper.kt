package me.tagavari.airmessage.helper

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.ByteOrder

object AudioDecodeHelper {
	private const val coderTimeout = 1_000L
	private const val samplesPerSecond = 10
	
	/**
	 * Gets a list of amplitudes for an audio file,
	 * or null
	 */
	@Throws(IOException::class)
	fun getAmplitudeList(file: File): List<Int> {
		val mediaExtractor = MediaExtractor()
		mediaExtractor.setDataSource(file.path)
		mediaExtractor.selectTrack(0)
		
		val mediaFormat = mediaExtractor.getTrackFormat(0)
		
		val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
		val codec = codecList.findDecoderForFormat(mediaFormat) ?:
			throw IllegalArgumentException("Failed to get codec for media format $mediaFormat")
		
		val decoder = MediaCodec.createByCodecName(codec)
		decoder.configure(mediaFormat, null, null, 0)
		decoder.start()
		
		val bufferInfo = MediaCodec.BufferInfo()
		val averageCollectorList = mutableListOf<Short>()
		val amplitudeList = mutableListOf<Int>()
		
		while(true) {
			//Read input
			val inputBufferIndex = decoder.dequeueInputBuffer(coderTimeout)
			if(inputBufferIndex >= 0) {
				val buffer = decoder.getInputBuffer(inputBufferIndex)!!
				val chunkSize = mediaExtractor.readSampleData(buffer, 0)
				if(chunkSize > 0) {
					decoder.queueInputBuffer(inputBufferIndex, 0, chunkSize, mediaExtractor.sampleTime, 0)
					mediaExtractor.advance()
				} else {
					decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
				}
			}
			
			//Handle output
			val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, coderTimeout)
			if(outputBufferIndex >= 0) {
				val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
				val outputFormat = decoder.getOutputFormat(outputBufferIndex)
				
				val sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
				val intervalSize = sampleRate / samplesPerSecond
				
				val shortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
				while(shortBuffer.hasRemaining()) {
					averageCollectorList.add(shortBuffer.get())
					
					if(averageCollectorList.size >= intervalSize) {
						val amplitude = averageCollectorList.max().toInt()
						amplitudeList.add(amplitude)
						averageCollectorList.clear()
					}
				}
				
				decoder.releaseOutputBuffer(outputBufferIndex, false)
				
				//Check for end of stream
				if(bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
					break
				}
			}
		}
		
		//Clean up
		decoder.stop()
		decoder.release()
		mediaExtractor.release()
		
		return amplitudeList
	}
}
