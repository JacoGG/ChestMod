package com.jaco.cofreprueba.commands;

import com.jaco.cofreprueba.CofrePruebaMod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ChestLootReloadCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("chestloot")
                        .then(CommandManager.literal("reload")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(context -> reloadLootTables(context.getSource()))
                        )
        );
    }

    private static int reloadLootTables(ServerCommandSource source) {
        try {
            CofrePruebaMod.loadLootConfig();
            source.sendFeedback(Text.literal("Configuración de loot recargada correctamente."), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Error al recargar la configuración de loot. Revisa la consola para más detalles."));
            return 0;
        }
    }
}