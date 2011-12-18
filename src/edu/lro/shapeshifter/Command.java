package edu.lro.shapeshifter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.presence.bot.IChatRoomMessageHandler;
import org.eclipse.ecf.presence.chatroom.IChatRoomMessage;

import edu.lro.shapeshifter.engine.Engine;
import edu.lro.shapeshifter.engine.Loader;
import edu.lro.shapeshifter.engine.Engine.Result;
import edu.lro.shapeshifter.engine.Loader.InputType;

/**
 * Abstract superclass of bot commands.
 * 
 * <p>
 * All of the commands see the global {@link Shapeshifter} instance as well as the {@link Engine}
 * instance. It is set during the initialization of the {@link IChatRoomMessageHandler}.<br>
 * The Command instances should be created there in the constructor as well (for details, see the
 * implementation of {@link Shapeshifter#Shapeshifter()}).
 * </p>
 * 
 * @author legendi
 */
//TODO: doc (to create HTML doc for the implemented commands)
public abstract class Command {
	
	public static final String DESC_SEPARATOR = " --> ";
	public static final int SENTENCE_SAMPLE_SIZE = 5;
	
	@SuppressWarnings("serial")
	public static class CommandSyntaxException extends Exception {
		public CommandSyntaxException(final String message) {
			super(message);
		}
	};
	
	private static final HashMap<String, Command> commands = new HashMap<String, Command>();
	public static Shapeshifter shapeshifter = null;
	private static Engine engine = null;

	public static Map<String, Command> getCommands() {
		return Collections.unmodifiableMap(commands);
	}
	
	public static void setShapeshifter(final Shapeshifter shapeshifter) {
		Command.shapeshifter = shapeshifter;
		Command.engine = shapeshifter.engine;
	}
	
	//---------------------------------------------------------------------------------------------------

	public final String head;

	public Command(final String head) {
		super();
		this.head = head;
		
		commands.put(head, this);
	}

	public abstract String getReply(IChatRoomMessage message, String args[])
			throws CommandSyntaxException;
	
