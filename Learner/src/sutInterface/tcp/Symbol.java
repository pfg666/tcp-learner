package sutInterface.tcp;

public enum Symbol {
	SNCLIENT,
	SNCLIENTP1,
	SNCLIENTP2,
	SNCLIENTPD,
	SNSERVER,
	SNSERVERP1,
	SNSERVERP2,
	SNSERVERPD,
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
	UNDEFINED ;
	
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
