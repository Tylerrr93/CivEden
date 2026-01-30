package com.github.longboyy.eve;

import com.github.longboyy.eve.model.Relay;
import com.github.longboyy.eve.model.SnitchHitType;
import com.untamedears.jukealert.model.Snitch;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.ChannelType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.GuildChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RelayManager {

    private final EvePlugin plugin;

    private final Map<Integer, Set<Relay>> relaysByGroupId;
    private final Map<String, Set<Relay>> relaysByChannelId;

    public RelayManager(EvePlugin plugin){
        this.plugin = plugin;
        this.relaysByGroupId = new HashMap<>();
        this.relaysByChannelId = new HashMap<>();
    }

    public boolean reloadRelays(){
        List<Relay> relayList = this.plugin.getDatabase().loadRelays();
        if(relayList == null){
            return false;
        }

        this.relaysByGroupId.clear();
        relayList.forEach(this::registerRelay);
        return true;
    }

    public boolean hasRelay(int groupId, String channelId){
        return this.findGroupRelayByChannel(groupId, channelId) != null;
    }

    public boolean createRelay(int groupId, String guildId, String channelId){
        if(this.hasRelay(groupId, channelId)) return false;

        Relay relay = this.plugin.getDatabase().insertRelay(groupId, guildId, channelId);
        if(relay == null) return false;

        this.registerRelay(relay);
        return true;
    }

    public boolean removeRelay(int groupId, String channelId){
        var foundRelay = findGroupRelayByChannel(groupId, channelId);

        if(foundRelay == null) return false;

        if(this.plugin.getDatabase().removeRelay(foundRelay.getRelayId())){
            this.relaysByGroupId.values().forEach(relays -> relays.removeIf(relay -> relay.getRelayId() == foundRelay.getRelayId()));
            this.relaysByChannelId.values().forEach(relays -> relays.removeIf(relay -> relay.getRelayId() == foundRelay.getRelayId()));
            return true;
        }

        return false;
    }

    public void publishSnitchHit(Player player, Snitch snitch, SnitchHitType hitType){
        if(snitch.getGroup() == null) return;
        if(!this.relaysByGroupId.containsKey(snitch.getGroup().getGroupId())) return;

        long currentTime = System.currentTimeMillis() / 1000L;
        var relays = this.relaysByGroupId.get(snitch.getGroup().getGroupId());

        String snitchName = snitch.getName();
        if(snitchName == null || snitchName.isBlank()){
            snitchName = "unnamed_snitch";
        }
        // Escape any backticks in the snitch name
        snitchName = snitchName.replace("`", "'");

        String groupName = snitch.getGroup().getName().replace("`", "'");
        String playerName = player.getName().replace("`", "'");

        String message = String.format("<t:%d:T> `[%s]` **%s** %s snitch `%s` at `%s`",
            currentTime,
            groupName,
            playerName,
            hitType.toString().toLowerCase(),
            snitchName,
            locationToString(player.getLocation())
        );

        relays.forEach(relay -> publishToDiscord(relay.getChannelId(), message));
    }

    private @Nullable Relay findGroupRelayByChannel(int groupId, String channelId){
        return relaysByGroupId.values().stream()
            .flatMap(Set::stream)
            .filter(relay -> relay.getGroupId() == groupId && relay.getChannelId().equals(channelId))
            .findFirst()
            .orElse(null);

    }

    private void registerRelay(Relay relay){
        if(!this.relaysByGroupId.containsKey(relay.getGroupId())){
            this.relaysByGroupId.put(relay.getGroupId(), new HashSet<>());
        }

        this.relaysByGroupId.get(relay.getGroupId()).add(relay);
    }

    private String locationToString(Location location){
        return "[" +
            location.getBlockX() + ", " +
            location.getBlockY() + ", " +
            location.getBlockZ() + "]";
    }

    private void publishToDiscord(String channelId, String message){
        try {
            GuildChannel channel = DiscordUtil.getJda().getGuildChannelById(channelId);
            if(channel == null || channel.getType() != ChannelType.TEXT) return;
            TextChannel textChannel = (TextChannel)channel;
            textChannel.sendMessage(message).queue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
