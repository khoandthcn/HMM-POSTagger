import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * @author khoand
 * This class used to train HMM model. We use HMM model with single state for each POS.
 * A bigram language model for training transition matrix 
 *
 */
public class Trainer {
	public static final String WORD_TAG_DELIM = "/";
	public static final String TOKEN_DELIM = " ";
	public static final String KEY_DELIM = " ";
	public static final String BEGIN_SENT = "<s>";
	public static final String END_SENT = "</s>";
	public static final String TRANSITION_PREFIX = "T";
	public static final String EMISSION_PREFIX = "E";
	public static final String NEW_LINE = "\n";
	
	protected HashMap<String, Integer> emissionAcc, transitionAcc, contextAcc;
	
	public Trainer()
	{
		reset();
	}
	
	public void reset()
	{
		this.emissionAcc = new HashMap<String, Integer>();
		this.transitionAcc = new HashMap<String, Integer>();
		this.contextAcc = new HashMap<String, Integer>();
	}
	
	/***
	 * Train single file
	 * @param file input file for training
	 */
	public void trainFile(File file)
	{
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(
					new InputStreamReader(
							new FileInputStream(file), "utf-8"));
			String line;
			
			while((line = reader.readLine()) != null)
			{
				if(line.trim().length() > 0){
					this.train(line);
				}
			}
		} catch (IOException ioe){
			ioe.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (Exception e){}
		}
	}
	
	/***
	 * Train single sentence
	 * @param line input sentence for training
	 */
	public void train(String line)
	{
		String previous = BEGIN_SENT;
		increase(contextAcc, previous);
		StringTokenizer tokens = new StringTokenizer(line, TOKEN_DELIM);
		// fetch token word/tag
		while(tokens.hasMoreTokens())
		{
			String token = tokens.nextToken();
			if(token != null && !token.isEmpty())
			{
				String[] wordTag = token.split(WORD_TAG_DELIM);
				if(wordTag.length == 2 && 
						wordTag[0] != null && !wordTag[0].isEmpty() &&
						wordTag[1] != null && !wordTag[1].isEmpty())
				{
					String word = wordTag[0];
					String tag = wordTag[1];
					
					// count transition from tag -> tag
					increase(transitionAcc, previous + KEY_DELIM + tag);
					// count total tag
					increase(contextAcc, tag);
					// count emission word from tag
					increase(emissionAcc, tag + KEY_DELIM + word);
					
					previous = tag;
				}
			}
		}
		increase(transitionAcc, previous + KEY_DELIM + END_SENT);
	}
	
	public void saveModel(String path)
	{
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(path), "utf-8"));
			for (String key : transitionAcc.keySet()) {
				String[] prevTag = key.split(KEY_DELIM);
				
				assert prevTag.length == 2;
				
				String previous = prevTag[0];
				//String word = prevTag[1];
				
				double value = getInt(transitionAcc, key);
				double contextCount = getInt(contextAcc, previous);
				
				String line = TRANSITION_PREFIX + KEY_DELIM + 
							  key + KEY_DELIM + 
							  (value/contextCount);
				writer.write(line);
				writer.write(NEW_LINE);
			}
			
			for (String key : emissionAcc.keySet()) {
				String[] prevTag = key.split(KEY_DELIM);
				
				assert prevTag.length == 2;
				
				String previous = prevTag[0];
				//String word = prevTag[1];
				
				double value = getInt(emissionAcc, key);
				double contextCount = getInt(contextAcc, previous);
				
				String line = EMISSION_PREFIX + KEY_DELIM + 
							  key + KEY_DELIM + 
							  (value/contextCount);
				writer.write(line);
				writer.write(NEW_LINE);
			}
		} catch (IOException ioe){
			ioe.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private int getInt(HashMap<String, Integer> map, String key)
	{
		if(map.containsKey(key)){
			return map.get(key);
		}
		
		return 0;
	}
	
	private void increase(HashMap<String, Integer> map, String key)
	{
		if(map.containsKey(key))
		{
			int c = map.get(key);
			map.put(key, c + 1);
		} else {
			map.put(key, 1);
		}
	}
}
