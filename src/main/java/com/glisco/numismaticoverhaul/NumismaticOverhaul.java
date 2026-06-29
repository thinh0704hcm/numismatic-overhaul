package com.glisco.numismaticoverhaul;

import com.glisco.numismaticoverhaul.block.*;
import com.glisco.numismaticoverhaul.currency.MoneyBagLootEntry;
import com.glisco.numismaticoverhaul.item.*;
import com.glisco.numismaticoverhaul.network.*;
import com.glisco.numismaticoverhaul.villagers.data.VillagerTradesResourceListener;
import com.glisco.numismaticoverhaul.villagers.json.VillagerTradesHandler;



import net.minecraft.world.item.CreativeModeTab;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;


import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;


import net.minecraft.core.registries.*;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRule;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.Map;

public class NumismaticOverhaul implements ModInitializer {

    public static final String MOD_ID = "numismatic-overhaul";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    public static final MenuType<ShopScreenHandler> SHOP_SCREEN_HANDLER_TYPE = new MenuType<>(ShopScreenHandler::new, FeatureFlagSet.of());
    public static final MenuType<PiggyBankScreenHandler> PIGGY_BANK_SCREEN_HANDLER_TYPE = new MenuType<>(PiggyBankScreenHandler::new, FeatureFlagSet.of());

    public static final SoundEvent PIGGY_BANK_BREAK = SoundEvent.createVariableRangeEvent(id("piggy_bank_break"));
    // MONEY_BAG_ENTRY registration removed (loot type unrolling in 26.2)

    public static final TagKey<EntityType<?>> THE_BOURGEOISIE = TagKey.create(Registries.ENTITY_TYPE, id("the_bourgeoisie"));
    public static final TagKey<Block> VERY_HEAVY_BLOCKS = TagKey.create(Registries.BLOCK, id("very_heavy_blocks"));

    public static final GameRule<Integer> MONEY_DROP_PERCENTAGE
        = GameRuleBuilder.forInteger(10).category(GameRuleCategory.PLAYER).range(0, 100).buildAndRegister(id("money_drop_percentage"));

    public static final GameRule<Integer> MONEY_MOB_DROP_VARIANCE
        = GameRuleBuilder.forInteger(50).category(GameRuleCategory.MOBS).range(0, 100).buildAndRegister(id("money_mob_drop_variance_percentage"));

    public static final DataComponentType<MoneyBagComponent> MONEY_BAG_COMPONENT = MoneyBagComponent.register();

