package com.mijoro.motorcyclemusichost;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity {
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";
	public static final String SERVICECMD = "com.android.music.musicservicecommand";
	public static final String CMDNAME = "command";
	public static final String CMDSTOP = "stop";

	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;

	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	
	TextView myLabel;

	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	
	volatile boolean stopWorker;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		myLabel = (TextView)findViewById(R.id.myLabel);
		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("SHOULD PAUSE");
				sendCommand(CMDTOGGLEPAUSE);
			}
		});

		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("SHOULD SKIP");
				sendCommand(CMDNEXT);
			}
		});
		
		findViewById(R.id.link).setOnClickListener(new View.OnClickListener() {
		      public void onClick(View v) {
		        try {
		          findBT();
		          openBT();
		        }
		        catch (IOException ex) { }
		      }
		    });
	}

	private void sendCommand(String cmd) {
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		if (mAudioManager.isMusicActive()) {
			System.out.println("MUSIC IS Active");
		}
		Intent i = new Intent(SERVICECMD);
		i.putExtra(CMDNAME, cmd);
		sendBroadcast(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	void findBT() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			myLabel.setText("No bluetooth adapter available");
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 0);
		}

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				if (device.getName().equals("SeeedBTSlave")) {
					mmDevice = device;
					break;
				}
			}
		}
		myLabel.setText("Bluetooth Device Found");
	}

	void openBT() throws IOException {
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Standard
																				// //SerialPortService
																				// ID
		mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
		mmSocket.connect();
		mmOutputStream = mmSocket.getOutputStream();
		mmInputStream = mmSocket.getInputStream();
		beginListenForData();
		myLabel.setText("Bluetooth Opened");
	}

	void beginListenForData() {
		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
								char cmd = (char)b;
								System.out.println("BYTE " + cmd);
								commandReceived(cmd);
							}
						}
					} catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
	}
	
	void commandReceived(char cmd) {
		if (cmd == 'p') {
			sendCommand(CMDTOGGLEPAUSE);
		}
	}

	void closeBT() throws IOException {
		stopWorker = true;
		mmOutputStream.close();
		mmInputStream.close();
		mmSocket.close();
		myLabel.setText("Bluetooth Closed");
	}

}
/*
 * Alternative command mechanism
 * 
 * long eventtime = SystemClock.uptimeMillis();
 * 
 * Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); KeyEvent
 * downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
 * KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
 * downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
 * sendOrderedBroadcast(downIntent, null);
 * 
 * Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null); KeyEvent
 * upEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_UP,
 * KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
 * upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
 * sendOrderedBroadcast(upIntent, null);
 * 
 * NEXT Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
 * KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
 * KeyEvent.KEYCODE_MEDIA_NEXT, 0); downIntent.putExtra(Intent.EXTRA_KEY_EVENT,
 * downEvent); sendOrderedBroadcast(downIntent, null);
 * 
 * PREVIOUS Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
 * KeyEvent downEvent = new KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN,
 * KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
 * downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
 * sendOrderedBroadcast(downIntent, null);
 */