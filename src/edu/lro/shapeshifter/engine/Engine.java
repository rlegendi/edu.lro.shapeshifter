package edu.lro.shapeshifter.engine;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.lro.shapeshifter.engine.Loader.InputType;

/**
 * Represents the knowledge base of the bot.
 * 
 * <p>
 * The class maintains the following data during its lifecycle:
 * <ul>
 * <li>The set of all tuples;</li>
 * <li>The successor relations (tuple x next words);</li>
 * <li>The preceding relations (tuple x previous words);</li>
 * <li>And for each word, we maintain which tuples is it stored in.</li>
 * </ul>
 * <p>
 * 
 * @author legendi
 */
public class Engine {
	
	//---------------------------------------------------------------------------------------------------
	//--- Representation --------------------------------------------------------------------------------
	
	/** The order of the used Markov chain. */
	private int markovOrder = 3;
	
	/** Bot counter-questions if it cannot understand something. */
	private static boolean hegedusHeuristic = true;
	
	/** Produces bigger sentences for higher values. */
	private static int strackEntropyCompensation = 7;
	
	private final Random random = new Random();

	private final HashMap<Tuple, LinkedHashSet<String>> followings = new HashMap<Tuple, LinkedHashSet<String>>();
	private final HashMap<Tuple, LinkedHashSet<String>> preceedings = new HashMap<Tuple, LinkedHashSet<String>>();
	private final HashMap<String, LinkedHashSet<Tuple>> containers = new HashMap<String, LinkedHashSet<Tuple>>();
	private final LinkedHashSet<Tuple> knownTuples = new LinkedHashSet<Tuple>();
	private final HashMap<Tuple, Tuple.Descriptor> descriptors = new HashMap<Tuple, Tuple.Descriptor>();
	
	private class Sentence {
		final LinkedList<String> parts = new LinkedList<String>();
		
		public Sentence(final Tuple tuple) {
			for (final String act : tuple) {
				parts.add(act);
			}
		}

		public void append(final String part) {
			parts.add(part);
		}

		public void prepend(final String part) {
			parts.add(0, part);
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();

			for (int i = 0; i < parts.size(); ++i) {
				if (i > 0) {
					sb.append(' ');
				}

				sb.append(parts.get(i));
			}

			return sb.toString();
		}
	}
	
	/**
	 * Represents a sentence candidate. The one with maximal <tt>entropy</tt> value is going to be given
	 * to the user as a result for his/her question.
	 */
	public class Result {
		public final String sentence;
		
		/** Strack-entropy value of the generated sentence. */
		public final int entropy;
		
		public Result(final String sentence, final int entrophy) {
			super();
			this.sentence = sentence;
			this.entropy = entrophy;
		}
		
