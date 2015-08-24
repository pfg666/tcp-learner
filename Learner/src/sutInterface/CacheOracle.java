package sutInterface;

import java.util.List;
import java.util.Map;

import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.WordImpl;

public class CacheOracle implements Oracle {
	private static final long serialVersionUID = 1L;
	private final Oracle oracle;
	private static final QueryCacheManager cacheManager = new QueryCacheManager();
	private static Map<List<Symbol>, List<Symbol>> inputToOutput = cacheManager.load("cache.txt"); 
	
	{
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				cacheManager.dump("cache.txt", inputToOutput); 
			}
		});
	}

	public CacheOracle(Oracle oracle) {
		this.oracle = oracle;
		
	}

	@Override
	public Word processQuery(Word input) throws LearningException {
		Word output = null;
		if (inputToOutput.containsKey(input.getSymbolList())) {
			List<Symbol> outputSymbols = inputToOutput.get(input
					.getSymbolList());
			output = new WordImpl(
					outputSymbols.toArray(new Symbol[outputSymbols.size()]));
			System.err.println("Cache hit!");

		} else {
			output = this.oracle.processQuery(input);
			List<Symbol> inputList = input.getSymbolList();
			List<Symbol> outputList = output.getSymbolList();
			inputToOutput.put(inputList, outputList);
		}
		return output;
	}
}
