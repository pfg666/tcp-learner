package sutInterface.tcp.init;

import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;

public class AdaptiveCachedInitOracle implements InitOracle {
	private CachedInitOracle cachedOracle;
	private AdaptiveInitOracle adaptiveOracle;
	

	public AdaptiveCachedInitOracle(TCPSutWrapper tcpWrapper) {
		this.cachedOracle = new CachedInitOracle(new InitCacheManager());
	//	this.adaptiveOracle = new AdaptiveInitOracle(tcpWrapper);
	}

	@Override
	public Boolean isResetting(TCPMapper mapper) {
		Boolean actualValue = cachedOracle.isResetting(mapper);
		if(actualValue == null) {
			actualValue = adaptiveOracle.isResetting(mapper);
			cachedOracle.storeTrace(actualValue);
		}
		return actualValue;
	}

	@Override
	public void setDefault() {
		cachedOracle.setDefault();
		adaptiveOracle.setDefault();
	}
}
