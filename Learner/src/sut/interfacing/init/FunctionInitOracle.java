package sut.interfacing.init;

import sut.mapper.Flag;
import sut.mapper.Symbol;
import sut.mapper.TCPMapper;

/**
 * An initial state oracle which works for Windows 8. It is based on a function over the mapper variables. Implementing a function, the oracle is
 * stateless.
 * @author Paul
 */
public class FunctionInitOracle implements InitOracle{
	
	// resetting function for Windows 8
	public boolean isResetting(TCPMapper mapper) {
		boolean isInitial = false;
		if(mapper.isLastResponseTimeout) {
			isInitial = (mapper.lastFlagsSent.has(Flag.RST) && mapper.lastAbstractSeqSent.is(Symbol.V)) ||
					mapper.isInit; 
		} else {
			isInitial = (mapper.lastFlagsReceived.has(Flag.RST) && mapper.lastAbstractSeqSent.is(Symbol.V)) &&
					mapper.lastFlagsSent.has(Flag.SYN); 
		}
		return isInitial;
	}

	// the resetting function is stateless 
	public void setDefault() {
		
	}
}
