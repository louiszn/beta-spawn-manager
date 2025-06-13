package com.louiszn.SpawnManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnManagerMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("SpawnManager");

    @Override
    public void onInitialize() {
        LOGGER.info("Loading mod");

        SpawnManagerConfig.load();

        if (FabricLoader.getInstance().isModLoaded("retrocommands")){
            CommandManager.load();
        }

        LOGGER.info("Loaded successfully");
    }
}
