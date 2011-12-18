package edu.lro.shapeshifter.engine;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A tuple consisting of <tt>N</tt> words.
 * 
 * <p>
 * Declared a <tt>Serializable</tt> to make it easy to dump. Maybe it will be
 * used in the future ;-)
 * </p>
 * 
 * @author legendi
 */
public class Tuple implements Serializable, Iterable<String> {
	
	/* (non-Javadoc) */
	private static final long serialVersionUID = 4244679687729206506L;
	
	/** The array of words. */
	private final String[] tokens;
	
	/**
	 * Descriptor for Tuples.
	 * 
	 * <p>
	 * Currently only 2 properties are taken care of:
	 * <ol>
	 * <li><tt>finisher</tt> true if the current tuple can be used to close a generated sentence.</li>
	 * <li><tt>starter</tt> true if the current tuple can be used to start a generated sentence.</li>
	 * </ol>
	 * </p>
	 */
	public static class Descriptor {
		/** If the current tuple can be used to close a generated sentence. */
		private boolean finisher;
		/** If the current tuple can be used to start a generated sentence. */
		private boolean starter;
		
		public boolean isFinisher() {
			return finisher;
		}
	
		public void setFinisher(final boolean finisher) {
			this.finisher = finisher;
		}
	
		public boolean isStarter() {
			return starter;
		}
	
		public void setStarter(final boolean starter) {
			this.starter = starter;
		}
	}
	
	//---------------------------------------------------------------------------------------------------

	public Tuple(final String[] tokens) {
		this.tokens = tokens;
	}

	public int length() {
		return tokens.length;
	}

	public String getToken(final int index) {
		if (index >= tokens.length)
			throw new ArrayIndexOutOfBoundsException(index);

		return tokens[index];
	}
	
	/**
	 * Returns a new tuple that has the components of this tuple expanded to right with the specified
	 * word.
	 * 
	 * <p>
	 * i.e.: <code>System.out.printLn(new Tuple(new String[] {"a", "b", "c"}).shiftRight("d"));</code><br>
	 * result:	<code>{"b", "c", "d"}</code>
	 * </p>
	 */
	public Tuple shiftRight(final String next) {
		final String[] tokens = new String[this.tokens.length];
		System.arraycopy(this.tokens, 1, tokens, 0, this.tokens.length - 1);
		tokens[this.tokens.length - 1] = next;
		return new Tuple(tokens);
	}

	/**
	 * Returns a new tuple that has the components of this tuple expanded to left with the specified
	 * word.
	 * 
	 * <p>
	 * i.e.: <code>System.out.printLn(new Tuple(new String[] {"a", "b", "c"}).shiftLeft("d"));</code><br>
	 * result:	<code>{"d", "a", "b"}</code>
	 * </p>
	 */
	public Tuple shiftLeft(final String prev) {
		final String[] tokens = new String[this.tokens.length];
		System.arraycopy(this.tokens, 0, tokens, 1, this.tokens.length - 1);
		tokens[0] = prev;
		return new Tuple(tokens);
	}

	public String firstToken() {
		return tokens[0];
	}
	
	public String lastToken() {
		return tokens[tokens.length - 1];
	}

	//---------------------------------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(tokens);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Tuple other = (Tuple) obj;
		if (!Arrays.equals(tokens, other.tokens))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<String> iterator() {
		return Arrays.asList(tokens).iterator();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Arrays.toString(tokens);
	}

}
