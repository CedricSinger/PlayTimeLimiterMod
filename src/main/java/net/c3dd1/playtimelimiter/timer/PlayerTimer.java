package net.c3dd1.playtimelimiter.timer;

import net.minecraft.nbt.*;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PlayerTimer {
    private Double leftPlaytime;

    public Double getLeftPlaytime() {
        return leftPlaytime;
    }

    public void setLeftPlaytime(Double amount) {
        leftPlaytime = amount;
    }

    public boolean decreaseLeftPlaytime(Double amount) {
        leftPlaytime -= amount;
        if(leftPlaytime <= 0.0){
            leftPlaytime = 0.0;
            return false;
        }
        return true;
    }

    public void copyFrom(PlayerTimer source) {
        this.leftPlaytime = source.leftPlaytime;
    }

    public void saveNBTData(CompoundTag nbt) {
        nbt.putDouble("leftPlaytime", leftPlaytime);
    }

    public void loadNBTData(CompoundTag nbt) {
        leftPlaytime = nbt.getDouble("leftPlaytime");
    }
}
