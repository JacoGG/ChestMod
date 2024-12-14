package com.jaco.cofreprueba;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.jaco.cofreprueba.commands.ChestLootReloadCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class CofrePruebaMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("ChestMod");
    private static Map<String, List<String>> biomeLootConfig;

    @Override
    public void onInitialize() {
        LOGGER.info("ChestMod se ha inicializado");
        loadLootConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ChestLootReloadCommand.register(dispatcher);
        });
    }

    public static void loadLootConfig() {
        try {
            InputStream inputStream = CofrePruebaMod.class.getClassLoader().getResourceAsStream("config/loot_config.json");
            if (inputStream == null) {
                LOGGER.error("El archivo loot_config.json no existe.");
                return;
            }

            InputStreamReader reader = new InputStreamReader(inputStream);
            Type lootConfigType = new TypeToken<Map<String, List<String>>>() {}.getType();
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            biomeLootConfig = new Gson().fromJson(json.getAsJsonObject("biomes"), lootConfigType);
            reader.close();
            LOGGER.info("Configuración de loot cargada correctamente.");
        } catch (Exception e) {
            LOGGER.error("Error al cargar loot_config.json", e);
        }
    }

    public static void onChestPlaced(ServerPlayerEntity player, World world, BlockPos pos) {
        GameMode gameMode = player.interactionManager.getGameMode();

        if (world.getServer() != null) {
            world.getServer().execute(() -> {
                ChestBlockEntity chest = (ChestBlockEntity) world.getBlockEntity(pos);
                if (chest != null) {
                    String gameModeTag = gameMode == GameMode.CREATIVE ? "creative" :
                            gameMode == GameMode.SURVIVAL ? "survival" : "other";

                    NbtCompound nbt = chest.toInitialChunkDataNbt();
                    nbt.putString("Mode", gameModeTag);
                    chest.readNbt(nbt);
                    chest.markDirty();

                    String modeTag = nbt.getString("Mode");

                    if ("creative".equals(modeTag)) {
                        LOGGER.info("Cofre colocado en creativo");

                        BlockPos playerPos = player.getBlockPos();
                        Biome biome = world.getBiome(playerPos).value();

                        Optional<Identifier> optionalId = world.getRegistryManager()
                                .get(RegistryKeys.BIOME)
                                .getKey(biome)
                                .map(RegistryKey::getValue);

                        if (optionalId.isPresent()) {
                            String biomeId = optionalId.get().toString();

                            if (biomeLootConfig != null && biomeLootConfig.containsKey(biomeId)) {
                                List<String> lootTables = biomeLootConfig.get(biomeId);
                                String lootTable = lootTables.get(new Random().nextInt(lootTables.size()));

                                if (lootTable.startsWith("minecraft:")) {
                                    lootTable = lootTable;
                                } else if (!lootTable.startsWith("cofreprueba:")) {
                                    lootTable = "cofreprueba:" + lootTable;
                                }

                                BlockEntity blockEntity = world.getBlockEntity(pos);
                                if (blockEntity instanceof LootableContainerBlockEntity container) {
                                    container.setLootTable(new Identifier(lootTable), new Random().nextLong());
                                    LOGGER.info("Asignada loot table [{}] al cofre en el bioma [{}].", lootTable, biomeId);
                                }
                            } else {
                                LOGGER.warn("No se encontró configuración de loot para el bioma: {}", biomeId);
                            }
                        } else {
                            LOGGER.warn("No se encontró identificador para el bioma.");
                        }
                    } else if ("survival".equals(modeTag)) {
                        LOGGER.info("Cofre colocado en survival");
                    } else {
                        LOGGER.info("Cofre colocado en otro modo ({})", modeTag);
                    }
                }
            });
        }
    }
}
