package com.louiszn.SpawnManager.command;

import com.louiszn.SpawnManager.SpawnManagerConfig;
import com.matthewperiut.retrocommands.api.Command;
import com.matthewperiut.retrocommands.util.ServerUtil;
import com.matthewperiut.retrocommands.util.SharedCommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;

public class SpawnCommand implements Command {
    @Override
    public String name() {
        return "spawn";
    }

    @Override
    public void manual(SharedCommandSource sharedCommandSource) {

    }

    @Override
    public void command(SharedCommandSource source, String[] strings) {
        String identifier = strings[1];

        if (identifier.equals("set")) {
            this.onSet(source, strings);
        } else if (identifier.equals("protection")) {
            this.onProtection(source, strings);
        }
    }

    private void onSet(SharedCommandSource source, String[] strings) {
        MinecraftServer server = ServerUtil.getServer();

        int positionX;
        int positionY;
        int positionZ;

        try {
            positionX = Integer.parseInt(strings[2]);
            positionY = Integer.parseInt(strings[3]);
            positionZ = Integer.parseInt(strings[4]);
        } catch (Exception e) {
            source.sendFeedback("Invalid syntax: /spawn set [x: int] [y: int] [z: int]");
            return;
        }

        ServerWorld world = server.getWorld(0);

        world.getProperties().setSpawn(positionX, positionY, positionZ);

        source.sendFeedback("Successfully set spawn to " + positionX + " " + positionY + " " + positionZ);
    }

    private void onProtection(SharedCommandSource source, String[] strings) {
        String identifier = strings[2];

        switch (identifier) {
            case "enable" -> {
                SpawnManagerConfig.config.isSpawnProtected = true;
                SpawnManagerConfig.save();

                source.sendFeedback("Successfully enabled spawn protection");
            }
            case "disable" -> {
                SpawnManagerConfig.config.isSpawnProtected = false;
                SpawnManagerConfig.save();

                source.sendFeedback("Successfully disabled spawn protection");
            }
            case "radius" -> this.onProtectionRadiusUpdate(source, strings);
        }
    }

    private void onProtectionRadiusUpdate(SharedCommandSource source, String[] strings) {
        int radius;

        try {
            radius = Integer.parseInt(strings[3]);
        } catch (Exception e) {
            source.sendFeedback("Invalid syntax: /spawn protection radius [radius: int]");
            return;
        }

        SpawnManagerConfig.config.protectionRadius = radius;
        SpawnManagerConfig.save();

        source.sendFeedback("Successfully set protection radius to " + radius);
    }
}
