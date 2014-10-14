/**
 * I am not sure who is the author of the essential part of this file's code.
 * 
 * I found it on this blog post http://mindtherobot.com/blog/580/android-audio-play-a-wav-file-on-an-audiotrack/
 * 
 * I figured out the missing parts of the code for myself and completed the class to be usable...
 * 
 * If anyone feels offended by the use of this code, please contact me at iganev@cytec.bg
 * 
 * Regards to the author! :)
 */

package bg.cytec.android.wav;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavToPCM {
	public static final String RIFF_HEADER = "RIFF";
	public static final String WAVE_HEADER = "WAVE";
	public static final String FMT_HEADER = "fmt ";
	public static final String DATA_HEADER = "data";

	public static final int HEADER_SIZE = 44;

	public static final String CHARSET = "ASCII";

	protected static void checkFormat(boolean assertion, String message) throws IOException
	{
		if (!assertion)
		{
			throw new IOException(message);
		}
	}
	
	public static class WavInfo {
		int sampleRate;
		int bits;
		int dataSize;
		
		boolean isStereo;
		
		public WavInfo(int rate, int bits, boolean isStereo, int dataSize) {
			this.sampleRate = rate;
			this.bits = bits;
			this.isStereo = isStereo;
			this.dataSize = dataSize;
		}
		
		public int getSampleRate()
		{
			return sampleRate;
		}
		
		public int getBits()
		{
			return bits;
		}
		
		public int getDataSize()
		{
			return dataSize;
		}
		
		public boolean isStereo()
		{
			return isStereo;
		}
	}
	
	public static WavInfo readHeader(InputStream wavStream) throws IOException {

		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

		buffer.rewind();
		buffer.position(buffer.position() + 20);
		
		int format = buffer.getShort();
		
		checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means
																		// Linear
																		// PCM
		int channels = buffer.getShort();
		
		checkFormat(channels == 1 || channels == 2, "Unsupported channels: "
				+ channels);
		
		int rate = buffer.getInt();
		
		checkFormat(rate <= 48000 && rate >= 11025, "Unsupported rate: " + rate);
		
		buffer.position(buffer.position() + 6);
		
		int bits = buffer.getShort();
		//checkFormat(bits == 16, "Unsupported bits: " + bits);
		
		int dataSize = 0;
		
		while (buffer.getInt() != 0x61746164) { // "data" marker
			int size = buffer.getInt();
			wavStream.skip(size);

			buffer.rewind();
			wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
			buffer.rewind();
		}
		
		dataSize = buffer.getInt();
		
		checkFormat(dataSize > 0, "wrong datasize: " + dataSize);

		return new WavInfo(rate, bits, channels == 2, dataSize);
	}

	public static byte[] readWavPcm(WavInfo info, InputStream stream) throws IOException {
		
		byte[] data = new byte[info.getDataSize()];
		stream.read(data, 0, data.length);
		
		return data;
	}
	
	public static byte[] readWavPcm(InputStream stream) throws IOException {
		
		WavInfo info = readHeader(stream);
		
		return readWavPcm(info, stream);
	}
}
