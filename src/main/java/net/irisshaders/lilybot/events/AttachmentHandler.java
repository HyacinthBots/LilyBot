package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.irisshaders.lilybot.LilyBot;
import net.irisshaders.lilybot.utils.ResponseHelper;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AttachmentHandler extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.size() == 0) return;
        User author = message.getAuthor();
        MessageChannel channel = message.getChannel();
        String messageId = message.getId();
        List<String> extensions = List.of("txt", "log");
        boolean shouldUpload = attachments.stream().map(Message.Attachment::getFileExtension).anyMatch(extensions::contains);
        if (shouldUpload) {
            /*
            done because apparently you can send multiple attachments from mobile at once
            not sure who would do that though
             */
            for (var attachment : attachments) {
                attachment.retrieveInputStream()
                        .thenAccept(stream -> {
                            StringBuilder builder = new StringBuilder();
                            byte[] buffer = new byte[1024];
                            int count;
                            try {
                                while ((count = stream.read(buffer)) > 0) {
                                    builder.append(new String(buffer, 0, count));
                                }
                                stream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            channel.sendMessageEmbeds(linkEmbed(author)).setActionRow(
                                    Button.of(ButtonStyle.DANGER, "hastebin" + messageId + ":yes", "Yes"),
                                    Button.of(ButtonStyle.SECONDARY, "hastebin" + messageId + ":no", "No")
                            ).queue(message1 -> waitForButton(author, message, builder.toString()));
                        });
            }
        }
    }

    private void waitForButton(User author, Message message, String attachment) {
        LilyBot.INSTANCE.waiter.waitForEvent(ButtonClickEvent.class, buttonClickEvent -> {
            if (!equalsAny(buttonClickEvent.getComponentId(), message.getId())) return false;
            return !buttonClickEvent.isAcknowledged();
        }, buttonClickEvent -> {
            String id = buttonClickEvent.getComponentId().split(":")[1];
            InteractionHook hook = buttonClickEvent.getHook();
            User buttonClickEventUser = buttonClickEvent.getUser();
            if (!buttonClickEventUser.equals(author)) {
                buttonClickEvent.replyEmbeds(ResponseHelper.genFailureEmbed(buttonClickEventUser,
                        "You aren't allowed to do that!",  "Only the message author can!")
                ).mentionRepliedUser(false).setEphemeral(true).queue();
                waitForButton(author, message, attachment);
                return;
            }
            buttonClickEvent.deferEdit().queue();
            switch (id) {
                case "yes" -> {
                    try {
                        hook.editOriginalEmbeds(finalLinkEmbed(author)).setActionRow(
                                Button.link(post(attachment), "Click here to see")
                        ).queue(message2 -> message.delete().queue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case "no" -> hook.deleteOriginal().queue();
            }
        });
    }

    private MessageEmbed linkEmbed(User user) {
        return new EmbedBuilder()
                .setTitle("Do you want to upload this file to Hastebin?")
                .setDescription("It's easier for helpers to view the file on Hastebin. Do you want it to be uploaded?")
                .setColor(0x9992ff)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .build();
    }

    private MessageEmbed finalLinkEmbed(User user) {
        return new EmbedBuilder()
                .setTitle("File uploaded to Hastebin")
                .setColor(0x9992ff)
                .setFooter("Requested by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .build();
    }

    private boolean equalsAny(String id, String messageId) {
        return id.equals("hastebin" + messageId + ":yes") ||
                id.equals("hastebin" + messageId + ":no");
    }

    private String post(String text) throws IOException {
        byte[] postData = text.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        String requestURL = "https://www.toptal.com/developers/hastebin/documents";
        URL url = new URL(requestURL);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "Hastebin Java Api");
        connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        connection.setUseCaches(false);
        String response = null;
        try (DataOutputStream stream = new DataOutputStream(connection.getOutputStream())) {
            stream.write(postData);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            response = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response.contains("\"key\"")) {
            response = response.substring(response.indexOf(":") + 2, response.length() - 2);
            String postURL = "https://www.toptal.com/developers/hastebin/";
            response = postURL + response;
        }
        return response;
    }

}
