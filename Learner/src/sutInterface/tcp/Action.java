package sutInterface.tcp;

import java.util.HashSet;


public enum Action {
	LISTEN, 
	ACCEPT, 
	CLOSESERVER, 
	CLOSECONNECTION;
	
	public static HashSet<String> getActionStrings() {

		  HashSet<String> values = new HashSet<String>();

		  for (Action c : Action.values()) {
		      values.add(c.name());
		  }

		  return values;
	}
}
