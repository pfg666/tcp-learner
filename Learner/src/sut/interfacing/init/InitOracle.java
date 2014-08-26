package sut.interfacing.init;

import sut.mapper.TCPMapper;


public interface InitOracle {
	public boolean isResetting(TCPMapper mapper);
	public void setDefault(); 
}
