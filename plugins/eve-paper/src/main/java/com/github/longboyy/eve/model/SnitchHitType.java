package com.github.longboyy.eve.model;

public enum SnitchHitType {

    ENTER("hit"), LOGIN("Logged in on"), LOGOUT("Logged out on");

    private final String prettyName;

    SnitchHitType(String prettyName){
        this.prettyName = prettyName;
    }

    @Override
    public String toString() {
        return this.prettyName;
    }

    public static SnitchHitType fromString(String value) {
        return SnitchHitType.valueOf(value.trim().toUpperCase());
    }

}
