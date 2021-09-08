package net.irisshaders.lilybot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.irisshaders.lilybot.LilyBot;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static net.irisshaders.lilybot.LilyBot.GUILD_ID;

@SuppressWarnings("ConstantConditions")
public class MuteList extends SlashCommand {

    public MuteList() {
        this.name = "mute-list";
        this.help = "Lists all muted members.";
        this.defaultEnabled = false;
        this.enabledRoles = new String[]{LilyBot.MODERATOR_ROLE};
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.botMissingPermMessage = "The bot does not have the `MANAGE_ROLES` permission.";
    }

    @Override
    protected void execute(SlashCommandEvent event) {

        JDA jda = event.getJDA();
        Guild guild = jda.getGuildById(GUILD_ID);
        Role mutedRole = guild.getRoleById(LilyBot.MUTED_ROLE);
        User user = event.getUser();
        List<Member> guildMembers = guild.getMembers();
        List<String> mutedMembers = new ArrayList<>();

        guildMembers.stream().filter(member -> member.getRoles().contains(mutedRole))
                .forEach(member -> mutedMembers.add(member.getAsMention()));

        if (mutedMembers.isEmpty()) {

            MessageEmbed noMutedMembers = new EmbedBuilder()
                    .setTitle("There are no muted members.")
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(noMutedMembers).mentionRepliedUser(false).setEphemeral(true).queue();

        } else {

            MessageEmbed muteListEmbed = new EmbedBuilder()
                    .setTitle("Mute List")
                    .setDescription(String.format("These are the muted members:\n %s", mutedMembers)
                            .replace("[", "").replace("]", ""))
                    .setColor(Color.CYAN)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(muteListEmbed).mentionRepliedUser(false).setEphemeral(true).queue();

        }

    }

}
