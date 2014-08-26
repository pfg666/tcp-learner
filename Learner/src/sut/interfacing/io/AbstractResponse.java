package sut.interfacing.io;

import sut.mapper.FlagSet;
import sut.mapper.Symbol;

public class AbstractResponse extends AbstractMessage {
	public AbstractResponse(FlagSet flags, Symbol seqSymbol, Symbol ackSymbol) {
		super(flags, seqSymbol, ackSymbol);
	}
}
