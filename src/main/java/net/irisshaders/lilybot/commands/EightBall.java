package net.irisshaders.lilybot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.utils.ResponseHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EightBall extends SlashCommand {

    public EightBall() {
        this.name = "eightball";
        this.help = "Ask Lily for some advice!";
        this.defaultEnabled = true;
        this.guildOnly = false;
        List<OptionData> optionData = new ArrayList<>();
        optionData.add(new OptionData(OptionType.STRING, "question", "The question for Lily to answer.").setRequired(true));
        this.options = optionData;
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        String[] advice = {"Yep!","I think so!","Nope!","Maybe!","Without a doubt!","<:crong:922788732011171860>","You should ask IMS","As likely as a good Sodium Forge port"};
        int adviceSize = advice.length - 1;
        String question = event.getOption("question") == null ? "" : event.getOption("question").getAsString();

        User user = event.getUser();

        EmbedBuilder eightBallEmbed = ResponseHelper.responseEmbed("Eightball", user, Color.ORANGE)
                .setDescription("Asks Lily for advice!");

        event.replyEmbeds(eightBallEmbed.build()).mentionRepliedUser(false).setEphemeral(false).queue(interactionHook -> {

            MessageEmbed finalEightBallEmbed = eightBallEmbed
                    .setDescription(String.format(
                            """
                            **Question**: `%s`
                            
                            Lily says...
                            
                            `%s`
                            """, question, advice[(int) Math.floor(Math.random()*(adviceSize-0+1)+0)]))
                    .build();

            interactionHook.editOriginalEmbeds(finalEightBallEmbed).queue();
        });
    }
}
