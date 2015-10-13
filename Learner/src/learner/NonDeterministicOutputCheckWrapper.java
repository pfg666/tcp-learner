package learner;

import util.exceptions.NonDeterminismException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Symbol;
import de.ls5.jlearn.interfaces.Word;

public class NonDeterministicOutputCheckWrapper implements Oracle {
    private static final long serialVersionUID = 1L;
    private Oracle oracle;
    private String suspiciousOutputSubstring = "FRESH,FRESH";
    
    public NonDeterministicOutputCheckWrapper(Oracle oracle) {
        this.oracle = oracle;
    }
    
    public NonDeterministicOutputCheckWrapper(String suspiciousOutputSubstring, Oracle oracle) {
        this(oracle);
        this.suspiciousOutputSubstring = suspiciousOutputSubstring;
    }
    
    @Override
    public Word processQuery(Word input) throws LearningException {
        Word output =  this.oracle.processQuery(input);
        if(isNonDetOutput(output)) {
            throw new NonDeterminismException(input);
        }
        return output;
    }

    private boolean isNonDetOutput(Word output) {
        for (Symbol sym : output.getSymbolArray()) {
            if (sym.toString().contains(suspiciousOutputSubstring)) {
                return true;
            }
        }
        return false;
    }

}
