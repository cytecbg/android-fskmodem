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

package bg.cytec.android.fskmodem.examples.decode.pcm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import bg.cytec.android.fskmodem.FSKConfig;
import bg.cytec.android.fskmodem.FSKDecoder;
import bg.cytec.android.fskmodem.FSKDecoder.FSKDecoderCallback;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.os.Bundle;

public class MainActivity extends ActionBarActivity {

	protected FSKConfig mConfig;
	protected FSKDecoder mDecoder;
	
	protected Runnable mPCMFeeder = new Runnable() {
		
		@Override
		public void run() {
			try {
				//open input stream to the signed 16bit raw PCM file
				InputStream input = getResources().getAssets().open("pcm_signed_16bit.raw");
				DataInputStream data = new DataInputStream(input);

				//the decoder has 1 second buffer (equals to sample rate), 
				//so we have to fragment the entire file, 
				//to prevent buffer overflow or rejection
				short[] buffer = new short[1024];
				int pointer = 0;
						
				//feed signal little by little... another way to do that is to 
				//check the returning value of appendSignal(), it returns the 
				//remaining space in the decoder signal buffer
				while (data.available() > 0) {
					//since this time we process 16bit data, 
					//we have to convert it to shorts first
					ByteBuffer bytes = ByteBuffer.allocate(2);
					bytes.order(ByteOrder.LITTLE_ENDIAN);
					
					bytes.put((byte) data.read());
					bytes.put((byte) data.read());
					bytes.rewind();
					
					buffer[pointer] = bytes.getShort();
					pointer++;
					
					if (pointer == 1024 || data.available() == 0) {
						mDecoder.appendSignal(buffer);
						
						Thread.sleep(100); //give the decoder time to consume
						
						buffer = new short[1024];
						pointer = 0;
					}
				}
				
				data.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/// INIT FSK CONFIG
		
		try {
			mConfig = new FSKConfig(FSKConfig.SAMPLE_RATE_29400, FSKConfig.PCM_16BIT, FSKConfig.CHANNELS_MONO, FSKConfig.SOFT_MODEM_MODE_4, FSKConfig.THRESHOLD_20P);
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
		
		///
		
		new Thread(mPCMFeeder).start();
	}
	
	@Override
	protected void onDestroy() {
		
		mDecoder.stop();
		
		super.onDestroy();
	}

}
