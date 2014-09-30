package sutInterface.tcp.init;

import sutInterface.tcp.TCPMapper;


/**
 * Initialization oracle interface
 */
public interface InitOracle {
	/** 
	 * Check if the input is resetting. (ie. it triggers transition to the initial state)
	 * Resetting state of an input can depend on the history of inputs checked after the last call to setDefault 
	 * or after instantiation of the oracle.
	 */
	public boolean isResetting(TCPMapper mapper);
	
	/**
	 *  Clears the input history.
	 */
	public void setDefault(); 
}
