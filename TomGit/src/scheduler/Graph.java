package scheduler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.util.Log;
import messages.Method;

public class Graph {
  //private boolean debugFlag = false;
  private final List<Method> methods;
  private final List<MethodTransition> transitions;

  public Graph(List<Method> methods, List<MethodTransition> transitions) {
    this.methods = methods;
    this.transitions = transitions;
  }

  public List<Method> getMethods() {
    return methods;
  }

  public List<MethodTransition> getTransitions() {
    return transitions;
  }
  
  public void Print()
  {
	  String o = "digraph finite_state_machine {" + "\n";
	  o += "rankdir=LR;" + "\n";
	  o += "size=\"" + (6*this.getMethods().size()) + "," + (6*this.getMethods().size()) + "\"" + "\n";
	  o += "node [shape = doublecircle]; " + Method.FinalPoint + "_2;" + "\n";
	  o += "node [shape = point ]; "+Method.StartingPoint + "_1" + "\n";
	  o += "node [shape = circle];" + "\n";
	  Log.d("Tom", "Methods size " + methods.size());
	  for (Method m : methods) {
		  o += m.label + "\n";
	  }
	  Log.d("Tom", "Trans size " + transitions.size());
	  for (MethodTransition t : transitions) {
		  Method s = t.getSource();
		  Method d = t.getDestination();
		  o += s.label + "->" + d.label + ";" + "\n";
	  }
	  o += "}";
	  Log.d("Tom", o);
  }
  
} 