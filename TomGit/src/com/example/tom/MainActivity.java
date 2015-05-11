package com.example.tom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.Complete;
import messages.Method;
import messages.Task;
import messages.TaskAssign;
import messages.Utility;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import scheduler.Node;
import scheduler.Point;
import scheduler.Schedule;
import scheduler.ScheduleElement;
import scheduler.Scheduler;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
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
    private Sensor mRot;
    float[] mGravity;
    float[] mGeomagnetic;
    private float mBearing;
    private float bearingOffset;
    private String agentId = "1";
    private Queue<Schedule> jobQueue = new ConcurrentLinkedQueue<Schedule>();
    private Schedule curSched = null;
    
    private String MASTER_AGENT = "0";
    private int totalNodes = 2;
    private HashMap<String, InProgressTask> inProgressTasks = new HashMap<String, InProgressTask>();
    
    // Used to pass the task between ui thread and thread to calculate the cost of task
    // This is needed since the task cost calculation is very slow and stop the robot getting
    // position updates via mqtt.
    Handler mHandler;
    private Task assignTask;
    
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
                /*if (!client.isConnected()) return;
                Method m1 = new Method("meep", 110, 110, System.nanoTime());
                Method m2 = new Method("OOH", 10, 10, System.nanoTime());
                Method m3 = new Method("beep", 120, 120, System.nanoTime());
                Method m4 = new Method("AAH", 100, 100, System.nanoTime());
                Method m5 = new Method("small", 20, 20, System.nanoTime());
                Method m6 = new Method("low", 30, 30, System.nanoTime());
                Task task = new Task("Bang", Task.AgentTypes.POLICE);
                task.addNode(m1);
                task.addNode(m2);
                task.addNode(m3);
                //task.addNode(m4);
                //task.addNode(m5);
                //task.addNode(m6);
                publishObject("tasklist", task);
                Log.d("Tom", "Sent task " + task.label + " with " + task.children.size());*/
            }
        });
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Connect to the mqtt server
		int port = 1883;
		String uri = "tcp://" + "192.168.0.100" + ":" + port;
		client = new MqttAndroidClient(this, uri, "bob" + agentId);
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
		
		// Set up the handler for inter thread comms
		mHandler = new Handler(this.getMainLooper()) {
			@Override
            public void handleMessage(Message inputMessage) {
				Log.d("Tom", "Handled!");
				if (inputMessage.arg1 == 0) {
					Utility u = (Utility) inputMessage.obj;
					publishObject("utility", u);
				} else if (inputMessage.arg1 == 1) {
					Complete c = (Complete) inputMessage.obj;
					publishObject("complete", c);
				}
			}
		};
	}
	
	public void publishObject(String topic, Object message) {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		try {
			ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(message);
    		o.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte bytes[]=b.toByteArray();
        try {
			client.publish(topic, bytes, 2, false);
			Log.d("Tom", "Published to " + topic);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	public void messageArrived(String arg0, MqttMessage mesg) {	
		try {
			ByteArrayInputStream b1 = new ByteArrayInputStream(mesg.getPayload());
			ObjectInputStream o1 = new ObjectInputStream(b1);
        	Object unknownMsg = o1.readObject();
        	Log.d("Tom", "Got object!");
	        if (unknownMsg.getClass() == Task.class) {
	        	//***BeginAgentMessages
	        	Task task = (Task) unknownMsg;
	        	InProgressTask ip = new InProgressTask(task);
	        	inProgressTasks.put(task.label, ip);
	        	Log.d("Tom", "Got task " + task.label + " with " + task.children.size());
	        	Point p;
	        	if (loc_x == -1) {
	        		Log.d("Tom", "Got no location, using loc 0, 0");
	        		p = new Point(-1, -1);
	        	} else {
	        		p = new Point(loc_x, loc_y);
	        	}
	        	new Thread(new SchedulerRunnable(agentId, task, p, mHandler)).start();
	        } else if (unknownMsg.getClass() == TaskAssign.class) {
	        	TaskAssign ta = (TaskAssign)unknownMsg;
	        	Log.d("Tom", "Task " + ta.task.label + " assigned to " + ta.agentId);
	        	if (ta.agentId.contentEquals(this.agentId)) {
	        		//***EndAgentMessages 
	        		// Task assigned to us!! Put it on the to do list
	        		Log.d("Tom", "We got a task");
	        		assignTask = ta.task;
	        		new Thread () {
	        			public void run() {
	        				Point p;
	                    	if (loc_x == -1) {
	                    		Log.d("Tom", "Got no location, using loc 0, 0");
	                    		p = new Point(-1, -1);
	                    	} else {
	                    		p = new Point(loc_x, loc_y);
	                    	}
	                		Scheduler scheduler = new Scheduler(p);
	                    	Schedule sched = scheduler.CalculateScheduleFromTaems(assignTask);
	                    	Log.d("Tom", "Assigning task " + assignTask.label + " with cost " + sched.TotalQuality);
	                		jobQueue.add(sched);
	        			}
	        		}.start();		
	        	}
	        } else if (unknownMsg.getClass() == Utility.class) {
	        	// We are the master agent
	        	Utility u = (Utility) unknownMsg;
	        	if (inProgressTasks.containsKey(u.taskId)) {
	        		Log.d("Tom", "Got utility " + u.cost + " from " + u.agentId);
	        		InProgressTask ip = inProgressTasks.get(u.taskId);
	        		ip.totalResponses++;
	        		if (u.cost < ip.bestCost || ip.bestCost == -1) {
	        			// We've found the new best cost
	        			ip.bestAgent = u.agentId;
	        			ip.bestCost = u.cost;
	        		}
	        		if (ip.totalResponses == totalNodes) {
	        			// We've got a response from everyone, assign task
	        			TaskAssign ta = new TaskAssign(ip.task, ip.bestAgent);
	        			publishObject("assign", (Object) ta);
	        			inProgressTasks.remove(ip.task);
	        		} else {
	        			// Update the in progress task
	        			inProgressTasks.put(ip.task.label, ip);
	        		}
	        	} else {
	        		Log.d("Tom", "Haven't seen this task before...");
	        	}
	        } else {
	        	Log.d("Tom", "Unknown mqtt message class");
	        }
        } catch (Exception e) {
        	// This must be a location message - not in a class since this is send
        	// from a c++ app.
			Log.d("CPS", "Got message: " + mesg.toString());
			String msg[] = mesg.toString().split("\\s+");
			if (msg[0].contentEquals("GPS")) {
				loc_x = Double.parseDouble(msg[1]);
				loc_y = Double.parseDouble(msg[2]);
			}  else {
				Log.d("Tom", "Unknown mqtt message string");
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
			client.subscribe("gps/" + agentId, 0); // This gets called ~5 times a second
			client.subscribe("tasklist", 2);
			client.subscribe("assign", 2);
			if (agentId.contentEquals(MASTER_AGENT)) {
				client.subscribe("utility", 2);
			}
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}	
	}
	
	
	class Looper extends BaseIOIOLooper {
		/** The on-board LED. */
		//***MainControlLoopParameters
		private DigitalOutput led_;
        private PID pid1_;
        private PID pid2_;
        private PID pid3_;
        private PID pid4_;
        
        float leftLP = 0;
		float rightLP = 0;

		float pDis = 0f;
		float iDis = 0f;
		float dDis = 0f;

		float pAng = 0f;
		float iAng = 0f;
		float dAng = 0f;

		float pD = 0.01f;
		float iD = 0.000000001f;
		float dD = 0.001f;

		float AD = pD + iD + dD;
		float BD = -(pD + 2*dD);
		float CD = dD;

		float pA = 0.4f;
		float iA = 0.0003f;
		float dA = 0.2f;

		float AA = pA + iA + dA;
		float BA = -(pA + 2*dA);
		float CA = dA;

		float mD[] = {0f,0f,0f};
		float cD = 0f;
		float mA[] = {0f,0f,0f};
		float cA = 0f;

		float itc = 1.0f;
		float tc = 0.1f;
		
		float a = 1.0f;
		float b = 0.3f;
		float lim = 0.5f;
		
		float angLP = 0.0f;
		float disLP = 0f;

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

		private double distBetweenPoints(double x1, double y1, double x2, double y2) {
			double x_diff = x1 - x2;
			double y_diff = y1 - y2;
			return Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		}
		
		private Method getNextTarget() {
        	ScheduleElement s;
        	if (curSched == null ) s = null;
        	else s = curSched.poll();
        	if (s == null) {
        		if (curSched != null) {
        			// We've finished this task. Need to send complete message
        			Message m = Message.obtain(mHandler);
        			Complete c = new Complete(curSched.topLevelTaskLabel, System.nanoTime());
        	    	m.obj = (Object) c;
        	    	m.arg1 = 1;
        	    	m.sendToTarget();
        	    	curSched = null;
        		}
    			if (jobQueue.isEmpty()) return null;
    			
    			curSched = jobQueue.poll();
        		s = curSched.poll();
        	}
        	if (s.getMethod() != null) {
        		Log.d("Tom", "Got target " + s.getMethod().x + " " + s.getMethod().y);
        	}
        	return s.getMethod();
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
		//***MainControlLoop
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			float driveVel = 0.0f;
            float driveAng = 0;
            // Do nothing if we have no waypoint
            if (curSched == null) {
            	Method m = getNextTarget();
            	if (m == null) {
            		target_x = loc_x;
                	target_y = loc_y;
                	Log.d("ctrl", "No target");
            	} else {
            		target_x = m.x;
            		target_y = m.y;
            	}	
            }
            
            if (loc_x != -1) {
		        Log.d("ctrl", "Target is " + target_x + " " + target_y + " " + loc_x + " " + loc_y);
				// Calculate the current bearing to take and distance to target
				double dist = distBetweenPoints(loc_x, loc_y, target_x, target_y);			
				double targetBearing = Math.atan2(loc_y - target_y, loc_x - target_x);
				double curBearing = capPI(mBearing - bearingOffset);
				double errBearing = -capPI(targetBearing - curBearing);
		
				//Magic control stuff
				if(Math.abs(errBearing) > Math.PI/2)dist = - dist;
				
				angLP = angLP * (1-itc) + (float)errBearing*itc;
				disLP = disLP * (1-itc) + (float)dist*itc;
				
				disLP = (float) (disLP * (Math.exp(-(angLP*angLP)/0.4)));
				
				if (Math.abs(dist) < 20){
					disLP = 0;
					angLP = 0;
					for(int i=0;i<3;i++){
						mD[i]=0;
						mA[i]=0;
					}
					cD = 0;
					cA = 0;
					
					// We've reached the waypoint!
					Method next = getNextTarget();
					if (next == null) {
						target_x = loc_x;
						target_y = loc_y;
					} else {
						target_x = next.x;
						target_y = next.y;
					}
				} else {	
					if (dist > 100) dist = 100;
					mD[0] = mD[1];
					mD[1] = mD[2];
					mD[2] = disLP;
					cD = cD
							+ mD[2] * AD
							+ mD[1] * BD
							+ mD[0] * CD;
					mA[0] = mA[1];
					mA[1] = mA[2];
					mA[2] = angLP;
					cA = cA
							+ mA[2] * AA
							+ mA[1] * BA
							+ mA[0] * CA;
			
					driveVel = cD;
					driveAng = cA;
			
					//end magic control stuff
			
			
					// Stopping the debug log from spamming the screen
					p++;
					if (p %10 == 0) {
						Log.d("ctrl", "Dist: " + disLP + " Ang: " + angLP );
						Log.d("ctrl", "D: " + driveVel + " A: " + driveAng);
					}
				}
            }
            try {
// Calculate wheel speeds and send commands via IOIO
				
				float left = driveVel*b + driveAng*a;
				float right = -driveVel*b + driveAng*a;

				
				//cap the values while maintaining ratio
				float scl = 1f;
				float sclL = Math.abs(left / lim);
				float sclR = Math.abs(right / lim);
				scl = Math.max(scl,sclL);
				scl = Math.max(scl, sclR);
				left = left / scl;
				right = right / scl;


				leftLP = leftLP * (1-tc) + left*tc;
				rightLP = rightLP * (1-tc) + right*tc;
				
				Log.d("ctrl", "Left " + leftLP + " " + rightLP);
				
				pid1_.setSpeed(-leftLP);
				pid2_.setSpeed(rightLP);
				pid3_.setSpeed(leftLP);
				pid4_.setSpeed(-rightLP);
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
