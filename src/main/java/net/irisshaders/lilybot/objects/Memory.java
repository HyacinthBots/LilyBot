package net.irisshaders.lilybot.objects;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import org.kohsuke.github.GitHub;

/**
 * Lily's "Memory".
 */
public class Memory {

    public static GitHub github;
    public static EventWaiter waiter;

    /**
     * A convenience method to "remember" objects.
     * @param github The GitHub instance.
     * @param waiter The EventWaiter instance.
     */
    public static void remember(GitHub github, EventWaiter waiter) {
        Memory.github = github;
        Memory.waiter = waiter;
    }

    /**
     * Gets Lily's GitHub instance.
     * @return GitHub
     */
    public static GitHub getGithub() {
        return github;
    }

    /**
     * Get Lily's EventWaiter instance.
     * @return EventWaiter
     */
    public static EventWaiter getWaiter() {
        return waiter;
    }

}
