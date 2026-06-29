package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import java.util.Set;

public class NumismaticOverhaulBlocks {

    public static Block SHOP;
    public static Block INEXHAUSTIBLE_SHOP;
    public static Block PIGGY_BANK;

    public static void register() {
        SHOP = new ShopBlock(ResourceKey.create(Registries.BLOCK, NumismaticOverhaul.id("shop")), false);
        INEXHAUSTIBLE_SHOP = new ShopBlock(ResourceKey.create(Registries.BLOCK, NumismaticOverhaul.id("inexhaustible_shop")), true);
        PIGGY_BANK = new PiggyBankBlock(ResourceKey.create(Registries.BLOCK, NumismaticOverhaul.id("piggy_bank")));

        Registry.register(BuiltInRegistries.BLOCK, NumismaticOverhaul.id("shop"), SHOP);
        Registry.register(BuiltInRegistries.BLOCK, NumismaticOverhaul.id("inexhaustible_shop"), INEXHAUSTIBLE_SHOP);
        Registry.register(BuiltInRegistries.BLOCK, NumismaticOverhaul.id("piggy_bank"), PIGGY_BANK);

        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("shop"), new BlockItem(SHOP, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("shop")))));
        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("inexhaustible_shop"), new BlockItem(INEXHAUSTIBLE_SHOP, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("inexhaustible_shop")))));
        Registry.register(BuiltInRegistries.ITEM, NumismaticOverhaul.id("piggy_bank"), new BlockItem(PIGGY_BANK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, NumismaticOverhaul.id("piggy_bank")))));

        Entities.register();
    }


    public static final class Entities {

        public static BlockEntityType<ShopBlockEntity> SHOP;
        public static BlockEntityType<PiggyBankBlockEntity> PIGGY_BANK;

        public static void register() {
            SHOP = new BlockEntityType<>(ShopBlockEntity::new, Set.of(
                NumismaticOverhaulBlocks.SHOP,
                NumismaticOverhaulBlocks.INEXHAUSTIBLE_SHOP));
            PIGGY_BANK = new BlockEntityType<>(PiggyBankBlockEntity::new, Set.of(
                NumismaticOverhaulBlocks.PIGGY_BANK));

            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NumismaticOverhaul.id("shop"), SHOP);
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NumismaticOverhaul.id("piggy_bank"), PIGGY_BANK);
        }
    }
}
