package net.irisshaders.lilybot.utils;

import java.util.Date;

public class DateHelper {
    /**
     * Formats a {@link Date} instance into a {@link String} with the date and time using the new discord time thing
     */
    public static String formatDateAndTime(Date date) {
        return "<t:"+ date.getTime()/1000+">";
    }
    
    /**
     * Formats a {@link Date} into a {@link String} to the relative time using the new discord time thing
     * @param date
     * @return
     */
    public static String formatRelative(Date date) {
        return "<t:"+ date.getTime()/1000+":R>";
    }
}
