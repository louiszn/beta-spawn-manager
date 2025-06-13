package com.louiszn.SpawnManager.mixin;

import com.louiszn.SpawnManager.SpawnManagerConfig;
import net.minecraft.block.Block;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Spawn protection is hardcoded so we have to manually edit how the logic works.
 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Unique
    private static final Set<Integer> INTERACTABLE_BLOCKS = createInteractableBlockList();

    @Unique
    private static Set<Integer> createInteractableBlockList() {
        Set<Integer> set = new HashSet<>();

        set.add(Block.CAKE.id);
        set.add(Block.FURNACE.id);
        set.add(Block.DOOR.id);
        set.add(Block.CRAFTING_TABLE.id);
        set.add(Block.DISPENSER.id);
        set.add(Block.WALL_SIGN.id);
        set.add(Block.BED.id);
        set.add(Block.TRAPDOOR.id);
        set.add(Block.CHEST.id);
        set.add(Block.LEVER.id);
        set.add(Block.TRAPDOOR.id);
        set.add(Block.JUKEBOX.id);
        set.add(Block.REPEATER.id);

        return set;
    }

    @Shadow
    private MinecraftServer server;
    @Shadow
    private ServerPlayerEntity player;

    @Shadow public abstract void sendPacket(Packet packet);

    @Inject(
            method = "handlePlayerAction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;isOperator(Ljava/lang/String;)Z"),
            cancellable = true
    )
    private void handlePlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        ci.cancel();

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

            if (packet.action == 2) {
                player.sendMessage("§cYou can't do that here!");
            }

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
    }

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (packet.side == 255) {
            return;
        }

        ci.cancel();

        ServerWorld world = server.getWorld(player.dimensionId);
        ItemStack selectedItem = player.inventory.getSelectedItem();
        ItemStack item = this.player.inventory.getSelectedItem();

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
        boolean isBlockInteractable = SpawnManagerConfig.config.isBlockInteractable;
        int protectionRadius = SpawnManagerConfig.config.protectionRadius;

        int blockId = world.getBlockId(positionX, positionY, positionZ);

        boolean canInteractBlock = isBlockInteractable && INTERACTABLE_BLOCKS.contains(blockId);

        boolean hasPermissions = !isSpawnProtected ||
                maxDistance > protectionRadius ||
                server.playerManager.isOperator(player.name) ||
                canInteractBlock;

        if (isCloseEnough && hasPermissions) {
            player.interactionManager.interactBlock(
                    player, world, selectedItem,
                    positionX, positionY, positionZ, packet.side
            );
        } else if (INTERACTABLE_BLOCKS.contains(blockId) || item != null) {
            player.sendMessage("§cYou can't do that here!");
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
    }
}
