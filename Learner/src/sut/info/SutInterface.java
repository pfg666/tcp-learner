package sut.info;

import sut.action.InputAction;
import sut.action.OutputAction;

public interface SutInterface {
	
	// process input to an output 
	public OutputAction sendInput(InputAction concreteInput);

	// reset SUT
	public void sendReset();
}
