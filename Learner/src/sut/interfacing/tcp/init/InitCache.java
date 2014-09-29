package sut.interfacing.tcp.init;

public interface InitCache {
	public void storeTrace(String [] inputs, Boolean initValue);
	public Boolean getTrace(String [] inputs);
}
