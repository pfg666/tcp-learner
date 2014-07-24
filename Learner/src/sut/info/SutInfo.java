package sut.info;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import de.ls5.jlearn.interfaces.Alphabet;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.shared.AlphabetImpl;
import de.ls5.jlearn.shared.SymbolImpl;

import sut.interfacing.SutSocketWrapper;
import util.ExceptionAdapter;
import util.Filesystem;

import sut.info.ActionSignature;
import abslearning.learner.SutInfoYaml;

public class SutInfo {
	public static String name;
	private static int minValue = 0;
	private static int maxValue = 255;
	private static List<Integer> constants = new ArrayList<Integer>();
	private static List<ActionSignature> inputSignatures;
	private static List<ActionSignature> outputSignatures;
	private static String sutWrapperClassName;
	public static String sutInfoYamlFile;
	private static int portNumber;

	protected static void addFlattenedSymbols(Alphabet result, String symbol,
			int min, int max, int numParams, int level) {
		for (int x = min; x <= max; x = x + 1) {
			String newSymbol = symbol + "_" + x;
			if (level == numParams) {
				result.addSymbol(new SymbolImpl(newSymbol));
			} else {
				addFlattenedSymbols(result, newSymbol, min, max, numParams,
						level + 1);
			}
		}
	}

	public static Alphabet generateInputAlphabet(int min, int max) {
		Alphabet result = new AlphabetImpl();

		for (ActionSignature sig : SutInfo.getInputSignatures()) {
			int numParams = sig.getParameterTypes().size();
			if (min == max || numParams == 0) {
				// min = max means that parameters can only have one value
				// thus we can ignore the parameters and only have the method
				// name as input symbol!
				result.addSymbol(new SymbolImpl(sig.getMethodName()));
			} else {
				addFlattenedSymbols(result, sig.getMethodName(), min, max,
						numParams, 1);
			}
		}
		return result;
	}

	public static String alphabetToString(Alphabet alphabet) {
		String result = "[ ";
		for (Symbol s : alphabet.getSymbolList()) {
			result = result + ", " + s.toString();
		}
		return result + " ]";
	}

	/*
	 * public Alphabet generateOutputAlphabet() { Alphabet result = new
	 * AlphabetImpl();
	 * 
	 * for (ActionSignature sig : SutInfo.getOutputSignatures()) {
	 * result.addSymbol(new SymbolImpl(sig.getMethodName())); } return result; }
	 */

	public static List<Integer> getConstants() {
		return constants;
	}

	public static void setConstants(List<Integer> constants) {
		SutInfo.constants = constants;
	}

	public static int getMinValue() {
		return minValue;
	}

	public static int getMaxValue() {
		return maxValue;
	}

	public static void setMinValue(int minValue) {
		SutInfo.minValue = minValue;
	}

	public static void setMaxValue(int maxValue) {
		SutInfo.maxValue = maxValue;
	}

	// ignored if using simulateSut
	public void setSutInterface(String sutInfoFileName, String sutInterface) {
		SutInfo.sutWrapperClassName = sutInterface;

		if (sutInterface.equals("SutSocketWrapper")) {
			// use socket wrapper :
			// -> uses static files :
			// - input/sutInfo.yaml
			// - sut/implementation/Sut.java
			SutInfo.sutInfoYamlFile = sutInfoFileName; // "input/sutInfo.yaml";
		} else {
			System.err.println("\nAbort:\n  Invalid sut interface class : \""
					+ SutInfo.sutWrapperClassName + "\"");
			System.exit(1);
		}
	}

	public static void initialize(String sutInfoYamlFile,
			String sutWrapperClassName, String outputDir) {

		SutInfo.sutWrapperClassName = sutWrapperClassName;

		SutInfo.sutInfoYamlFile = sutInfoYamlFile;

		
		if (SutInfo.sutWrapperClassName.equals("SutSocketWrapper")) {
			// use socket wrapper :
			// -> uses static files :
			// - input/sutInfo.yaml
			// - sut/implementation/Sut.java
			// sutInfoYamlFile = "input/sutInfo.yaml";
			
			Filesystem.copyfile(sutInfoYamlFile, outputDir + "/sutinfo.yaml");
			paulAdditions(outputDir);
		} else {
			System.err.println("\nAbort:\n  Invalid sut wrapper class : \""
					+ SutInfo.sutWrapperClassName + "\"");
			System.exit(1);
		}

	}
	
