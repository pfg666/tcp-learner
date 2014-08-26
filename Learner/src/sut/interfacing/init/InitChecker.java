package sut.interfacing.init;

import sut.mapper.TCPMapper;


public interface InitChecker {	
	public boolean checkTrace(String [] inputs);
	public boolean checkInitial(TCPMapper mapper);
}
