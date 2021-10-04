package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Constants;
import net.irisshaders.lilybot.utils.Memory;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Shutdown extends SlashCommand {

    public Shutdown() {
        this.name = "shutdown";
        this.help = "Shuts down the bot.";
        this.ownerCommand = true;
        this.guildOnly = true;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();
        JDA jda = event.getJDA();
        TextChannel actionLog = jda.getTextChannelById(Constants.ACTION_LOG);

        MessageEmbed shutdownEmbed = new EmbedBuilder()
                .setTitle("Shut Down")
                .setDescription("Do you want to shutdown the bot? Respond with the buttons below.")
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

        event.replyEmbeds(shutdownEmbed).addActionRow(
                Button.of(ButtonStyle.PRIMARY, "shutdown:yes", "Yes", Emoji.fromUnicode("\u2705")),
                Button.of(ButtonStyle.PRIMARY, "shutdown:no", "No", Emoji.fromUnicode("\u274C"))
        ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
            if (!buttonClickEvent.getUser().equals(user)) return false;
            if (!equalsAny(buttonClickEvent.getButton().getId())) return false;
            return !buttonClickEvent.isAcknowledged();
        }, buttonClickEvent -> {

            User buttonClickEventUser = buttonClickEvent.getUser();
            String id = buttonClickEvent.getButton().getId().split(":")[1];

            switch (id) {

                case "yes" -> {

                    MessageEmbed finalShutdownEmbed = new EmbedBuilder()
                            .setTitle("Shutting down...")
                            .setDescription("Note: It may take a few minutes for Discord to update my presence and say that I am offline.")
                            .setColor(Color.RED)
                            .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build();

                    buttonClickEvent.replyEmbeds(finalShutdownEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
                    actionLog.sendMessageEmbeds(finalShutdownEmbed).queue();
                    LoggerFactory.getLogger(Shutdown.class).info("Shutting down!");

                    // Wait for it to send the embed and respond to any other commands. Can be reduced to a lower number if testing allows for it.
                    try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }

                    jda.shutdownNow();
                    try {
                        TimeUnit.SECONDS.sleep(3L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);

                }
                case "no" -> {

                    MessageEmbed noShutdownEmbed = new EmbedBuilder()
                            .setTitle("I am not shutting down")
                            .setColor(Color.GREEN)
                            .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build();

                    buttonClickEvent.replyEmbeds(noShutdownEmbed).mentionRepliedUser(false).setEphemeral(true).queue();

                }

            }

        }));

    }

    private boolean equalsAny(String id) {
        return id.equals("shutdown:yes") ||
                id.equals("shutdown:no");
    }

}
