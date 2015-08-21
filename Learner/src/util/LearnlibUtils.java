package util;

import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class LearnlibUtils {
	public static Word symbolsToWords(String... symbolStrings) {
		SymbolImpl[] symbols = new SymbolImpl[symbolStrings.length];
		for (int i = 0; i < symbolStrings.length; i++) {
			symbols[i] = new SymbolImpl(symbolStrings[i]);
		}
		return new WordImpl(symbols);
	}
}
