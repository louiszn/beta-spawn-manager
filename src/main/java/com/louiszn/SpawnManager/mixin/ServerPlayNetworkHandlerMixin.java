package com.louiszn.SpawnManager.mixin;

import com.louiszn.SpawnManager.SpawnManagerConfig;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spawn protection is hardcoded so we have to manually edit how the logic works.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public MinecraftServer server;
    @Shadow public ServerPlayerEntity player;

    @Shadow public abstract void sendPacket(Packet packet);

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void handlePlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.action == 4) {
            player.dropSelectedItem();
            return;
        }

        ServerWorld world = server.getWorld(player.dimensionId);

        Vec3i spawnPosition = world.getSpawnPos();

        int positionX = packet.x;
        int positionY = packet.y;
        int positionZ = packet.z;

        int spawnX = spawnPosition.x;
        int spawnZ = spawnPosition.z;

        int distanceX = (int) MathHelper.abs((float) (positionX - spawnX));
        int distanceZ = (int) MathHelper.abs((float) (positionZ - spawnZ));

        int maxDistance = Math.max(distanceX, distanceZ);

        boolean isSpawnProtected = SpawnManagerConfig.config.isSpawnProtected;
        int protectionRadius = SpawnManagerConfig.config.protectionRadius;

        if (isSpawnProtected && maxDistance <= protectionRadius && !server.playerManager.isOperator(player.name)) {
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(positionX, positionY, positionZ, world));
            ci.cancel();
            return;
        }

        if (packet.action == 0) {
            player.interactionManager.onBlockBreakingAction(positionX, positionY, positionZ, packet.direction);
        } else if (packet.action == 2) {
            player.interactionManager.continueMining(positionX, positionY, positionZ);

            if (world.getBlockId(positionX, positionY, positionZ) != 0) {
                player.networkHandler.sendPacket(new BlockUpdateS2CPacket(positionX, positionY, positionZ, world));
            }
        }

        ci.cancel();
    }

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        ServerWorld world = server.getWorld(player.dimensionId);
        ItemStack selectedItem = player.inventory.getSelectedItem();

        if (packet.side == 255) {
            if (selectedItem != null) {
                player.interactionManager.interactItem(player, world, selectedItem);
            }
        } else {
            Vec3i spawnPosition = world.getSpawnPos();

            int positionX = packet.x;
            int positionY = packet.y;
            int positionZ = packet.z;

            int spawnX = spawnPosition.x;
            int spawnZ = spawnPosition.z;

            int distanceX = (int) MathHelper.abs((float) (positionX - spawnX));
            int distanceZ = (int) MathHelper.abs((float) (positionZ - spawnZ));

            int maxDistance = Math.max(distanceX, distanceZ);

            boolean isCloseEnough = player.getSquaredDistance(
                    (double) positionX + 0.5F,
                    (double) positionY + 0.5F,
                    (double) positionZ + 0.5F
            ) < 64.0F;

            boolean isSpawnProtected = SpawnManagerConfig.config.isSpawnProtected;
            int protectionRadius = SpawnManagerConfig.config.protectionRadius;

            if (isCloseEnough && (!isSpawnProtected || (maxDistance > protectionRadius || server.playerManager.isOperator(player.name)))) {
                player.interactionManager.interactBlock(
                        player, world, selectedItem,
                        positionX, positionY, positionZ, packet.side
                );
            }

            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(positionX, positionY, positionZ, world));

            switch (packet.side) {
                case 0 -> --positionY;
                case 1 -> ++positionY;
                case 2 -> --positionZ;
                case 3 -> ++positionZ;
                case 4 -> --positionX;
                case 5 -> ++positionX;
            }

            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(positionX, positionY, positionZ, world));
        }

        selectedItem = player.inventory.getSelectedItem();
        if (selectedItem != null && selectedItem.count == 0) {
            player.inventory.main[player.inventory.selectedSlot] = null;
        }

        player.skipPacketSlotUpdates = true;
        player.inventory.main[player.inventory.selectedSlot] =
                ItemStack.clone(player.inventory.main[player.inventory.selectedSlot]);

        Slot slot = player.currentScreenHandler.getSlot(player.inventory, player.inventory.selectedSlot);
        player.currentScreenHandler.sendContentUpdates();
        player.skipPacketSlotUpdates = false;

        if (!ItemStack.areEqual(player.inventory.getSelectedItem(), packet.stack)) {
            this.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    player.currentScreenHandler.syncId, slot.id, player.inventory.getSelectedItem()));
        }

        ci.cancel();
    }
}
