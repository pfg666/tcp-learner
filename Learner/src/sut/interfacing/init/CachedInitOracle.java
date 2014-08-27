package sut.interfacing.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sut.interfacing.Serializer;
import sut.mapper.Flag;
import sut.mapper.TCPMapper;

/***
 * An initialization oracle which keeps track of input history via an initCache.
 * We define trace as the inputs checked after the last reset.
 */
public class CachedInitOracle implements InitOracle {
	private List<String> inputs = new ArrayList<String>();
	private InitCache initCache;
	private InitChecker initChecker;

	public CachedInitOracle(InitChecker initChecker, InitCache initCache) {
		this.initChecker = initChecker;
		this.initCache = initCache;
	}

	/**
	 * Checks whether the input is resetting. 
	 * If the trace is already present in the trace cache, then return the resetting state from the cache.
	 * Otherwise, use the checker to compute the reset state of the trace and store it in the cache.
	 */
	//TODO This taking the mapper and building the last input sent is not pretty. 
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
				// if the input is deemed non-resetting, then the reset state of the trace is equal to the reset state of the trace before the input
				// that is, resetState(i1, i2... i(n-1), i(n)) -> resetState(i1, i2... i(n-1)) (since i(n) is non resetting)
				isResetting = getPreviousResettingState();
				storeTrace(isResetting);
			}
		}
		if (isResetting) {
			setDefault();
		}
		return isResetting;
	}
	

	/**
	 * Checks whether the package inputed can reset. We assume that only packets with RST or SYN flags can
	 * reset the system. This reduces the number of checks needed.
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
	
	protected boolean getPreviousResettingState() {
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
