package com.example.watertm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ControllerMotor extends Activity {
	String deviceName, deviceAddress, deviceUuid;
	private ConnectedThread mConnectedThread;
	BluetoothAdapter btAdapter;
	private StringBuilder sb = new StringBuilder();
	BluetoothSocket btSocket = null;
	final int RECIEVE_MESSAGE = 1; // Status for Handler
	Handler h;
	EditText txtArduino;
	String sbprint;
	String TAG = "bluetooth";
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller_motor);
		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1C87B9")));
		bar.setTitle(Html.fromHtml("<h2>MOTOR CONTROL</h2>"));
		txtArduino = (EditText) findViewById(R.id.editText1);
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		deviceName = getIntent().getExtras().getString("devicename");
		deviceAddress = getIntent().getExtras().getString("macaddress");
		deviceUuid = getIntent().getExtras().getString("uuid");
		((TextView) findViewById(R.id.textView1)).setText(deviceName + "\n"
				+ deviceAddress);
		h = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case RECIEVE_MESSAGE: // if receive massage
					byte[] readBuf = (byte[]) msg.obj;
					String strIncom = new String(readBuf, 0, msg.arg1);

					sb.append(strIncom); // append string
					int endOfLineIndex = sb.indexOf("\r\n"); // determine the
																// end-of-line
					if (endOfLineIndex > 0) { // if end-of-line,
						sbprint = sb.substring(0, endOfLineIndex); // extract
																	// string
						sb.delete(0, sb.length()); // and clear
						txtArduino.setText("Reading :\n" + " " + sbprint); // update
						Log.i("Reading : ", sbprint);
						// put your value

						// TextView
					}
					// Log.d(TAG, "...String:"+ sb.toString() + "Byte:" +
					// msg.arg1 + "...");
					break;
				}
			};
		};
	}

	private void errorExit(String title, String message) {
		Toast.makeText(getBaseContext(), title + " - " + message,
				Toast.LENGTH_LONG).show();
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();

		BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and socket create failed: "
					+ e.getMessage() + ".");
		}
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.
		Log.d(TAG, "...Connecting...");
		try {
			btSocket.connect();
			Log.d(TAG, "....Connection ok...");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error",
						"In onResume() and unable to close socket during connection failure"
								+ e2.getMessage() + ".");
			}
		}

		// Create a data stream so we can talk to server.
		Log.d(TAG, "...Create Socket...");

		mConnectedThread = new ConnectedThread(btSocket);
		mConnectedThread.start();
	}

	public void on(View view) {
		ConnectedThread connectedThread = new ConnectedThread(btSocket);
		connectedThread.write("a");
	}

	public void off(View view) {
		ConnectedThread connectedThread = new ConnectedThread(btSocket);
		connectedThread.write("b");
	}

	public void onCLickA(View view) {
		ConnectedThread connectedThread = new ConnectedThread(btSocket);
		connectedThread.write("A");
	}

	public void onCLickB(View view) {
		ConnectedThread connectedThread = new ConnectedThread(btSocket);
		connectedThread.write("B");
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device)
			throws IOException {
		if (Build.VERSION.SDK_INT >= 10) {
			try {
				final Method m = device.getClass().getMethod(
						"createInsecureRfcommSocketToServiceRecord",
						new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection", e);
			}
		}
		return device.createRfcommSocketToServiceRecord(MY_UUID);
	}

	@Override
	public void onPause() {
		super.onPause();

		Log.d(TAG, "...In onPause()...");

		try {
			btSocket.close();
		} catch (IOException e2) {
			errorExit("Fatal Error", "In onPause() and failed to close socket."
					+ e2.getMessage() + ".");
		}
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {

			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[256]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer); // Get number of bytes and
					// message in "buffer"
					h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer)
							.sendToTarget(); // Send to message queue Handler
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {
			Log.d(TAG, "...Data to send: " + message + "...");
			byte[] msgBuffer = message.getBytes();
			try {
				mmOutStream.write(msgBuffer);
			} catch (IOException e) {
				Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add("CHAT");
		menu.add("RESET");
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		AlertDialog alertDialog = new AlertDialog.Builder(ControllerMotor.this)
				.create();
		if (item.getTitle().equals("RESET")) {
			alertDialog.setTitle("RESET");
			alertDialog.setIcon(R.drawable.restart);
			alertDialog.setMessage("Do you really want to RESET?");
			alertDialog.setButton("YES", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					ConnectedThread connectedThread = new ConnectedThread(
							btSocket);
					connectedThread.write("c");
					Toast.makeText(getApplicationContext(), "RESETTED",
							Toast.LENGTH_LONG).show();
				}
			});
			alertDialog.setButton2("NO", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub

				}
			});
			alertDialog.show();
		} else {
			alertDialog.setTitle("CHAT!");
			alertDialog.setIcon(R.drawable.chat);
			final EditText editText = new EditText(this);
			editText.setHint("Type here..");
			alertDialog.setView(editText);

			alertDialog.setButton("SEND",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							ConnectedThread connectedThread = new ConnectedThread(
									btSocket);
							connectedThread
									.write(editText.getText().toString());
							Toast.makeText(getApplicationContext(),
									editText.getText().toString(),
									Toast.LENGTH_LONG).show();
						}
					});
			alertDialog.show();
		}

		return super.onOptionsItemSelected(item);
	}

}
