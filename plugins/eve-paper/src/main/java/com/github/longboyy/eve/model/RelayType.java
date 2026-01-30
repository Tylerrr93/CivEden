package com.github.longboyy.eve.model;

public enum RelayType {
    CHAT, SNITCH;

    public static RelayType fromString(String s){
        return valueOf(s.toUpperCase());
    }
}
