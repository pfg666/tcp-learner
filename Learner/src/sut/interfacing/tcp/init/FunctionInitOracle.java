package sut.interfacing.tcp.init;

import sut.interfacing.tcp.Flag;
import sut.interfacing.tcp.Symbol;
import sut.interfacing.tcp.TCPMapper;

/**
 * An initial state oracle which works for Windows 8 and Linux for a limited alphabet. When this function is applied on the mapper,
 * it should tell if the sut is in the initial state (hence, all numbers in the follow up input are valid) or it's isn't (and only
 * a limited selection is valid)
 * 
 * The problem in Linux is that RST+ACK(V,INV) which based on the specification, should reset the system, doesn't in some cases. 
 * More specifically, if the SUT is in SYN_RECV, then RST+ACK packets are dropped and ignored should the ack. num. be invalid.
 * 
 * If you use this, remove RST+ACK(V,INV) as an input and it should work. 
 */
public class FunctionInitOracle implements InitOracle{
	
	/***
	 *  resetting function for Windows 8
	 */
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

	/***
	 *  the resetting function is stateless 
	 */
	public void setDefault() {
		
	}
}
