package org.ros.rosserial;

import org.ros.Node;
import org.ros.RosUtils;
import org.ros.rosserial.*;
import org.ros.rosserial.RosSerial.ROSTopic;

import com.android.future.usb.UsbManager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class RosserialLauncherActivity extends Activity {

	Node nh;
	RosserialADK adk = null;
	Handler mHandler;
	
	public class TextViewHandler implements Runnable{
		
		TextView view;
		String msg;
		
		TextViewHandler(TextView v, String text){
			view = v;
			msg = text;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
		view.setText(msg);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.launcher);

		mHandler = new Handler();
		
		if (RosserialADK.isAttached( UsbManager.getInstance(this) ) ){
			nh = RosUtils.createExternalMaster("RosserialADK", "http://10.0.129.89:11311");

			adk = new RosserialADK(this, nh);
			adk.setOnConnectonListener(new RosserialADK.onConnectionListener() {
				
				@Override
				public void trigger(boolean connection) {
					// TODO Auto-generated method stub
					if (connection){
						TextView v = (TextView) findViewById(R.id.ConncectionStatusView);
						v.setText("Now Connected to the ADK!");
					}
					else{
						TextView v = (TextView) findViewById(R.id.ConncectionStatusView);
						v.setText("Disconnected.");
					}
				}
			});
			
			
		
			adk.setOnPublicationCB(new RosSerial.onNegotionListener() {
				
				@Override
				public void onNegotiation(ROSTopic topic) {
					// TODO Auto-generated method stub
					TextView v = (TextView) findViewById(R.id.PublicationsView);
					ROSTopic[] topics = adk.getPublications();
					StringBuilder txt = new StringBuilder(300);
					txt.append("Publishing : \n");
					for (int i=0; i< topics.length; i++){
						txt.append( topics[i].name);
						txt.append(" : ");
						txt.append(topics[i].type);
						txt.append("\n");
					}
					
					mHandler.post(new TextViewHandler(v, "Got a publication") );
				}
			});
			
			
				
			adk.open();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (adk!=null) this.adk.shutdown();
		this.finish();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
	}
	
	

}                     
