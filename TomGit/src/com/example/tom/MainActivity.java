package com.example.tom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map.Entry;

import messages.Method;
import messages.Task;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.PID;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MainActivity extends IOIOActivity implements MqttCallback, IMqttActionListener, SensorEventListener {
	private MqttAndroidClient client;
	private double loc_x, loc_y, target_x, target_y;
	private SensorManager mSensorManager;
    private Sensor mMag;
    private Sensor mAcc;
    private Sensor mRot;
    float[] mGravity;
    float[] mGeomagnetic;
    private float mBearing;
    private float bearingOffset;
    private String id = "0";
    
    int p = 0;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final Button button = (Button) findViewById(R.id.calOffset);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Log.d("Tom", "Button!");
                bearingOffset = mBearing;
                if (!client.isConnected()) return;
                Method m1 = new Method("OOH", 10, 10, System.nanoTime());
                Task task = new Task("Bang", Task.AgentTypes.POLICE);
                task.addMethod(m1);
        		ByteArrayOutputStream b = new ByteArrayOutputStream();
				try {
					ObjectOutputStream o = new ObjectOutputStream(b);
					o.writeObject(task);
	        		o.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        		byte bytes[]=b.toByteArray();
                try {
					client.publish("tasklist", bytes, 2, false);
					Log.d("Tom", "Published!");
				} catch (MqttPersistenceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Connect to the mqtt server
		int port = 1883;
		String uri = "tcp://" + "192.168.1.136" + ":" + port;
		client = new MqttAndroidClient(this, uri, "bob"+id);
		try {
			client.connect(null, this);
			client.setCallback(this);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		loc_x = -1;
		loc_y = -1;
		target_x = -1;
		target_y = -1;
		
		// Set up android sensors for rotation
		mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mRot = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		mSensorManager.registerListener(this, mRot, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void connectionLost(Throwable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void messageArrived(String arg0, MqttMessage mesg) throws Exception {	
		ByteArrayInputStream b1 = new ByteArrayInputStream(mesg.getPayload());
        ObjectInputStream o1 = new ObjectInputStream(b1);
        Object unknownMsg = o1.readObject();
        
        if (unknownMsg == Task.class) {
        	Task task = (Task) unknownMsg;
        	Log.d("Tom", "Got methodss: ");
        	for(Entry<String, Method> entry : task.methods.entrySet()) {
        		Method m = entry.getValue();
        		Log.d("Tom", "M" + m.methodId + ": " + m.wayPointX + " " + 
        				m.wayPointY + " " + task.agentType);
        	}
        } else {
			Log.d("Tom", "Got message: " + mesg.toString());
			String msg[] = mesg.toString().split("\\s+");
			if (msg[0].contentEquals("GPS")) {
				loc_x = Double.parseDouble(msg[1]);
				loc_y = Double.parseDouble(msg[2]);
			}  else {
				Log.d("Tom", "Unknown mqtt message");
			}
        }
	}

	@Override
	public void onFailure(IMqttToken arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		Log.d("Tom", "Fail");
		Log.d("Tom", arg1.getMessage());
	}

	/*
	 *  We've connected to mqtt, now subscribe to gps and waypoint topics(non-Javadoc)
	 * @see org.eclipse.paho.client.mqttv3.IMqttActionListener#onSuccess(org.eclipse.paho.client.mqttv3.IMqttToken)
	 */
	@Override
	public void onSuccess(IMqttToken arg0) {
		Log.d("Tom", "Success!");
		try {
			client.subscribe("gps/" + id, 0); // This gets called ~5 times a second
			client.subscribe("tasklist", 2);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}	
	}
	
	
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		private DigitalOutput led_;
        private PID pid1_;
        private PID pid2_;
        private PID pid3_;
        private PID pid4_;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 *
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 *
         */
		@Override
		protected void setup() throws ConnectionLostException{
			showVersions(ioio_, "IOIO connected!");
			led_ = ioio_.openDigitalOutput(0, true);
            pid1_ = ioio_.openPID(1);
            pid2_ = ioio_.openPID(2);
            pid3_ = ioio_.openPID(3);
            pid4_ = ioio_.openPID(4);
            try {
                pid1_.setParam(1,0,0);
                pid2_.setParam(1,0,0);
                pid3_.setParam(1,0,0);
                pid4_.setParam(1,0,0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            enableUi(true);
		}
		
		private double capPI(double a) {
			if (a > Math.PI) a = a - 2 * Math.PI;
			if (a < -Math.PI) a = a + 2 * Math.PI;
			return a;
		}
		
		/*
		 * Get the angle between 2 bearings
		 */
		private double bearingDiff(double current, double target) {
			current = capPI(current);
			target = capPI(target);
		    double d = current - target;
		    return capPI(d);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 *
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException
		 * 				When the IOIO thread has been interrupted.
		 *
		 * @see ioio.lib.util.IOIOLooper#loop()
		 */
		float driveLP = 0;
		float angLP = 0;
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			float driveVel = 0.0f;
            float driveAng = 0;
            // Do nothing if we have no waypoint
			if (loc_x != -1 && loc_y != -1 && target_x != -1 && target_y != -1) {
				// Calculate the current bearing to take and distance to target
				double x_diff = loc_x - target_x;
				double y_diff = loc_y - target_y;
				double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
				double targetBearing = Math.atan2(y_diff, x_diff);
				double curBearing = capPI(mBearing - bearingOffset);
				
				if (dist > 20) driveVel = 0.4f;
				float vltc = 0.1f;
				float antc = 1.0f;
				driveLP = driveLP * (1-vltc)+ driveVel * vltc;
				double bdiff = bearingDiff(curBearing, targetBearing);
				driveAng = (float) (bdiff * 3);
				if (driveAng > 2) driveAng = 1.0f;
				if (driveAng < -2) driveAng = -1.0f;
				angLP = angLP * (1-antc) + driveAng * antc;
				driveVel = driveLP;
				driveAng = angLP;
				// Stopping the debug log from spamming the screen
				p++;
				if (p %10 == 0) {
					Log.d("Tom", "Dist: " + dist + " TBearing: " + targetBearing + " RBearing: " + curBearing + " bd: " + bdiff);
					Log.d("Tom", "D: " + driveVel + " A: " + driveAng);
				}
			}
            try {
            	// Calculate wheel speeds and send commands via IOIO
                float left = driveVel * (float)Math.cos((double)driveAng) + driveVel * (float)Math.sin((double)driveAng);
                float right = -driveVel * (float)Math.cos((double)driveAng) + driveVel * (float)Math.sin((double)driveAng);
                pid1_.setSpeed(-left);
                pid2_.setSpeed(right);
                pid3_.setSpeed(left);
                pid4_.setSpeed(-right);
            } catch (IOException e) {
                e.printStackTrace();
            }
			Thread.sleep(10);
		}

		/**
		 * Called when the IOIO is disconnected.
		 *
		 * @see ioio.lib.util.IOIOLooper#disconnected()
		 */
		@Override
		public void disconnected() {
			enableUi(false);
			toast("IOIO disconnected");
		}

		/**
		 * Called when the IOIO is connected, but has an incompatible firmware version.
		 *
		 * @see ioio.lib.util.IOIOLooper#incompatible(IOIO)
		 */
		@Override
		public void incompatible() {
			showVersions(ioio_, "Incompatible firmware version!");
		}
}

	/**
	 * A method to create our IOIO thread.
	 *
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	private void showVersions(IOIO ioio, String title) {
		toast(String.format("%s\n" +
				"IOIOLib: %s\n" +
				"Application firmware: %s\n" +
				"Bootloader firmware: %s\n" +
				"Hardware: %s",
				title,
				ioio.getImplVersion(VersionType.IOIOLIB_VER),
				ioio.getImplVersion(VersionType.APP_FIRMWARE_VER),
				ioio.getImplVersion(VersionType.BOOTLOADER_VER),
				ioio.getImplVersion(VersionType.HARDWARE_VER)));
	}

	private void toast(final String message) {
		final Context context = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	/*
	 * IOIO boiler plate
	 */
	private int numConnected_ = 0;
	private void enableUi(final boolean enable) {
		// This is slightly trickier than expected to support a multi-IOIO use-case.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (enable) {
					if (numConnected_++ == 0) {
						//button_.setEnabled(true);
					}
				} else {
					if (--numConnected_ == 0) {
						//button_.setEnabled(false);
					}
				}
			}
		});
	}

	/*
	 *  This is where the current bearing is calculated (in rads)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		float[] mRotationMatrix = new float[9];
		if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;
		 SensorManager.getRotationMatrixFromVector(mRotationMatrix,
	                event.values);
	        SensorManager
	                .remapCoordinateSystem(mRotationMatrix,
	                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
	                        mRotationMatrix);
	        float[] orientationVals = new float[3];
			SensorManager.getOrientation(mRotationMatrix, orientationVals);
			if (orientationVals != null) mBearing = (float) orientationVals[0];
	}

	/*
	 * Not needed, part of sensor interface
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d("Tom", "Sensor changed accuracy");
	}
}
