package com.example.tom;

import java.util.Iterator;

import scheduler.Point;
import scheduler.Schedule;
import scheduler.ScheduleElement;
import scheduler.Scheduler;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import messages.Task;
import messages.Utility;

public class SchedulerRunnable implements Runnable {
	private Task task;
	private Point location;
	private String agentId;
	private Handler mainTask;
	
	SchedulerRunnable(String agentId, Task task, Point location, Handler mainTask) {
		this.task = task;
		this.location= location;
		this.agentId = agentId;
		this.mainTask = mainTask;
	}
	
	@Override
	public void run() {
    	Scheduler scheduler = new Scheduler(location);
    	Schedule sched = scheduler.CalculateScheduleFromTaems(task);
    	Iterator<ScheduleElement> steps = sched.getItems();
    	while (steps.hasNext()) {
				ScheduleElement se = steps.next();
				Log.d("Tom", "Next is: " + se.toString());
		}
    	Utility u = new Utility(task.label, agentId, sched.TotalQuality);
    	Message m = Message.obtain(mainTask);
    	m.obj = (Object) u;
    	m.sendToTarget();
    	
    	Log.d("Tom", "Got cost " + sched.TotalQuality);
	}

}
