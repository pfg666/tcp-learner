package sutInterface.tcp;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class FlagSet {
	public static final FlagSet EMPTY = new FlagSet();
	private Set<Flag> flagSet;
	public FlagSet() {
		this.flagSet = new LinkedHashSet<Flag>();
	}
	public FlagSet(Flag... flags) {
		this();
		this.flagSet.addAll(Arrays.asList(flags));	
	}
	
	public FlagSet(char... flagInitials) {
		this();
		for(char flagInitial : flagInitials) {
			Flag flag = Flag.getFlagWithInitial(flagInitial);
			if (flag != null) {
				this.flagSet.add(flag);
			}
		}
	}
	
	public FlagSet(String flags) {
		this();
		this.flagSet.addAll(Flag.parseFlags(flags));
	}
	
	public Flag [] toFlagArray() {
		return this.flagSet.toArray(new Flag[this.flagSet.size()]);
	}
	
	public char [] toInitials() {
		byte flagNr = (byte) flagSet.size();
		char [] flagInitials = new char [flagNr];
		Flag [] flagArray = flagSet.toArray(new Flag[flagNr]); 
		for(int flagIndex = 0; flagIndex < flagNr; flagIndex ++) {
			flagInitials[flagIndex] = flagArray[flagIndex].initial();
		}
		return flagInitials;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (Flag flag : flagSet) {
			result.append(flag.name()); 
		}
		return result.toString();
	}
	
	public boolean has(Flag... flags) {
		boolean hasAllFlags = this.flagSet.containsAll(Arrays.asList(flags));
		return hasAllFlags;
	}
	
	public boolean is(Flag... flags) {
		boolean hasAllFlags = has(flags) && flags.length == this.flagSet.size();
		return hasAllFlags;
	}
}
