package net.irisshaders.lilybot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.Instant;

public class ResponseHelper {

    /**
     * This method generates a Failure embed for ease of use.
     * @param user The user of the event.
     * @param title The title of the embed.
     * @param description The description of the embed.
     * @return the built embed
     */
    public static MessageEmbed genFailureEmbed(User user, String title, String description) {

        EmbedBuilder embed = responseEmbed("Something went wrong!", user, Color.RED)
                .setDescription("Please try again!");

        if (title != null) embed.setTitle(title);
        if (description != null) embed.setDescription(description);

        return embed.build();
    }
    
    /**
     * Creates an {@link EmbedBuilder} and populates it with the current timestamp, the given {@link User} as the
     * requester and the given {@link Color} as its color.
     * 
     * @param title The title to set the embed to, or {@code null} to set no title (prefer using {@link #responseEmbed(User, Color)} instead of null)
     * @param requester The user to display information from in the "Requested by" section. Must not be {@code null}
     * @param color The color to set the embed to. If it's {@code null}, the embed will have no color
     * @return An {@link EmbedBuilder} with the given parameters
     * 
     * @see #responseEmbed(User, Color)
     */
    public static EmbedBuilder responseEmbed(String title, User requester, Color color) {
        return responseEmbed(requester, color)
            .setTitle(title);
    }
    
    /**
     * Creates an {@link EmbedBuilder} and populates it with the current timestamp, the given {@link User} as the
     * requester and the given {@link Color} as its color.<p>
     * 
     * If you want to set a title, consider using {@link #responseEmbed(String, User, Color)} instead
     * 
     * @param requester The user to display information from in the "Requested by" section. Must not be {@code null}
     * @param color The color to set the embed to. If it's {@code null}, the embed will have no color
     * @return An {@link EmbedBuilder} with the given parameters
     * 
     * @see #responseEmbed(String, User, Color)
     */
    public static EmbedBuilder responseEmbed(User requester, Color color) {
        return new EmbedBuilder()
            .setTimestamp(Instant.now())
            .setColor(color)
            .setFooter("Requested by " + requester.getAsTag(), requester.getEffectiveAvatarUrl());
    }
    
    /**
     * Creates a {@link String} containing the user as a mention, and their user tag {@code @user (user#1234)},
     * to be used in moderation embeds where linking with the user id and their possibly changed name is desirable,
     * but knowing the tag at the time of action is also important.
     * 
     * @param user The user to produce the string from
     * @return The {@link String} being the user's mention followed by the user's tag (in parenthesis)
     */
    public static String userField(User user) {
        return user.getAsMention() + " (" + user.getAsTag() + ")";
    }

}
