package net.irisshaders.lilybot.objects;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import org.kohsuke.github.GitHub;

public class Memory {

    public static GitHub github;
    public static EventWaiter waiter;

    public static void remember(GitHub github, EventWaiter waiter) {
        Memory.github = github;
        Memory.waiter = waiter;
    }

    public static GitHub getGithub() {
        return github;
    }

    public static EventWaiter getWaiter() {
        return waiter;
    }

}
