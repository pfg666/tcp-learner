package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.List;

import learner.Statistics;

import sutInterface.tcp.Flag;
import sutInterface.tcp.Packet;
import sutInterface.tcp.Symbol;
import sutInterface.tcp.TCPMapper;
import sutInterface.tcp.TCPSutWrapper;
import util.InputAction;
import util.Log;
import util.OutputAction;
import util.exceptions.BugException;

public class AdaptiveInitOracle implements InitOracle {
	private final TCPSutWrapper tcpWrapper;
	// used to buffer all the packets we send to the SUT.
	private List<Packet> inputBuffer;
	private CachedInitOracle cachedOracle;
	private InitOracle partialOracle;
	
	public AdaptiveInitOracle(int sutPort ) {
		this.tcpWrapper = new TCPSutWrapper(sutPort);
		this.cachedOracle = new CachedInitOracle(new InitCacheManager());
		this.inputBuffer = new ArrayList<Packet>(); 
		this.partialOracle = null;
	}
	
	public AdaptiveInitOracle(int sutPort , InitOracle partialOracle) {
		this(sutPort);
		this.partialOracle = partialOracle;
	}
	
	

	public Boolean isResetting(TCPMapper mapper) {
		inputBuffer.add(mapper.lastPacketSent);
		Boolean isResetting = getInitFromCache(mapper);
		if (isResetting == null) {
			isResetting = getInitFromPartialOracle(mapper);
			if (isResetting == null) {
				isResetting = discoverNextInit(mapper);
				storeInitToCache(isResetting);
				restoreSutAndMapper(mapper, isResetting);
			} else {
				Log.info("Fetched initial state from partial oracle: " + isResetting);
				Statistics.getStats().totalQueriesSavedByPartialOracle ++;
				//storeInitToCache(isResetting);
			}
		}
		
		return isResetting;
	}
	
	public Boolean getInitFromPartialOracle(TCPMapper mapper) {
		Boolean isResetting = null;
		if (partialOracle != null) {
			isResetting = partialOracle.isResetting(mapper);
		}
		return isResetting;
	}

	private Boolean getInitFromCache(TCPMapper mapper) {
		Boolean isResetting = cachedOracle.isResetting(mapper);
		return isResetting;
	}
	
	private void storeInitToCache(Boolean isResetting) {
		if (isResetting == null) {
			throw new BugException("Cannot store null values. We are supposed to have found init");
		}
		cachedOracle.storeTrace(isResetting);
	}
	
	private Boolean discoverNextInit(TCPMapper mapper) {
		boolean isResetting = false;
		TCPMapper clone = cloneMapper(mapper, new TruthfulInitOracle());

		Log.info("Setting mapper stub.");
		tcpWrapper.setMapper(clone);
		Log.info("Sending SYN with random seq.");
		OutputAction output = tcpWrapper.sendInput(new InputAction("SYN(RAND,RAND)"));
		String outputString = output.getValuesAsString();
		Packet outputPacket = new Packet(outputString);
		// it was in the listening state (init is ok, so the seq and ack values should have been updated)
		if(outputString.contains("SYN+ACK") && clone.lastAckReceived == clone.lastSeqSent + 1) {//outputPacket.flags.is(Flag.SYN, Flag.ACK)) {
			Log.info("SYN acknowledged, we were in the list state.");
			tcpWrapper.sendReset(); 
			isResetting = true;
		} 
		// it it wasn't, then it is possible that sending a SYN sent it to the listening state
		// or that the input was just ignored. To ensure the connection is cleaned, we send a reset
		// with the previous state of the mapper.
		else {
			Log.info("SYN not acknowledged. We weren't in the list state.");
			clone = cloneMapper(mapper, new TruthfulInitOracle());
			tcpWrapper.setMapper(clone);
			tcpWrapper.sendReset();
			// we return it
			isResetting = false;
		}
		Log.info("Returning " + isResetting);
		return isResetting;
	}

	private TCPMapper cloneMapper(TCPMapper mapper, InitOracle nonAdaptiveOracle) {
		// we need to alter the current run to make init experiments, but preserve the current state 
		// of the mapper, hence we make a clone of it and use it instead of the current mapper
		TCPMapper clone = mapper.clone(); 
		// we set a truth init oracle, so the new values are stored. And so that we don't end in
		// an unfortunate endless recursion.
		clone.setInitOracle(nonAdaptiveOracle);
		return clone;
	}

	public void setDefault() {
		inputBuffer.clear();
		cachedOracle.setDefault();
	}
	
	public void restoreSutAndMapper(TCPMapper mapper, boolean isResetting) {
		// if the system was in the resetting state, by sending the reset, it should still remain
		// reset state
		if (isResetting == true) {
			return;
		} 
		Statistics.getStats().totalAdditionalQueriesByAdaptiveOracle ++;
		
		// if is resetting is false, then it could be that the SYN we sent altered the system state,
		// so we need to recover the system state by rerunning all the inputs. 
		// Next time this recursion is run, isResetting will be found in the cache
		Log.info("Restoring SUT to state before SYN by running:" + inputBuffer);
		tcpWrapper.setMapper(mapper);
		// we need to store this in a buffer, on default the input buffer is cleared.
		List<Packet> copyInputBuffer = new ArrayList<Packet>(inputBuffer);
		mapper.setDefault();
		for(Packet input : copyInputBuffer) {
			tcpWrapper.sendInput(new InputAction(input.serialize())); 
		}
	}
	
	
	class TruthfulInitOracle implements InitOracle {
		public Boolean isResetting(TCPMapper mapper) {
			return true;
		}

		public void setDefault() {
			
		}
	}
}
