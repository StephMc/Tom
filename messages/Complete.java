package messages;

import java.io.Serializable;

public class Complete implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8374608950095074803L;

	public String taskId;
	public long timeComplete;
	
	
	public Complete(String topLevelTaskLabel, long nanoTime) {
		this.taskId = topLevelTaskLabel;
		this.timeComplete = nanoTime;
	}
}
