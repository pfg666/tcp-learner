package sutInterface.tcp.init;

import sutInterface.tcp.Flag;
import sutInterface.tcp.Symbol;
import sutInterface.tcp.TCPMapper;
import util.Log;

/**
 * An initial state oracle designed to work for Windows 8 and Linux for a very limited alphabet. When this function is applied on the mapper,
 * it should tell if the sut is in the initial state (hence, all numbers in the follow up input are valid) or it's isn't (and only
 * a limited selection is valid)
 * 
 * The problem in Linux is that RST+ACK(V,INV) which based on the specification, should reset the system, doesn't in some cases. 
 * More specifically, if the SUT is in SYN_RECV, then RST+ACK packets are dropped and ignored should the ack. num. be invalid.
 * 
 * If you use this, remove RST+ACK(V,INV) as an input and it should work. 
 */
public class FunctionalInitOracle implements InitOracle{
	
	/***
	 *  resetting function for Windows 8
	 */
	public Boolean isResetting(TCPMapper mapper) {
		boolean isInitial = false;
		
		if(mapper.isLastResponseTimeout) {
            isInitial = (mapper.lastPacketSent.flags.has(Flag.RST) && mapper.lastPacketSent.seq.is(Symbol.V)) ||
                            mapper.isInit; 
	    } else {
	            isInitial = (mapper.lastPacketReceived.flags.has(Flag.RST) && mapper.lastPacketSent.seq.is(Symbol.V)) &&
	                            mapper.lastPacketSent.flags.has(Flag.SYN);
	    }
		Log.info("Is initial state: " + isInitial);
		return isInitial;
	}

	/***
	 *  the resetting function is stateless 
	 */
	public void setDefault() {
		
	}
}