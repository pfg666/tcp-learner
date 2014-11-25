package sutInterface.tcp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import sutInterface.SocketWrapper;
import sutInterface.SutWrapper;
import util.InputAction;
import util.OutputAction;

// SutWrapper used for learning TCP (uses abstraction) 
// Unlike SimpleSutWrapper, all communication is directed through a mapper component
public class TCPSutWrapper implements SutWrapper{

	private final SocketWrapper socketWrapper;
	private TCPMapper mapper;
	private boolean exitIfInvalid = false;
	private static final Set<String> ACTION_COMMANDS = new HashSet<>(Arrays.asList(new String[]
			{"LISTEN", "ACCEPT", "CLOSESERVER", "CLOSECONNECTION"}));
	
	public TCPSutWrapper(int tcpServerPort, TCPMapper mapper) {
		this.socketWrapper = new SocketWrapper(tcpServerPort);
		this.mapper = mapper;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.err.println("Closing stuff");
				System.err.flush();
				if (socketWrapper != null) {
					try {
						socketWrapper.writeInput("exit");
					} finally {
						socketWrapper.close();
					}
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
		OutputAction symbolicOutput;
		
		// Build concrete input
		String abstractRequest = symbolicInput.getValuesAsString(); 
		String concreteRequest;

		// processing of action-commands
		// note: mapper is not updated with action commands
		if(ACTION_COMMANDS.contains(abstractRequest)) {
			concreteRequest = abstractRequest.toLowerCase();
		}
		// only processing of packet-requests
		else {
			concreteRequest = processOutgoingPacket(abstractRequest);
		}
		
		// Handle non-concretizable abstract input case
		if(concreteRequest.equalsIgnoreCase(Symbol.UNDEFINED.name())) {
			symbolicOutput = new OutputAction(Symbol.UNDEFINED.name());
		}
		// Send concrete input, receive output from SUT and make abs
		else {
			String concreteResponse = sendPacket(concreteRequest);
			String abstractResponse = processIncomingPacket(concreteResponse);
			symbolicOutput = new OutputAction(abstractResponse);
		}
		
		return symbolicOutput;
	}

	
	/**
	 * called by the learner to reset the automaton
	 */
	public void sendReset() {
		System.out.println("******** RESET ********");
		String rstMessage = mapper.processOutgoingReset();
		socketWrapper.writeInput(rstMessage);
		socketWrapper.writeInput("reset");
		socketWrapper.readOutput();
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
		if (concreteRequest == null) {
			socketWrapper.writeInput("nil");
		} else {
			socketWrapper.writeInput(concreteRequest);
		}
		String concreteResponse = socketWrapper.readOutput();
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
