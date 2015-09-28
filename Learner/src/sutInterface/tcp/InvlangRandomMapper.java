package sutInterface.tcp;

import invlang.inverter.Reducer;
import invlang.types.EnumValue;
import invlang.types.FlagSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import sutInterface.Serializer;
import sutInterface.tcp.InvlangMapper.Inputs;
import sutInterface.tcp.InvlangMapper.Mappings;
import util.InputAction;
import util.Log;
import util.OutputAction;
import util.RangePicker;

public class InvlangRandomMapper extends InvlangMapper {
	private static final int RANDOM_ATTEMPTS = 100;
	private final Random random = new Random();
	
	public InvlangRandomMapper(File file) throws IOException {
		super(file);
	}

	public InvlangRandomMapper(String mapperName) throws IOException {
		super(mapperName);
		Reducer.debugStream = System.out;
		if (Reducer.debugStream == null) {
			System.err.println("debug stream is null");
		} else {
			System.err.println("debug stream not null");
		}
	}
	
	@Override
	public String processOutgoingRequest(FlagSet flags, String absSeq,
			String absAck) {
		LinkedList<Long> pointsOfInterest = new LinkedList<>();
		pointsOfInterest.add(0L);
		for (Entry<String, Object> entry : this.handler.getState().entrySet()) {
			if (entry.getValue() instanceof Integer) {
				int value = (Integer) entry.getValue();
				if (value != InvlangMapper.NOT_SET) {
					pointsOfInterest.add(InvlangMapper.getUnsignedInt(value));
				}
			}
		}
		RangePicker picker = new RangePicker(random, 0, 0xffffffffL, pointsOfInterest);
		for (int i = 0; i < RANDOM_ATTEMPTS; i++) {
			int concSeq = (int) picker.getRandom(), concAck = (int) picker.getRandom();
			handler.setFlags(Outputs.FLAGS_OUT, flags);
			handler.setInt(Outputs.CONC_SEQ, concSeq);
			handler.setInt(Outputs.CONC_ACK, concAck);
			handler.execute(Mappings.OUTGOING_REQUEST, false);
			EnumValue resultingAbsSeq = handler.getEnumResult(Outputs.ABS_SEQ);
			EnumValue resultingAbsAck = handler.getEnumResult(Outputs.ABS_ACK);
			if (resultingAbsSeq.getValue().equals(Validity.getValidity(absSeq).toInvLang())
					&& resultingAbsAck.getValue().equals(Validity.getValidity(absAck).toInvLang())) {
				handler.setFlags(Outputs.FLAGS_OUT, flags);
				handler.setInt(Outputs.CONC_SEQ, concSeq);
				handler.setInt(Outputs.CONC_ACK, concAck);
				handler.execute(Mappings.OUTGOING_REQUEST);
				long lConcSeq = getUnsignedInt(concSeq), lConcAck = getUnsignedInt(concAck);
				return Serializer.concreteMessageToString(flags, lConcSeq, lConcAck);
			}
		}
		return super.processOutgoingRequest(flags, absSeq, absAck);
	}
}
