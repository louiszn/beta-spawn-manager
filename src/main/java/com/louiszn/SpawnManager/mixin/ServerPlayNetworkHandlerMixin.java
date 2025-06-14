package com.louiszn.SpawnManager.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.louiszn.SpawnManager.SpawnManagerConfig;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spawn protection is hardcoded so we have to manually edit how the logic works.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    private MinecraftServer server;
    @Shadow
    private ServerPlayerEntity player;

    @Shadow
    private boolean teleported;

    @Shadow
    public abstract void sendPacket(Packet packet);

    @Unique
    private void sendDeniedMessage() {
        player.sendMessage("§cYou can't do that here!");
    }

    @Unique
    private boolean isBlockUsable(Block block) {
        try {
            // onUse(World world, int x, int y, int z, PlayerEntity player)
            block.getClass().getDeclaredMethod("onUse", World.class, int.class, int.class, int.class, PlayerEntity.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Unique
    private int getProtectionRadius() {
        return SpawnManagerConfig.config.isSpawnProtected
                ? SpawnManagerConfig.config.protectionRadius
                : -1; // Use -1 to disable the protection since distance cannot lower than 0
    }

    @Unique
    private Block getBlockById(int id) {
        return BlockRegistry.INSTANCE.get(id);
    }

    @ModifyConstant(method = {"handlePlayerAction", "onPlayerInteractBlock"}, constant = @Constant(intValue = 16))
    private int modifyProtectionRadius(int constant) {
        return getProtectionRadius();
    }

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
                    ordinal = 0
            )
    )
    private void onLightBlockBreak(PlayerActionC2SPacket packet, CallbackInfo ci) {
        ServerWorld world = server.getWorld(player.dimensionId);

        int blockId = world.getBlockId(packet.x, packet.y, packet.z);
        Block block = BlockRegistry.INSTANCE.get(blockId);

        if (block == null) {
            return;
        }

        // Blocks with 0 hardness doesn't trigger packet.action 2 so we will consider this as a light block
        if (block.getHardness() == 0) {
            sendDeniedMessage();
        }
    }

    @Inject(
            method = "handlePlayerAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
                    ordinal = 1
            )
    )
    private void onHardBlockBreak(PlayerActionC2SPacket packet, CallbackInfo ci) {
        this.sendDeniedMessage();
    }

    @Inject(
            method = "onPlayerInteractBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
                    ordinal = 0
            )
    )
    private void onBlockInteract(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        ServerWorld world = server.getWorld(player.dimensionId);

        int positionX = packet.x;
        int positionY = packet.y;
        int positionZ = packet.z;

        Vec3i spawnPosition = world.getSpawnPos();

        int distanceX = (int) MathHelper.abs(positionX - spawnPosition.x);
        int distanceZ = (int) MathHelper.abs(positionZ - spawnPosition.z);

        int maxDistance = Math.max(distanceX, distanceZ);

        double playerDistance = this.player.getSquaredDistance(
                (double) positionX + 0.5,
                (double) positionY + 0.5,
                (double) positionZ + 0.5
        );

        int protectionRadius = getProtectionRadius();

        boolean isFailedToInteract = !(this.teleported && playerDistance < 64.0 && (maxDistance > protectionRadius || world.bypassSpawnProtection));

        if (!isFailedToInteract) {
            return;
        }

        int blockId = world.getBlockId(positionX, positionY, positionZ);
        Block block = getBlockById(blockId);

        if (block == null) {
            return;
        }

        ItemStack itemStack = player.inventory.getSelectedItem();

        boolean canUseBlock = SpawnManagerConfig.config.areProtectedBlocksUsable;

        if (canUseBlock && isBlockUsable(block)) {
            player.interactionManager.interactBlock(player, world, itemStack, positionX, positionY, positionZ, packet.side);
        } else {
            this.sendDeniedMessage();
        }
    }

    @Inject(
            method = "onPlayerInteractBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/ScreenHandler;sendContentUpdates()V"
            )
    )
    private void updateItemSlot(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        ItemStack stack = player.inventory.getSelectedItem();

        if (stack == null) {
            return;
        }

        Item item = stack.getItem();

        // Bucket doesn't send correct item state so we should update it manually.
        if (item instanceof BucketItem) {
            Slot slot = player.currentScreenHandler.getSlot(player.inventory, player.inventory.selectedSlot);
            this.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.currentScreenHandler.syncId, slot.id, player.inventory.getSelectedItem()));
        }
    }
}
