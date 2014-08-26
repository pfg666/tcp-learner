package sut.interfacing.init;

import sut.mapper.TCPMapper;

public class DummyInitChecker implements InitChecker{
	/**
	 * Initialization checking should never rely on the DummyInitChecker. This implementation is, however, called, since
	 * when running an "init-testing" trace, we use a mapper that should have cached all init information on the trace until
	 * the last step is reached. In the last step, there is no cached data and a default true is returned. Afterwards, a 
	 * distinguishing string is called which leads to yet again, this method being called. 
	 * 
	 * Basically, this method is called only when the next state of the mapper is irrelevant.
	 * 
	 * Example:
	 * Testing init of trace ACK ACK ACK: 
	 * ACK (not init from cache) ACK (not init from cache) ACK (init <- dummy method) distinguishing-SYN (init <- dummy method)
	 * 
	 * 
	 */
	public boolean checkTrace(String[] inputs) {
		return false;
	}
	
	public boolean checkInput(String input) {
		return false;
	}

	public boolean checkInitial(TCPMapper mapper) {
		return false;
	}
}
