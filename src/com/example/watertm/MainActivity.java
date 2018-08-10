package com.example.watertm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private BluetoothAdapter BA;
	ListView listView, listView2;
	private Set<BluetoothDevice> pairedDevices;
	DeviceListAdapter mAdapter;
	private ArrayList<BluetoothDevice> unpairedlist = new ArrayList<BluetoothDevice>();
	ProgressDialog mProgressDlg;
	ArrayAdapter adapter;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1C87B9")));
		bar.setTitle(Html.fromHtml("<h2>Aqua Siren</h2>"));
		listView = (ListView) findViewById(R.id.listView1);
		listView2 = (ListView) findViewById(R.id.listView2);
		BA = BluetoothAdapter.getDefaultAdapter();
		if (!BA.isEnabled()) 
		{
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
					.create();
			alertDialog.setTitle("Enable BlueTooth!");
			// alertDialog.setIcon(R.drawable.attention);
			alertDialog.setCancelable(true);
			alertDialog
					.setMessage("Bluetooth status is disabled.you want to enable it?");
			alertDialog.setButton("YES", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					Intent turnOn = new Intent(
							BluetoothAdapter.ACTION_REQUEST_ENABLE);
					startActivityForResult(turnOn, 0);
				}
			});
			alertDialog.setButton2("NO", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
				}
			});
			alertDialog.show();
		}

		IntentFilter filter = new IntentFilter();

		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

		registerReceiver(broadcastreceiver, filter);
		showpaireddevices();
		setdiscoverable();
		mProgressDlg = new ProgressDialog(this);
		mProgressDlg.setMessage("Scanning...");
		mProgressDlg.setCancelable(false);
		mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						BA.cancelDiscovery();
					}
				});
	}

	public void setdiscoverable() {
		Intent getVisible = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		startActivityForResult(getVisible, 0);
	}

	public void showpaireddevices() {

		pairedDevices = BA.getBondedDevices();

		final ArrayList list = new ArrayList();
		final ArrayList<String> mac = new ArrayList<String>();
		final ArrayList<String> uuid = new ArrayList<String>();
		for (BluetoothDevice bt : pairedDevices) {
			list.add(bt.getName());
			mac.add(bt.getAddress());
			uuid.add(bt.getUuids().toString());
		}
		Toast.makeText(getApplicationContext(), "Showing Paired Devices",
				Toast.LENGTH_SHORT).show();
		adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,
				list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				Bundle bundle = new Bundle();
				bundle.putString("devicename", list.get(arg2).toString());
				bundle.putString("macaddress", mac.get(arg2).toString());
				bundle.putString("uuid", uuid.get(arg2).toString());
				startActivity(new Intent(MainActivity.this,
						ControllerMotor.class).putExtras(bundle));
				// Toast.makeText(getApplicationContext(),list.get(arg2).toString()+"\n"+mac.get(arg2).toString(),
				// Toast.LENGTH_LONG).show();
			}
		});
	}

	@Override
	public void onPause() {
		if (BA != null) {
			if (BA.isDiscovering()) {
				BA.cancelDiscovery();
			}
		}

		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(broadcastreceiver);
		super.onDestroy();
	}

	public void list(View v) {

		BA.startDiscovery();
	}

	public void showToast(String show) {
		Toast.makeText(getApplicationContext(), show, Toast.LENGTH_LONG).show();
	}

	private final BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
				final int state = intent
						.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
								BluetoothDevice.ERROR);
				final int prevState = intent.getIntExtra(
						BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
						BluetoothDevice.ERROR);

				if (state == BluetoothDevice.BOND_BONDED
						&& prevState == BluetoothDevice.BOND_BONDING) {
					showToast("Paired");
				} else if (state == BluetoothDevice.BOND_NONE
						&& prevState == BluetoothDevice.BOND_BONDED) {
					showToast("Unpaired");
				}

				mAdapter.notifyDataSetChanged();
			}

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				final int state = intent.getIntExtra(
						BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				if (state == BluetoothAdapter.STATE_ON) {
					showToast("Enabled");
					// showEnabled();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				unpairedlist = new ArrayList<BluetoothDevice>();

				mProgressDlg.show();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				mProgressDlg.dismiss();
				final ArrayList<BluetoothDevice> mDeviceList = unpairedlist;
				mAdapter = new DeviceListAdapter(MainActivity.this);
				mAdapter.setData(mDeviceList);
				mAdapter.setListener(new DeviceListAdapter.OnPairButtonClickListener() {

					@Override
					public void onPairButtonClick(int position) {
						BluetoothDevice device = mDeviceList.get(position);

						if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
							unpairDevice(device);
							recreate();
						} else {
							showToast("Pairing...");
							pairDevice(device);
						}
					}
				});
				listView2.setAdapter(mAdapter);
				registerReceiver(broadcastreceiver, new IntentFilter(
						BluetoothDevice.ACTION_BOND_STATE_CHANGED));

				// Intent newIntent = new
				// Intent(MainActivity.this,DeviceListActivity.class);
				//
				// newIntent.putParcelableArrayListExtra("device.list",unpairedlist);
				//
				// startActivity(newIntent);
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = (BluetoothDevice) intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				unpairedlist.add(device);

				showToast("Found device " + device.getName());
			}
		}
	};

	private void pairDevice(BluetoothDevice device) {
		try {
			Method method = device.getClass().getMethod("createBond",
					(Class[]) null);
			method.invoke(device, (Object[]) null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void unpairDevice(BluetoothDevice device) {
		try {
			Method method = device.getClass().getMethod("removeBond",
					(Class[]) null);
			method.invoke(device, (Object[]) null);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add("Refresh");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		recreate();
		return super.onOptionsItemSelected(item);
	}
}
