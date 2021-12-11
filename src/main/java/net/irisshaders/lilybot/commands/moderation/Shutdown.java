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

import java.awt.*;
import java.time.Instant;

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
            if (!equalsAny(buttonClickEvent.getComponentId())) return false;
            return !buttonClickEvent.isAcknowledged();
        }, buttonClickEvent -> {

            User buttonClickEventUser = buttonClickEvent.getUser();
            String id = buttonClickEvent.getComponentId().split(":")[1];

            switch (id) {

                case "yes" -> {

                    MessageEmbed finalShutdownEmbed = new EmbedBuilder()
                            .setTitle("Shutting down...")
                            .setDescription("Note: It may take a few minutes for Discord to update my presence and say that I am offline.")
                            .setColor(Color.RED)
                            .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build();

                    buttonClickEvent.editComponents().setEmbeds(finalShutdownEmbed).queue();
                    actionLog.sendMessageEmbeds(finalShutdownEmbed).queue();
                    LilyBot.LOG_LILY.info("Shutting down due to a request from " + buttonClickEventUser.getAsTag() + "!");

                    Mute.cancelTimers(); // Cancels timers, since they block shutdown by nothing being left (since they are left)

                    jda.shutdown();

                }
                case "no" -> {

                    MessageEmbed noShutdownEmbed = new EmbedBuilder()
                            .setTitle("Shutdown canceled")
                            .setColor(Color.GREEN)
                            .setFooter("Requested by " + buttonClickEventUser.getAsTag(), buttonClickEventUser.getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now())
                            .build();

                    buttonClickEvent.editComponents().setEmbeds(noShutdownEmbed).queue();

                }

            }

        }));

    }

    private boolean equalsAny(String id) {
        return id.equals("shutdown:yes") ||
                id.equals("shutdown:no");
    }

}
