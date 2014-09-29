package learner.exceptions;

public final class Messages {
	
	//BugException
	public final static String LEARNING_PROBLEM = "Problem with learning.";
	//Problems with abstraction
	public final static String CANNOT_FIND_PREDICATE = "Unable to find new predicate!";
	public final static String CANNOT_FIND_OUTPUT_ABSTRACTION = "Unable to abstract output action: ";
	public final static String CANNOT_FIND_OUTPUT_ABSTRACTION_PARAM = ", parameter index: ";
	public final static String TREE_CANNOT_FIND_OUTPUT_ABSTRACTION = "No source of output parameter could be found in the concrete tree!";
	public final static String FROM_ABSTRACTION_CE_EXCEPTION_TO_LEARNER_CE_EXCEPTION = "AbstractionCounterExampleException has changed to LearnerCounterExampleException!";
	//Wrong action parameter indexes
	public final static String NOT_FIRST_OR_LAST = "ActionParameterIndex is set to neither first nor last! ";
	public final static String FIRST_AND_LAST = "ActionParameterIndex set to first and last! ";
	public final static String BETWEEN_FIRST_AND_LAST = "Error: in method isFirst in Trace.java: not first, and not last, but somewhere in between";
	//Unknown things
	public final static String UNKNOWN_ABSTRACT_OUTPUT = "Unknown abstract output";
	public final static String UNKNOWN_ACTION_TYPE = "Adding unknown action type";
	public final static String UNKNOWN_ABSTRACT_PARAMETER_VALUE = "Unknown abstract parameter value!";
	//Other
	public final static String INVALID_ACTION_STRING = "Invalid action String from LearnLib when parsing: ";
	public final static String INVALID_ACTION_STRING_PARAM = ", invalid parameter value: ";
	public final static String EXISTING_CHILD = "Error! Adding child with already existing abstract input!";
	public final static String LOOPING_EDGE = "Looping Edge! ";
	public final static String UPDATING_CONSTANT = "Updating values in the constant action!";
	
	//ConfigurationException
	//Wrong ranges
	public final static String RANGE_ERROR = "The range defined by minValue and maxValue in the config.yaml file is not large enough for learning. Please increase the range.";
	public final static String WRONG_LEARNING_VALUES = "learning.maxValue cannot be smaller than learning.minValue";
	public final static String WRONG_TESTING_VALUES = "testing.maxTraceLength cannot be smaller than testing.minTraceLength";
	public final static String WRONG_CONSTANT_RANGE = "Constant cannot be larger than learning.maxValue or smaller than learning.minValue";
	//Missing files
	public final static String NO_MODEL_FILE = "For sut simulation a modelFile must be set in the config file";
	public final static String NO_SUTINFO_FILE = "For learning a sutinfo file must be set in the config file";
	public final static String NO_LOG_FILE = "Cannot find log file: ";
	
	//RestartLearningException
	public final static String RESTART_LEARNING = "Counter example handled.";
	
	//Exception
	public final static String BUG = "Unexpected exception thrown.";
}
