package com.louiszn.SpawnManager.mixin;

import com.louiszn.SpawnManager.SpawnManagerConfig;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @ModifyConstant(method = "canInteract", constant = @Constant(intValue = 16))
    private int modifyProtectionRadius(int constant) {
        return SpawnManagerConfig.config.isSpawnProtected
                ? SpawnManagerConfig.config.protectionRadius
                : -1; // Use -1 to disable the protection since distance cannot lower than 0
    }
}