    public static final CreativeModeTab NUMISMATIC_GROUP = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 6)
        .icon(() -> new ItemStack(NumismaticOverhaulItems.GOLD_COIN))
        .title(Component.translatable("itemGroup.numismatic-overhaul.general"))
        .displayItems((params, output) -> {
            output.accept(NumismaticOverhaulItems.BRONZE_COIN);
            output.accept(NumismaticOverhaulItems.SILVER_COIN);
            output.accept(NumismaticOverhaulItems.GOLD_COIN);
            output.accept(NumismaticOverhaulItems.MONEY_BAG);
        })
        .build();

    public static final Map<EntityType<?>, Integer> MOBS_IN_BOURGEOISIE = new HashMap<>();

    public static final com.glisco.numismaticoverhaul.NumismaticOverhaulConfig CONFIG = com.glisco.numismaticoverhaul.NumismaticOverhaulConfig.createAndLoad();

    @Override
    public void onInitialize() {
        // this type of code truly feels like DH code
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ModComponents.init(server.getWorldPath(LevelResource.ROOT)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ModComponents.saveAll());
        ServerLifecycleEvents.SERVER_STARTED.register(NumismaticOverhaul::loadMobDropConfig);

        // Register items and blocks manually
        NumismaticOverhaulItems.register();
        NumismaticOverhaulBlocks.register();

        Registry.register(BuiltInRegistries.SOUND_EVENT, PIGGY_BANK_BREAK.location(), PIGGY_BANK_BREAK);
        Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, id("money_bag"), MoneyBagLootEntry.CODEC); // Fixed registration for MC 26.2

        Registry.register(BuiltInRegistries.MENU, id("shop"), SHOP_SCREEN_HANDLER_TYPE);
        Registry.register(BuiltInRegistries.MENU, id("piggy_bank"), PIGGY_BANK_SCREEN_HANDLER_TYPE);

        ServerPlayNetworking.registerGlobalReceiver(RequestPurseActionC2SPacket.ID, RequestPurseActionC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(ShopScreenHandlerRequestC2SPacket.ID, ShopScreenHandlerRequestC2SPacket::handle);


        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(NumismaticOverhaul.id("villager_trades"), new VillagerTradesResourceListener());
        VillagerTradesHandler.registerDefaultAdapters();

        CommandRegistrationCallback.EVENT.register(NumismaticCommand::register);

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            VillagerTradesHandler.broadcastErrors(server);
            CONFIG.subscribeToMobsToBaseValues(NumismaticOverhaul::reloadMobDropConfig);
        });

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), NUMISMATIC_GROUP);

        if (CONFIG.generateCurrencyInChests()) {
            LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
                if (anyMatch(key.identifier(),
                    BuiltInLootTables.STRONGHOLD_LIBRARY.identifier(),
                    BuiltInLootTables.BASTION_TREASURE.identifier(),
                    BuiltInLootTables.STRONGHOLD_CORRIDOR.identifier(),
                    BuiltInLootTables.PILLAGER_OUTPOST.identifier(),
                    BuiltInLootTables.BURIED_TREASURE.identifier(),
                    BuiltInLootTables.SIMPLE_DUNGEON.identifier(),
                    BuiltInLootTables.ABANDONED_MINESHAFT.identifier())) {
                     tableBuilder.pool(LootPool.lootPool()
                         .add(LootItem.lootTableItem(NumismaticOverhaulItems.GOLD_COIN)
                             .when(LootItemRandomChanceCondition.randomChance(0.01f)))
                         .build());
                }
            });

            LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
                if (anyMatch(key.identifier(), BuiltInLootTables.DESERT_PYRAMID.identifier())) {
                    tableBuilder.pool(LootPool.lootPool().add(MoneyBagLootEntry.builder(CONFIG.lootOptions().desertMinLoot(), CONFIG.lootOptions().desertMaxLoot()))
                        .when(LootItemRandomChanceCondition.randomChance(0.45f)).build());
                } else if (anyMatch(key.identifier(), BuiltInLootTables.SIMPLE_DUNGEON.identifier(), BuiltInLootTables.ABANDONED_MINESHAFT.identifier())) {
                    tableBuilder.pool(LootPool.lootPool().add(MoneyBagLootEntry.builder(CONFIG.lootOptions().dungeonMinLoot(), CONFIG.lootOptions().dungeonMaxLoot()))
                        .when(LootItemRandomChanceCondition.randomChance(0.75f)).build());
                } else if (anyMatch(key.identifier(), BuiltInLootTables.BASTION_TREASURE.identifier(), BuiltInLootTables.STRONGHOLD_CORRIDOR.identifier(), BuiltInLootTables.PILLAGER_OUTPOST.identifier(), BuiltInLootTables.BURIED_TREASURE.identifier())) {
                    tableBuilder.pool(LootPool.lootPool().add(MoneyBagLootEntry.builder(CONFIG.lootOptions().structureMinLoot(), CONFIG.lootOptions().structureMaxLoot()))
                        .when(LootItemRandomChanceCondition.randomChance(0.75f)).build());
                } else if (anyMatch(key.identifier(), BuiltInLootTables.STRONGHOLD_LIBRARY.identifier())) {
                    tableBuilder.pool(LootPool.lootPool().add(MoneyBagLootEntry.builder(CONFIG.lootOptions().strongholdLibraryMinLoot(), CONFIG.lootOptions().strongholdLibraryMaxLoot()))
                        .when(LootItemRandomChanceCondition.randomChance(0.85f)).build());
                }
            });
        }
    }

    private static boolean anyMatch(Identifier target, Identifier... comparisons) {
        for (Identifier comparison : comparisons) {
            if (target.equals(comparison)) return true;
        }
        return false;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void loadMobDropConfig(MinecraftServer ignored) {
        CONFIG.mobsToBaseValues().forEach((s, baseValue) -> {
            if (s.startsWith("#")) {
                for (var holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(TagKey.create(Registries.ENTITY_TYPE, Identifier.parse(s.split("#")[1])))) {
                    MOBS_IN_BOURGEOISIE.put(holder.value(), baseValue);
                }
            } else {
                var entityOpt = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(s));
                entityOpt.ifPresentOrElse(ref -> MOBS_IN_BOURGEOISIE.put(ref.value(), baseValue), () -> {
                    LOGGER.error("[Numismatic Overhaul] Could not find entity type '{}' when applying mob drops", s);
                });
            }
        });
    }

    private static void reloadMobDropConfig(Map<String, Integer> ignored) {
        MOBS_IN_BOURGEOISIE.clear();
        // CONFIG.load(); // removed for MC 26.2
        loadMobDropConfig(null);
    }
}