	public String getHelpDescription() {
		return "Usage: ~" + head;
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class ReCommand extends Command {

		public ReCommand() {
			super("re");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args) {
			return "Hello thar! I'm Shapeshifter, have you seen this fantastic blog? http://roante.dyndns.org/";
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + DESC_SEPARATOR + "About info.";
		}
	}

	//---------------------------------------------------------------------------------------------------
	public static class ReplyCommand extends Command {
		public ReplyCommand() {
			super("reply");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args) {
			final String startingWord = (args.length > 0)
				? args[shapeshifter.random.nextInt(args.length)]
				: null;
			
			ArrayList<Result> results = new ArrayList<Result>(SENTENCE_SAMPLE_SIZE);
			String ret = "N/A";
			int maxEntrophy = -1;
			for (int i=0; i<SENTENCE_SAMPLE_SIZE; ++i) {
				Result result = engine.generateSentence(startingWord);
				if (result.entropy > maxEntrophy) {
					maxEntrophy = result.entropy;
					ret = result.sentence;
				}
				results.add(result);
			}
			
			System.out.println();
			System.out.println();
			System.out.println("----------------------------------------------------------------------");
			System.out.println("Candidates for input string [" + startingWord + "]");
			System.out.println("----------------------------------------------------------------------");
			for (Result result : results) System.out.println(result);
			System.out.println("----------------------------------------------------------------------");
			
			engine.addString(MyUtils.join(args));
			return ret;
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + " [question]" + DESC_SEPARATOR +
				"Used to generate a new sentence. If a parameter is specified, the bot tries to respond for it.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	private static String reinit(final String[] args) throws CommandSyntaxException {
		try {
			final URL url = new URL( (args.length > 0) ? args[0] : "file:/C:/test.txt" );
			Loader.InputType inputType;
			
			try {
				inputType = (args.length > 1)
					? InputType.valueOf( args[1].toUpperCase() )
					: InputType.TXT;
			} catch (Exception e) {
				inputType = InputType.TXT;
				e.printStackTrace();
			}
			
			try {
				shapeshifter.getSender().sendMessage("Started parsing specified file as " + inputType + "...");
			} catch (ECFException e) {
				e.printStackTrace();
			}
			
			final long time = engine.init(url, inputType);
			return "Engine reinitialization of " + url.getPath() + " performed [" + inputType + "], took " + time + " msecs. " +
					"A sum of " + engine.getKnownTuples().size() + " tuples were created.";
		} catch (final MalformedURLException e) {
			throw new CommandSyntaxException("Cannot find specified file: " + e.getMessage());
		} catch (final IOException e) {
			throw new CommandSyntaxException("IOException: " + e.getMessage());
		}
	}
	
	public static class ReinitCommand extends Command {
		public ReinitCommand() {
			super("reinit");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			return reinit(args);
		}

		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + " [URL]" + DESC_SEPARATOR +
					"Reinitilaizes the knowledge of the bot with the default knowledge base. " +
					"If an URL is specified, it tries to connect the specified source and learns from its " +
					"content. It is hardly encouraged to use simple txt files.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class HelpCommand extends Command {
		public HelpCommand() {
			super("help");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			if (0 == args.length) {
				return getHelpDescription();
			}
			
			String commandName = new String(args[0]);
			if (commandName.startsWith("~")) {
				commandName = commandName.substring(1);
			}
			
			final Command command = commands.get( args[0] );
			
			if ( null == command) {
				return "No such command: " + args[0];
			}
			
			return command.getHelpDescription();
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + " [command]" + DESC_SEPARATOR +
				"Shows usage info for the given command (type ~list for the list of commands).";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class OrderCommand extends Command {
		public OrderCommand() {
			super("order");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			if ( 0 == args.length ) {
				return "Current Markov-order is " + engine.getOrder() + ".";
			}
			
			try {
				final int order = Integer.parseInt(args[0]);
				engine.setOrder(order);
				
				return "Markov-order was set to " + order + ". " + reinit( Arrays.copyOfRange(args, 1, args.length));
			} catch (final NumberFormatException e) {
				e.printStackTrace();
				throw new CommandSyntaxException("Argument must be an integer. " + e.getMessage());
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
				throw new CommandSyntaxException("Illegal argument. " + e.getMessage());
			}
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + " [order]" + DESC_SEPARATOR +
				"Sets the order of the used Markov-chains. Order must be an integer value between [1,5]. " +
				"Automatically reinits the knowledge base. If no order was specified, prints the current order setting.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class DieCommand extends Command {
		public DieCommand() {
			super("die");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			try {
				shapeshifter.getSender().sendMessage("Buh-bye! I'm gonna die soon...");
			} catch (ECFException e) {
				e.printStackTrace();
			}
			
			System.exit(0);
			return null;
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + DESC_SEPARATOR +
				"Kills the bot immediately.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class ListCommand extends Command {
		public ListCommand() {
			super("list");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			StringBuilder sb = new StringBuilder("Available commands: ");
			String[] commandNames = commands.keySet().toArray(new String[commands.keySet().size()]);
			Arrays.sort(commandNames);
			
			for (int i=0; i<commandNames.length; ++i) {
				if (i>0) sb.append(", ");
				sb.append(commandNames[i]);
			}
			
			sb.append(".");
			return sb.toString();
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + DESC_SEPARATOR +
				"Lists the available commands.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class HHCommand extends Command {
		public HHCommand() {
			super("hh");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			Engine.setHegedusHeuristic( ! Engine.hegedusHeuristic() );
			return "Hegedus heuristic is now turned " +
				( Engine.hegedusHeuristic() ? "on" : "off" ) + ".";
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + DESC_SEPARATOR +
				"Turns using Hegedus-heuristics on/off.";
		}
	}
	
	//---------------------------------------------------------------------------------------------------
	public static class SECommand extends Command {
		public SECommand() {
			super("se");
		}

		@Override
		public String getReply(final IChatRoomMessage message, final String[] args)
				throws CommandSyntaxException {
			
			if ( 0 == args.length ) {
				return "Current Strack-entrophy compensation is " + Engine.getStrackEntrophyCompensation() + ".";
			}
			
			try {
				final int value = Integer.parseInt(args[0]);
				Engine.setStrackEntrophyCompensation(value);
				return "Strack-entrophy compensation was set to " + value + ".";
			} catch (final NumberFormatException e) {
				e.printStackTrace();
				throw new CommandSyntaxException("Argument must be an integer. " + e.getMessage());
			}
		}
		
		@Override
		public String getHelpDescription() {
			return super.getHelpDescription() + DESC_SEPARATOR +
				"Sets the Strack-entropy compensation value (by default it's 7).";
		}
	}
	
}