	public static void paulAdditions(String outputDir) {
		String projectDir = System.getProperty("user.dir");
		String interfacingDir = projectDir+"/src/sut/interfacing";
		Filesystem.copyfile(interfacingDir + "/Mapper.java", outputDir+"/Mapper.java");
		Filesystem.copyfile(interfacingDir + "/SutSocketWrapper.java", outputDir+"/SutSocketWrapper.java");
	}

	public static SutInterface newSutWrapper() {
		return newSutWrapper(false);
	}

	public static SutInterface newSutWrapper(boolean flatten) {

		SutInterface sutWrapper = null;
		if (SutInfo.sutWrapperClassName.equals("SutSocketWrapper")) {
			sutWrapper = new SutSocketWrapper(portNumber);
		} else {
			System.err.println("\nAbort:\n  Invalid sut wrapper class : \""
					+ SutInfo.sutWrapperClassName + "\"");
			System.exit(1);
		}
		return sutWrapper;
	}

	public static int getIndexOfInputSignature(String methodName) {
		List<ActionSignature> actionSignatures = new ArrayList<ActionSignature>(
				inputSignatures);
		for (int i = 0; i < actionSignatures.size(); i++) {
			ActionSignature as = actionSignatures.get(i);
			if (as.getMethodName().equals(methodName)) {
				return i;
			}
		}

		return -1;
	}

	public static List<ActionSignature> getInputSignatures() {
		return new ArrayList<ActionSignature>(inputSignatures);
	}

	public static void setInputSignatures(Map<String, List<String>> signatures) {
		SutInfo.inputSignatures = new ArrayList<ActionSignature>();
		for (Entry<String, List<String>> entry : signatures.entrySet()) {
			SutInfo.inputSignatures.add(new ActionSignature(entry.getKey(),
					entry.getValue()));
		}
	}

	public static ActionSignature getInputSignature(String methodName) {
		for (ActionSignature sig : inputSignatures) {
			if (sig.getMethodName().equals(methodName)) {
				return sig;
			}
		}
		return null;
	}

	public static void addInputSignature(String methodName,
			List<String> parameters) {
		SutInfo.inputSignatures
				.add(new ActionSignature(methodName, parameters));
	}

	public static List<ActionSignature> getOutputSignatures() {
		return new ArrayList<ActionSignature>(outputSignatures);
	}

	public static void setOutputSignatures(Map<String, List<String>> signatures) {
		SutInfo.outputSignatures = new ArrayList<ActionSignature>();
		for (Entry<String, List<String>> entry : signatures.entrySet()) {
			SutInfo.outputSignatures.add(new ActionSignature(entry.getKey(),
					entry.getValue()));
		}
	}

	public static ActionSignature getOutputSignature(String methodName) {
		for (ActionSignature sig : outputSignatures) {
			if (sig.getMethodName().equals(methodName)) {
				return sig;
			}
		}
		return null;
	}

	public static void addOutputSignature(String methodName,
			List<String> parameters) {
		SutInfo.outputSignatures
				.add(new ActionSignature(methodName, parameters));
	}

	public static void loadFromYaml(String sutInfoYamlFileName) {
		InputStream sutYamlStream = null;
		try {
			sutYamlStream = new FileInputStream(sutInfoYamlFileName);
		} catch (FileNotFoundException fnfe) {
			System.err
					.println("FileNotFoundException in loadFromYaml in sut.info.SutInfo class, file: "
							+ sutInfoYamlFileName);
			throw new ExceptionAdapter(fnfe);
		}

		Yaml yamlSut = new Yaml(new Constructor(SutInfoYaml.class));
		SutInfoYaml sutinterfaces = (SutInfoYaml) yamlSut.load(sutYamlStream);

		SutInfo.name = sutinterfaces.name;
		SutInfo.setConstants(sutinterfaces.constants);
		SutInfo.setInputSignatures(sutinterfaces.inputInterfaces);
		SutInfo.setOutputSignatures(sutinterfaces.outputInterfaces);
	}

	public static String getName() {
		return name;
	}

	public static int getPortNumber() {
		return portNumber;
	}

	public static void setPortNumber(int portNumber) {
		SutInfo.portNumber = portNumber;
	}

}
