package net.irisshaders.lilybot.commands.services;

import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.Memory;
import org.kohsuke.github.*;

import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GitHub extends SlashCommand {

    public GitHub() {
        this.name = "github";
        this.help = "Gets some information from GitHub.";
        this.defaultEnabled = true;
        this.guildOnly = false;
        this.children = new SlashCommand[]{
          new GitHubIssueSubCommand(),
          new GitHubRepoSubCommand(),
          new GitHubUserSubCommand()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // ignored because all commands are sub commands
    }

    @SuppressWarnings("ConstantConditions")
    public static class GitHubIssueSubCommand extends SlashCommand {

        public GitHubIssueSubCommand() {
            this.name = "issue";
            this.help = "Gets some information about an issue from a repository on GitHub.";
            this.defaultEnabled = true;
            this.guildOnly = false;
            List<OptionData> optionData = new ArrayList<>();
            optionData.add(new OptionData(OptionType.STRING, "repo", "The repo to look up. Format: User/Repo or Org/Repo.").setRequired(true));
            optionData.add(new OptionData(OptionType.STRING, "issue", "The issue. Provide a number or a search term.").setRequired(true));
            this.options = optionData;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            GHIssue issue;

            User user = event.getUser();
            String repo = event.getOption("repo").getAsString();
            String term = event.getOption("issue").getAsString();

            try {
                issue = parseMessage(repo, term);
            } catch (Exception e) {
                MessageEmbed noMatchesEmbed = new EmbedBuilder()
                        .setDescription(
                                "Invalid issue number or an invalid repository was provided." +
                                " Please ensure the issue exists and the repository is public."
                        )
                        .setColor(Color.RED)
                        .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build();
                event.replyEmbeds(noMatchesEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
                return;
            }
            if (issue == null) return;

            try {
                event.replyEmbeds(issueBuilder(issue, user)).mentionRepliedUser(false).setEphemeral(false).queue();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public GHIssue parseMessage(String repo, String term) throws Exception {

            GHIssue issue;
            int issueNum;

            try {
                issueNum = Integer.parseInt(term);
                issue = LilyBot.INSTANCE.gitHub.getRepository(repo).getIssue(issueNum);
            } catch (NumberFormatException e) {
                PagedIterator<GHIssue> iterator = LilyBot.INSTANCE.gitHub.searchIssues()
                        .q(term + " repo:" + repo)
                        .order(GHDirection.DESC)
                        .list()
                        ._iterator(1);
                if (iterator.hasNext()) {
                    issue = iterator.next();
                } else {
                    throw new Exception("Could not find specified issue in repo `" + repo + "`!");
                }
                int num = issue.getNumber();
                issue = LilyBot.INSTANCE.gitHub.getRepository(repo).getIssue(num);
                return issue;
            }
            return issue;

        }

        public static MessageEmbed issueBuilder(GHIssue issue, User user) {

            EmbedBuilder e = new EmbedBuilder();
            e.setTitle(issue.getTitle(), String.valueOf(issue.getHtmlUrl()));

            if (issue.getBody() != null) {
                if (issue.getBody().length() > 400) {
                    e.setDescription(issue.getBody().substring(0, 399) + "...");
                } else {
                    e.setDescription(issue.getBody());
                }
            }

            boolean open = issue.getState() == GHIssueState.OPEN;
            boolean merged = false;
            boolean draft = false;

            if (issue.isPullRequest()) {

                e.setAuthor("Information for Pull Request #" + issue.getNumber() + " in " + issue.getRepository().getFullName());

                try {
                    GHPullRequest pull = issue.getRepository().getPullRequest(issue.getNumber());
                    merged = pull.isMerged();
                    draft = pull.isDraft();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    e.setTitle("Error!");
                    e.setDescription("Error occurred initializing Pull request. How did this happen?");
                    return e.build();
                }
            } else {
                e.setAuthor("Information for Issue #" + issue.getNumber() + " in " + issue.getRepository().getFullName());
            }

            if (merged) {
                e.setColor(Color.decode("#6f42c1"));
                e.addField("Status:", "Merged", false);
            } else if (!open) {
                e.setColor(Color.decode("#cb2431"));
                e.addField("Status:", "Closed", false);
            } else if (draft) {
                e.setColor(Color.decode("#ffffff"));
                e.addField("Status:", "Draft", false);
            } else {
                e.setColor(Color.decode("#2cbe4e"));
                e.addField("Status:", "Open", false);
            }

            try {
                GHUser author = issue.getUser();
                if (author.getName() != null) e.addField("Author:", "[" + author.getLogin() + " (" + author.getName() + ")" + "](https://github.com/" + author.getLogin() + ")", false);
                else e.addField("Author:", "[" + author.getLogin() + "](https://github.com/" + author.getLogin() + ")", false);
            } catch (IOException ioException) {
                e.addField("Author:", "Unknown Author", false);
            }

            try {
                e.setFooter("Opened");
                e.setTimestamp(issue.getCreatedAt().toInstant());
                List<CharSequence> labels = new ArrayList<>();

                for (GHLabel label : issue.getLabels()) {
                    labels.add(label.getName());
                }

                if (labels.size() > 0) e.addField("Labels:", String.join(", ", labels), false);
                List<CharSequence> assignees = new ArrayList<>();
                for (GHUser assignee : issue.getAssignees()) {
                    assignees.add(assignee.getLogin());
                }
                if (assignees.size() > 0) e.addField("Assignees:", String.join(", ", assignees), false);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            e.setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl());
            e.setTimestamp(Instant.now());

            return e.build();
        }

    }

    @SuppressWarnings("ConstantConditions")
    public static class GitHubRepoSubCommand extends SlashCommand {

        public GitHubRepoSubCommand() {
            this.name = "repo";
            this.help = "Gets some information about a repository on GitHub.";
            this.defaultEnabled = true;
            this.guildOnly = false;
            List<OptionData> optionData = new ArrayList<>();
            optionData.add(new OptionData(OptionType.STRING, "repo", "The repo to look up. Format: User/Repo or Org/Repo.").setRequired(true));
            this.options = optionData;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            User user = event.getUser();
            String repoName = event.getOption("repo").getAsString();

            if (!repoName.contains("/")) {
                MessageEmbed incorrectSyntaxEmbed = new EmbedBuilder()
                        .setDescription("""
                            Make sure your input is formatted like this:
                            Format: User/Repo or Org/Repo
                            For example: `IrisShaders/Iris`
                            """
                        )
                        .setColor(Color.RED)
                        .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build();
                event.replyEmbeds(incorrectSyntaxEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
                return;
            }

            GHRepository repo;

            try {
                repo = LilyBot.INSTANCE.gitHub.getRepository(repoName);
            } catch (IOException e) {
                MessageEmbed noMatchesEmbed = new EmbedBuilder()
                        .setDescription("Invalid repository name. Make sure this repository exists!")
                        .setColor(Color.RED)
                        .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build();
                event.replyEmbeds(noMatchesEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
                return;
            }

            event.replyEmbeds(getRepoData(repo)).mentionRepliedUser(false).setEphemeral(false).queue();

        }

        public static MessageEmbed getRepoData(GHRepository repo) {

            EmbedBuilder e = new EmbedBuilder();

            try {
                e.setTitle("GitHub repository Info for " + repo.getFullName(), "https://github.com/" + repo.getFullName());
                e.setDescription(repo.getDescription());
                if (repo.getLicense() != null) e.addField("License:", repo.getLicense().getName(), false);
                e.addField("Open Issues/PRs:", String.valueOf(repo.getOpenIssueCount()), false);
                e.addField("Stars:", String.valueOf(repo.getStargazersCount()), false);
                e.addField("Size:", bytesToFriendly(repo.getSize()), false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            return e.build();

        }

        private static String bytesToFriendly(long bytes) {

            int k = 1024;
            String[] measure = new String[]{"B", "KB", "MB", "GB", "TB"};
            double i;

            if (bytes == 0) i = 0; else i = Math.floor(Math.log(bytes) / Math.log(k));

            DecimalFormat df = new DecimalFormat("#.##");

            return df.format(bytes / Math.pow(k, i)) + " " + measure[(int) i + 1];

        }

    }

    @SuppressWarnings("ConstantConditions")
    public static class GitHubUserSubCommand extends SlashCommand {

        public GitHubUserSubCommand() {
            this.name = "user";
            this.help = "Gets some information about a user on GitHub.";
            List<OptionData> optionData = new ArrayList<>();
            optionData.add(new OptionData(OptionType.STRING, "username", "The user to look up.").setRequired(true));
            this.options = optionData;
        }

        @Override
        protected void execute(SlashCommandEvent event) {

            GHUser ghUser = null;

            User user = event.getUser();
            String username = event.getOption("username").getAsString();

            try {
                ghUser = LilyBot.INSTANCE.gitHub.getUser(username);
            } catch (IOException e) {
                MessageEmbed noMatchesEmbed = new EmbedBuilder()
                        .setDescription("Invalid username. Make sure this user exists.")
                        .setColor(Color.RED)
                        .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .build();
                event.replyEmbeds(noMatchesEmbed).mentionRepliedUser(false).setEphemeral(true).queue();
            }

            event.replyEmbeds(getUser(ghUser, user)).mentionRepliedUser(false).setEphemeral(false).queue();

        }

        public static MessageEmbed getUser(GHUser ghUser, User user) {

            EmbedBuilder e = new EmbedBuilder();

            try {
                e.setThumbnail(ghUser.getAvatarUrl());
                e.addField("Repositories:", String.valueOf(ghUser.getPublicRepoCount()), false);
                e.setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl());
                e.setTimestamp(Instant.now());

                boolean isOrg = ghUser.getType().equals("Organization");

                if (!isOrg) {
                    e.setTitle("GitHub profile for " + ghUser.getLogin(), "https://github.com/" + ghUser.getLogin());
                    e.setDescription(ghUser.getBio());
                    e.addField("Followers:", String.valueOf(ghUser.getFollowersCount()), false);
                    e.addField("Following:", String.valueOf(ghUser.getFollowingCount()), false);
                    if (ghUser.getCompany() != null) e.addField("Company:", ghUser.getCompany(), false);
                    if (!ghUser.getBlog().equals("")) e.addField("Website:", ghUser.getBlog(), false);
                    if (ghUser.getTwitterUsername() != null) e.addField("Twitter:",
                            "[@" + ghUser.getTwitterUsername() + "](https://twitter.com/" + ghUser.getTwitterUsername() + ")", false);
                } else {
                    GHOrganization org = LilyBot.INSTANCE.gitHub.getOrganization(ghUser.getLogin());
                    e.setTitle("GitHub profile for " + ghUser.getLogin(), "https://github.com/" + ghUser.getLogin());
                    e.addField("Public Members:", String.valueOf(org.listMembers().toArray().length), false);
                }

            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

            return e.build();

        }

    }

}
