package com.github.longboyy.eve;

import net.dv8tion.jda.api.entities.User;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import java.util.Arrays;
import java.util.List;

public class EvePermissionHandler {

    private static PermissionType createRelay;

    private EvePermissionHandler(){}

    public static void setup(){
        List<GroupManager.PlayerType> adminsAndAbove = Arrays.asList(GroupManager.PlayerType.ADMINS, GroupManager.PlayerType.OWNER);
        createRelay = PermissionType.registerPermission("CREATE_EVE_RELAY", adminsAndAbove);
    }

    public static PermissionType getCreateRelayPermission(){
        return createRelay;
    }

}
