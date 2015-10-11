package util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import learner.Main;

import util.exceptions.InconsistencyException;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.SymbolImpl;
import de.ls5.jlearn.shared.WordImpl;

public class ObservationTree implements Serializable {
	private static final long serialVersionUID = 6001736L;
	private static boolean removeOnNonDet = false;
	private final ObservationTree parent;
	private final Symbol parentOutput;
	private final Map<Symbol, ObservationTree> children;
	private final Map<Symbol, Symbol> outputs;
	
	public static void removeBranchOnNonDeterminism(boolean removeBranch) {
	    removeOnNonDet = removeBranch;
	}
	
	public ObservationTree() {
		this(null, null);
	}
	
	private ObservationTree(ObservationTree parent, Symbol parentSymbol) {
		this.children = new HashMap<>();
		this.outputs = new HashMap<>();
		this.parent = parent;
		this.parentOutput = parentSymbol;
	}
	
	/**
	 * @return The outputs observed from the root of the tree until this node
	 */
	private List<Symbol> getOutputChain() {
		if (this.parent == null) {
			return new LinkedList<Symbol>();
		} else {
			List<Symbol> parentChain = this.parent.getOutputChain();
			parentChain.add(parentOutput);
			return parentChain;
		}
	}

	/**
	 * Add one input and output symbol and traverse the tree to the next node
	 * @param input
	 * @param output
	 * @return the next node
	 * @throws InconsistencyException 
	 */
	public ObservationTree addObservation(Symbol input, Symbol output) throws InconsistencyException {
		Symbol previousOutput = this.outputs.get(input);
		if (previousOutput == null) {
			// input hasn't been queried before, make a new branch for it and traverse
			this.outputs.put(input, output);
			ObservationTree child = new ObservationTree(this, output);
			this.children.put(input, child);
			return child;
		} else if (!previousOutput.equals(output)) {
			// input is inconsistent with previous observations, remove that branch and throw exception
			List<Symbol> oldOutputChain = this.children.get(input).getOutputChain();
			List<Symbol> newOutputChain = this.getOutputChain();
			newOutputChain.add(output);
			boolean action = removeOnNonDet; //askForRemoval(oldOutputChain, newOutputChain);
			if (action) {
			    Main.writeCacheTree(this, false);
    			this.children.remove(input);
    			this.outputs.remove(input);
			}
			throw new InconsistencyException(oldOutputChain, newOutputChain);
		} else {
			// input is consistent with previous observations, just traverse
			return this.children.get(input);
		}
	}
	
	private boolean askForRemoval(List<Symbol> oldOutputChain, List<Symbol> newOutputChain) {
	    System.out.println("Do you want the input removed from the tree? (1/0)");
	    System.out.println("Old output chain: " + oldOutputChain);
	    System.out.println("New output chain: " + newOutputChain);
	    boolean toRemove = true;
	    try{
        char answer = (char)System.in.read();
        if (answer == '1') {
            System.out.println("OK, removing");
            toRemove = true;
        } else {
            System.out.println("OK, not removing");
            toRemove = false;
        }
	    }catch(IOException e) {
	        System.err.println("IO Exception. Tree gets to leave another day.");
	        System.exit(0);
	    }
	    
	    return toRemove;
	}
	
	public void addObservation(Word inputs, Word outputs) throws InconsistencyException {
		addObservation(new LinkedList<>(inputs.getSymbolList()), new LinkedList<>(outputs.getSymbolList()));
	}
	
	public void addObservation(List<Symbol> inputs, List<Symbol> outputs) throws InconsistencyException {
		if (inputs.isEmpty() && outputs.isEmpty()) {
			return;
		} else if (inputs.isEmpty() || outputs.isEmpty()) {
			throw new RuntimeException("Input and output words should have the same length:\n" + inputs + "\n" + outputs);
		} else {
			Symbol firstInput = inputs.remove(0), firstOutput = outputs.remove(0);
			this.addObservation(firstInput, firstOutput).addObservation(inputs, outputs);
		}
	}
	
	public Word getObservation(Word inputs) {
		List<Symbol> outputs = getObservation(new LinkedList<>(inputs.getSymbolList()));
		if (outputs == null) {
			return null;
		}
		Symbol[] outputArray = new Symbol[outputs.size()];
		int i = 0;
		for (Symbol s : outputs) {
			outputArray[i++] = s;
		}
		return new WordImpl(outputArray);
	}
	
	public LinkedList<Symbol> getObservation(LinkedList<Symbol> inputs) {
		if (inputs.isEmpty()) {
			return new LinkedList<>();
		} else {
			Symbol firstInput = inputs.removeFirst();
			LinkedList<Symbol> observationTail = null;
			ObservationTree child = null;
			if ((child = this.children.get(firstInput)) == null
					|| (observationTail = child.getObservation(inputs)) == null) {
				return null;
			} else {
				Symbol firstOutput = this.outputs.get(firstInput);
				observationTail.addFirst(firstOutput);
				return observationTail;
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
	    System.out.println("Do you the input removed from the tree? (0/1)");
	    char answer = (char)System.in.read();
	    if (answer == '0') {
	        System.out.println("OK, removing");
	    } else {
	        System.out.println("OK, not removing");
	    }
	    
//		String[] lookup = "LISTEN SYN(V,V) ACK(V,V) ACCEPT CLOSECONNECTION ACK(V,V) CLOSE FIN+ACK(V,V)"
//				.split("\\s+");
//		ObservationTree observations = Main.readCacheTree();
//		LinkedList<Symbol> symbols = new LinkedList<>();
//		for (String string : lookup) {
//			symbols.add(new SymbolImpl(string));
//		}
//		System.out.println(symbols);
//		System.out.println(observations.getObservation(symbols));
	}
}
