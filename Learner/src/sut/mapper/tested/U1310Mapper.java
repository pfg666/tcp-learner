package sut.mapper.tested;

import sut.interfacing.init.InitOracle;
import sut.mapper.TCPMapper;
import sut.mapper.Flag;
import sut.mapper.Symbol;

/**
 * TCP Mapper for Ubuntu. Works with all valid combinations and some invalid combinations.
 */

public class U1310Mapper extends TCPMapper{
	public U1310Mapper() {
		super(new Ubuntu1310FunctionInitOracle());
	}
}

class Ubuntu1310FunctionInitOracle implements InitOracle {
	public Ubuntu1310FunctionInitOracle() {
	}

	@Override
	public boolean isResetting(TCPMapper mapper) {
		boolean isInit = false;
		if (mapper.isLastResponseTimeout) {
			isInit = (mapper.lastFlagsSent.is(Flag.RST) && mapper.lastAbstractSeqSent.is(Symbol.V)) ||
			(mapper.lastFlagsSent.is(Flag.ACK, Flag.RST)  && mapper.lastAbstractSeqSent.is(Symbol.V) && mapper.lastAbstractAckSent.is(Symbol.V)) || // && mapper.lastFlagsReceived.is(Flag.SYN, Flag.ACK)) ||  //t2
			mapper.isInit;	
		} else {
			isInit = 	(mapper.lastFlagsSent.has(Flag.ACK) && mapper.lastFlagsReceived.is(Flag.RST) && mapper.lastAbstractSeqSent.is(Symbol.V) && mapper.lastAbstractAckSent.is(Symbol.V))  || //t1
					(mapper.lastFlagsSent.has(Flag.RST) && mapper.lastAbstractSeqSent.is(Symbol.V)) || // covers RST[+ACK](V,_)->
					(mapper.lastFlagsSent.has(Flag.SYN) && mapper.lastFlagsReceived.is(Flag.RST,Flag.ACK) && mapper.lastAbstractSeqSent.is(Symbol.V)); //
		}
		return isInit;
	}

	@Override
	public void setDefault() {

	}
}