package sut.mapper.tested;

import sut.interfacing.init.InitOracle;
import sut.mapper.TCPMapper;
import sut.mapper.Flag;
import sut.mapper.Symbol;

public class U1410Mapper extends TCPMapper {
	public U1410Mapper() {
		super(new Ubuntu1410FunctionInitOracle());
	}
}

class Ubuntu1410FunctionInitOracle implements InitOracle {
	public Ubuntu1410FunctionInitOracle() {

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
