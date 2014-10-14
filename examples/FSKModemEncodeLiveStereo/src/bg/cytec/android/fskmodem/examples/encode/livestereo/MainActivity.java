/**    
 *   This file is part of the FSKModem java/android library for 
 *   processing FSK audio signals. 
 *   
 *   The FSKModem library is developed by Ivan Ganev, CEO at
 *   Cytec BG Ltd.
 *
 *   Copyright (C) 2014  Cytec BG Ltd. office@cytec.bg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bg.cytec.android.fskmodem.examples.encode.livestereo;

import java.io.IOException;
import java.nio.ByteBuffer;

import bg.cytec.android.fskmodem.FSKConfig;
import bg.cytec.android.fskmodem.FSKDecoder;
import bg.cytec.android.fskmodem.FSKDecoder.FSKDecoderCallback;
import bg.cytec.android.fskmodem.FSKEncoder;
import bg.cytec.android.fskmodem.FSKEncoder.FSKEncoderCallback;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;

public class MainActivity extends ActionBarActivity {

	public static final String ENCODER_DATA = "Hello World! This text has been encoded realtime and then fed to audio playback stream and the FSK decoder that actually displays it.";
	
	protected FSKConfig mConfig;
	protected FSKEncoder mEncoder;
	protected FSKDecoder mDecoder;
	
	protected AudioTrack mAudioTrack;
	
	protected Runnable mDataFeeder = new Runnable() {
		
		@Override
		public void run() {
			byte[] data = ENCODER_DATA.getBytes();
			
			if (data.length > FSKConfig.ENCODER_DATA_BUFFER_SIZE) {
				//chunk data
				
				byte[] buffer = new byte[FSKConfig.ENCODER_DATA_BUFFER_SIZE];
				
				ByteBuffer dataFeed = ByteBuffer.wrap(data);
				
				while (dataFeed.remaining() > 0) {
					
					if (dataFeed.remaining() < buffer.length) {
						buffer = new byte[dataFeed.remaining()];
					}
					
					dataFeed.get(buffer);
					
					mEncoder.appendData(buffer);
					
					try {
						Thread.sleep(100); //wait for encoder to do its job, to avoid buffer overflow and data rejection
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				mEncoder.appendData(data);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/// INIT FSK CONFIG
		
		try {
			mConfig = new FSKConfig(FSKConfig.SAMPLE_RATE_44100, FSKConfig.PCM_16BIT, FSKConfig.CHANNELS_STEREO, FSKConfig.SOFT_MODEM_MODE_4, FSKConfig.THRESHOLD_20P);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/// INIT FSK DECODER
		
		mDecoder = new FSKDecoder(mConfig, new FSKDecoderCallback() {
			
			@Override
			public void decoded(byte[] newData) {
				
				final String text = new String(newData);
				
				runOnUiThread(new Runnable() {
					public void run() {
						
						TextView view = ((TextView) findViewById(R.id.result));
						
						view.setText(view.getText()+text);
					}
				});
			}
		});
		
		/// INIT FSK ENCODER
		
		mEncoder = new FSKEncoder(mConfig, new FSKEncoderCallback() {
			
			@Override
			public void encoded(byte[] pcm8, short[] pcm16) {
				if (mConfig.pcmFormat == FSKConfig.PCM_8BIT) {
					//8bit buffer is populated, 16bit buffer is null
					
					mAudioTrack.write(pcm8, 0, pcm8.length);
					
					mDecoder.appendSignal(pcm8);
				}
				else if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
					//16bit buffer is populated, 8bit buffer is null
					
					mAudioTrack.write(pcm16, 0, pcm16.length);
					
					mDecoder.appendSignal(pcm16);
				}
			}
		});
		
		///
		
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
				mConfig.sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, 1024,
				AudioTrack.MODE_STREAM);
		
		mAudioTrack.play();
		
		///
		
		new Thread(mDataFeeder).start();
	}
	
	@Override
	protected void onDestroy() {
		mDecoder.stop();
		
		mEncoder.stop();
		
		mAudioTrack.stop();
		mAudioTrack.release();
		
		super.onDestroy();
	}

}
