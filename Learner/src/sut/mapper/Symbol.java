package sut.mapper;

public enum Symbol {
	SNCLIENT,
	SNCLIENTP1,
	SNSERVER,
	SNSERVERP1,
	ANSENT,
	SNSENT,
	SNPSENT,
	FRESH,
	ZERO,
	V,
	INV, 
	IWIN,
	OWIN,
	WIN,
	UNDEFINED;
	
	public String toString(){
		return this.name().replace('P', '+');
	}
	
	public boolean is(Symbol symbol) {
		return this.equals(symbol);
	}
	
	public boolean equals(String str) {
		return this.toString().compareToIgnoreCase(str) == 0;
	}
	
	public static Symbol toSymbol(String str) {
		String newStr = str.toUpperCase();
		newStr = newStr.replace('+', 'P');
		return Symbol.valueOf(newStr);
	}
}
