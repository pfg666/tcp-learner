package util.learnlib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.State;
import de.ls5.jlearn.interfaces.Symbol;

public class AutomatonUtil {

		  public static List<State> getStatesInBFSOrder(Automaton automaton)
		  {
			List<Symbol> inputs=automaton.getAlphabet().getSymbolList();
			java.util.Collections.sort(inputs);

		    Queue <State> statestovisit = new LinkedList  <State>();
		    List <State> result = new ArrayList <State> ();
		    HashSet <State> states = new HashSet <State> (); // to check if state is not seen already by other transition

		    statestovisit.offer(automaton.getStart());
		    result.add(automaton.getStart());
		    states.add(automaton.getStart());

		    State current = (State)statestovisit.poll();
		    while (current != null)
		    {
		      for( Symbol input:  inputs ) {
		    	  State s = current.getTransitionState(input);
			      if ((s != null) && (!states.contains(s))) {
				          statestovisit.offer(s);
				          result.add(s);
				          states.add(s);
				  }
		      }

		      if (statestovisit.isEmpty()) {
		        break;
		      }
		      current = (State)statestovisit.poll();
		    }

		    return result;
		  }	
}
