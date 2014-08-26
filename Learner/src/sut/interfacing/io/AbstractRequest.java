package sut.interfacing.io;

import sut.mapper.FlagSet;
import sut.mapper.Symbol;

public class AbstractRequest extends AbstractMessage{
	public AbstractRequest(FlagSet flags, Symbol seqSymbol, Symbol ackSymbol) {
		super(flags, seqSymbol, ackSymbol);
	}
}
