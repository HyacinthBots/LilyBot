package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
public class Shutdown extends SlashCommand {

    private final EventWaiter waiter;

    public Shutdown(EventWaiter waiter) {
        this.name = "shutdown";
        this.help = "Shuts down the bot.";
        this.ownerCommand = true;
        this.guildOnly = true;
        this.waiter = waiter;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        User user = event.getUser();
        JDA jda = event.getJDA();
        TextChannel action_log = jda.getTextChannelById(LilyBot.ACTION_LOG);

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
        ).mentionRepliedUser(false).setEphemeral(true).queue(interactionHook -> waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
            if (buttonClickEvent.getUser() != user) return false;
            if (!equalsAny(buttonClickEvent.getButton().getId())) return false;
            return !buttonClickEvent.isAcknowledged();
        }, buttonClickEvent -> {

            User buttonClickEventUser = buttonClickEvent.getUser();
            String id = buttonClickEvent.getButton().getId();

            switch (id) {

                case "shutdown:yes" -> {

                    MessageEmbed finalShutdownEmbed = new EmbedBuilder()
                            .setTitle("Shutting down...")
                            .setDescription("Note: It may take a few minutes for Discord to update my presence and say that I am offline.")
                            .setColor(Color.RED)
                            .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build();

                    buttonClickEvent.replyEmbeds(finalShutdownEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
                    action_log.sendMessageEmbeds(finalShutdownEmbed).queue();

                    // Wait for it to send the embed and respond to any other commands. Can be reduced to a lower number if testing allows for it.
                    try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }

                    jda.shutdown();

                }
                case "shutdown:no" -> {

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
