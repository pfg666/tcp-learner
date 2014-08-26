package sut.interfacing.io;

import sut.mapper.FlagSet;

public class ConcreteRequest extends ConcreteMessage{
	public ConcreteRequest(FlagSet flags, long seqNumber, long ackNumber) {
		super(flags, seqNumber, ackNumber);
	}
}
