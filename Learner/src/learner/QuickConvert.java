package learner;

import java.io.File;

import de.ls5.jlearn.util.DotUtil;

public class QuickConvert {
	public static String dotFile = "simple.dot";
	public static String pdfFile = "simple.pdf";
	public static void main(String[] args) throws Exception{
		
		DotUtil.invokeDot(new File(dotFile), "pdf", new File(pdfFile));	
	}

}
