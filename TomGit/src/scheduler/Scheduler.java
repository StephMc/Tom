package scheduler;

import java.util.ArrayList;
import java.util.Iterator;

import messages.Method;
import messages.Task;

import java.util.*;

//import android.util.Log;

public class Scheduler {

	private boolean debugFlag = true;
	//Represents the start time when this schedule is being calculated, 
	public static Date startTime = new Date();

	private Point location;

	public Scheduler(Point location)
	{
		this.location = location;
	}

	//Method takes a Teams structure as input and outputs all the possible schedules resulting from that
	//task structure. This output is then fed to a generic Dijkstra's algorithm to calculate the optimum schedule
	//corresponding to the optimum path from the starting task to the ending task
	public Schedule CalculateScheduleFromTaems(Task topLevelTask)
	{
		//Reinitialize the schedule item
		Schedule schedule = new Schedule(topLevelTask.label);
		//Reinitialize the start time of calculation
		startTime = new Date();

		//Create set of Nodes representing all methods that can be executed in the eventual schedule
		ArrayList<Method> nodes = new ArrayList<Method>();
		//Create set of all possible transitions of execution from one method to another, which represents an actual
		//pass through a possible schedule
		ArrayList<MethodTransition> edges = new ArrayList<MethodTransition>();
		//Add an initial node, which will act as our starting point
		Point agentPos = location;
		Method initialMethod = new Method(Method.StartingPoint, agentPos.x,agentPos.y, 0);
		nodes.add(initialMethod);
		Method finalMethod = new Method(Method.FinalPoint, agentPos.x,agentPos.y, 0);
		nodes.add(finalMethod);
		//Append all possible schedule options to this set, after parsing the input Taems structure
		Method[] finalMethodList = AppendAllMethodExecutionRoutes(nodes, edges, topLevelTask, new Method[]{initialMethod}, null, true);
		for(int i=0;i<finalMethodList.length;i++)
		{
			MethodTransition t = new MethodTransition(
					"From " + finalMethodList[i].label + " to " + finalMethod.label, 
					finalMethodList[i], finalMethod);
			edges.add(t);
		}

		//Log.d("Tom", "About to make graph");
		//Create a Graph of these methods and run Dijkstra Algorithm on it
		Graph graph = new Graph(nodes, edges);
		//Log.d("Tom", "About to print graph");
		//graph.Print();
		//Log.d("Tom", "About to run algo");
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph, agentPos);
		dijkstra.execute(initialMethod);
		LinkedList<Method> path = dijkstra.getPath(finalMethod);
		//Log.d("Tom", "Done path");
		//Print the determined schedule
		int totalquality = 0;
		if (path!=null)
			//Log.d("Tom", "Starting path print");
			for (Method vertex : path) {
				if (!vertex.label.equals("Final Point"))//Ignore final point as its only necessary to complete graph for dijkstra, but we don't need to visit it
				{
					totalquality += vertex.getOutcome().getQuality();
					schedule.addItem(new ScheduleElement(vertex));
					//Log.d("Tom", "M: " + vertex.label);
				}
			}
		schedule.TotalQuality = totalquality;
		return schedule;
	}

	//A helper method used internally by CalculateScheduleFromTaems method
	private void Permute(Node[] inputArray, int start, int end, ArrayList<Node[]> permutations)
	{
		if(start==end) 
		{
			permutations.add(inputArray.clone());
		}
		else
		{
			for(int i=start;i<=end;i++)
			{
				Node t = inputArray[start];
				inputArray[start]=inputArray[i]; 
				inputArray[i]=t;
				Permute(inputArray,start+1,end,permutations);
				t=inputArray[start];
				inputArray[start]=inputArray[i]; 
				inputArray[i]=t;
			}
		}
	}

	private void printNodeArray(Node[] n)
	{
		String m = "";
		for(Node s:n)
		{
			m += s.label + " > ";
		}
		//Log.d("Tom", "[Scheduler 185] Node Methods: " + m );
	}

	//A helper method used internally by CalculateScheduleFromTaems method
	private Method[] AppendAllMethodExecutionRoutes(ArrayList<Method> nodes, ArrayList<MethodTransition> edges, Node task,
			Method[] appendTo, Node Parent, boolean makeMethodsUnique)
	{
		ArrayList<Method> lastMethodList = new ArrayList<Method>();
		for(int mIndex = 0; mIndex<appendTo.length; mIndex++) {
			//For subtasks, look at the relevant QAF, which will constrain how the tasks must be scheduled (and executed)
			Method lastMethod = appendTo[mIndex];
			if (!task.IsTask()) {
				Method m; //= new Method("ARARAR", ((Method)task).x, ((Method)task).y, 0);
				//if (makeMethodsUnique)
				m = new Method((Method)task);
				//else
					//m = (Method)task;
				//Log.d("Tom", "Made method " + m.label + " with task " + task.label);
				nodes.add(m);
				MethodTransition t = new MethodTransition("From " + lastMethod.label + " to " + m.label, lastMethod, m);
				edges.add(t);
				lastMethodList.add(m);
			} else {
				Method[] localLastMethodList = new Method[]{lastMethod};
				Task tk = (Task)task;
				//Log.d("Tom", "[Scheduler 212] Node is Task. Enumerating children for " + tk.label);
				//Designate a parent for this task, which can be used for completion notifications up the hierarchy
				//All tasks must be executed, though not necessarily in sequence
				//Create task list whose permutations need to be found
				ArrayList<Node> subtasksList = new ArrayList<Node>();
				Iterator<Node> subtasks = tk.getSubtasks();
				while(subtasks.hasNext()) {
					Node subtask = subtasks.next();
					subtasksList.add(subtask);
				}
				Node[] subTaskListForSumPermutation = subtasksList.toArray(new Node[subtasksList.size()]);
				printNodeArray(subTaskListForSumPermutation);
				//Find permutations for this task list
				ArrayList<Node[]> permutations = new ArrayList<Node[]>(); 
				Permute(subTaskListForSumPermutation,0,subTaskListForSumPermutation.length-1,permutations);
				//Now create paths for each permutation possible
				Method[] permutationLinkMethodsList = localLastMethodList;
				for (Node[] s : permutations)
				{
					Method[] m = new Method[]{lastMethod};
					//If there are multiple methods, we want them to be separated out in the graph to avoid cross linkages of permuted values. But if there is
					//only one, then for aesthetic purposes, we can have the same object repeated
					boolean multiplePermutationRequringUniqueMethodsForGraph = true;
					if (s.length<2) multiplePermutationRequringUniqueMethodsForGraph = false;
					for(int i=0;i<s.length;i++)
					{
						permutationLinkMethodsList = AppendAllMethodExecutionRoutes(nodes, edges, s[i], permutationLinkMethodsList, tk, multiplePermutationRequringUniqueMethodsForGraph);
					}
					for(int y=0;y<permutationLinkMethodsList.length;y++)
					{
						lastMethodList.add(permutationLinkMethodsList[y]);
					}
					//Reset permutation starting point
					permutationLinkMethodsList = localLastMethodList;
				}

			}
		}
		Method[] result = lastMethodList.toArray(new Method[lastMethodList.size()]);
		return result;
	}


}


