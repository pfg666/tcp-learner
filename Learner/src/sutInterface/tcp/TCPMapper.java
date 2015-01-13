package sutInterface.tcp;

import sutInterface.Serializer;
import sutInterface.tcp.init.FunctionInitOracle;
import sutInterface.tcp.init.InitOracle;
import util.Calculator;

/**
 * Mapper component from abs to conc and conc to abs. Does NOT handle TIMEOUTS.
 * 
 * @author paul
 */

public class TCPMapper {
	public static final long NOT_SET = Integer.MAX_VALUE;
	public static final long DATA_LENGTH = 4;
	public static final long WIN_SIZE = 8192;

	/* data variables of the mapper, determined from request/responses */
	public long lastSeqSent, lastAckSent, serverSeq, clientSeq;
	public long dataAcked;
	public FlagSet lastFlagsSent;
	public FlagSet lastFlagsReceived;
	public Symbol lastAbstractSeqSent;
	public Symbol lastAbstractAckSent;
	public Symbol lastAbstractSeqReceived;
	public Symbol lastAbstractAckReceived;
	
	/* The only purpose of this is to inform the cache init oracle of the last action sent */
	public Action lastActionSent;
	public boolean isLastInputAnAction = false;

	/*
	 * boolean state variables, determined from data variables and the current
	 * values of the boolean variables
	 */
	public boolean isInit;
	public boolean isLastResponseTimeout;
	
	
	private InitOracle oracle;

	public TCPMapper() {
		this( new FunctionInitOracle());
	}
	
	public TCPMapper(InitOracle oracle) {
		this.oracle = oracle;
		setDefault();
	}
	
	public InitOracle getInitOracle() {
		return this.oracle;
	}
	
	public void setInitOracle(InitOracle oracle) {
		this.oracle = oracle;
	}

	/* sets all the variables to their default values */
	public void setDefault() {
		this.lastSeqSent = this.lastAckSent = NOT_SET;
		this.serverSeq = this.clientSeq = NOT_SET;
		this.lastFlagsSent = FlagSet.EMPTY;
		this.lastFlagsReceived = FlagSet.EMPTY;
		this.lastAbstractSeqSent = this.lastAbstractAckSent = Symbol.INV;
		this.lastAbstractSeqReceived = this.lastAbstractAckReceived = Symbol.INV;
		this.isInit = true;
		this.isLastResponseTimeout = false;
		this.isLastInputAnAction = false;
		if(this.oracle != null)
			this.oracle.setDefault();
	}

	/* checks whether the abstractions are defined for the given inputs */
	public boolean isConcretizable(Symbol abstractSeq, Symbol abstractAck) {
		return !this.isInit || (!Symbol.INV.equals(abstractAck) && !Symbol.INV.equals(abstractSeq));
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
		if(this.isInit == true) {
			this.clientSeq = concreteSeq;
		}
		this.lastSeqSent = concreteSeq;
		this.lastAckSent = concreteAck;
		this.lastFlagsSent = flags;
		this.lastAbstractSeqSent = abstractSeq;
		this.lastAbstractAckSent = abstractAck;
		this.isLastInputAnAction = false;

		/* build concrete input */
		String concreteInput = Serializer.concreteMessageToString(flags,
				concreteSeq, concreteAck);
		return concreteInput;
	}
	
	public String processOutgoingReset() {
		return Serializer.concreteMessageToString(new FlagSet(Flag.RST), (clientSeq == NOT_SET)?0:clientSeq, 0);
	}
	
	public void processOutgoingAction(Action action) {
		this.lastActionSent = action;
		this.isLastInputAnAction = true;
	}
	
