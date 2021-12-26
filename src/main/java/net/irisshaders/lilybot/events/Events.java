package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.CommandType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.irisshaders.lilybot.utils.Constants;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Events that Lily listens for.
 */
@SuppressWarnings("ConstantConditions")
public class Events extends ListenerAdapter {

    /**
     * What Lily does when she starts.
     * @param event Lily's ReadyEvent.
     */
    @Override
    public void onReady(ReadyEvent event) {

        JDA jda = event.getJDA();
        String tag = jda.getSelfUser().getAsTag();
        TextChannel actionLog = jda.getTextChannelById(Constants.ACTION_LOG);

        jda.getGuildById(Constants.GUILD_ID).upsertCommand(new CommandData(CommandType.MESSAGE_CONTEXT, "Report message")).queue();

        LoggerFactory.getLogger(Events.class).info(String.format("Logged in as %s", tag));

        MessageEmbed onlineEmbed = new EmbedBuilder()
                .setTitle("Lily is now online!")
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .build();

        actionLog.sendMessageEmbeds(onlineEmbed).queue();

    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        Member member = event.getMember();
        Guild guild = event.getGuild();
        List<Member> memberList = guild.getMembers();
        TextChannel joinMessages = guild.getTextChannelById(Constants.JOIN_MESSAGES);

        MessageEmbed joinEmbed = new EmbedBuilder()
                .setTitle("User joined the server!")
                .setDescription("Everyone welcome " + member.getUser().getAsTag() + "!")
                .addField("ID:", member.getId(), true)
                .setColor(Color.GREEN)
                .setFooter("Member count: " + memberList.size())
                .setTimestamp(Instant.now())
                .build();

        joinMessages.sendMessageEmbeds(joinEmbed).queue();

    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {

        Member member = event.getMember();
        Guild guild = event.getGuild();
        List<Member> memberList = guild.getMembers();
        TextChannel joinMessages = guild.getTextChannelById(Constants.JOIN_MESSAGES);
        String timeJoined = member.getTimeJoined().format(DateTimeFormatter.RFC_1123_DATE_TIME);

        MessageEmbed leaveEmbed = new EmbedBuilder()
                .setTitle("User left the server!")
                .setDescription("Goodbye " + member.getUser().getAsTag() + "!")
                .addField("ID:", member.getId(), true)
                .addField("Joined on:", timeJoined, true)
                .setColor(Color.RED)
                .setFooter("Member count: " + memberList.size())
                .setTimestamp(Instant.now())
                .build();

        joinMessages.sendMessageEmbeds(leaveEmbed).queue();

    }

}
