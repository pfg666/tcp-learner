package sutInterface.tcp;

import invlang.types.FlagSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import learner.Main;

import sutInterface.SocketWrapper;
import sutInterface.SutWrapper;
import util.InputAction;
import util.Log;
import util.OutputAction;

public class InvlangSutWrapper implements SutWrapper {
	private final static Map<Integer,SocketWrapper> socketWrapperMap = new HashMap<Integer, SocketWrapper>();
	
	private final InvlangMapper mapper;
	private final SocketWrapper socketWrapper;
	
	public InvlangSutWrapper(int tcpServerPort, String mapperName) {
		try {
			this.mapper = new InvlangRandomMapper(mapperName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if(socketWrapperMap.containsKey(tcpServerPort)) {
			this.socketWrapper = socketWrapperMap.get(tcpServerPort); 
		} else {
			this.socketWrapper = new SocketWrapper(tcpServerPort);
			socketWrapperMap.put(tcpServerPort,this.socketWrapper);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Log.fatal("Detected shutdown, commencing connection "+ 
							socketWrapper + " termination");
					if (socketWrapper != null) {
						try {
							Log.fatal("Sending an exit message to the adapter");
							socketWrapper.writeInput("exit");
						} finally {
							Log.fatal("Closing the socket");
							socketWrapper.close();
						}
					}
				}
			});
		}
	}
	
	public void sendReset() {
		Log.info("******** RESET ********");
		Main.absAndConcTraceOut.println("reset\n");
		String rstMessage = mapper.processOutgoingReset();
		// mapper made a pertinent reset message
		if(rstMessage != null) {
			socketWrapper.writeInput(rstMessage);
			socketWrapper.readOutput();
		}
		socketWrapper.writeInput("reset");
		this.mapper.sendReset();
	}
	
	public OutputAction sendInput(InputAction symbolicInput) {
		OutputAction symbolicOutput;
		
		// Build concrete input
		String abstractRequest = symbolicInput.getValuesAsString();
		Log.info("ABSTRACT REQUEST:" + abstractRequest);
		String concreteRequest;
		
		Log.info("MAPPER BEFORE:" + mapper.getState());
		Main.absAndConcTraceOut.println(mapper.getState());
		// processing of action-commands
		// note: mapper is not updated with action commands
		
		if(Action.isAction(abstractRequest)) {
			this.mapper.processOutgoingAction(abstractRequest);
			concreteRequest = abstractRequest.toLowerCase();
		}
		// only processing of packet-requests
		else {
			concreteRequest = processOutgoingPacket(abstractRequest);
		}
		Main.absAndConcTraceOut.println(abstractRequest + " \\ " + concreteRequest);
		// Handle non-concretizable abstract input case
		if(concreteRequest.equalsIgnoreCase(Symbol.UNDEFINED.name())) {
			symbolicOutput = new OutputAction(Symbol.UNDEFINED.name());
			Log.info("ABSTRACT RESPONSE: " + Symbol.UNDEFINED.name() + " (no transition)");
			Main.absAndConcTraceOut.println("> " + Symbol.UNDEFINED.name() + " - " + this.mapper.getLastConstraints());
		}
		// Send concrete input, receive output from SUT and make abs
		else {
			Log.info("MAPPER INTER:" + mapper.getState());
			Main.absAndConcTraceOut.println(mapper.getState());
			String concreteResponse = sendPacket(concreteRequest);
			String abstractResponse = processIncomingPacket(concreteResponse);
			symbolicOutput = new OutputAction(abstractResponse);
			Log.info("ABSTRACT RESPONSE:" + abstractResponse);
			Log.info("MAPPER AFTER:" + mapper.getState());
			Main.absAndConcTraceOut.println("> " + abstractResponse + " \\ " + concreteResponse);
		}
		return symbolicOutput;
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
	
	private String processIncomingPacket(String concreteResponse) {
		if (concreteResponse.equals("timeout")) {
			return mapper.processIncomingTimeout();
		} else {
			String[] inputValues = concreteResponse.split(" ");
			String flags = inputValues[0];
			int seqReceived = (int)Long.parseLong(inputValues[1]);
			int ackReceived = (int)Long.parseLong(inputValues[2]);
			return mapper.processIncomingResponse(FlagSet.fromAcronym(flags), seqReceived, ackReceived);
		}
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
		String concreteInput;
		try {
			concreteInput = mapper.processOutgoingRequest(new FlagSet(flags),
				abstractSeq, abstractAck);
		} catch (Exception e) {
			throw new RuntimeException("Cannot map abstract input '" + input + "'", e);
		}
		return concreteInput;
	}
	
	public void close() {
		this.socketWrapper.close();
	}
	
	@Override
	public String toString() {
		return this.mapper.getState().toString();
	}
}
