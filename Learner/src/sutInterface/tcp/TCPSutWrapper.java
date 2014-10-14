package sutInterface.tcp;

import sutInterface.SocketWrapper;
import sutInterface.SutWrapper;
import util.InputAction;
import util.OutputAction;

// SutWrapper used for learning TCP (uses abstraction) 
// Unlike SimpleSutWrapper, all communication is directed through a mapper component
public class TCPSutWrapper implements SutWrapper{

	private SocketWrapper socket;
	private TCPMapper mapper;
	private boolean exitIfInvalid = false;

	public TCPSutWrapper(int tcpServerPort, TCPMapper mapper) {
		this.socket = new SocketWrapper(tcpServerPort);
		this.mapper = mapper;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.err.println("Closing stuff");
				System.err.flush();
				if (socket != null) {
					socket.writeInput("exit");
					socket.close();
				}
			}
		});
	}

	public void setMapper(TCPMapper mapper) {
		this.mapper = mapper;
	}

	public TCPMapper getMapper() {
		return mapper;
	}
	
	public void setInvalidExit(boolean exitWhenInvalid) {
		this.exitIfInvalid = exitWhenInvalid;
	}
	
	public OutputAction sendInput(InputAction symbolicInput) {
		// Send input to SUT
		String abstractRequest = symbolicInput.getValuesAsString(); 
		String concreteRequest = processOutgoingPacket(abstractRequest);
		
		// Receive output from SUT
		String concreteResponse = sendPacket(concreteRequest);
		String abstractResponse = processIncomingPacket(concreteResponse);
		return new OutputAction(abstractResponse);
	}

	
	/**
	 * called by the learner to reset the automaton
	 */
	public void sendReset() {
		String rstMessage = mapper.processOutgoingReset();
		socket.writeInput(rstMessage);
		socket.writeInput("reset");
		socket.readOutput();
		mapper.setDefault();
	}

	/**
	 * Updates seqToSend and ackToSend correspondingly.
	 * 
	 * @param abstract input e.g. "FA(V,INV)"
	 * @return concrete output of the form "flags seqNr ackNr" describing a
	 *         packet, e.g. "FA 651 814", through the socket.
	 */
	private String processOutgoingPacket(String input) {
		String[] inputValues = input.split("\\(|,|\\)"); // of form {flags, seq,
															// ack}, e.g.
															// {"FIN+ACK", "V",
															// "INV"}
		String flags = inputValues[0];
		String abstractSeq = inputValues[1];
		String abstractAck = inputValues[2];

		String concreteInput = mapper.processOutgoingRequest(flags,
				abstractSeq, abstractAck);
		return concreteInput;
	}

	private String sendPacket(String concreteRequest) {
		socket.writeInput(concreteRequest);
		String concreteResponse = socket.readOutput();
		return concreteResponse;
	}

	/**
	 * 
	 * @param concreteResponse
	 *            of the form "flags seqNr ackNr", e.g. "FA 1000 2000"
	 * @return output, e.g. "FA(V,INV)"
	 */
	private String processIncomingPacket(String concreteResponse) {
		String abstractResponse;
		if (concreteResponse.equals("timeout")) {
			mapper.processIncomingTimeout();
			abstractResponse = "TIMEOUT";
		} else {
			String[] inputValues = concreteResponse.split(" ");
			String flags = inputValues[0];

			long seqReceived = Long.parseLong(inputValues[1]);
			long ackReceived = Long.parseLong(inputValues[2]);
			abstractResponse = mapper.processIncomingResponse(flags,
					seqReceived, ackReceived);
			if(abstractResponse.contains(Symbol.INV.name()) && this.exitIfInvalid) {
				System.exit(0);
			}
		}
		return abstractResponse;
	}
}
