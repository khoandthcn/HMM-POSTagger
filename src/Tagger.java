import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * @author khoand
 * This class used to decode a new sentence into POS sequence
 */
/**
 * @author khoand
 *
 */
public class Tagger {
	protected HashMap<String, Double> transitionProb, emissionProb, possibleTags;
	
	public Tagger(String modelPath){
		transitionProb = new HashMap<String, Double>();
		emissionProb = new HashMap<String, Double>();
		possibleTags = new HashMap<String, Double>();
		
		// Load model
		BufferedReader reader = null;
		
		try{
			reader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(modelPath), "utf-8"));
			String line;
			
			while((line = reader.readLine()) != null){
				String[] tokens = line.split(Trainer.KEY_DELIM);
				assert tokens.length == 4;
				
				String type = tokens[0];
				String context = tokens[1];
				String word = tokens[2];
				String prob = tokens[3];
				
				possibleTags.put(context, 1.0);
				
				if(type.equals(Trainer.TRANSITION_PREFIX)){
					transitionProb.put(context + " " + word, Double.parseDouble(prob));
				} else {
					emissionProb.put(context + " " + word, Double.parseDouble(prob));
				}
			}
		} catch (IOException ioe){
			System.out.println("Failure to read transition matrix.");
			ioe.printStackTrace();
		} finally {
			try{
				reader.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Evaluate a file
	 * @param testFile
	 * @return output accuracy to default system output
	 */
	public void evaluatFile(File testFile){
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(testFile), "utf-8"));
			String line;
			//int lineCount = 0;
			int wordCount = 0;
			double matchedCount = 0;
			
			while((line = reader.readLine()) != null)
			{
				if(line.trim().length() > 0){
					//lineCount++;
					int words = line.split(" ").length;
					wordCount += words;
					matchedCount += evaluate(line) * words;
				}
			}
			
			System.out.println("Overal [" + testFile.getName() + "]: " + matchedCount*100/wordCount + "%");
		} catch (IOException ioe){
			ioe.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e){}
		}
	}
	
	/**
	 * Evaluate a sentence
	 * @param testLine input sentence
	 * @return accuracy in percentage
	 */
	public double evaluate(String testLine){
		String line = "";
		
		String[] tokens = testLine.split(" ");
		for(int i = 0; i < tokens.length; i++){
			String[] wordTag = tokens[i].split(Trainer.WORD_TAG_DELIM);
			assert wordTag.length == 2;
			line += wordTag[0] + " ";
		}
		
		// Tagging
		int matched = 0;
		String tagStr = tag(line);
		//System.out.println(tagStr);
		String[] tagTokens = tagStr.split(" ");
		assert tagTokens.length == tokens.length;
		
		for (int i = 0; i < tagTokens.length; i++) {
			if(tagTokens[i].equalsIgnoreCase(tokens[i])){
				matched ++;
			}
		}
		
		return (double)matched/tokens.length;
	}
	
	/**
	 * decode a line into POS sequence
	 * @param line
	 * @return sentence with POS notation (same format as in training data)
	 */
	public String tag(String line){
		HashMap<String, Double> bestScore;
		HashMap<String, String> bestEdge;
		
		bestScore = new HashMap<String, Double>();
		bestEdge = new HashMap<String, String>();
		
		bestScore.put("0" + " " + Trainer.BEGIN_SENT, 0.0);
		bestEdge.put("0" + " " + Trainer.BEGIN_SENT, null);
		
		String[] words = line.split(" ");
		int len = words.length;
		
		// forward step
		for(int i = 0; i < len; i++){
			for (String prev : possibleTags.keySet()) {
				for (String next : possibleTags.keySet()) {
					if(bestScore.containsKey(i + " " + prev)
							&& transitionProb.containsKey(prev + " " + next)){
						double score = getDouble(bestScore,i + " " + prev) 
								- Math.log(getDouble(transitionProb,prev + " " + next))
								- Math.log(getDouble(emissionProb, next + " " + words[i]));
						// keep minimum score
						if(!bestScore.containsKey((i+1) + " " + next) 
								|| getDouble(bestScore,(i+1) + " " + next) > score){
							bestScore.put((i+1) + " " + next, score);
							bestEdge.put((i+1) + " " + next, i + " " + prev);
						}
					}
				}
				//System.out.print(i + "_" + prev + "[" + (int)getDouble(bestScore, i + " " + prev) + "]\t\t");
			}
			//System.out.println();
		}
		// final state
		for(String tag : possibleTags.keySet()){
			if(bestScore.containsKey(len + " " + tag)){
				double tagScore = getDouble(bestScore, len + " " + tag);
				double tranScore = -Math.log(getDouble(transitionProb, tag + " " + Trainer.END_SENT));
				double score = tagScore + tranScore;
				
				if(!bestScore.containsKey(len + " " + Trainer.END_SENT) 
						|| getDouble(bestScore, len + " " + Trainer.END_SENT) > score){
					String key = len + " " + Trainer.END_SENT;
					bestScore.put(key, score);
					bestEdge.put(key, len + " " + tag);
				}
			}
		}
		
		// backward step
		ArrayList<String> tags = new ArrayList<String>();
		String key = len + " " + Trainer.END_SENT;
		String nextEdge = bestEdge.get(key);
		while(!nextEdge.equals("0" + " " + Trainer.BEGIN_SENT)){
			String[] posTag = nextEdge.split(" ");
			assert posTag.length == 2;
			tags.add(posTag[1]);
			nextEdge = bestEdge.get(nextEdge);
		}
		// reverse tags
		String result = "";
		assert tags.size() == words.length;
		for (int i = tags.size() - 1; i >= 0; i--){
			result += words[len - 1 - i] + "/" + tags.get(i) + " ";
		}
		
		return result;
	}
	
	/**
	 * Process for unknown word
	 * @param map
	 * @param key
	 * @return
	 */
	private double getDouble(HashMap<String, Double> map, String key){
		if(map.containsKey(key)){
			return map.get(key);
		}
		//System.out.println("WARNING: unknown " + key);
		return 0.00001;
	}
}
