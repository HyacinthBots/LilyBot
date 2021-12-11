package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
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
import net.irisshaders.lilybot.utils.ResponseHelper;

import org.slf4j.LoggerFactory;

import java.awt.*;
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

        MessageEmbed shutdownEmbed = ResponseHelper.responseEmbed("Shut Down", user, Color.RED)
                .setDescription("Do you really want to shutdown the bot?")
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

                    MessageEmbed finalShutdownEmbed = ResponseHelper.responseEmbed("Shutting down...", buttonClickEventUser, Color.RED)
                            .setDescription("Note: It may take a few minutes for Discord to update my presence and say that I am offline.")
                            .build();

                    buttonClickEvent.editComponents().setEmbeds(finalShutdownEmbed).queue();
                    actionLog.sendMessageEmbeds(finalShutdownEmbed).queue();
                    LoggerFactory.getLogger(Shutdown.class).info("Shutting down due to a request from " + buttonClickEventUser.getAsTag() + "!");

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

                    MessageEmbed noShutdownEmbed = ResponseHelper.responseEmbed(user, Color.GREEN)
                            .setDescription("Shutdown canceled")
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
