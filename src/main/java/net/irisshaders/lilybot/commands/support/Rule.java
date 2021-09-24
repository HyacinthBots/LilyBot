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

            event.replyEmbeds(ruleUtils(user, "Rule 1", RuleDescriptions.DESC_1))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 2", RuleDescriptions.DESC_2))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 3", RuleDescriptions.DESC_3))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 4", RuleDescriptions.DESC_4))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 5", RuleDescriptions.DESC_5))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 6", RuleDescriptions.DESC_6))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 7", RuleDescriptions.DESC_7))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

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

            event.replyEmbeds(ruleUtils(user, "Rule 8", RuleDescriptions.DESC_8))
                    .mentionRepliedUser(false).setEphemeral(false).queue();

        }

    }

    private static MessageEmbed ruleUtils(User user, String title, String description) {

        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Color.RED)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .setTimestamp(Instant.now())
                .build();

    }

    private interface RuleDescriptions {

        String DESC_1 = "Be decent to one another. We're all human. Any and all forms of" +
                " bigotry, harassment, doxxing, exclusionary, or otherwise abusive behavior will not be tolerated." +
                " Excessive rudeness, impatience, and hostility are not welcome. Do not rage out or make personal attacks" +
                " against other people. Do not encourage users to brigade/raid other communities.";

        String DESC_2 = "Keep chat clean. Do not spam text, images, user mentions, emojis," +
                " reactions, or anything else. Do not post offensive, obscene, politically charged," +
                " or sexually explicit content. Avoid using vulgar language and excessive profanity." +
                " Use English at all times in public channels.";

        String DESC_3 = "Keep discussion on-topic. This server is focused around Iris." +
                " General chatter unrelated to Iris is best kept to other Discord communities or to DMs.";

        String DESC_4 = "Understand that support is not guaranteed. Support will be provided" +
                " on a best-effort basis.";

        String DESC_5 = "5. Do not ask for support on compiling Iris, and refrain from providing this support."
            " There is sufficient information for people who know what they're doing to compile the mod themselves without help."
            " If we would like you to use a given version of the mod, we will give you a compiled build of that version.";

        String DESC_6 = "No links to executable files or JAR files. Uploading or directly" +
                " linking to executable files is not allowed without prior approval.";

        String DESC_7 = "Refrain from sending unsolicited pings and direct messages. Pings" +
                " and DMs can be annoying to deal with, so please avoid using them unless they are necessary. Use pings in" +
                " replies with discretion as well. Unsolicited support requests made with pings and DMs are a big no.";

        String DESC_8 = "Adhere to the Discord Terms of Service. I'd like to avoid getting" +
                " this community banned. No piracy! Absolutely no support will be provided for people running cracked" +
                " versions of the game. Providing support for these people counts as a rule violation!";

    }

}
