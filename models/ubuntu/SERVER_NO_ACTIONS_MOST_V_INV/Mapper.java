package sut.mapper;

import sut.interfacing.Serializer;
import util.Gen;

/**
 * Mapper component from abs to conc and conc to abs. Does NOT handle TIMEOUTS.
 * 
 * @author paul
 */

public class Mapper {
	public static final long NOT_SET = Integer.MAX_VALUE;
	public static final long DATA_LENGTH = 4;

	/* data variables of the mapper, determined from request/responses */
	protected long lastSeqSent, lastAckSent, initialServerSeq,
			lastValidClientSeq;
	protected FlagSet lastFlagsSent;
	protected FlagSet lastFlagsReceived;
	protected Symbol lastAbstractSeqSent;
	protected Symbol lastAbstractAckSent;
	protected Symbol lastAbstractSeqReceived;
	protected Symbol lastAbstractAckReceived;

	/*
	 * boolean state variables, determined from data variables and the current
	 * values of the boolean variables
	 */
	protected boolean isInit;
	protected boolean isLastResponseTimeout;

	/*
	 * set it to true whenever you want to log a trace meeting your conditions
	 * of choice
	 */
	public boolean [] isTraceable;
	

	public Mapper() {
		setDefault();
	}

	/* sets all the variables to their default values */
	public void setDefault() {
		this.lastSeqSent = this.lastAckSent = NOT_SET;
		this.initialServerSeq = this.lastValidClientSeq = NOT_SET;
		this.lastFlagsSent = FlagSet.EMPTY;
		this.lastFlagsReceived = FlagSet.EMPTY;
		this.lastAbstractSeqSent = this.lastAbstractAckSent = Symbol.INV;
		this.lastAbstractSeqReceived = this.lastAbstractAckReceived = Symbol.INV;
		this.isInit = true;
		this.isTraceable = new boolean [] {false, false, false, false};
		this.isLastResponseTimeout = false;
	}

	/* checks whether the abstractions are defined for the given inputs */
	public boolean isConcretizable(String abstractSeq, String abstractAck) {
		return !this.isInit || (Symbol.V.equals(abstractAck) && Symbol.V.equals(abstractSeq));
	}
	
	public boolean isConcretizable(Symbol abstractSeq, Symbol abstractAck) {
		return !this.isInit || (Symbol.V.equals(abstractAck) && Symbol.V.equals(abstractSeq));
	}
	
	public String processOutgoingRequest(FlagSet flags, Symbol abstractSeq,
			Symbol abstractAck) {
		/* check if abstraction is defined */
		if (!isConcretizable(abstractSeq, abstractAck)) {
			return Symbol.UNDEFINED.toString();
		}

		/* generate input numbers */
		long concreteSeq = getConcrete(abstractSeq, getNextValidSeq());
		long concreteAck = getConcrete(abstractAck, getNextValidAck());
		
		/* do updates on input */
		//this.isInit = (flags.is(Flag.SYN) && this.lastFlagsReceived.is(Flag.SYN, Flag.ACK)) || this.isInit;
		if(this.isInit == true && flags.is(Flag.SYN) && abstractSeq.is(Symbol.V)) {
			this.lastValidClientSeq = concreteSeq;
		}
		this.lastSeqSent = concreteSeq;
		this.lastAckSent = concreteAck;
		this.lastFlagsSent = flags;
		this.lastAbstractSeqSent = abstractSeq;
		this.lastAbstractAckSent = abstractAck;

		/* build abstract input */
		String concreteInput = Serializer.concreteMessageToString(flags,
				concreteSeq, concreteAck);
		return concreteInput;
	}
	
	public long getConcrete(Symbol absToSend, long nextValidNumber) {
		long nextNumber;
		switch (absToSend) {
		case V:
			nextNumber = nextValidNumber;
			break;
		case INV:
			nextNumber = Gen.newOtherThan(nextValidNumber);
			break;
		default:
			throw new RuntimeException("Invalid parameter \"" + absToSend
					+ "\". The input-action used (should be \"V\" or \"INV\")");
		}
		return nextNumber;
	}

	public long getNextValidSeq() {
		long nextSeq;
		if (this.isInit == true) {
			nextSeq = Gen.newValue();
		} else {
			nextSeq = this.lastValidClientSeq;
		}
		return nextSeq;
	}

	public long getNextValidAck() {
		long nextAck;
		if (this.isInit == true) {
			nextAck = Gen.newValue();
		} else {
			nextAck = Gen.next(this.initialServerSeq);
		}
		return nextAck;
	}
	
	public void processIncomingTimeout() {
		/* state 0 detecting condition */
		this.isInit = 
				(this.lastFlagsSent.is(Flag.RST) && this.lastAbstractSeqSent.is(Symbol.V)) ||
				(this.lastFlagsSent.is(Flag.ACK, Flag.RST)  && this.lastAbstractSeqSent.is(Symbol.V) && this.lastAbstractAckSent.is(Symbol.V)) || 
				this.isInit;
		this.isLastResponseTimeout = true;
	}
	
