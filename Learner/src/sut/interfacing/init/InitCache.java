package sut.interfacing.init;

public interface InitCache {
	public void storeTrace(String [] inputs, Boolean initValue);
	public Boolean getTrace(String [] inputs);
}
