package learner;

import util.Log;
import util.ObservationTree;
import util.exceptions.NonDeterminismException;
import de.ls5.jlearn.abstractclasses.LearningException;
import de.ls5.jlearn.interfaces.Oracle;
import de.ls5.jlearn.interfaces.Word;

public class NonDeterminismValidatorWrapper implements Oracle{
    private static final long serialVersionUID = 1L;
    private Oracle oracle;
    private int numberTries;
    
    public NonDeterminismValidatorWrapper(int numberTries, Oracle oracle) {
        this.oracle = oracle;
        this.numberTries = numberTries;
    }

    @Override
    public Word processQuery(Word word) throws LearningException {
        Word output = null;
        try {
            output = oracle.processQuery(word);
        } catch(NonDeterminismException nonDet) {
            Log.err("Rerunning word which caused non-determinism: \n" + word);
            try{
                ObservationTree.removeBranchOnNonDeterminism(true);
                oracle.processQuery(word);
            }catch(NonDeterminismException nonDet2){
                Log.err("Branch from tree removed for input word: " + nonDet2.getInputs());
                throw nonDet2;
            }
            Log.err("Running word several times to see if non-determinism still exists");
            for (int i=0; i<numberTries; i++) {
                output = oracle.processQuery(word);
            }
            Log.err("Non-determinism judged to be due to packet miss.\n Learning can continue");
            ObservationTree.removeBranchOnNonDeterminism(false);
        }
        return output;
    }

}
