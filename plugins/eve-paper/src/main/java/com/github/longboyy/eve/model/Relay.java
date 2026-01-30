package com.github.longboyy.eve.model;

public class Relay {

    private final int groupId;
    private final String guildId;
    private final String channelId;
    private final int relayId;

    public Relay(int groupId, String guildId, String channelId, int relayId){
        this.groupId = groupId;
        this.guildId = guildId;
        this.channelId = channelId;
        this.relayId = relayId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getGuildId() {
        return guildId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getRelayId() {
        return relayId;
    }

}