	private long newInvalidWithinWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 2), Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 1));
	}
	
	//modSum(serverSeq, maxNum/2+2), modSum(serverSeq, maxNum - win + 1), modSum(serverSeq, -8191)
	private long newInvalidOutsideWindow(long refNumber) {
		return Calculator.randWithinRange(Calculator.sum(refNumber, Calculator.MAX_NUM/2 + 2), Calculator.sum(refNumber, Calculator.MAX_NUM - WIN_SIZE + 1));
	}
	
	private long getConcrete(Symbol absToSend, long nextValidNumber) {
		long nextNumber;
		switch (absToSend) {
		case RAND:
			nextNumber = Calculator.newValue();
		case V:
			nextNumber = nextValidNumber;
			break;
		case INV:
			nextNumber = Calculator.newOtherThan(nextValidNumber);
			break;
		case IWIN:
			nextNumber = newInvalidWithinWindow(this.serverSeq);
			break;
		case OWIN:
			nextNumber = newInvalidOutsideWindow(this.serverSeq);
			break;
		case WIN:  //not yet tried
			//nextNumber = Gen.randWithinRange(Gen.sum(1, nextValidNumber), Gen.sum(WIN_SIZE, nextValidNumber));
			nextNumber = Calculator.randWithinRange(Calculator.sub(nextValidNumber, WIN_SIZE ), Calculator.sub(nextValidNumber, 1));
			break;
		default:
			throw new RuntimeException("Invalid parameter \"" + absToSend
					+ "\". The input-action used ");
		}
		return nextNumber;
	}

	public long getNextValidSeq() {
		long nextSeq;
		if (this.isInit == true) {
			nextSeq = Calculator.newValue();
		} else {
			nextSeq = this.clientSeq;
		}
		return nextSeq;
	}

	public long getNextValidAck() {
		long nextAck;
		if (this.isInit == true) {
			nextAck = Calculator.newValue();
		} else {
			nextAck = Calculator.next(this.serverSeq);
		}
		return nextAck;
	}
	
	public void processIncomingTimeout() {
		/* state 0 detecting condition */
		this.isLastResponseTimeout = true;
		this.isInit = checkInit(true);
	}
	
	public String processIncomingResponse(FlagSet flags, long concreteSeq,
			long concreteAck) {
		/* generate output symbols */
		Symbol abstractSeq = getAbstract(concreteSeq);
		Symbol abstractAck = getAbstract(concreteAck);
		
		/* do updates on output */
		if (abstractAck == Symbol.SNCLIENTP1 || abstractAck == Symbol.SNCLIENTPD) {
			this.clientSeq = concreteAck;
		}
		if (abstractSeq == Symbol.FRESH || abstractSeq == Symbol.SNSERVERP1) {
			this.serverSeq = concreteSeq;
		}
		
		/* state 0 detecting condition */
		this.isLastResponseTimeout = false;
		this.lastFlagsReceived = flags;
		this.lastAbstractSeqReceived = abstractSeq;
		this.lastAbstractAckReceived = abstractAck;
		this.isInit = checkInit(false);

		/* build concrete output */
		String abstractOutput = Serializer.abstractMessageToString(
				flags, abstractSeq,
				abstractAck);
		return abstractOutput;
	}

	private Symbol getAbstract(long nrReceived) {
		Symbol checkedSymbol;
		if (nrReceived == Calculator.next(this.clientSeq)) {
			checkedSymbol = Symbol.SNCLIENTP1;
		} else if (nrReceived == Calculator.nth(this.clientSeq, 2)) {
			checkedSymbol = Symbol.SNCLIENTP2;
		} else if (nrReceived == this.clientSeq) {
			checkedSymbol = Symbol.SNCLIENT;
		} else if (nrReceived == this.serverSeq) {
			checkedSymbol = Symbol.SNSERVER;
		} else if (nrReceived == Calculator.next(this.serverSeq)) {
			checkedSymbol = Symbol.SNSERVERP1;
		} else if (nrReceived == Calculator.nth(this.serverSeq, 2)) {
			checkedSymbol = Symbol.SNSERVERP2;
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
	
	protected boolean checkInit(boolean isTimeout) {
		return oracle.isResetting(this);
	}

	public String getState() {
		return "MAPPER[INIT=" + this.isInit + "; " +
				"lastSeqSent=" + this.lastSeqSent + 
				"; lastAckSent=" + this.lastAckSent + 
				"; lastValidClientSeq=" + this.clientSeq + 
				"; lastValidServerSeq=" + this.serverSeq + "]";
	}

	public String processOutgoingRequest(String flags, String abstractSeq,
			String abstractAck) {
		System.out.println(this);

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
	/*public static void main(String[] args) {
		TCPMapper mapper = new TCPMapper(null);
		for (int i = 0; i < 1000; i++) {
			System.out.println(mapper.newInvalidOutsideWindow(10000));
			System.out.println(mapper.newInvalidWithinWindow(10000));	
		}
	}*/
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Class<TCPMapper> mapperClass = TCPMapper.class;
		sb.append("Mapper state:\n");
		
		sb.append("lastSeqSent: " + lastSeqSent + "   ");
		sb.append("lastAckSent: " + lastAckSent + "   ");
		sb.append("initialServerSeq: " + initialServerSeq + "   ");
		sb.append("lastValidClientSeq: " + lastValidClientSeq + "   ");
		sb.append("dataAcked: " + dataAcked + "   ");
		sb.append("lastFlagsSent: " + lastFlagsSent + "   ");
		sb.append("lastFlagsReceived: " + lastFlagsReceived + "   ");
		sb.append("lastAbstractSeqSent: " + lastAbstractSeqSent + "   ");
		sb.append("lastAbstractAckSent: " + lastAbstractAckSent + "   ");
		sb.append("lastAbstractSeqReceived: " + lastAbstractSeqReceived + "   ");
		sb.append("lastAbstractAckReceived: " + lastAbstractAckReceived + "   ");
		sb.append("isInit: " + isInit + "   ");
		sb.append("isLastResponseTimeout: " + isLastResponseTimeout + "   ");
		/*
		 * boolean state variables, determined from data variables and the current
		 * values of the boolean variables
		 */
		return sb.toString();
	}
}
