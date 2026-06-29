package com.glisco.numismaticoverhaul.block;

import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import org.jetbrains.annotations.Nullable;
import java.util.stream.Stream;

public class PiggyBankBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PiggyBankBlock> CODEC = BlockBehaviour.simpleCodec(p -> new PiggyBankBlock(ResourceKey.create(Registries.BLOCK, NumismaticOverhaul.id("piggy_bank"))));

    private static final VoxelShape NORTH_SHAPE = Stream.of(
            Block.box(7, 2, 4, 9, 4, 5),
            Block.box(5, 1, 5, 11, 6, 11),
            Block.box(5, 0, 5, 6, 1, 7),
            Block.box(5, 0, 9, 6, 1, 11),
            Block.box(10, 0, 9, 11, 1, 11),
            Block.box(10, 0, 5, 11, 1, 7)
    ).reduce(Shapes::or).get();

    private static final VoxelShape SOUTH_SHAPE = Stream.of(
            Block.box(7, 2, 11, 9, 4, 12),
            Block.box(5, 1, 5, 11, 6, 11),
            Block.box(10, 0, 9, 11, 1, 11),
            Block.box(10, 0, 5, 11, 1, 7),
            Block.box(5, 0, 5, 6, 1, 7),
            Block.box(5, 0, 9, 6, 1, 11)
    ).reduce(Shapes::or).get();

    private static final VoxelShape EAST_SHAPE = Stream.of(
            Block.box(11, 2, 7, 12, 4, 9),
            Block.box(5, 1, 5, 11, 6, 11),
            Block.box(9, 0, 5, 11, 1, 6),
            Block.box(5, 0, 5, 7, 1, 6),
            Block.box(5, 0, 10, 7, 1, 11),
            Block.box(9, 0, 10, 11, 1, 11)
    ).reduce(Shapes::or).get();

    private static final VoxelShape WEST_SHAPE = Stream.of(
            Block.box(4, 2, 7, 5, 4, 9),
            Block.box(5, 1, 5, 11, 6, 11),
            Block.box(5, 0, 10, 7, 1, 11),
            Block.box(9, 0, 10, 11, 1, 11),
            Block.box(9, 0, 5, 11, 1, 6),
            Block.box(5, 0, 5, 7, 1, 6)
    ).reduce(Shapes::or).get();

    public PiggyBankBlock(ResourceKey<Block> key) {
        super(BlockBehaviour.Properties.of().strength(1.25F, 4.2F).setId(key));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider factory) {
                player.openMenu(factory);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (entity instanceof FallingBlockEntity fallingBlock && fallingBlock.getBlockState().is(NumismaticOverhaul.VERY_HEAVY_BLOCKS) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof PiggyBankBlockEntity piggyBank) {
                Containers.dropContents(level, pos.relative(level.getBlockState(pos).getValue(FACING).getOpposite()), piggyBank.inventory());
            }
            level.removeBlock(pos, false);
            level.playSound(null, pos, NumismaticOverhaul.PIGGY_BANK_BREAK, SoundSource.BLOCKS);
            ((ServerLevel) level).sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, NumismaticOverhaulBlocks.PIGGY_BANK.defaultBlockState()),
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                6 * (int) Math.round(fallDistance), 0.75, 0.75, 0.75, 2.0
            );
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof PiggyBankBlockEntity piggyBank && player.isCreative() && !level.isClientSide() && !piggyBank.inventory().isEmpty()) {
            for (ItemStack invStack : piggyBank.inventory()) {
                if (!invStack.isEmpty()) {
                    ItemEntity entity = new ItemEntity(level, pos.getX() + .5d, pos.getY() + .5d, pos.getZ() + .5d, invStack.copy());
                    entity.setDefaultPickUpDelay();
                    level.addFreshEntity(entity);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PiggyBankBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
