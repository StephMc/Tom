package com.example.tom;

import messages.Task;

public class InProgressTask {
	public Task task;
	public double bestCost;
	public String bestAgent;
	public int totalResponses;

	public InProgressTask(Task task) {
		this.task = task;
		this.bestAgent = null;
		this.bestCost = -1;
		this.totalResponses = 0;
	}
}
