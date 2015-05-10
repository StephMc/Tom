package scheduler;

import messages.Method;

public class MethodTransition  {
	  private final String id; 
	  private final Method source;
	  private final Method destination; 
	  
	  public MethodTransition(String id, Method source, Method destination) {
	    this.id = id;
	    this.source = source;
	    this.destination = destination;
	  }
	  
	  public String getId() {
	    return id;
	  }
	  public Method getDestination() {
	    return destination;
	  }

	  public Method getSource() {
	    return source;
	  }
	  public DijkstraDistance getPathUtility( DijkstraDistance distanceTillPreviousNode, Point agentPos) {
	    return destination.getPathUtilityRepresentedAsDistance(distanceTillPreviousNode, agentPos);
	  }
	  
	  @Override
	  public String toString() {
	    return source + " to " + destination;
	  }
	  
	  public String toStringLong() {
		    return source.toString() + " to " + destination.toString();
	  }
	  
	  
	} 