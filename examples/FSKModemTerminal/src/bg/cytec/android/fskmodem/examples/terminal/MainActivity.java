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

package bg.cytec.android.fskmodem.examples.terminal;

import java.io.IOException;

import bg.cytec.android.fskmodem.FSKConfig;
import bg.cytec.android.fskmodem.FSKDecoder;
import bg.cytec.android.fskmodem.FSKDecoder.FSKDecoderCallback;
import bg.cytec.android.fskmodem.FSKEncoder.FSKEncoderCallback;
import bg.cytec.android.fskmodem.FSKEncoder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;

public class MainActivity extends ActionBarActivity {

	protected FSKConfig mConfig;
	protected FSKEncoder mEncoder;
	protected FSKDecoder mDecoder;
	
	protected AudioTrack mAudioTrack;
	
	protected AudioRecord mRecorder;
	
	protected int mBufferSize = 0;
	
	protected boolean mScrollLock = true;
	
	protected ScrollView mScroll;
	protected TextView mTerminal;
	protected EditText mInput;
	
	protected Runnable mRecordFeed = new Runnable() {
		
		@Override
		public void run() {
			
			while (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				
				short[] data = new short[mBufferSize/2]; //the buffer size is in bytes
				
				// gets the audio output from microphone to short array samples
				mRecorder.read(data, 0, mBufferSize/2);
				
				mDecoder.appendSignal(data);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// INIT VIEWS
		
		mTerminal = (TextView) findViewById(R.id.terminal);
		mInput = (EditText) findViewById(R.id.input);
		
		mInput.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					
					String text = view.getText().toString()+"\n";
					
					mEncoder.appendData(text.getBytes());
					
					mTerminal.setText(mTerminal.getText()+text);
					
					mInput.setText("");
					
					if (mScrollLock) {
						mScroll.fullScroll(ScrollView.FOCUS_DOWN);
					}
					
					return true;
				}
				
				return false;
			}
		});
		
		mScroll = (ScrollView) findViewById(R.id.scrollview);
		
		mScroll.setOnTouchListener(new OnTouchListener() {
			
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				
				if (event.getAction() == MotionEvent.ACTION_UP) {
					//finger released, detect if view is scrolled to bottom
					
					int diff = (mScroll.getScrollY()+mScroll.getHeight()) - mTerminal.getHeight();
					
					if (diff == 0) {
						mScrollLock = true;
					}
				}
				else if (event.getAction() == MotionEvent.ACTION_DOWN) {
					//finger placed down, lock scroll
					
					mScrollLock = false;
				}
				
				return false;
			}
		});
		
		/// INIT FSK CONFIG
		
		try {
			mConfig = new FSKConfig(FSKConfig.SAMPLE_RATE_44100, FSKConfig.PCM_16BIT, FSKConfig.CHANNELS_MONO, FSKConfig.SOFT_MODEM_MODE_4, FSKConfig.THRESHOLD_20P);
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
						
						mTerminal.setText(mTerminal.getText()+text);
						
						if (mScrollLock) {
							mScroll.fullScroll(ScrollView.FOCUS_DOWN);
						}
					}
				});
			}
		});
		
		/// INIT FSK ENCODER
		
		mEncoder = new FSKEncoder(mConfig, new FSKEncoderCallback() {
			
			@Override
			public void encoded(byte[] pcm8, short[] pcm16) {
				if (mConfig.pcmFormat == FSKConfig.PCM_16BIT) {
					//16bit buffer is populated, 8bit buffer is null
					
					mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
							mConfig.sampleRate, AudioFormat.CHANNEL_OUT_MONO,
							AudioFormat.ENCODING_PCM_16BIT, pcm16.length*2,
							AudioTrack.MODE_STATIC);
					
					mAudioTrack.write(pcm16, 0, pcm16.length);
					
					mAudioTrack.play();
				}
			}
		});
		
		///
		
		//make sure that the settings of the recorder match the settings of the decoder
		//most devices cant record anything but 44100 samples in 16bit PCM format...
		mBufferSize = AudioRecord.getMinBufferSize(FSKConfig.SAMPLE_RATE_44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
		//scale up the buffer... reading larger amounts of data
		//minimizes the chance of missing data because of thread priority
		mBufferSize *= 10;
		
		//again, make sure the recorder settings match the decoder settings
		mRecorder = new AudioRecord(AudioSource.MIC, FSKConfig.SAMPLE_RATE_44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);

		if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
			mRecorder.startRecording();
			
			//start a thread to read the audio data
			Thread thread = new Thread(mRecordFeed);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}
		else {
			Log.i("FSKDecoder", "Please check the recorder settings, something is wrong!");
		}
	}
	
	@Override
	protected void onDestroy() {
		
		mDecoder.stop();
		mEncoder.stop();
		
		if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED)
		{
			mRecorder.stop();
			mRecorder.release();
		}
		
		if (mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.STATE_INITIALIZED)
		{
			mAudioTrack.stop();
			mAudioTrack.release();
		}
		
		super.onDestroy();
	}

}
