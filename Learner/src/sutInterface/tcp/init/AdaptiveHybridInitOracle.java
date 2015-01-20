package sutInterface.tcp.init;

import sutInterface.tcp.TCPMapper;

public class AdaptiveHybridInitOracle extends AdaptiveInitOracle implements InitOracle{

	// sometimes knows the response and give it quicker. Other times, we need to fetch 
	// it as we would with the adaptive
	private InitOracle partialOracle;
	
	public AdaptiveHybridInitOracle(int serverPort, InitOracle partialOracle ) {
		super(serverPort);
		this.partialOracle = partialOracle;
	}
	@Override
	public Boolean isResetting(TCPMapper mapper) {
		return super.isResetting(mapper);
	}
	
	
	
	@Override
	public void setDefault() {
		super.setDefault();
	}
	
	

}
