package net.edenciv.shortwave.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MessageUtils {
    
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    /**
     * Parse MiniMessage format to Component
     */
    public static Component parse(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * Format a simple colored message
     */
    public static String format(String message) {
        return message.replace("&", "§");
    }
}
