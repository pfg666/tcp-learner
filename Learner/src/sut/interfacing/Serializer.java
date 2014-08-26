package sut.interfacing;

import sut.interfacing.io.AbstractRequest;
import sut.interfacing.io.ConcreteResponse;
import sut.mapper.FlagSet;
import sut.mapper.Symbol;

public class Serializer {

	public static String concreteMessageToString(String flags, long seqNr,
			long ackNr) {
		StringBuilder result = new StringBuilder();

		String[] flagArray = flags.split("\\+");
		for (String flag : flagArray) {
			result.append(Character.toUpperCase(flag.charAt(0))); 
		}
		
		result.append(" ");
		result.append(seqNr);
		result.append(" ");
		result.append(ackNr);
		return result.toString();
	}
	
	public static String concreteMessageToString(FlagSet flags, long seqNr,
			long ackNr) {
		StringBuilder result = new StringBuilder();

		result.append(flags.toInitials());
		
		result.append(" ");
		result.append(seqNr);
		result.append(" ");
		result.append(ackNr);
		return result.toString();
	}

	public static String abstractMessageToString(char[] flags,
			String seqValidity, String ackValidity) {
		StringBuilder result = new StringBuilder();

		result.append(charToFlag(flags[0]));
		for (int i = 1; i < flags.length; i++) {
			result.append("+");
			result.append(charToFlag(flags[i]));
		}

		result.append("(");
		result.append(seqValidity);
		result.append(",");
		result.append(ackValidity);
		result.append(")");
		return result.toString();
	}
	
	public static String abstractMessageToString(FlagSet flags,
			Symbol seqValidity, Symbol ackValidity) {
		char[] flagInitials = flags.toInitials();
		String seqString = seqValidity.name();
		String ackString = ackValidity.name();
		String result = abstractMessageToString(flagInitials, seqString, ackString);
		return result;
	}
	
	public static AbstractRequest stringToAbstractRequest(String abstractRequestString) {
		String[] inputValues = abstractRequestString.split("\\(|,|\\)"); 
		FlagSet flags = new FlagSet(inputValues[0]); 
		Symbol abstractSeq = Symbol.toSymbol(inputValues[1]);
		Symbol abstractAck = Symbol.toSymbol(inputValues[2]);
		return new AbstractRequest(flags, abstractSeq, abstractAck);
	}
	
	public static ConcreteResponse stringToConcreteResponse(String concreteResponseString) {
		String[] inputValues = concreteResponseString.split(" ");
		FlagSet flags = new FlagSet(inputValues[0].toCharArray());
		long seqReceived = Long.parseLong(inputValues[1]);
		long ackReceived = Long.parseLong(inputValues[2]);
		return new ConcreteResponse(flags, seqReceived, ackReceived);
	}
	
	public static String concreteResponseToString(ConcreteResponse response) {
		return concreteMessageToString(response.flags, response.seqNumber, response.ackNumber);
	}
	
	public static String charToFlag(char c) {
		c = Character.toLowerCase(c);
		switch (c) {
		case 's':
			return "SYN";
		case 'a':
			return "ACK";
		case 'f':
			return "FIN";
		case 'r':
			return "RST";
		case 'p':
			return "PSH";
		default:
			return "???"; // if a flag is returned that is not listed here, use
							// "???" in the resulting model
		}
	}
}
