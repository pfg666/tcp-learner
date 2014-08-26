package sut.interfacing.io;

import sut.interfacing.Serializer;
import sut.mapper.FlagSet;
import sut.mapper.Symbol;

public abstract class AbstractMessage {
	public final FlagSet flags;
	public final Symbol seqSymbol;
	public final Symbol ackSymbol;
	
	public AbstractMessage(FlagSet flags, Symbol seqSymbol, Symbol ackSymbol) {
		super();
		this.flags = flags;
		this.seqSymbol = seqSymbol;
		this.ackSymbol = ackSymbol;
	}
	
	public String serialize() {
		return Serializer.abstractMessageToString(flags, seqSymbol, ackSymbol);
	}
}
