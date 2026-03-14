package com.programmerdan.minecraft.civspy.listeners.impl;

import com.programmerdan.minecraft.civspy.DataManager;
import com.programmerdan.minecraft.civspy.PointDataSample;
import com.programmerdan.minecraft.civspy.annotations.RequirePlugins;
import com.programmerdan.minecraft.civspy.listeners.ServerDataListener;
import isaac.bastion.BastionBlock;
import isaac.bastion.event.BastionCreateEvent;
import isaac.bastion.event.BastionDamageEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import vg.civcraft.mc.citadel.model.Reinforcement;

public final class SignChangeListener extends ServerDataListener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public SignChangeListener(DataManager target, Logger logger, String server) {
        super(target, logger, server);
    }

    @Override
    public void shutdown() {
        // NO OP
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSignChange(SignChangeEvent event){
        try{
            Location loc = event.getBlock().getLocation();
            Chunk chunk = loc.getChunk();
            var data = new PointDataSample(
                "sign.change",
                this.getServer(),
                loc.getWorld().getName(),
                event.getPlayer().getUniqueId(),
                chunk.getX(),
                chunk.getZ(),
                this.getSignString(event));
            this.record(data);
        }catch(Exception e){
            logger.log(Level.SEVERE, "Failed to track Sign Change Event in CivSpy", e);
        }
    }

    private String getSignString(SignChangeEvent event) {
        return String.format("\"%s | %s | %s | %s\"",
            PLAIN_TEXT.serialize(Objects.requireNonNullElse(event.line(0), Component.empty())),
            PLAIN_TEXT.serialize(Objects.requireNonNullElse(event.line(1), Component.empty())),
            PLAIN_TEXT.serialize(Objects.requireNonNullElse(event.line(2), Component.empty())),
            PLAIN_TEXT.serialize(Objects.requireNonNullElse(event.line(3), Component.empty())));
    }

}
