package edu.lro.shapeshifter.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * A resource handler class, performs the loading of files into the {@link Engine}.
 * 
 * <p>
 * Currently there are 2 supported resource files through {@link InputType}:
 * <ol>
 * <li><b>Simple TXT files</b> Parsed on per-character basis.<br>
 *  The text is splitted into sentences when a punctuation character (any of the "<tt>.?!</tt>"
 *  characters) is found.</li>
 * <li><b>Pure IRC logs</b> Parsed on per-line basis. <br>
 * 	<i>Please make sure you've removed all unnecessary
 * 	text from the file, such as system notifications, join/part events, timestamps, etc.</i>.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * The classes methods must be called in a static way; instantiation is prohibited.
 * </p>
 * 
 * @author legendi
 */
public class Loader {
	
	/** Guess what :P */
	public static final String PUNCTUATIONS = ".!?";
	
	/** Defines the possible input values. */
	public static enum InputType {TXT, IRC_LOG};
	
	public static void loadTXT(final URL url, final Engine engine)
			throws IOException {
		
		BufferedReader br = null;

		try {
			br = new BufferedReader( new InputStreamReader(url.openStream()) );
			StringBuilder sb = new StringBuilder(100);
			int ch = 0, ctr = 0;
			
			while ( (ch = br.read() ) != -1 ) {
				sb.append((char)ch);
				
				if ( PUNCTUATIONS.indexOf(ch) >=0 ) {
					engine.addString(sb.toString().trim());
					
					if (++ctr % 10000 == 0) {
						System.out.println("Parsed " + ctr + " lines so far.");
					}
					
					sb.delete(0, sb.length());
				}
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();

			if (br != null) {
				try {
					br.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			
			throw ioe;
		}
	}//S+loadTXT(URL,Engine)
	
	public static void loadIRCLog(final URL url, final Engine engine) throws IOException {
		BufferedReader br = null;

		try {
			br = new BufferedReader( new InputStreamReader(url.openStream()) );
			int ctr = 0;
			
			while ( br.ready() ) {
				engine.addString(br.readLine());
				
				if (++ctr % 10000 == 0) {
					System.out.println("Parsed " + ctr + " lines so far.");
				}
			}
			
		} catch (final IOException ioe) {
			ioe.printStackTrace();

			if (br != null) {
				try {
					br.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			
			throw ioe;
		}
	}//S+loadIRCLog(URL,Engine) 
	
	/** Hiding the constructor, to prohibit instantiation. */
	private Loader() {};
}
