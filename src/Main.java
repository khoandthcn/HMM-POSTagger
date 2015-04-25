import java.io.File;
import java.io.FilenameFilter;



public class Main {

	public static void main(String[] args) {
		System.out.println("Hello HMM!");
		
		Trainer trainer = new Trainer();
		// User must update this path before run application
		String testFolder = "Trainset-POS-1/";
		String testFile = "Trainset-POS-1/79859.test.pos";
		
		File testDir = new File(testFolder);
		if(testDir.isDirectory()){
			File[] files = testDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					// use file with *.pos & *.seg.pos for training
					if(name.endsWith("pos") && ! name.contains("test")){
						return true;
					}
					return false;
				}
			});
			for (File file : files) {
				trainer.trainFile(file);
			}
		}
		System.out.println("Training done.");
		
		trainer.saveModel("model");
		 
		Tagger tagger = new Tagger("model");
		
		// evaluate on training set
		if(testDir.isDirectory()){
			File[] files = testDir.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					if(name.endsWith("pos") && ! name.contains("test")){
						return true;
					}
					return false;
				}
			});
			for (File file : files) {
				//tagger.evaluatFile(file);
			}
		}
		
		// evaluate test set
		tagger.evaluatFile(new File(testFile));
		
		// tag new sentence
		// Trường/N bổ_túc/V công_nông/N thu_hút/V số_lượng/N cán_bộ/N trẻ/A đi/V học/V nhiều/A hơn/R ./CH
		System.out.println(
			tagger.tag("Trường bổ_túc công_nông thu_hút số_lượng cán_bộ trẻ đi học nhiều hơn ."));
	}
	
	public static String[] getTrainingFiles(String testFolder){
		return null;
	}

}
