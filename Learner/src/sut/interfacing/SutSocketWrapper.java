package sut.interfacing;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import sut.action.InputAction;
import sut.action.OutputAction;
import sut.action.Parameter;
import sut.info.SutInterface;

public class SutSocketWrapper implements SutInterface {
	private static boolean stopAtInvalid = true;
	private static ConnectionWrapper socket;
	private static TraceLogger traceLogger = new TraceLogger();
	private boolean isValid = true;
	public static Mapper mapper = new Mapper();

	/**
	 * if this is false, output is written to the console while learning,
	 * containing information about the outputs sent and inputs received
	 */
	private static boolean VERBOSE = false;

	/**
	 * @param verbose
	 *            if this is false, output is written to the console while
	 *            learning, containing information about the outputs sent and
	 *            inputs received
	 */
	public static void setVerbose(boolean verbose) {
		VERBOSE = verbose;
	}

	public SutSocketWrapper(int portNumber) {
		mapper.setDefault();
		if (socket == null) {
			System.out.println("Setting socket");
			System.out.println(portNumber);
			socket = new ConnectionWrapper(InetAddress.getLoopbackAddress()
					.getHostAddress(), portNumber);
		}
	}

	/**
	 * called by the learner when an input needs to be processed
	 * 
	 * @param concreteInput
	 *            the abstract query
	 * @return the corresponding abstract response
	 */
	@Override
	public OutputAction sendInput(InputAction concreteInput) {

		// get abstract input
		String abstractRequest = concreteInput.getMethodName();

		// print input if necessary
		if (!VERBOSE) {
			System.out.println("* new request/response *");
			System.out.print("input: " + abstractRequest);
			for (int i = abstractRequest.length(); i < 20; i++) {
				System.out.print(" ");
			}
			System.out.println(mapper.getState());
		}

		String concreteRequest = processOutgoingPacket(abstractRequest);

		if (!VERBOSE) {
			System.out.println(concreteRequest);
		}
		
		updateLogger(abstractRequest, concreteRequest);
		
		String concreteResponse = sendPacket(concreteRequest);
		
		String abstractResponse = processIncomingPacket(concreteResponse);

		// print output if necessary, format it nicely
		if (!VERBOSE) {
			System.out.print("output: " + abstractResponse);
			for (int i = abstractResponse.length(); i < 19; i++) {
				System.out.print(" ");
			}
			System.out.println(concreteResponse);
			System.out.println(mapper.getState());
		}
		
		updateLogger(abstractResponse, concreteResponse);
		checkOutputValidity(abstractResponse);

		// return the final answer
		return new OutputAction(abstractResponse, new ArrayList<Parameter>());
	}
	
	/**
	 * Logs the trace data in different log files, depending on booleans, then discards all the trace data.
	 */
	private void logRun () {
		if (isValid == false) {
			traceLogger.logTrace(TraceLogger.CEX_TRACES_FILE);
		} 
		traceLogger.logTrace(TraceLogger.REGULAR_TRACES_FILE);
		if(mapper.isTraceable == true) {
			traceLogger.logTrace(TraceLogger.INTERESTING_TRACES_FILE);
		}
		traceLogger.reset();
	}
	
	private void checkOutputValidity(String abstractOutput) {
		isValid = !abstractOutput.contains(Mapper.INVALID);
		if(isValid == false && stopAtInvalid == true) {
			System.out.println("Stopping at invalid");
			logRun();
			System.exit(0);
		} 
	}

	/**
	 * called by the learner to reset the automaton
	 */
	@Override
	public void sendReset() {  
		long seq = mapper.getNextValidSeq();
		socket.sendOutput("reset "+seq);
		logRun();
		if(isValid == false && stopAtInvalid == true) {
			System.out.println("Stopping at invalid");
			System.exit(0);
		} 
		mapper.setDefault();
		isValid = true;
	}


	/**
	 * Updates seqToSend and ackToSend correspondingly.
	 * 
	 * @param abstract input e.g. "FIN+ACK(V,INV)"
	 * @return concrete output of the form "flags seqNr ackNr" describing a
	 *         packet, e.g. "FA 651 814", through the socket.
	 */
	public String processOutgoingPacket(String input) {
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
	
	public String sendPacket(String concreteRequest) {
		String concreteResponse = null;
		if (!VERBOSE) {
			System.out.println(concreteRequest);
		}
		if(Mapper.UNDEFINED.equals(concreteRequest)) {
			concreteResponse =  Mapper.UNDEFINED;
		} else {
			socket.sendOutput(concreteRequest);
			concreteResponse = socket.receiveInput();
		}
		return concreteResponse;
	}

	private void updateLogger(String abs, String conc) {
		traceLogger.addAbstract(abs);
		traceLogger.addConcrete(conc + "  " + mapper.isConsidered);
	}

	/**
	 * 
	 * @param concreteResponse
	 *            of the form "flags seqNr ackNr", e.g. "FA 1000 2000"
	 * @return output, e.g. "FIN+ACK(V,INV)"
	 */
	public String processIncomingPacket(String concreteResponse) {
		String abstractResponse;
		if (concreteResponse.equals("timeout")) {
			abstractResponse = "timeout";
		} else {
			if(concreteResponse.equals(Mapper.UNDEFINED)) {
				abstractResponse = Mapper.UNDEFINED;
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
	
	public boolean hasFlag(char [] flags, char flag) {
		return Arrays.asList(flags).contains(flag);
	}
	
	public String processResponse(String flags, long seqReceived, long ackReceived) {
		String seqValidity = mapper.processIncomingSeq(flags, seqReceived);
		String ackValidity = mapper.processIncomingAck(flags, ackReceived);
		
		String abstractInput = Serializer.abstractMessageToString(flags.toCharArray(), seqValidity,
				ackValidity);
		return abstractInput;
	}
}