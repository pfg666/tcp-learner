package sut.interfacing.io;

import sut.mapper.FlagSet;

public class ConcreteResponse extends ConcreteMessage{
	public ConcreteResponse(FlagSet flags, long seqNumber, long ackNumber) {
		super(flags, seqNumber, ackNumber);
	}
}
