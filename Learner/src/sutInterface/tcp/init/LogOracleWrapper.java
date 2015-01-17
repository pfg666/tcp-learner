package sutInterface.tcp.init;

import java.util.ArrayList;
import java.util.List;

import learner.Main;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class LogOracleWrapper  implements Oracle{
	private static final long serialVersionUID = 1L;
	private Oracle oracle;
	
	public LogOracleWrapper(Oracle oracle) {
		this.oracle = oracle;
	}
	
	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = this.oracle.processQuery(input);
		for(int i = 0; i < input.getSymbolArray().length; i ++) {
			Main.tcpOut.println(input.getSymbolArray()[i].toString());
			Main.tcpOut.println("!" + output.getSymbolArray()[i].toString());
		}
		Main.tcpOut.println("reset");
		if(!invCheck(output)) {
			System.exit(0);
		}
		return output;
	}
	
	
	private boolean invCheck(Word word) {
		boolean noInv = true;
		for(String message : toMessages(word)) {
			if(message.contains("INV")) {
				noInv = false;
				break;
			}
		}
		return noInv;
	}
	
	private List<String> toMessages(Word word) {
		List<String> inputs = new ArrayList<String>();
		for (de.ls5.jlearn.interfaces.Symbol symbol : word.getSymbolArray()) {
			inputs.add(symbol.toString());
		}
		return inputs;
	}
}
