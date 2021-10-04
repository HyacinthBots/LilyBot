package net.irisshaders.lilybot.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.SlashCommand;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Constants;

/**
 * Add before building jda or race conditions will occur, since this gets jda from onReady!
 *
 */
public class SlashCommandHandler extends ListenerAdapter {
	private volatile boolean ready = false;
	private final List<SlashCommand> preReadyQueue = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, SlashCommand> commands = new HashMap<>();
	private final Map<String, Optional<String>> commandsIds = new HashMap<>();
	private final Set<String> commandsToRemove = Collections.synchronizedSet(new HashSet<>());
	private final CommandClient client; // For some reason they need this to run
	private JDA jda;
	
	public SlashCommandHandler(CommandClient client) {
		this.client = client;
	}
	
	@Override
	public void onSlashCommand(SlashCommandEvent event) {
		SlashCommand command = commands.get(event.getName());
		if (command != null) {
			command.run(event, client);
		}
	}
	
	@Override
	public void onReady(ReadyEvent event) {
		if (ready)
			throw new IllegalStateException("Ready was called twice, what");
		ready = true;
		jda = event.getJDA();
		removeStrayCommands();
		synchronized (preReadyQueue) {
			for (var command: preReadyQueue) {
				registerCommand(command);
			}
			preReadyQueue.clear();
		}
	}
	
	/**
	 * Adds a {@link SlashCommand} to the server, no matter what time it is or if Lily already connected. <p>
	 * Will edit the command if it already exists
	 * @param slashCommand
	 */
	public void addSlashCommand(SlashCommand slashCommand) {
		if (ready) {
			registerCommand(slashCommand);
		} else {
			synchronized(preReadyQueue) { //Very good multithreaded code ik, at least it should be safe, I've seen good projects doing remotely similar things
				if (!ready) {
					preReadyQueue.add(slashCommand);
					return;
				}
			}
			registerCommand(slashCommand);
		}
	}
	
	/**
	 * Gets a registered {@link SlashCommand}.<p>
	 * Note that changes to its structure/syntax won't be updated until it gets sent back to
	 * {@link #addSlashCommand(SlashCommand)} (it will be updated), however behaviour can be changed
	 * and Discord won't care (like what's done when executed).<p>
	 * 
	 * This method always returns {@code null} if the bot hasn't received the ready event
	 */
	public SlashCommand getSlashCommand(String name) {
		SlashCommand command = commands.get(name);
		return command == null ? null : command;
	}
	
	/**
	 * Removes a {@link SlashCommand} from the bot and requests Discord to remove it from the list of available ones
	 */
	public void removeSlashCommand(String name) {
		Optional<String> command = commandsIds.get(name);
		if (command == null) {
			LilyBot.LOG_LILY.error("Tried to remove non-existing command", new Throwable());
			return;
		}
		if (!command.isPresent()) {
			commandsToRemove.add(name);
		}
		actuallyRemove(name);
	}
	
	public void removeStrayCommands() {
		String ourId = jda.getSelfUser().getApplicationId();
		Consumer<List<Command>> deleter = guildCommands -> {
			guildCommands.stream().filter(c -> ourId.equals(c.getId()) && !commands.containsKey(c.getName())).forEach(cmd -> {
				jda.getGuildById(Constants.GUILD_ID).deleteCommandById(cmd.getId()).queue();
				LilyBot.LOG_LILY.info("Removing stray command: "+cmd.getName());
			});
		};
		jda.getGuildById(Constants.GUILD_ID).retrieveCommands().submit().thenAccept(deleter);
		jda.retrieveCommands().submit().thenAccept(deleter);
	}
	
	/**
	 * Actually removes the command. Already must have checked that we have the id
	 */
	private void actuallyRemove(String name) {
		String id = commandsIds.get(name).get();
		LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID).deleteCommandById(id).queue();
		commandsIds.remove(name);
		commands.remove(name);
		commandsToRemove.remove(name);
	}
	
	/**
	 * Actually registers the command to Discord. To be used when {@link #ready} got true
	 */
	private void registerCommand(SlashCommand command) {
		// Copy from CommandClientImpl
		CommandData data = command.buildCommandData();
        Guild guild = LilyBot.INSTANCE.jda.getGuildById(Constants.GUILD_ID);
        List<CommandPrivilege> privileges = command.buildPrivileges(client);
        guild.upsertCommand(data).queue(command1 -> {
            if (!privileges.isEmpty())
                command1.updatePrivileges(guild, privileges).queue();
            // End of copy: Now we add id so we can remove it
            commandsIds.put(command1.getName(), Optional.of(command1.getId()));
            if (commandsToRemove.contains(command1.getName())) {
            	actuallyRemove(command1.getName());
            }
            LilyBot.LOG_LILY.info("Command "+command1.getName()+" accepted by Discord");
        });
        commands.put(command.getName(), command);
        commandsIds.put(command.getName(), Optional.empty());
	}
}
