package debug;

import java.util.LinkedList;
import java.util.Scanner;

import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.SymbolImpl;

import learner.Main;
import util.ObservationTree;

public class CacheViewer {
	private static ObservationTree root, current;
	private static boolean exit = false;
	private static Scanner scanner;
	
	public static void main(String[] args) {
		root = Main.readCacheTree(Main.CACHE_FILE);
		if (root == null) {
			System.err.println("Aborting");
			System.exit(1);
		}
		current = root;
	 	try {
	 		scanner = new Scanner(System.in);
			while(!exit && scanner.hasNextLine()) {
				process(scanner.nextLine());
			}
		} finally {
			scanner.close();
		}
	}
	
	private static void process(String arg) {
		switch(arg) {
		case "exit":
		case "quit":
		case "stop":
			exit = true;
			break;
		case "reset":
			current = root;
			break;
		case "remove":
			if (current == root) {
				System.out.println("Cannot remove root");
			} else {
				current.remove();
				current = root;
			}
			break;
		case "save":
		case "store":
			System.out.println("Does not currently work, cache viewer doesn't support multiple trees yet");
			//Main.writeCacheTree(root);
			break;
		case "view":
			System.out.println(current.getInputs());
			break;
		/*case "find":
			System.out.println("Type a regex to search for");
			if (scanner.hasNextLine()) {
				current.find(scanner.nextLine());
			} else {
				System.err.print("no regex supplied");
			}
			break;*/
		case "depth":
			System.out.println(current.getDepth());
			break;
		default:
			LinkedList<Symbol> input = new LinkedList<>();
			input.add(new SymbolImpl(arg));
			ObservationTree child = current.getState(input);
			if (child == null) {
				System.out.println("No input '" + arg + "' in the current node");
			} else {
				System.out.println("output '" + current.getObservation(input).get(0) + "'");
				current = child;
			}
			break;
		}
	}
}
