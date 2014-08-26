package sut.mapper.tested;

import sut.interfacing.init.InitOracle;
import sut.mapper.TCPMapper;
import sut.mapper.Flag;
import sut.mapper.Symbol;

/**
 * Windows 8 Mapper component
 * 
 * @author paul
 *
 */

public class WIN8Mapper extends TCPMapper{

	public WIN8Mapper() {
		super(new Win8FunctionInitOracle());
	}
	
}

class Win8FunctionInitOracle implements InitOracle{
	
	@Override
	public boolean isResetting(TCPMapper mapper) {
		System.out.println("CALLLLLLLLLLLL");
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

	@Override
	public void setDefault() {
		
	}
}
