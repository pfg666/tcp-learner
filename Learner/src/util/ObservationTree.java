package util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import util.exceptions.InconsistencyException;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;
import de.ls5.jlearn.shared.WordImpl;

public class ObservationTree implements Serializable {
	private static final long serialVersionUID = 6001736L;
	private final ObservationTree parent;
	private final Symbol parentOutput;
	private final Map<Symbol, ObservationTree> children;
	private final Map<Symbol, Symbol> outputs;
	
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
			this.children.remove(input);
			this.outputs.remove(input);
			throw new InconsistencyException(oldOutputChain, newOutputChain);
		} else {
			// input is consistent with previous observations, just traverse
			return this.children.get(input);
		}
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
}