		@Override
		public String toString() {
			return "[" + entropy + "]\t" + sentence;
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	
	public Engine() {
		;
	}

	public Engine(final URL url) {
		this(url, InputType.TXT);
	}
	
	public Engine(final URL url, final Loader.InputType inputType) {
		try {
			init(url, inputType);
		} catch (final IOException e) {
			System.err.println("Cannot initialize default knowledge base, exiting.");
			e.printStackTrace();
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	//--- Interface -------------------------------------------------------------------------------------
	
	public Map<Tuple, LinkedHashSet<String>> getFollowings() {
		return Collections.unmodifiableMap(followings);
	}

	public Map<Tuple, LinkedHashSet<String>> getPreceedings() {
		return Collections.unmodifiableMap(preceedings);
	}

	public Map<String, LinkedHashSet<Tuple>> getContainers() {
		return Collections.unmodifiableMap(containers);
	}

	public Set<Tuple> getKnownTuples() {
		return Collections.unmodifiableSet(knownTuples);
	}
	
	public int getOrder() {
		return markovOrder;
	}

	public void setOrder(final int markovOrder) {
		// Increase the upper limit on your own risk!
		if (markovOrder < 1 || 5 < markovOrder) {
			throw new IllegalArgumentException("Argument must be in the interval [1,5].");
		}
		
		this.markovOrder = markovOrder;
		clear();
	}
	
	public static boolean hegedusHeuristic() {
		return hegedusHeuristic;
	}

	public static void setHegedusHeuristic(final boolean hegedusHeuristic) {
		Engine.hegedusHeuristic = hegedusHeuristic;
	}
	
	public static int getStrackEntrophyCompensation() {
		return strackEntropyCompensation;
	}

	public static void setStrackEntrophyCompensation(final int strackEntrophyCompensation) {
		Engine.strackEntropyCompensation = strackEntrophyCompensation;
	}

	public void clear() {
		followings.clear();
		preceedings.clear();
		containers.clear();
		knownTuples.clear();

		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc();
	}

	public long init(final URL url, final Loader.InputType inputType) throws IOException {
		long time = -System.currentTimeMillis();
		clear();	// performing clear could consume CPU time for greater inputs
		
		switch (inputType) {
			case IRC_LOG:{
				Loader.loadIRCLog(url, this);
				break;
			}
			
			case TXT: {
				Loader.loadTXT(url, this);
				break;
			}
			
			default:{
				System.err.println("Unresolved input type, parsing as simple TXT file: " + inputType);
				Loader.loadTXT(url, this);
			}
		}
		
		time += System.currentTimeMillis();
		System.out.println("Reinitialization performed, took " + time + " msecs.");
		return time;
	}

	/**
	 * Returns a list of Strings (words) in the specified message.
	 * 
	 * <p>
	 * The <tt>Engine</tt> considers everything a word that is bordered by whitespace characters.
	 * </p>
	 */
	private ArrayList<Tuple> parseTokens(String message) {
		assert (message != null) 
		: "Message cannot be null!";

		final ArrayList<Tuple> ret = new ArrayList<Tuple>();
		message = message.trim().replaceAll("\\s+", " ");

		final ArrayList<String> components = new ArrayList<String>();
		components.addAll(Arrays.asList(message.split(" ")));

		for (int i = 0; i < components.size() - markovOrder + 1; ++i) {
			final List<String> subList = components.subList(i, i + markovOrder);
			ret.add(new Tuple(subList.toArray(new String[subList.size()])));
		}

		return ret;
	}

	private void addToFollowing(final Tuple tuple, final String value) {
		if (!followings.containsKey(tuple)) {
			followings.put(tuple, new LinkedHashSet<String>());
		}

		followings.get(tuple).add(value);
	}
	
	private void addToPrecedings(final Tuple tuple, final String value) {
		if (!preceedings.containsKey(tuple)) {
			preceedings.put(tuple, new LinkedHashSet<String>());
		}

		preceedings.get(tuple).add(value);
	}

	private void addToContainer(final String component, final Tuple container) {
		if (!containers.containsKey(component)) {
			containers.put(component, new LinkedHashSet<Tuple>());
		}

		containers.get(component).add(container);
	}

	/**
	 * Adds the specified string to the bot's knowledge.
	 * 
	 * <p>
	 * This function iterates through the words of the message and creates the corresponding tuples.
	 * Besides it sets the necessary properties also (if the created tuple is a finisher/starter one).
	 * </p>
	 * 
	 * @param message
	 * @return true if the bot learned something new from the input;
	 * 			false otherwise.
	 */
	public boolean addString(final String message) {
		final ArrayList<Tuple> tokens = parseTokens(message);

		for (int i = 0; i < tokens.size(); ++i) {
			final Tuple tuple = tokens.get(i);
			if (!knownTuples.contains(tuple)) {
				knownTuples.add(tuple);
				descriptors.put(tuple, new Tuple.Descriptor());
			}

			for (final String act : tuple) {
				addToContainer(act, tuple);
			}

			if (i + 1 < tokens.size()) {
				// If there's more tokens to continue
				final String followingWord = tokens.get(i + 1).lastToken();
				addToFollowing(tuple, followingWord);
			} else {
				// Otherwise we simply mark the tuple as a finisher
				descriptors.get( tuple ).setFinisher(true);
			}
			
			if (i>0) {
				final String precedingWord = tokens.get(i-1).firstToken();
				addToPrecedings(tuple, precedingWord);
			} else {
				descriptors.get( tuple ).setStarter(true);
			}
		}

		// returns if we learned anything
		return (tokens.size() > 0);
	}
	
	public Result generateSentence() {
		return generateSentence(null);
	}
	
	/**
	 * If the parameter is <code>null</code> returns a sentence built from a random word in the database.
	 * Otherwise it tries to reply to the specified <tt>startingWord</tt> parameter.
	 * 
	 * <p>
	 * The algorithm is documented in its details in the attached <i>readme.txt</i> file.
	 * </p>
	 */
	public Result generateSentence(final String startingWord) {
		if (0 == knownTuples.size())
			return new Result("I ain't lern mysellf w00t. u teach me!", 0);
		
		final boolean knownWord = startingWord == null ||
				containers.containsKey(startingWord);
		
		if (hegedusHeuristic && !knownWord) {
			String ret = new String(startingWord);
			if (!startingWord.endsWith("?")) ret += '?';
			return new Result(ret, 0);
		}
		
		int entrophy = 0;
		
		System.out.println(">> Trying to reply to word: " + startingWord +
				( !knownWord ? " [UNKNOWN]" : ""));
		
		final Tuple[] tuples = (containers.containsKey(startingWord))
				? containers.get(startingWord).toArray(new Tuple[0])
				: knownTuples.toArray(new Tuple[knownTuples.size()]);
		
		final Tuple chosenTuple = tuples[random.nextInt(tuples.length)];
		Tuple tuple = chosenTuple;
		
		System.out.println(">> Selected tuple: " + tuple);
		System.out.println();
		
		System.out.println("--------------------------------------------------------- Building postfix");
		
		final Sentence sentence = new Sentence(tuple);

		while (! descriptors.get(tuple).isFinisher()) {
			final String[] nextTokens = followings.get(tuple).toArray(new String[0]);
			entrophy += nextTokens.length - 1 + strackEntropyCompensation;
			System.out.println(">> Available next tokens: " + Arrays.toString(nextTokens));
			final String next = nextTokens[random.nextInt(nextTokens.length)];
			System.out.println(">> Chosen one is: " + next);
			sentence.append(next);
			
			final Tuple candidate = tuple.shiftRight(next);
			System.out.println(">> Shifted tuple is: " + candidate +
					(descriptors.get(candidate).isFinisher() ? " (finisher)" : ""));
			
			for (final Tuple act : knownTuples) {
				if (act.equals(candidate)) {
					tuple = act;
				}
			}
			
			System.out.println();
		}
		
		System.out.println(">> Finished!");
		System.out.println();
		System.out.println("--------------------------------------------------------- Building prefix");
		tuple = chosenTuple;
		while (!descriptors.get(tuple).isStarter()) {
			final String[] prevTokens = preceedings.get(tuple).toArray(new String[0]);
			entrophy += prevTokens.length - 1 + strackEntropyCompensation;
			System.out.println(">> Available prev tokens: " + Arrays.toString(prevTokens));
			final String prev = prevTokens[random.nextInt(prevTokens.length)];
			System.out.println(">> Chosen one is: " + prev);
			sentence.prepend(prev);

			final Tuple candidate = tuple.shiftLeft(prev);
			System.out.println(">> Shifted tuple is: " + candidate +
					(descriptors.get(candidate).isStarter() ? " (starter)" : ""));
			
			for (final Tuple act : knownTuples) {
				if (act.equals(candidate)) {
					tuple = act;
				}
			}
			
			System.out.println();
		}
		
		System.out.println(">> Finished!");
		System.out.println();
		final Result result = new Result(sentence.toString(), entrophy);
		System.out.println(result);
		System.out.println("==========================================================================");
		System.out.println();
		System.out.println();

		return result;
	}// +generateSentence(String):Result
	
	//===================================================================================================
	
	/** JFT - Just For Testing ;] */
	public static void main(final String[] args) throws IOException {
		final Engine engine = new Engine();
		long time = -System.currentTimeMillis();
		Loader.loadIRCLog(new File("C:/tmp/test.txt").toURI().toURL(), engine);
		time += System.currentTimeMillis();
		
		System.out.println("Initialization took: " + time + " msec");
		
		System.out.println(engine.generateSentence());
		System.out.println(engine.generateSentence());
		System.out.println(engine.generateSentence());
		System.out.println(engine.generateSentence());
	}
	
}
