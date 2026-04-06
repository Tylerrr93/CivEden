package net.civmc.kitpvp.ranked;

public record RankedConfig(
    int matchTimeoutMinutes,
    int maxHeight,
    String defaultKitName,
    double eloKFactor,
    int recentMatchCooldownSeconds,
    double borderCenterX,
    double borderCenterZ,
    double borderInitialSize,
    double borderFinalSize,
    long borderShrinkSeconds,
    double borderDamageAmount,
    double spawn1X, double spawn1Y, double spawn1Z, float spawn1Yaw, float spawn1Pitch,
    double spawn2X, double spawn2Y, double spawn2Z, float spawn2Yaw, float spawn2Pitch,
    int initialEloGap,
    int gapAt20s,
    int gapAt40s,
    int gapAt60s
) {}
