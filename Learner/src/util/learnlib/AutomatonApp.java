package util.learnlib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import de.ls5.jlearn.interfaces.Automaton;
import de.ls5.jlearn.interfaces.Symbol;

public class AutomatonApp {
	private BufferedReader in;
	private PrintStream out;
	private Deque<String> commands;
	
	public AutomatonApp(BufferedReader in, PrintStream out) {
		this.in = in;
		this.out = out;
		this.commands = new ArrayDeque<String>();
	}
	
	public AutomatonApp() {
		this(new BufferedReader(new InputStreamReader(System.in)), System.out);
	}
	
	public void bufferCommands(Collection<String> commands) {
		this.commands.addAll(commands);
	}
	
	private String ask(String msg) throws IOException{
		out.println(msg);
		if (!commands.isEmpty()) {
			return commands.remove();
		}
		return in.readLine().trim();
	}
	
	public void play() throws IOException {
		Automaton loadedHyp = null;
		while (true) {
			out.println("Welcome to the hyp assistent. Today you can: " +
					"\n 1. Load a new hypothesis \n 2. Get trace to state \n " +
					"3. Get distinguishing seq between two states \n 4. Quit");
			
			String command = ask("Command:");
			switch(command) {
			case "1":
				String hyp = ask("Hypothesis:");
				loadedHyp = Dot.readDotFile(hyp);
				if (loadedHyp != null) {
					out.println("Loaded successfully");
				}
				break;
			case "2":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId = Integer.valueOf(ask("State ID:"));
					List<Symbol> traceToState = AutomatonUtils.traceToState(loadedHyp, stateId);
					out.println("Trace to state: " + traceToState); 
				}
				break;

			case "3":
				if (loadedHyp == null) {
					out.println("Load a hyp first!");
				} else {
					int stateId1 = Integer.valueOf(ask("State ID1:"));
					int stateId2 = Integer.valueOf(ask("State ID2:"));
					List<Symbol> distSeq = AutomatonUtils.distinguishingSeq(loadedHyp, stateId1, stateId2);
					out.println("Distinguishing trace: " + distSeq); 
				}
				break;
				
			case "4":
				out.println("Byee");
				return ;
			}
			
		}
	}

	public static void main(String args[]) throws IOException {
		AutomatonApp app = new AutomatonApp();
		if (args.length > 0) {
			app.bufferCommands(Arrays.asList(args));
		}
		app.play();
	}
	
}
