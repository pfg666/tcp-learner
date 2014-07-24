package sut.interfacing;

public class Serializer {
	/**
	 * Converts the parameters of a concrete message to a single string, so it
	 * can be used by the network interface
	 * 
	 * @param flags
	 *            separated with "+", e.g. "FIN+ACK"
	 * @param seqNr
	 *            seq nr. sent
	 * @param ackNr
	 *            ack nr. sent
	 * @return a single string which can be used by the network interface, e.g.
	 *         "FA 123 456"
	 */
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
	
	/**
	 * Converts the parameters of an abstract message to a single string, so it
	 * can be used by the leaner
	 * 
	 * @param flags
	 *            the characters of the flags set
	 * @param seqValidity
	 *            either "V" of "INV"
	 * @param ackValidity
	 *            either "V" of "INV"
	 * @return a single string in the output alphabet, e.g. "FIN+ACK(123,456)"
	 */
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
	
	/**
	 * detemines the flags abbreviation from its staring letter
	 * 
	 * @param c
	 *            starting letter, e.g. 's' or 'A'
	 * @return the abbreviation, e.g. "SYN" or "ACK"
	 */
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
