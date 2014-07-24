package sut.interfacing;


/**
 * Mapper component from abs to conc and conc to abs. Does NOT handle TIMEOUTS.
 * 
 * @author paul
 */

public class Mapper {
	public static final String VALID = "V";
	public static final String INVALID = "INV";
	public static final String UNDEFINED = "UNDEFINED";
	public static final long NOT_SET = 0;
	public static final long MAX_NUM = (long) (Math.pow(2, 32) - 1);

	/* data variables of the mapper, determined from request/responses */
	protected long lastSeqSent, lastAckSent, lastSeqReceived, lastAckReceived;
	protected long lastInvalidAckSent;
	
	
	/* boolean state variables, determined from data variables and the current values of the boolean variables */
	protected boolean isInit = true;
	protected boolean isConsidered = true; 
	
	/* set it to true whenever you want to log a trace meeting your conditions of choice */
	boolean isTraceable = false;
	

	public Mapper() {
		setDefault();
	}
	
	public void setDefault() {
		this.lastSeqSent = this.lastAckSent = this.lastSeqReceived = this.lastAckReceived = NOT_SET;
		this.lastInvalidAckSent = NOT_SET;
		this.isInit = true;
		this.isTraceable = false;
	}

	private long newValue() {
		return (long) (Math.random() * MAX_NUM);
	}

	private long newValueOtherThan(long value) {
		long newValue = 0;
		do {
			newValue = newValue();
		} while (newValue == value);
		return newValue;
	}

	private long nextValue(long regValue) {
		return (regValue + 1) % (MAX_NUM + 1);
	}

	public boolean isAbstractionDefined(String abstractSeq, String abstractAck) {
		return (VALID.equals(abstractAck) && VALID.equals(abstractSeq))
				|| (!this.isInit);
	}
	
	public String processOutgoingRequest(String flags, String abstractSeq,
			String abstractAck) {
		if (!isAbstractionDefined(abstractSeq, abstractAck)) {
			return UNDEFINED;
		}
		this.isConsidered = 
				((VALID.equals(abstractSeq) && VALID.equals(abstractAck))) || 
				(VALID.equals(abstractSeq) && flags.contains("S"));
		long seqToSend = processOutgoingSeq(flags, abstractSeq);
		long ackToSend = processOutgoingAck(flags, abstractAck);
		this.isInit = (this.isInit) || 
				(flags.contains("R") && VALID.equals(abstractSeq));
		String concreteInput = Serializer.concreteMessageToString(flags,
				seqToSend, ackToSend);
		return concreteInput;
	}
	
	public long getNextValidSeq() {
		long nextSeq;
		if (this.isInit == true) {
			nextSeq = newValue();
		} else {
			nextSeq = this.lastAckReceived;
		}
		return nextSeq;
	}
	
	public long getNextValidAck() {
		long nextAck;
		if (this.isInit == true) {
			nextAck = newValue();
		} else {
			nextAck = nextValue(this.lastSeqReceived);
		}
		return nextAck;
	}

	public long processOutgoingSeq(String flags, String syn) {
		long nextSeq = 0;
		if (syn.equals(VALID)) { // if seqNr is valid
			nextSeq = getNextValidSeq();
			this.lastSeqSent = nextSeq;
		} else if (syn.equals(INVALID)) {
			nextSeq = newValueOtherThan(getNextValidSeq()); // this.isInit will never be true here
		} else {
			throw new RuntimeException(
					"Invalid seq-parameter in input-action used (should be \"V\" or \"INV\")");
		}
		return nextSeq;
	}

	public long processOutgoingAck(String flags, String ack) {
		long nextAck;
		if (ack.equals(VALID)) { // if ackNr is valid
			nextAck = getNextValidAck();
			this.lastAckSent = nextAck;
		} else if (ack.equals(INVALID)) {
			nextAck = newValueOtherThan(getNextValidAck()); // this.isInit will never be true here
	//		this.lastAckSent = nextAck;
			this.lastInvalidAckSent = nextAck;
		} else {
			throw new RuntimeException(
					"Invalid ack-parameter in input-action used (should be \"V\" or \"INV\")");
		}
		return nextAck;
	}
	
	public String processIncomingResponseComp(String flags, String seqReceived, String ackReceived) {
		long seq = Long.valueOf(seqReceived);
		long ack = Long.valueOf(ackReceived);
		return processIncomingResponse(flags, seq, ack);
	}

	// all responses to UNDEFINED requests also yield UNDEFINED, see SutSocketWrapper.
	public String processIncomingResponse(String flags, long seqReceived,
			long ackReceived) {
		String seqValidity = processIncomingSeq(flags, seqReceived);
		String ackValidity = processIncomingAck(flags, ackReceived);
		this.isInit = (flags.contains("R"))&& this.isConsidered; //seqReceived != ackReceived;
		if(flags.equals("R"))
			this.isTraceable = true;
		String abstractInput = Serializer.abstractMessageToString(
				flags.toCharArray(), seqValidity, ackValidity);
		return abstractInput;
	}

	
	public String processIncomingSeq(String flags, long seqReceived) {
		String seqValidity;

		if (	(this.isInit)) {
			seqValidity = VALID;
			this.lastSeqReceived = seqReceived;
		} 
		else {
			if(		(seqReceived == this.lastSeqReceived) || 
					(flags.equals("A") && seqReceived == nextValue(this.lastSeqReceived)) ||
					(flags.equals("R") && seqReceived == this.lastInvalidAckSent) ||
					(flags.equals("R") && seqReceived == this.lastAckSent) ||
					(flags.equals("RA") && seqReceived == 0)) {
				seqValidity = VALID;
			} else {
				seqValidity = INVALID;
			}
		}
		return seqValidity;
	}

	public String processIncomingAck(String flags, long ackReceived) {
		String ackValidity = VALID;
		if (	(flags.contains("A") && ackReceived == nextValue(this.lastSeqSent))) {
			this.lastAckReceived = ackReceived;
			ackValidity = VALID;
		}
		else {
			
			if(		(flags.contains("A") && ackReceived == this.lastAckSent) ||
					(flags.equals("R") && ackReceived == this.lastAckReceived && ackReceived == this.lastSeqSent) || 
					(flags.contains("A") && ackReceived == this.lastAckReceived) || 
					(flags.equals("R") && ackReceived == this.lastInvalidAckSent) || 
					(flags.equals("R") && ackReceived == this.lastAckSent)) {  
				ackValidity = VALID;
			} else {
				ackValidity = INVALID;
			}
		}
			
		return ackValidity;
	}

	public String getState() {
		return "MAPPER[INIT=" + this.isInit + "; VALID=" + this.isConsidered + "; lastSeqSent="
				+ this.lastSeqSent + "; lastValidAckSent=" + this.lastAckSent +"; lastInvalidAckSent="+this.lastInvalidAckSent
				+ "; lastSeqReceived=" + this.lastSeqReceived
				+ "; lastAckReceived=" + this.lastAckReceived + "]";
	}
	
	public boolean imp(boolean c1, boolean c2) {
		return !c1 || c2;
	}
}
