package util;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.SymbolImpl;

import learner.Main;

public class TreePruneApp {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the observation tree pruner. Which observationTree do you want to prune?\n(note: it removes it from the original file. Making a back-up is you own task)");
		String fileName = scanner.nextLine();
		ObservationTree tree = null;
		try {
			tree = Main.readCacheTree(fileName);
			if (tree == null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			System.out.println("Could not read tree, aborting...");
			System.exit(1);
		}
		System.out.println("Which output do you want to remove? You can use a java-regex, such as '.*PSH.*'");
		String suspiciousOutput = scanner.nextLine();
		sanitizeBranch(tree, Pattern.compile(suspiciousOutput));
		Main.writeCacheTree(tree, true);
		System.out.println("Tree written");
	}
	
	private static void sanitizeBranch(ObservationTree tree, Pattern outputToRemove) {
		Set<Symbol> inputs = new HashSet<>(tree.getInputs());
		for (Symbol input : inputs) {
			ObservationTree child = tree.getState(input);
			String output = tree.getOutput(input).toString();
			Matcher m = outputToRemove.matcher(output);
			if (m.matches()) {
				child.remove();
			} else {
				sanitizeBranch(child, outputToRemove);
			}
		}
	}
}
