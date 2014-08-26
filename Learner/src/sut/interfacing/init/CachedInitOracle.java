package sut.interfacing.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sut.interfacing.Serializer;
import sut.mapper.Flag;
import sut.mapper.TCPMapper;

public class CachedInitOracle implements InitOracle {
	private List<String> inputs = new ArrayList<String>();
	private InitCache initCache;
	private InitChecker initChecker;

	public CachedInitOracle(InitChecker initChecker, InitCache initCache) {
		this.initChecker = initChecker;
		this.initCache = initCache;
	}

	/**
	 * Checks whether the mapper is in a reset state by checking the trace
	 * cache. The default implementation never adds to the cache.
	 */
	public boolean isResetting(TCPMapper mapper) {
		boolean isResetting = false;
		String input = Serializer.abstractMessageToString(mapper.lastFlagsSent,
				mapper.lastAbstractSeqSent, mapper.lastAbstractAckSent);
		append(input);
		if (hasTrace()) {
			isResetting = initCache.getTrace(getInputs());
			System.out.println("CACHE HIT "+ inputs + " -> " + isResetting);
		} else {
			if(isResetCandidate(input)) {
				isResetting = initChecker.checkInitial(mapper);
				storeTrace(isResetting);
			} else {
				isResetting = getLastStoredState();
				storeTrace(isResetting);
			}
		}
		if (isResetting) {
			setDefault();
		}
		return isResetting;
	}
	

	/**
	 * Checks whether the input is candidate for a resetting state. We assume that only packets with RST or SYN flags can
	 * reset the system.
	 */
	private boolean isResetCandidate(String input) {
		return input.contains(Flag.RST.name()) || input.contains(Flag.SYN.name()) ;
	}

	public void setDefault() {
		inputs.clear();
	}

	private void append(String input) {
		System.out.println("append");
		inputs.add(input);
	}

	protected String[] getInputs() {
		return inputs.toArray(new String[inputs.size()]);
	}

	protected Boolean getStoredState() {
		return initCache.getTrace(getInputs());
	}
	
	protected boolean getLastStoredState() {
		String [] inputs = getInputs();
		Boolean lastStoredState = null;
		while(inputs.length > 0 && initCache.getTrace(inputs) == null) {
			inputs = Arrays.copyOf(inputs, inputs.length - 1);
		}
		if(inputs.length > 0) {
			lastStoredState = initCache.getTrace(inputs);
		}
		return inputs.length == 0 || Boolean.TRUE.equals(lastStoredState);
	}

	protected boolean hasTrace() {
		return getStoredState() != null;
	}

	protected void storeTrace(boolean value) {
		initCache.storeTrace(getInputs(), value);
	}

	public static void main(String[] args) {
		System.out.println("ACK+SYN(FRESH,V)".matches("((ACK\\+SYN)|(SYN\\+ACK))\\((?!FRESH).*"));;
	}
}
