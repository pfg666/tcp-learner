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
			    scanner.close();
				throw new NullPointerException();
			}
		} catch (Exception e) {
			System.out.println("Could not read tree, aborting...");
			System.exit(1);
		}
		System.out.println("Do you want to remove an input (0) or an output (!0)");
		boolean isInput = scanner.nextLine().trim().equals("0");
		String msgType = isInput ? "input" : "output";
		System.out.println("Which " + msgType + "  do you want to remove? You can use a java-regex, such as '.*PSH.*'");
		String suspiciousOutput = scanner.nextLine();
		sanitizeBranch(tree, isInput, Pattern.compile(suspiciousOutput));
		Main.writeCacheTree(tree, true);
		System.out.println("Tree written");
		scanner.close();
	}
	
	private static void sanitizeBranch(ObservationTree tree, boolean isInput, Pattern removePattern) {
		Set<Symbol> inputs = new HashSet<>(tree.getInputs());
		for (Symbol input : inputs) {
		    ObservationTree child = tree.getState(input);
		    if (!isInput) {
    			String outputString = tree.getOutput(input).toString();
    			Matcher m = removePattern.matcher(outputString);
    			if (m.matches()) {
    				child.remove();
    			} else {
    				sanitizeBranch(child, isInput, removePattern);
    			}
		    } else {
		        String inputString = input.toString();
		        Matcher m = removePattern.matcher(inputString);
                if (m.matches()) {
                    child.remove();
                } else {
                    sanitizeBranch(child, isInput, removePattern);
                }
		    }
		}
	}
}
