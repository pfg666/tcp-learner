package sut.interfacing;

import sut.mapper.Symbol;
import sut.mapper.TCPMapper;

public class MapperSocketWrapper {
	private ConnectionWrapper socket;
	
	private TCPMapper mapper;

	public MapperSocketWrapper(int portNumber) {
		socket = ConnectionManager.getConnection(portNumber);
	}
	
	public void setMapper(TCPMapper mapper) {
		this.mapper = mapper;
	}
	
	public TCPMapper getMapper() {
		return mapper;
	}
	
	public String sendInputs(String [] abstractRequests) {
		String lastOutput = null;
		for(String abstractRequest : abstractRequests ) {
			lastOutput = sendInput(abstractRequest);
		}
		return lastOutput;
	}
	
	
	public String sendInput(String abstractRequest) {
		return sendInput(abstractRequest, this.mapper);
	}
	
	public String sendInput(String abstractRequest, TCPMapper mapper) {
		String concreteRequest = processOutgoingPacket(abstractRequest, mapper);
		System.out.println(concreteRequest);
		String concreteResponse = sendPacket(concreteRequest);
		System.out.println(concreteResponse);
		String abstractResponse = processIncomingPacket(concreteResponse, mapper);
		return abstractResponse;
	}

	/**
	 * called by the learner to reset the automaton
	 */
	public void sendReset() {  
		long seq = mapper.getNextValidSeq();
		socket.sendOutput("reset "+seq);
		mapper.setDefault();
	}


	/**
	 * Updates seqToSend and ackToSend correspondingly.
	 * 
	 * @param abstract input e.g. "FIN+ACK(V,INV)"
	 * @return concrete output of the form "flags seqNr ackNr" describing a
	 *         packet, e.g. "FA 651 814", through the socket.
	 */
	private String processOutgoingPacket(String input, TCPMapper mapper) {
		String[] inputValues = input.split("\\(|,|\\)"); // of form {flags, seq,
															// ack}, e.g.
															// {"FIN+ACK", "V",
															// "INV"}
		String flags = inputValues[0];
		String abstractSeq = inputValues[1];
		String abstractAck = inputValues[2];
		
		String concreteInput = mapper.processOutgoingRequest(flags, abstractSeq, abstractAck);
		return concreteInput;
	}
	
	private String sendPacket(String concreteRequest) {
		String concreteResponse = null;
		if(Symbol.UNDEFINED.equals(concreteRequest)) {
			concreteResponse =  Symbol.UNDEFINED.toString();
		} else {
			socket.sendOutput(concreteRequest);
			concreteResponse = socket.receiveInput();
		}
		return concreteResponse;
	}

	/**
	 * 
	 * @param concreteResponse
	 *            of the form "flags seqNr ackNr", e.g. "FA 1000 2000"
	 * @return output, e.g. "FIN+ACK(V,INV)"
	 */
	private String processIncomingPacket(String concreteResponse, TCPMapper mapper) {
		String abstractResponse;
		if (concreteResponse.equals("timeout")) {
			abstractResponse = "timeout";
			mapper.processIncomingTimeout();
		} else {
			if(concreteResponse.equals(Symbol.UNDEFINED.toString())) {
				abstractResponse = Symbol.UNDEFINED.toString();
			}
			else {
				String[] inputValues = concreteResponse.split(" ");
				String flags = inputValues[0];
				
				long seqReceived = Long.parseLong(inputValues[1]);
				long ackReceived = Long.parseLong(inputValues[2]);
				abstractResponse = mapper.processIncomingResponse(flags, seqReceived, ackReceived);
			}
		}
		return abstractResponse;
	}
}
