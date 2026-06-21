package com.amicbeam.recipelinkage.block;

import com.amicbeam.recipelinkage.block.entity.ResearchTableBlockEntity;
import com.amicbeam.recipelinkage.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class ResearchTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = makeShape();

    public ResearchTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ResearchTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            table.ensureSampleGraph(serverPlayer);
            NetworkHooks.openScreen(serverPlayer, table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ResearchTableBlockEntity table) {
                table.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
            table.setChanged();
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, box(0.0D, 11.0D, 0.0D, 16.0D, 14.0D, 16.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.0D, 14.0D, 1.0D, 15.0D, 15.0D, 15.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.0D, 0.0D, 1.0D, 3.0D, 11.0D, 3.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(13.0D, 0.0D, 1.0D, 15.0D, 11.0D, 3.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.0D, 0.0D, 13.0D, 3.0D, 11.0D, 15.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(13.0D, 0.0D, 13.0D, 15.0D, 11.0D, 15.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(3.0D, 8.0D, 0.0D, 13.0D, 11.0D, 2.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(3.0D, 8.0D, 14.0D, 13.0D, 11.0D, 16.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(0.0D, 8.0D, 3.0D, 2.0D, 11.0D, 13.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(14.0D, 8.0D, 3.0D, 16.0D, 11.0D, 13.0D), BooleanOp.OR);
        shape = Shapes.join(shape, box(2.0D, 15.0D, 2.0D, 14.0D, 16.0D, 14.0D), BooleanOp.OR);
        return shape.optimize();
    }
}
