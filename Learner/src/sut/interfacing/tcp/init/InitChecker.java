package sut.interfacing.tcp.init;

import sut.interfacing.tcp.TCPMapper;


public interface InitChecker {	
	public boolean checkTrace(String [] inputs);
	public boolean checkInitial(TCPMapper mapper);
}
