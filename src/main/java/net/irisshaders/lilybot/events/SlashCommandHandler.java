package net.irisshaders.lilybot.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.SlashCommand;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.Command.Option;
import net.dv8tion.jda.api.interactions.commands.Command.Subcommand;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
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
    private final Map<String, SlashCommand> commands = new ConcurrentHashMap<>();
    private final Map<String, Optional<String>> commandsIds = new ConcurrentHashMap<>();
    private final Set<String> commandsToRemove = ConcurrentHashMap.newKeySet();
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
        jda = event.getJDA();
        ready = true;
        var currentCommands = getCurrentCommands();
        synchronized (preReadyQueue) {
            for (var command: preReadyQueue) {
                registerCommandIfNoEquivalentIsPresent(currentCommands, command);
            }
            preReadyQueue.clear();
        }
        removeStrayCommands();
    }
    
    /**
     * Returns the commands that are currently registered <i>in Discord</i>. Can differ from the ones we have registered here
     */
    private Map<String, Command> getCurrentCommands() {
        var map = new ConcurrentHashMap<String, Command>();
        Consumer<List<Command>> commandAdder = list -> {
            for (Command cmd : list) {
                map.put(cmd.getName(), cmd);
            }
        };
        CompletableFuture<?> cf1 = jda.getGuildById(Constants.GUILD_ID).retrieveCommands().submit().thenAccept(commandAdder);
        CompletableFuture<?> cf2 = jda.retrieveCommands().submit().thenAccept(commandAdder);
        cf1.join(); // Could this be properly multithreaded?
        cf2.join();
        return map;
    }
    
    /**
     * Adds a {@link SlashCommand} to the server, no matter what time it is or if Lily already connected. <p>
     * Will edit the command if it already exists
     * @param slashCommand The {@link SlashCommand} to add
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
        return commands.get(name);
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
        } else {
            actuallyRemove(name);
        }
    }
    
    private void removeStrayCommands() { // This could use the result of getCurrentCommands that was already computed, but it would make it not async
        String ourId = jda.getSelfUser().getApplicationId();
        Consumer<List<Command>> deleter = guildCommands -> {
            guildCommands.stream().filter(c -> ourId.equals(c.getApplicationId()) && !commands.containsKey(c.getName())).forEach(cmd -> {
                jda.getGuildById(Constants.GUILD_ID).deleteCommandById(cmd.getId()).queue(nothing -> 
                    LilyBot.LOG_LILY.info("Removed stray command: "+cmd.getName())
                );
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
        jda.getGuildById(Constants.GUILD_ID).deleteCommandById(id).queue();
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
        Guild guild = jda.getGuildById(Constants.GUILD_ID);
        List<CommandPrivilege> privileges = command.buildPrivileges(client);
        guild.upsertCommand(data).queue(command1 -> {
            if (!privileges.isEmpty())
                command1.updatePrivileges(guild, privileges).queue();
            // End of copy: Now we add id so we can remove it
            commandsIds.put(command1.getName(), Optional.of(command1.getId()));
            if (commandsToRemove.contains(command1.getName())) {
                actuallyRemove(command1.getName());
            }
            LilyBot.LOG_LILY.info("Command '"+command1.getName()+"' accepted by Discord");
        });
        commands.put(command.getName(), command);
        commandsIds.put(command.getName(), Optional.empty());
    }
    
    /**
     * Registers a command if there isn't an equivalent one already registered in commands.<p>
     * 
     * To be used when registering all commands at startup.<p>
     * 
     * This calls {@link #registerCommand(SlashCommand)} for registering the command if it's not present.
     * 
     * @implNote The code is awful because our JDA doesn't use the same class for a command that we create
     *           and a command that is registered in Discord
     */
    private void registerCommandIfNoEquivalentIsPresent(Map<String, Command> existingCommands, SlashCommand slashCommand) {
        Command currentCommand = existingCommands.get(slashCommand.getName());
        if (currentCommand != null) {
            if (currentCommand.getDescription().equals(slashCommand.getHelp()) // They are called differently, but are the same
                    && currentCommand.getOptions().size() == slashCommand.getOptions().size()
                    && currentCommand.isDefaultEnabled() == slashCommand.isDefaultEnabled()
                    && currentCommand.getSubcommands().size() == slashCommand.getChildren().length
                    && optionsMatches(slashCommand.getOptions(), currentCommand.getOptions()))
            {
                boolean everySubcommandMatches = true;
                for (SlashCommand subcommand : slashCommand.getChildren()) {
                    Subcommand equivalent = null;
                    for (Subcommand currentSubcommand: currentCommand.getSubcommands()) {
                        if (subcommand.getName().equals(currentSubcommand.getName())) {
                            equivalent = currentSubcommand;
                            break;
                        }
                    }
                    if (equivalent == null || !(subcommand.getHelp().equals(equivalent.getDescription())
                        && subcommand.getOptions().size() == equivalent.getOptions().size()
                        && optionsMatches(subcommand.getOptions(), equivalent.getOptions())
                        ))
                    {
                        everySubcommandMatches = false;
                        break;
                    }
                }
                if (everySubcommandMatches) {
                    // The command is equivalent. Register its id and things only, without sending it
                    commands.put(slashCommand.getName(), slashCommand);
                    commandsIds.put(slashCommand.getName(), Optional.of(currentCommand.getId()));
                    LilyBot.LOG_LILY.info("Equivalent to '"+slashCommand.getName()+"' is already registered to Discord as is, not sending it");
                    return;
                }
            }
        }
        // The command isn't equivalent or doesn't exist. Sending it will create or update it
        registerCommand(slashCommand);
    }
    
    /**
     * Part of {@link #registerCommandIfNoEquivalentIsPresent(Map, SlashCommand)}.<p>
     * 
     * List size must have already been checked to be equals
     */
    private boolean optionsMatches(List<OptionData> optionDataList, List<Option> optionList) {
        for (OptionData optionData : optionDataList) {
            Option equivalent = null;
            for (Option option : optionList) {
                if (option.getName().equals(optionData.getName())) {
                    equivalent = option;
                    break;
                }
            }
            if (equivalent == null || !(optionData.getType() == equivalent.getType()
                    && optionData.isRequired() == equivalent.isRequired()
                    && optionData.getDescription().equals(equivalent.getDescription())
                    && optionData.getChoices().equals(equivalent.getChoices())))
            {
                return false;
            }
        }
        return true;
    }
}
