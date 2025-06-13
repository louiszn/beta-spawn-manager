package com.louiszn.SpawnManager;

import com.louiszn.SpawnManager.command.SpawnCommand;
import com.matthewperiut.retrocommands.api.CommandRegistry;

public class CommandManager {
    public static void load() {
        CommandRegistry.add(new SpawnCommand());
    }
}
