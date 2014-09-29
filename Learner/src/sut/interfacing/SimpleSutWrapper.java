package sut.interfacing;

import sut.action.InputAction;
import sut.action.OutputAction;
import sut.info.SutInterface;

// SutWrapper which connects the learner directly to the i/o sut without Mappers
// Used for sut which can be learned with regular L*.
public class SimpleSutWrapper implements SutInterface {
	private SocketWrapper socket;
	
	public SimpleSutWrapper(int port) {
		socket = new SocketWrapper(port);
	}

	public OutputAction sendInput(InputAction symbolicInput) {
		// Send input to SUT
		String symbolicInputString = symbolicInput.serialize();
		socket.writeInput(symbolicInputString);

		// Receive output from SUT
		String symbolicOutputString = socket.readOutput();
		OutputAction symbolicOutput = new OutputAction(symbolicOutputString);
		
		return symbolicOutput;
	}

	public void sendReset() {
		socket.writeInput("reset");
	}
}
