package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.network.UpdateShopScreenS2CPacket;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class ShopBlock extends BaseEntityBlock {
    private static final VoxelShape MAIN_PILLAR = Block.box(1, 0, 1, 14, 8, 14);
    private static final VoxelShape PLATE = Block.box(0, 8, 0, 16, 12, 16);
    private static final VoxelShape PILLAR_1 = Block.box(13, 0, 0, 16, 8, 3);
    private static final VoxelShape PILLAR_2 = Block.box(0, 0, 0, 3, 8, 3);
    private static final VoxelShape PILLAR_3 = Block.box(0, 0, 13, 3, 8, 16);
    private static final VoxelShape PILLAR_4 = Block.box(13, 0, 13, 16, 8, 16);
    private static final VoxelShape SHAPE = Shapes.or(MAIN_PILLAR, PLATE, PILLAR_1, PILLAR_2, PILLAR_3, PILLAR_4);

    private final boolean inexhaustible;

    // Constructor without explicit ID – will be set via setId in registration
    public ShopBlock(ResourceKey<Block> key) {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(5.0f).setId(key));
        this.inexhaustible = false;
    }

    public ShopBlock(ResourceKey<Block> key, boolean inexhaustible) {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(5.0f).setId(key));
        this.inexhaustible = inexhaustible;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Simplified codec – uses a generic key; actual registration supplies proper keys
        return BlockBehaviour.simpleCodec(p -> new ShopBlock(ResourceKey.create(Registries.BLOCK, NumismaticOverhaul.id("shop"))));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)) return InteractionResult.FAIL;
        if (shop.busy) return InteractionResult.SUCCESS;
        if (shop.getOwner().equals(player.getUUID()) && !player.isShiftKeyDown()) {
            player.openMenu(state.getMenuProvider(level, pos));
            ServerPlayNetworking.send((ServerPlayer) player, new UpdateShopScreenS2CPacket(shop, ItemStack.EMPTY));
            shop.busy = true;
            return InteractionResult.SUCCESS;
        }
        var merchant = shop.getMerchant();
        merchant.updateTrades();
        merchant.setTradingPlayer(player);
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (level.isClientSide()) return;
        if (!(placer instanceof ServerPlayer)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (itemStack.has(DataComponents.CUSTOM_NAME) && level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
            shop.setComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, itemStack.getHoverName()).build());
        }
        ((ShopBlockEntity) level.getBlockEntity(pos)).setOwner(placer.getUUID());
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        BlockState newState = level.getBlockState(pos);
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
                shop.getMerchant().setTradingPlayer(null);
                CurrencyConverter.getAsValidStacks(shop.getStoredCurrency())
                    .forEach(stack -> Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                Containers.dropContents(level, pos, shop);
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    public boolean inexhaustible() {
        return this.inexhaustible;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, NumismaticOverhaulBlocks.Entities.SHOP, ShopBlockEntity::tick);
    }
}
