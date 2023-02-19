package net.c3dd1.playtimelimiter.timer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTimerProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<PlayerTimer> PLAYER_TIMER = CapabilityManager.get(new CapabilityToken<PlayerTimer>() { });

    private PlayerTimer timer = null;
    private final LazyOptional<PlayerTimer> optional = LazyOptional.of(this::createPlayerTimer);

    private PlayerTimer createPlayerTimer() {
        if(this.timer == null) {
            this.timer = new PlayerTimer();
        }

        return this.timer;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == PLAYER_TIMER) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createPlayerTimer().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerTimer().loadNBTData(nbt);
    }
}
