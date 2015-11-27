package sutInterface.tcp;

import invlang.mapperReader.InvLangHandler;
import invlang.semantics.programTree.expressionTree.Expression;
import invlang.types.EnumValue;
import invlang.types.FlagSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import sutInterface.Serializer;

public class InvlangMapper {
	
	public enum Validity {
		VALID("VALID", "V"), INVALID("INV", "INV");

		private final String invlangRepresentation, learnerInput;
		
		private Validity(String invlangRepresentation, String learnerInput) {
			this.invlangRepresentation = invlangRepresentation;
			this.learnerInput = learnerInput;
		}
		
		public static Validity getValidity(String learnerInput) {
			for (Validity v : Validity.values()) {
				if (v.learnerInput.equals(learnerInput)) {
					return v;
				}
			}
			throw new RuntimeException("Unknown input validity '" + learnerInput + "'");
		}
		
		public String toInvLang() {
			return this.invlangRepresentation;
		}
	}
	
	public static final int NOT_SET = -3;
	
	private final InvLangHandler handler;
	private Expression lastConstraints; // for debugging purposes only
	
	public InvlangMapper(String mapperName) throws IOException {
		this(new File("input/mappers/" + mapperName));
	}
	
	public InvlangMapper(File file) throws IOException {
		try (BufferedReader input = new BufferedReader(new FileReader(file))) {
			int c;
			StringBuilder sb = new StringBuilder();
			while ((c = input.read()) != -1) {
				sb.append((char) c);
			}
			handler = new InvLangHandler(sb.toString(), true);
		}
	}
	
	public Map<String, Object> getState() {
		// create a new map, in which all (signed) integers are converted to unsigned, and unset integers are set to '?'
		Map<String, Object> state = new HashMap<>(this.handler.getState());
		for (Entry<String, Object> entry : this.handler.getState().entrySet()) {
			if (entry.getValue() instanceof Integer) {
				int value = (Integer) entry.getValue();
				if (value == InvlangMapper.NOT_SET) {
					state.put(entry.getKey(), "?");
				} else {
					state.put(entry.getKey(), InvlangMapper.getUnsignedInt(value));
				}
			}
		}
		return state;
	}

	public String processIncomingResponse(FlagSet flags, int seqNr, int ackNr) {
		handler.setFlags("flagsIn", flags);
		handler.setInt("concSeqIn", seqNr);
		handler.setInt("concAckIn", ackNr);
		handler.execute("incomingResponse");
		EnumValue absSeq = handler.getEnumResult("absSeqIn");
		EnumValue absAck = handler.getEnumResult("absAckIn");
		return Serializer.abstractMessageToString(flags, absSeq.getValue(), absAck.getValue());
	}
	
	public String processIncomingTimeout() {
		handler.setInt("tmp", 0); // invlang-thing: functions need at least 1 argument
		handler.execute("incomingTimeout");
		return "TIMEOUT";
	}
	
	public String processOutgoingRequest(FlagSet flags, String absSeq, String absAck) {
		return processOutgoingRequest(flags, Validity.getValidity(absSeq), Validity.getValidity(absAck));
	}
	
	private String processOutgoingRequest(FlagSet flags, Validity absSeq, Validity absAck) {
		handler.setFlags("flagsOut2", flags);
		handler.setEnum("absSeqOut", "absin", absSeq.toInvLang());
		handler.setEnum("absAckOut", "absin", absAck.toInvLang());
		this.lastConstraints = handler.executeInverted("outgoingRequest");
		if (handler.hasResult()) {
			int concSeq = handler.getIntResult("concSeqOut");
			int concAck = handler.getIntResult("concAckOut");
			long lConcSeq = getUnsignedInt(concSeq), lConcAck = getUnsignedInt(concAck);
			return Serializer.concreteMessageToString(flags, lConcSeq, lConcAck);
		} else {
			return "UNDEFINED";
		}
	}
	
	public Expression getLastConstraints() {
		return this.lastConstraints;
	}
	
	/**
	 * Reads an int (which is always signed in java) as unsigned,
	 * stored in a long
	 * @param x
	 * @return
	 */
	public static long getUnsignedInt(int x) {
	    return x & 0x00000000ffffffffL;
	}
	
	public String processOutgoingAction(String action) {
		return action.toLowerCase();
	}
	
	public void sendReset() {
		this.handler.reset();
	}
	
	public String processOutgoingReset() {
		long learnerSeq = getUnsignedInt((int)this.handler.getState().get("learnerSeq"));
		return (learnerSeq == NOT_SET)? null : Serializer.concreteMessageToString(new sutInterface.tcp.FlagSet(Flag.RST), learnerSeq, 0);
	}

	public static void main(String[] args) throws IOException {
		InvlangMapper mapper = new InvlangMapper("CLIENT_NO_ACTIONS_ALL_V");
		System.out.println(mapper.processIncomingResponse(new FlagSet("SYN"), 123, 0));
		System.out.println(mapper.getState());
		System.out.println(mapper.processOutgoingRequest(new FlagSet("ACK"), Validity.VALID, Validity.VALID));
		System.out.println(mapper.getState());
		System.out.println(mapper.processIncomingResponse(new FlagSet("FIN"), 125, 500));
		System.out.println(mapper.getState());
		
		/*
		InvLangHandler handler = mapper.handler;
		FlagSet flagsIn1 = new FlagSet("SYN");
		handler.setFlags("flagsIn", flagsIn1);
		handler.setInt("concSeqIn", 1<<24+1);
		handler.setInt("concAckIn", 0);
		handler.execute("incomingResponse");
		EnumValue absSeq = handler.getEnumResult("absSeqIn");
		EnumValue absAck = handler.getEnumResult("absAckIn");
		System.out.println(Serializer.abstractMessageToString(flagsIn1, absSeq.getValue(), absAck.getValue()));
		System.out.println(handler.getState());
		handler.setFlags("flagsOut2", new FlagSet("ACK+FIN"));
		handler.setEnum("absSeqOut", "absin", "VALID");
		handler.setEnum("absAckOut", "absin", "VALID");
		Expression constr = handler.executeInverted("outgoingRequest");
		System.out.println(constr);
		if (handler.hasResult()) {
			System.out.println(handler.getResults());
			System.out.println(mapper.getState());
		} else {
			System.out.println("Undefined");
		}
		System.err.println("\n\n" + constr);
		for (Object o : InvLangHandler.lastConstraints) {
			System.out.println(o);
		}*/
	}
}
