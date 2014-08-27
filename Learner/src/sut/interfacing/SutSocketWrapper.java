package sut.interfacing;

import java.util.ArrayList;
import java.util.List;

import sut.action.InputAction;
import sut.action.OutputAction;
import sut.action.Parameter;
import sut.info.SutInterface;
import sut.interfacing.init.ActiveInitChecker;
import sut.interfacing.init.CacheManager;
import sut.interfacing.init.CachedInitOracle;
import sut.interfacing.init.InitCache;
import sut.interfacing.init.InitChecker;
import sut.interfacing.init.StoringInitCache;
import sut.mapper.Symbol;
import sut.mapper.TCPMapper;
import abslearning.learner.TCPConfig;

public class SutSocketWrapper implements SutInterface {
	private MapperSocketWrapper wrapper;
	private boolean VERBOSE;
	private boolean isAdaptive = false;
	private List<String> traceInputs = new ArrayList<String>();

	public SutSocketWrapper(TCPConfig config) {
		wrapper = new MapperSocketWrapper(config.learningPort);
		isAdaptive = config.oracle.toLowerCase().equals("adaptive");
		TCPMapper mapper = TCPBuilder.buildMapper(config);
		wrapper.setMapper(mapper);
	}
	
	public void setMapper() {
		InitChecker initChecker = new ActiveInitChecker(wrapper);
		InitCache initCache = new StoringInitCache();
		wrapper.setMapper(new TCPMapper(new CachedInitOracle(initChecker, initCache)));
	}

	/**
	 * called by the learner when an input needs to be processed
	 * @param concreteInput
	 *            the abstract query
	 * @return the corresponding abstract response
	 */
	@Override
	public OutputAction sendInput(InputAction concreteInput) {

		// get abstract input
		String abstractRequest = concreteInput.getMethodName();
		String abstractResponse;

		// print input if necessary
		if (VERBOSE) {
			System.out.println("* new request/response *");
			System.out.print("input: " + abstractRequest);
			for (int i = abstractRequest.length(); i < 20; i++) {
				System.out.print(" ");
			}
		}
		
		
		if(isAdaptive) {
			traceInputs.add(abstractRequest);
			wrapper.getMapper().setDefault();
			abstractResponse = wrapper.sendInputs(traceInputs.toArray(new String [traceInputs.size()]));
			wrapper.sendReset();
		} else {
			abstractResponse = wrapper.sendInput(abstractRequest);
		}
		
		System.out.println(abstractRequest);
		System.out.println(abstractResponse);
		System.out.println(wrapper.getMapper().getState());
		
		// print output if necessary, format it nicely
		if (VERBOSE) {
			System.out.print("output: " + abstractResponse);
			for (int i = abstractResponse.length(); i < 19; i++) {
				System.out.print(" ");
			}
		}
		checkInvalid(abstractResponse);
		
		// return the final answer
		return new OutputAction(abstractResponse, new ArrayList<Parameter>());
	}
	
	private void checkInvalid(String abstractResponse) { 
		if(abstractResponse.contains(Symbol.INV.toString())) {
			CacheManager cm = new CacheManager();
			cm.dump("cache.txt");
			System.out.println("Invalid input param ");
			System.out.println(wrapper.getMapper().getState());
			System.exit(0);
		} 		
	}
	
	/**
	 * called by the learner to reset the automaton
	 */
	@Override
	public void sendReset() {  
		if(isAdaptive == false) {
			System.out.println(isAdaptive);
			wrapper.sendReset();
		}
		traceInputs.clear();
		System.out.println("=======RESET=======");
		System.out.println("==Learning Trace==");
	}
}