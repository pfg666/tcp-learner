package sutInterface.tcp;

import sutInterface.Serializer;
import sutInterface.tcp.init.ClientInitOracle;
import sutInterface.tcp.init.FunctionalInitOracle;
import sutInterface.tcp.init.InitOracle;
import util.Calculator;
import util.exceptions.BugException;

/**
 * Mapper component from abs to conc and conc to abs.
 * 
 * @author paul
 */

public class TCPMapper {
	public static final long NOT_SET = -3; //Integer.MIN_VALUE;
	public static final long DATA_LENGTH = 4;
	public static final long WIN_SIZE = 8192;

	/* data variables of the mapper, determined from request/responses */
	public long lastSeqSent, lastAckSent, serverSeq, clientSeq;
	public long dataAcked;
	public long lastAckReceived;
	public long lastSeqReceived;
	public Packet lastPacketSent;
	public Packet lastPacketReceived;
	
	/* strings updated for all requests (actions, packets) and responses (timeout, packets) */
	public String lastMessageSent;
	public String lastMessageReceived;
	
	/* The only purpose of this is to inform the cache init oracle of the last action sent */
	public Action lastActionSent;
	public boolean isLastInputAnAction;

	/*
	 * boolean state variables, determined from data variables and the current
	 * values of the boolean variables
	 */
	public boolean freshSeqEnabled;
	public boolean freshAckEnabled;
	public boolean startState;
	public boolean isLastResponseTimeout;
	
	
	private InitOracle oracle;

	public TCPMapper() {
		this( new ClientInitOracle());
		setDefault();
		//this( new FunctionalInitOracle());
		//this( new CachedInitOracle(new InitCacheManager("/home/student/GitHub/tcp-learner/output/1421437324088/cache.txt")));
	}
	
	public TCPMapper(InitOracle oracle) {
		setInitOracle(oracle);
		//by default, we assume that the start state is the listening state
		setStartState(true);  
		setDefault();
	}
	
	public TCPMapper clone() {
		TCPMapper mapper = new TCPMapper();
		mapper.setInitOracle(this.getInitOracle());
		mapper.serverSeq = this.serverSeq;
		mapper.clientSeq = this.clientSeq;
		mapper.lastSeqSent = this.lastSeqSent;
		mapper.lastAckSent = this.lastAckSent;
		mapper.lastPacketSent = this.lastPacketSent;
		mapper.lastPacketReceived = this.lastPacketReceived;
		mapper.lastActionSent = this.lastActionSent;
		mapper.freshSeqEnabled = this.freshSeqEnabled;
		mapper.freshAckEnabled = this.freshAckEnabled;
		mapper.isLastInputAnAction = this.isLastInputAnAction;
		mapper.startState = this.startState;
		return mapper;
	}
	