	public String processIncomingResponse(FlagSet flags, long concreteSeq,
			long concreteAck) {
		/* generate output symbols */
		Symbol abstractSeq = getAbstract(concreteSeq);
		Symbol abstractAck = getAbstract(concreteAck);
		
		/* do updates on output */
		if (abstractAck == Symbol.SNCLIENTP1) {
			this.lastValidClientSeq = concreteAck;
		}
		if (abstractSeq == Symbol.FRESH) {
			this.initialServerSeq = concreteSeq;
		}
		
		System.out.println((this.isInit && !flags.is(Flag.SYN, Flag.ACK)));
		
		/* state 0 detecting condition */
		this.isTraceable[0] = (this.isInit && !flags.is(Flag.SYN, Flag.ACK));
		this.isInit =
				(this.lastFlagsSent.has(Flag.ACK) && flags.is(Flag.RST) && this.lastAbstractSeqSent.is(Symbol.V) && this.lastAbstractAckSent.is(Symbol.V))  || //t1
				(this.lastFlagsSent.has(Flag.RST) && this.lastAbstractSeqSent.is(Symbol.V)) || // covers RST[+ACK](V,_)->
				(this.lastFlagsSent.has(Flag.SYN) && flags.is(Flag.RST,Flag.ACK) && this.lastAbstractSeqSent.is(Symbol.V)) ||// || 
				(this.isInit && !flags.is(Flag.SYN, Flag.ACK)); //|| this.isInit; // && this.lastAbstractSeqSent.is(Symbol.V) && this.lastAbstractAckSent.is(Symbol.INV));
		this.isTraceable[1] = (this.lastFlagsSent.has(Flag.ACK) && flags.is(Flag.RST) && this.lastAbstractSeqSent.is(Symbol.V) && this.lastAbstractAckSent.is(Symbol.V));
		this.isTraceable[2] = (this.lastFlagsSent.has(Flag.RST) && this.lastAbstractSeqSent.is(Symbol.V));
		this.isTraceable[3] = (this.lastFlagsSent.has(Flag.SYN) && flags.is(Flag.RST,Flag.ACK) && this.lastAbstractSeqSent.is(Symbol.V));
		this.isLastResponseTimeout = false;
		this.lastFlagsReceived = flags;
		this.lastAbstractSeqReceived = abstractSeq;
		this.lastAbstractAckReceived = abstractAck;

		/* build concrete output */
		String abstractOutput = Serializer.abstractMessageToString(
				flags, abstractSeq,
				abstractAck);
		return abstractOutput;
	}

	public Symbol getAbstract(long nrReceived) {
		Symbol checkedSymbol;
		if (nrReceived == Gen.next(this.lastValidClientSeq)) {
			checkedSymbol = Symbol.SNCLIENTP1;
		} else if (nrReceived == this.lastValidClientSeq) {
			checkedSymbol = Symbol.SNCLIENT;
		} else if (nrReceived == this.initialServerSeq) {
			checkedSymbol = Symbol.SNSERVER;
		} else if (nrReceived == Gen.next(this.initialServerSeq)) {
			checkedSymbol = Symbol.SNSERVERP1;
		} else if (nrReceived == this.lastSeqSent) {
			checkedSymbol = Symbol.SNSENT;
		} else if (nrReceived == this.lastAckSent) {
			checkedSymbol = Symbol.ANSENT;
		} else if (nrReceived == 0) {
			checkedSymbol = Symbol.ZERO;
		} else if (this.isInit) {
			checkedSymbol = Symbol.FRESH;
		} else {
			checkedSymbol = Symbol.INV;
		}
		return checkedSymbol;
	}

	public String getState() {
		return "MAPPER[INIT=" + this.isInit + "; " +
				"lastSeqSent=" + this.lastSeqSent + 
				"; lastAckSent=" + this.lastAckSent + 
				"; lastValidClientSeq=" + this.lastValidClientSeq + 
				"; lastValidServerSeq=" + this.initialServerSeq + "]";
	}

	public String processOutgoingRequest(String flags, String abstractSeq,
			String abstractAck) {

		/* generate enum classes */
		Symbol seqSymbol = Symbol.toSymbol(abstractSeq);
		Symbol ackSymbol = Symbol.toSymbol(abstractAck);
		FlagSet flagSet = new FlagSet(flags);
		
		/* call actual method */
		String concreteInput = processOutgoingRequest(flagSet, seqSymbol, ackSymbol);
		return concreteInput;
	}	
	
	public String processIncomingResponse(String flags, long concreteSeq,
			long concreteAck) {
		
		/* generate enum classes */
		FlagSet flagSet = new FlagSet(flags.toCharArray());
		
		/* call actual method */
		String abstractOutput = processIncomingResponse(flagSet, concreteSeq, concreteAck);
		return abstractOutput;
	}

	/* compatibility version */
	public String processIncomingResponseComp(String flags, String seqReceived,
			String ackReceived) {
		long seq = Long.valueOf(seqReceived);
		long ack = Long.valueOf(ackReceived);
		String abstractOutput = processIncomingResponse(flags, seq, ack);
		return abstractOutput;
	}
}
