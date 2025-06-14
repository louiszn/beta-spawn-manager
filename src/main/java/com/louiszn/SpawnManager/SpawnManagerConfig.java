package com.louiszn.SpawnManager;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class SpawnManagerConfig {
    public static final String CONFIG_PATH = "config/spawn-manager.yml";

    public static final YamlConfigurationLoader LOADER = YamlConfigurationLoader.builder()
            .path(Path.of(CONFIG_PATH))
            .nodeStyle(NodeStyle.BLOCK)
            .build();

    public static ModConfig config;

    public static void load() {
        CommentedConfigurationNode root;

        try {
            root = LOADER.load();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }

        if (root.empty()) {
            config = new ModConfig();
            save();
        } else {
            try {
                config = root.get(ModConfig.class, new ModConfig());
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void save() {
        try {
            CommentedConfigurationNode node = LOADER.createNode();
            node.set(ModConfig.class, config);
            LOADER.save(node);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    @ConfigSerializable
    public static class ModConfig {
        @Setting(value = "isSpawnProtected")
        public boolean isSpawnProtected = true;

        @Setting(value = "protectionRadius")
        public int protectionRadius = 16;

        @Setting(value = "areProtectedBlocksUsable")
        public boolean areProtectedBlocksUsable = false;
    }
}