	public void setStartState(boolean isListening) {
		this.startState = isListening;
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
		this.lastSeqReceived = this.lastAckReceived = NOT_SET;
		this.serverSeq = this.clientSeq = NOT_SET;
		this.lastPacketSent = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV);
		this.lastPacketReceived = new Packet(FlagSet.EMPTY, Symbol.INV, Symbol.INV);			
		this.freshSeqEnabled = this.startState;
		this.freshAckEnabled = this.startState;
		this.isLastResponseTimeout = false;
		this.isLastInputAnAction = false;
		if(this.oracle != null)
			this.oracle.setDefault();
	}

	/* checks whether the abstractions are defined for the given inputs */
	public boolean isConcretizable(Symbol abstractSeq, Symbol abstractAck) {
		return !this.freshSeqEnabled || (!Symbol.INV.equals(abstractAck) && !Symbol.INV.equals(abstractSeq));
	}
	
	public String processOutgoingRequest(FlagSet flags, Symbol abstractSeq,
			Symbol abstractAck) {
		this.lastActionSent = null; // no action sent
		
		/* check if abstraction is defined */
		if (!isConcretizable(abstractSeq, abstractAck)) {
			return Symbol.UNDEFINED.toString();
		}

		/* generate input numbers */
		long concreteSeq = getConcrete(abstractSeq, getNextValidSeq());
		long concreteAck = getConcrete(abstractAck, getNextValidAck());
		
		/* do updates on input */
		if(this.freshSeqEnabled == true) {
			this.clientSeq = concreteSeq;
		}
		this.lastSeqSent = concreteSeq;
		this.lastAckSent = concreteAck;
		this.isLastInputAnAction = false;
		this.lastPacketSent = new Packet(flags, abstractSeq, abstractAck);
		this.lastMessageSent = this.lastPacketSent.serialize();
		
		checkInit(true);
		
		/* build concrete input */
		String concreteInput = Serializer.concreteMessageToString(flags,
				concreteSeq, concreteAck);
		return concreteInput;
	}
	
	public String processOutgoingReset() {
		return (clientSeq == NOT_SET)? null : Serializer.concreteMessageToString(new FlagSet(Flag.RST), clientSeq, 0);
	}
	
	public void processOutgoingAction(Action action) {
		this.lastActionSent = action;
		this.lastMessageSent = action.name();
		this.isLastInputAnAction = true;
		
		checkInit(true);
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
		if (this.freshSeqEnabled == true) {
			nextSeq = Calculator.newValue();
		} else {
			nextSeq = this.clientSeq;
		}
		return nextSeq;
	}

	public long getNextValidAck() {
		long nextAck;
		if (this.freshAckEnabled == true) {
			nextAck = Calculator.newValue();
		} else {
			if (this.lastPacketReceived != null) {
				nextAck = Calculator.nth(this.serverSeq, this.lastPacketReceived.payload());
			} else {
				nextAck = this.serverSeq;
			}
		}
		return nextAck;
	}
	
	public void processIncomingTimeout() {
		/* state 0 detecting condition */
		this.isLastResponseTimeout = true;
		this.lastMessageReceived = "TIMEOUT";
		//this.lastPacketReceived = null;
		checkInit(false);
	}
	
	public String processIncomingResponse(FlagSet flags, long concreteSeq,
			long concreteAck) {
		/* generate output symbols */
		Symbol abstractSeq = getAbstract(concreteSeq, true);
		Symbol abstractAck = getAbstract(concreteAck, false);
		
		/* do updates on output */
		if (abstractAck == Symbol.SNCLIENTP1 || abstractAck == Symbol.SNCLIENTPD) {
			this.clientSeq = concreteAck;
		}
		if (abstractSeq == Symbol.FRESH || abstractSeq == Symbol.SNSERVERP1) {
			this.serverSeq = concreteSeq;
		}
		
		/* state 0 detecting condition */
		this.isLastResponseTimeout = false;
		this.lastSeqReceived = concreteSeq;
		this.lastAckReceived = concreteAck;
		this.lastPacketReceived = new Packet(flags, abstractSeq, abstractAck);
		this.lastMessageReceived = this.lastPacketReceived.serialize();

		/* build concrete output */
		String abstractOutput = Serializer.abstractMessageToString(
				flags, abstractSeq,
				abstractAck);
		
		checkInit(false);
		
		return abstractOutput;
	}

	private Symbol getAbstract(long nrReceived, boolean isIncomingSeq) {
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
		} else if (isIncomingSeq == true && this.freshAckEnabled) {
			checkedSymbol = Symbol.FRESH;
		} else {
			checkedSymbol = Symbol.INV;
		}
		return checkedSymbol;
	}
	
	protected void checkInit(boolean outgoing) {
		/*Boolean isResetting = oracle.isResetting(this); 
		if (isResetting == null) {
			throw new BugException("Oracle doesn't know the init value");
		} else {
			return isResetting.booleanValue();
		}*/
		
		
//		if(outgoing == false) {
//			if(isLastResponseTimeout) {
//	            freshSeqEnabled = (lastPacketSent.flags.has(Flag.RST) && lastPacketSent.seq.is(Symbol.V)) ||
//	                            freshSeqEnabled; 
//	            freshAckEnabled = (lastPacketSent.flags.has(Flag.RST) && lastPacketSent.seq.is(Symbol.V)) ||
//	            				freshAckEnabled;
//		    } else {
//			    freshSeqEnabled = (lastPacketReceived.flags.has(Flag.RST) && lastPacketSent.seq.is(Symbol.V)) &&
//			                            lastPacketSent.flags.has(Flag.SYN);
//			    freshAckEnabled = 
//		    } 
//		} else {
//			
//			
//		}
		if (outgoing ) {
//			if (freshSeqEnabled && lastAckReceived == lastSeqReceived+1) {
//				freshSeqEnabled = false;
//			} 
			//freshSeqEnabled = false;
			
		} else {
			boolean sentRST = lastPacketSent.flags.has(Flag.RST) && this.lastPacketSent.seq.is(Symbol.V);
			boolean receivedRST = !isLastResponseTimeout && lastPacketReceived.flags.has(Flag.RST);
			
			
			if (!freshSeqEnabled && (receivedRST || sentRST) || this.lastSeqReceived == 0) {
				freshSeqEnabled = true;
			} else {
				freshSeqEnabled = !(lastPacketSent.flags.has(Flag.SYN)  
						&& this.lastAckReceived == this.lastSeqSent + 1)
						&& freshSeqEnabled;
			}
	
			if (receivedRST || sentRST) {
				freshAckEnabled = true;
			} else if (!outgoing && lastSeqReceived == lastAckSent || (!isLastResponseTimeout && lastPacketReceived.flags.is(Flag.SYN))) {
				freshAckEnabled = false;
			}
		}
	}

	public String getState() {
		return "MAPPER [FRESH_SEQ=" + this.freshSeqEnabled + "; " +
				"FRESH_ACK=" + this.freshAckEnabled + "; " +
				"lastSeqSent=" + this.lastSeqSent + 
				"; lastAckSent=" + this.lastAckSent + 
				"; clientSeq=" + this.clientSeq + 
				"; serverSeq=" + this.serverSeq + "]";
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
		sb.append("Mapper state:\n");
		
		sb.append("serverSeq: " + serverSeq + "   ");
		sb.append("clientSeq: " + clientSeq + "   ");
		sb.append("packetSent: " + lastPacketSent + "   ");
		sb.append("packetRecv: " + lastPacketReceived + "   ");
//		sb.append("lastSeqSent: " + lastSeqSent + "   ");
//		sb.append("lastAckSent: " + lastAckSent + "   ");
//		sb.append("dataAcked: " + dataAcked + "   ");
//		sb.append("lastFlagsSent: " + lastFlagsSent + "   ");
//		sb.append("lastFlagsReceived: " + lastFlagsReceived + "   ");
//		sb.append("lastAbstractSeqSent: " + lastAbstractSeqSent + "   ");
//		sb.append("lastAbstractAckSent: " + lastAbstractAckSent + "   ");
//		sb.append("lastAbstractSeqReceived: " + lastAbstractSeqReceived + "   ");
//		sb.append("lastAbstractAckReceived: " + lastAbstractAckReceived + "   ");
		sb.append("isInit: " + freshSeqEnabled + "   ");
		sb.append("isLastResponseTimeout: " + isLastResponseTimeout + "   ");
		/*
		 * boolean state variables, determined from data variables and the current
		 * values of the boolean variables
		 */
		return sb.toString();
	}
}
