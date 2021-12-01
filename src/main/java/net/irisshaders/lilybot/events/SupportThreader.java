package net.irisshaders.lilybot.events;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.irisshaders.lilybot.utils.Constants;

public class SupportThreader implements EventListener {
    private TextChannel supportChannel = null;

    public static void createSupportThread(TextChannel channel, Member user, String parentMessage) {
        String initialMessage = "Welcome to your support thread " + user.getAsMention() + "!";
        channel.createThreadChannel("Support thread for "+ user.getEffectiveName(), parentMessage).queue(thread -> {
            thread.sendMessage(initialMessage).queue(message -> {
                message.editMessage(initialMessage + " The " + message.getGuild().getRoleById(Constants.SUPPORT_TEAM).getAsMention() + " will arrive shortly").queue();
            });
        });
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof MessageReceivedEvent messageEvent) {
            if (supportChannel == null) { // Prevents getting the channel every single message
                supportChannel = event.getJDA().getTextChannelById(Constants.SUPPORT_CHANNEL);
            }
            if (messageEvent.getChannel().equals(supportChannel)) {
                createSupportThread(supportChannel, messageEvent.getMember(), messageEvent.getMessageId());
            }
        }
    }
}
