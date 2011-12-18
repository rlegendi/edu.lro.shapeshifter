package edu.lro.shapeshifter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Random;

import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.presence.bot.IChatRoomBotEntry;
import org.eclipse.ecf.presence.bot.IChatRoomMessageHandler;
import org.eclipse.ecf.presence.chatroom.IChatRoomContainer;
import org.eclipse.ecf.presence.chatroom.IChatRoomMessage;
import org.eclipse.ecf.presence.chatroom.IChatRoomMessageSender;

import edu.lro.shapeshifter.Command.CommandSyntaxException;
import edu.lro.shapeshifter.engine.Engine;

/**
 * The message handler (the bot, in practice).
 * 
 * @author legendi
 */
public class Shapeshifter implements IChatRoomMessageHandler {

	private IChatRoomMessageSender sender;
	
	public final Random random = new Random();
	public final Engine engine = new Engine();
	
	public Shapeshifter()
			throws MalformedURLException, IOException {
		
		Command.setShapeshifter(this);
		
		new Command.ReCommand();
		new Command.ReplyCommand();
		new Command.ReinitCommand();
		new Command.HelpCommand();
		new Command.OrderCommand();
		new Command.DieCommand();
		new Command.ListCommand();
		new Command.HHCommand();
		new Command.SECommand();
	}
	
	public IChatRoomMessageSender getSender() {
		return sender;
	}
	
	//---------------------------------------------------------------------------------------------------
	
	private class InputCommand {
		public final String head;
		public final String[] args;
		public InputCommand(final String head, final String[] args) {
			super();
			this.args = args;
			this.head = head;
		}
	}
	
	private InputCommand parseCommandArguments(String command) {
		command = command.substring(1);// chopping the beginning '~'
		final String[] parts = command.trim().replaceAll("\\s", " ").split(" ");
		
		if (parts.length > 1 ) {
			return new InputCommand(parts[0], Arrays.copyOfRange(parts, 1, parts.length) );
		}
		
		return new InputCommand(parts[0], new String[0]);
	}
	
	@SuppressWarnings("serial")
	private static class UnresolvedCommandException extends Exception {};
	
	private String processCommand(final IChatRoomMessage message, final InputCommand inputCommand)
			throws UnresolvedCommandException, CommandSyntaxException {
		
		final Command command = Command.getCommands().get(inputCommand.head);
		
		if (null == command) {
			throw new UnresolvedCommandException();
		}
		
		return command.getReply(message, inputCommand.args);
	}
	
	//---------------------------------------------------------------------------------------------------
	
	private Thread worker = null;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.presence.bot.IChatRoomMessageHandler#handleRoomMessage
	 * (org.eclipse.ecf.presence.chatroom.IChatRoomMessage)
	 */
	@Override
	public void handleRoomMessage(final IChatRoomMessage message) {
		final String msg = message.getMessage();
		
		if (!msg.startsWith("~")) { //$NON-NLS-1$
			return;
		}
		
		// The bot processes only one command at a time
		
		if (worker != null) {
			try {
				sender.sendMessage("Whooa easy maaaaan I'm still working on the previous requests!!");
			} catch (final ECFException e) {
				e.printStackTrace();
			}
			
			return;
		}
		
		worker = new Thread() {
			@Override
				public void run() {
					final InputCommand command = parseCommandArguments(msg);
					
					try {
						try {
							sender.sendMessage( processCommand(message, command ) );
						} catch (final UnresolvedCommandException e) {
							sender.sendMessage("Unresolved command: " + command.head);
							e.printStackTrace();
						} catch (final CommandSyntaxException e) {
							sender.sendMessage(e.getMessage());
							e.printStackTrace();
						}
					} catch (final ECFException e) {
						e.printStackTrace();
					} finally {
						worker = null;
					}
			}
		};
		
		worker.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.presence.bot.IChatRoomMessageHandler#init(org.eclipse
	 * .ecf.presence.bot.IChatRoomBotEntry)
	 */
	@Override
	public void init(final IChatRoomBotEntry robot) {
		;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.presence.bot.IChatRoomContainerAdvisor#preChatRoomConnect
	 * (org.eclipse.ecf.presence.chatroom.IChatRoomContainer,
	 * org.eclipse.ecf.core.identity.ID)
	 */
	@Override
	public void preChatRoomConnect(final IChatRoomContainer roomContainer, final ID roomID) {
		sender = roomContainer.getChatRoomMessageSender();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ecf.presence.bot.IContainerAdvisor#preContainerConnect(org
	 * .eclipse.ecf.core.IContainer, org.eclipse.ecf.core.identity.ID)
	 */
	@Override
	public void preContainerConnect(final IContainer container, final ID targetID) {
		;
	}

}
