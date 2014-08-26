package sut.interfacing.io;

import sut.interfacing.Serializer;
import sut.mapper.FlagSet;

public class ConcreteMessage {
	public final FlagSet flags;
	public final long seqNumber;
	public final long ackNumber;

	public ConcreteMessage(FlagSet flags, long seqNumber, long ackNumber) {
		super();
		this.flags = flags;
		this.seqNumber = seqNumber;
		this.ackNumber = ackNumber;
	}
	
	public String serialize() {
		return Serializer.concreteMessageToString(flags, seqNumber, ackNumber);
	}
}
