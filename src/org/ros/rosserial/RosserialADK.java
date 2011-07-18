package org.ros.rosserial;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.ros.rosserial.*;
import org.ros.rosserial.RosSerial.ROSTopic;
import org.ros.rosserial.RosSerial.onNegotionListener;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.ros.Node;

public class RosserialADK {

	static final String TAG= "RosserialADK";
	private static final String ACTION_USB_PERMISSION = "org.ros.rosserial.action.USB_PERMISSION";

	
	private RosSerial rosserial;
	Thread ioThread;
	
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	FileDescriptor fd;

	private Node node;
	
	private Context mContext;
	
	public interface onConnectionListener{
		public void trigger(boolean connection);
	}
	
	private onConnectionListener connectionCB;
	public void setOnConnectonListener(onConnectionListener list){
		this.connectionCB = list;
	}
	
	

	public RosserialADK( Context context, Node node){
		
		this.node = node;
		this.mContext = context;
		
		mUsbManager = UsbManager.getInstance(context);
		mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		context.registerReceiver(mUsbReceiver, filter);
		
	}
	
	
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			
			if (action.equals(ACTION_USB_PERMISSION) ){
				if (intent.getBooleanExtra(
						UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					Log.d(TAG, "RosserialADK Recieved permission.  Now attaching.");

					open();
				}
				else{
					Log.d(TAG, "Permission for RosserialADK accesory denied.");
				}
			}
			
			else if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)){
				Log.d(TAG, "RosserialADK detached");
				closeAccessory();
			}
			
			else if(action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)){
				open();
			}
		}
	};
	
	
	private static UsbAccessory getAccessory( UsbManager man){
		UsbAccessory[] accessories = man.getAccessoryList();
		UsbAccessory  accessory= (accessories == null ? null : accessories[0]);
		
		if (accessory == null) return null;
		
		
		if  (accessory.getModel().equals("RosserialADK") 
				&& accessory.getManufacturer().equals("Willow Garage")){
			return accessory;
		}
		return null;

	}
	
	//Check to see if an RosserialADK board is attached
	public static  boolean  isAttached(UsbManager man){
		if ( getAccessory(man) == null) return false;
		else return true;
	}

	
	private boolean openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		System.out.println(mFileDescriptor.toString());
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			//rosserial =  new RosSerial(node, mInputStream, mOutputStream);
			//ioThread = new Thread(rosserial);
			//ioThread.start();
			if (connectionCB != null) connectionCB.trigger(true);
			System.out.println("accessory opened");
			return true;

		} else {
			
			System.out.println("accessory open fail");
			return false;
		}
	}
	
	//Try to open the device by 
	public boolean open() {
		
		UsbAccessory accessory  =  getAccessory(mUsbManager);
		if (accessory == null) return false;
		
		if (mUsbManager.hasPermission(accessory) ){
			openAccessory(accessory);
		}
		else{
			mUsbManager.requestPermission(accessory,
					mPermissionIntent);
		}
		return true;
	}

	private void closeAccessory() {

		try {
			if (mFileDescriptor != null) {
				if(rosserial!=null) {
					rosserial.shutdown();
					ioThread.interrupt();
					rosserial = null;
				}
				
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			if (connectionCB != null) connectionCB.trigger(false);

		}
	}
	
	public void shutdown(){
		closeAccessory();
		mContext.unregisterReceiver(mUsbReceiver);
	}
	
	public boolean isConnected(){
		return (mOutputStream != null);
	}
	
	
	ROSTopic[] getSubscriptions(){
		return rosserial.getSubscriptions();
	}
	ROSTopic[] getPublications(){
		return rosserial.getPublications();
	}
	
	//Set Callback function for new subscription
	void setOnSubscriptionCB(onNegotionListener cb){
		rosserial.setOnSubscriptionCB(cb);
	}
	
	//Set Callback for new publication
	void setOnPublicationCB(onNegotionListener cb){
		rosserial.setOnPublicationCB(cb);
	}
	 
}
