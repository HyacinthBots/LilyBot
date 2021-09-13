package net.irisshaders.lilybot.commands.support;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.time.Instant;

public class Rule extends SlashCommand {

    public Rule() {
        this.name = "rule";
        this.help = "Gives information about the rules.";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.children = new SlashCommand[]{
            new RuleOne(),
            new RuleTwo(),
            new RuleThree(),
            new RuleFour(),
            new RuleFive(),
            new RuleSix(),
            new RuleSeven(),
            new RuleEight()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // ignored because all commands are sub commands
    }

    public static class RuleOne extends SlashCommand {

        public RuleOne() {
            this.name = "1";
            this.help = "Reminds the user of Rule 1.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleOneEmbed = new EmbedBuilder()
                    .setTitle("Rule 1")
                    .setDescription(
                            "Be decent to one another. We're all human. Any and all forms of" +
                            " bigotry, harassment, doxxing, exclusionary, or otherwise abusive behavior will not be tolerated." +
                            " Excessive rudeness, impatience, and hostility are not welcome. Do not rage out or make personal attacks" +
                            " against other people. Do not encourage users to brigade/raid other communities."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleOneEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleTwo extends SlashCommand {

        public RuleTwo() {
            this.name = "2";
            this.help = "Reminds the user of Rule 2.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleTwoEmbed = new EmbedBuilder()
                    .setTitle("Rule Two")
                    .setDescription(
                            "Keep chat clean. Do not spam text, images, user mentions, emojis," +
                            " reactions, or anything else. Do not post offensive, obscene, politically charged," +
                            " or sexually explicit content. Avoid using vulgar language and excessive profanity." +
                            " Use English at all times in public channels."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleTwoEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleThree extends SlashCommand {

        public RuleThree() {
            this.name = "3";
            this.help = "Reminds the user of Rule 3.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleThreeEmbed = new EmbedBuilder()
                    .setTitle("Rule 3")
                    .setDescription(
                            "Keep discussion on-topic. This server is focused around Iris." +
                            " General chatter unrelated to Iris is best kept to other Discord communities or to DMs."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleThreeEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleFour extends SlashCommand {

        public RuleFour() {
            this.name = "4";
            this.help = "Reminds the user of Rule 4.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleFourEmbed = new EmbedBuilder()
                    .setTitle("Rule 4")
                    .setDescription(
                            "Understand that support is not guaranteed. Support will be provided" +
                            " on a best-effort basis."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleFourEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleFive extends SlashCommand {

        public RuleFive() {
            this.name = "5";
            this.help = "Reminds the user of Rule 5.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleFiveEmbed = new EmbedBuilder()
                    .setTitle("Rule 5")
                    .setDescription(
                            "Do not ask for support on compiling Iris, and refrain from providing this support." +
                            " There is sufficient information for people who know what they're doing to compile the mod themselves" +
                            " without help. The fact that compiled builds are only distributed to Patrons is intentional, and is" +
                            " intended to keep the support burden manageable."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleFiveEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleSix extends SlashCommand {

        public RuleSix() {
            this.name = "6";
            this.help = "Reminds the user of Rule 6.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleSixEmbed = new EmbedBuilder()
                    .setTitle("Rule 6")
                    .setDescription(
                            "No links to executable files or JAR files. Uploading or directly" +
                            " linking to executable files is not allowed without prior approval."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleSixEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleSeven extends SlashCommand {

        public RuleSeven() {
            this.name = "7";
            this.help = "Reminds the user of Rule 7.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleSevenEmbed = new EmbedBuilder()
                    .setTitle("Rule 7")
                    .setDescription(
                            "Refrain from sending unsolicited pings and direct messages. Pings" +
                            " and DMs can be annoying to deal with, so please avoid using them unless they are necessary. Use pings in" +
                            " replies with discretion as well. Unsolicited support requests made with pings and DMs are a big no."
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleSevenEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    public static class RuleEight extends SlashCommand {

        public RuleEight() {
            this.name = "8";
            this.help = "Reminds the user of Rule 8.";
            this.defaultEnabled = true;
            this.guildOnly = false;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();

            MessageEmbed ruleEightEmbed = new EmbedBuilder()
                    .setTitle("Rule 8")
                    .setDescription(
                            "Adhere to the Discord Terms of Service. I'd like to avoid getting" +
                            " this community banned. No piracy! Absolutely no support will be provided for people running cracked" +
                            " versions of the game. Providing support for these people counts as a rule violation!"
                    ).setColor(Color.RED)
                    .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                    .setTimestamp(Instant.now())
                    .build();

            event.replyEmbeds(ruleEightEmbed).mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

}
