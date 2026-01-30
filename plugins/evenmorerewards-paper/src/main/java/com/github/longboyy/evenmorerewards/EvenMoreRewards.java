package com.github.longboyy.evenmorerewards;

import com.github.longboyy.evenmorerewards.rewards.EssenceRewardType;
import com.oheers.fish.api.EMFAPI;
import com.oheers.fish.api.reward.RewardType;
import vg.civcraft.mc.civmodcore.ACivMod;
import java.util.ArrayList;
import java.util.List;

public class EvenMoreRewards extends ACivMod {

    private List<RewardType> rewardTypes = new ArrayList<>();

    public EvenMoreRewards() {
        rewardTypes.add(new EssenceRewardType(this));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        rewardTypes.forEach(RewardType::register);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        rewardTypes.forEach(RewardType::unregister);
    }
}
