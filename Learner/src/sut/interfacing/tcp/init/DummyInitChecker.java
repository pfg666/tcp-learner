package sut.interfacing.tcp.init;

import sut.interfacing.tcp.TCPMapper;

/**
 * Stub init checker. It should only be used when the boolean output is irrelevant/not used.
 */
public class DummyInitChecker implements InitChecker{
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
