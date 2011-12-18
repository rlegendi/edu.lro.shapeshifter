package edu.lro.shapeshifter;

/**
 * Place of utility functions. Not a point of interest yet :-)
 * 
 * <p>
 * The classes methods must be called in a static way; instantiation is
 * prohibited.
 * </p>
 * 
 * @author legendi
 */
public class MyUtils {

	/**
	 * Joins the given array of Strings into one String.
	 * 
	 * @param arr an array of Strings to be concatenated
	 * @return the concatenated strings, separated with a space character 
	 */
	public static String join(final String[] arr) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < arr.length; ++i) {
			if (i > 0) sb.append(' ');
			sb.append(arr[i]);
		}

		return sb.toString();
	}

	/** Hiding the constructor, to prohibit instantiation. */
	private MyUtils() {};
}
