package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sutInterface.Serializer;
import sutInterface.tcp.TCPMapper;

public class CachedInitOracle implements InitOracle {
	private List<String> inputs = new ArrayList<String>();
	private InitCacheManager initCache;

	public CachedInitOracle(InitCacheManager initCache) {
		this.initCache = initCache;
	}

	/**
	 * Checks whether the input is resetting. If the trace is already present in
	 * the trace cache, then return the resetting state from the cache.
	 * Otherwise, use the checker to compute the reset state of the trace and
	 * store it in the cache.
	 */
	// TODO This taking the mapper and building the last input sent is not
	// pretty.
	public boolean isResetting(TCPMapper mapper) {
		boolean isResetting = false;
		String input = Serializer.abstractMessageToString(mapper.lastFlagsSent,
				mapper.lastAbstractSeqSent, mapper.lastAbstractAckSent);
		append(input);
		if (hasTrace()) {
			isResetting = initCache.getTrace(getInputs());
		} else {
			System.err.println("Could not find trace in cache");
			System.exit(0);
		}
		if (isResetting) {
			setDefault();
		}
		return isResetting;
	}
	
	public void setDefault() {
		inputs.clear();
	}

	private void append(String input) {
		inputs.add(input);
	}

	protected String[] getInputs() {
		return inputs.toArray(new String[inputs.size()]);
	}

	protected Boolean getStoredState() {
		return initCache.getTrace(getInputs());
	}

	protected boolean getPreviousResettingState() {
		String[] inputs = getInputs();
		Boolean lastStoredState = null;
		while (inputs.length > 0 && initCache.getTrace(inputs) == null) {
			inputs = Arrays.copyOf(inputs, inputs.length - 1);
		}
		if (inputs.length > 0) {
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
		System.out.println("ACK+SYN(FRESH,V)"
				.matches("((ACK\\+SYN)|(SYN\\+ACK))\\((?!FRESH).*"));
		;
	}

}
