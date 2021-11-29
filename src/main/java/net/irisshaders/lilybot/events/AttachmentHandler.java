package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;

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
        List<String> extensions = List.of("txt", "log");
        boolean shouldUpload = attachments.stream().map(Message.Attachment::getFileExtension).anyMatch(extensions::contains);
        if (shouldUpload) {
            /*
            done because apparently you can send multiple attachments from mobile at once
            not sure who would do that though
             */
            for (var attachment : attachments) {
                if (!extensions.contains(attachment.getFileExtension())) {
                    continue;
                } else {
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
                                try {
                                    channel.sendMessageEmbeds(linkEmbed(author)).setActionRow(
                                            Button.link(post(builder.toString()), "Click here to view")
                                    ).queue();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                }
            }
        }
    }

    private MessageEmbed linkEmbed(User user) {
        return new EmbedBuilder()
                .setTitle("File uploaded to Hastebin")
                .setColor(0x9992ff)
                .setFooter("Uploaded by " + user.getAsTag(), user.getEffectiveAvatarUrl())
                .build();
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
