package io.github.foundationgames.sandwichable;

import io.github.foundationgames.sandwichable.blocks.BlocksRegistry;
import io.github.foundationgames.sandwichable.blocks.ShrubBlock;
import io.github.foundationgames.sandwichable.blocks.entity.container.BottleCrateScreenHandler;
import io.github.foundationgames.sandwichable.blocks.entity.container.DesalinatorScreenHandler;
import io.github.foundationgames.sandwichable.blocks.entity.container.screen.BottleCrateScreen;
import io.github.foundationgames.sandwichable.blocks.entity.container.screen.DesalinatorScreen;
import io.github.foundationgames.sandwichable.blocks.entity.renderer.*;
import io.github.foundationgames.sandwichable.entity.EntitiesRegistry;
import io.github.foundationgames.sandwichable.entity.SandwichTableMinecartEntity;
import io.github.foundationgames.sandwichable.entity.render.SandwichTableMinecartEntityRenderer;
import io.github.foundationgames.sandwichable.fluids.FluidsRegistry;
import io.github.foundationgames.sandwichable.particle.Particles;
import io.github.foundationgames.sandwichable.items.ItemsRegistry;
import io.github.foundationgames.sandwichable.util.RenderFlags;
import io.github.foundationgames.sandwichable.util.SpreadRegistry;
import io.github.foundationgames.sandwichable.util.Util;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.Function;

public class SandwichableClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.SANDWICHTABLE_BLOCKENTITY, SandwichTableBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.SANDWICH_BLOCKENTITY, SandwichBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.CUTTINGBOARD_BLOCKENTITY, CuttingBoardBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.TOASTER_BLOCKENTITY, ToasterBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.BASIN_BLOCKENTITY, BasinBlockEntityRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(BlocksRegistry.PICKLEJAR_BLOCKENTITY, PickleJarBlockEntityRenderer::new);

        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> !state.get(ShrubBlock.SNIPPED) ? BiomeColors.getGrassColor(view, pos) : FoliageColors.getDefaultColor(), BlocksRegistry.SHRUB, BlocksRegistry.POTTED_SHRUB);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex > 0 ? -1 : GrassColors.getColor(0.5D, 1.0D), BlocksRegistry.SHRUB.asItem());

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if(stack.getOrCreateTag().getString("spreadType") != null) {
                if(SpreadRegistry.INSTANCE.fromString(stack.getOrCreateTag().getString("spreadType")) != null) {
                    return SpreadRegistry.INSTANCE.fromString(stack.getOrCreateTag().getString("spreadType")).getColor(stack);
                }
            }
            return 0xFFFFFF;
        },
        ItemsRegistry.SPREAD);

        ScreenRegistry.<DesalinatorScreenHandler, DesalinatorScreen>register(Sandwichable.DESALINATOR_HANDLER, DesalinatorScreen::new);
        ScreenRegistry.<BottleCrateScreenHandler, BottleCrateScreen>register(Sandwichable.BOTTLE_CRATE_HANDLER, BottleCrateScreen::new);

        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.SHRUB, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.POTTED_SHRUB, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.LETTUCE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.TOMATOES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.CUCUMBERS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.ONIONS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(BlocksRegistry.PICKLE_JAR, RenderLayer.getCutout());

        EntityRendererRegistry.INSTANCE.register(EntitiesRegistry.SANDWICH_TABLE_MINECART, (dispatcher, context) -> new SandwichTableMinecartEntityRenderer(dispatcher));

        ClientPlayNetworking.registerGlobalReceiver(Util.id("sync_sandwich_table_cart"), (client, handler, buf, responseSender) -> {
            Entity e = client.player.getEntityWorld().getEntityById(buf.readInt());
            NbtCompound tag = buf.readNbt();
            client.execute(() -> {
                if(e instanceof SandwichTableMinecartEntity) {
                    ((SandwichTableMinecartEntity)e).readSandwichTableData(tag);
                }
            });
        });

        ClientSidePacketRegistry.INSTANCE.register(Util.id("cutting_board_particles"), (ctx, buf) -> {
            ItemStack stack = buf.readItemStack();
            int top = buf.readInt();
            int layers = buf.readInt();
            BlockPos pos = buf.readBlockPos();
            Random random = new Random();
            World world = MinecraftClient.getInstance().world;
            ctx.getTaskQueue().execute(() -> {
                for (int i = 0; i < layers; i++) {
                    for (int j = 0; j < 2 + random.nextInt(2); j++) {
                        double x = pos.getX() + 0.5 + ((random.nextDouble() - 0.5) / 3);
                        double y = pos.getY() + 0.094 + ((top - i) * 0.03124);
                        double z = pos.getZ() + 0.5 + ((random.nextDouble() - 0.5) / 3);
                        world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, stack), x, y, z, 0, (random.nextDouble() + 1.0) * 0.066, 0);
                    }
                }
            });
        });

        FabricModelPredicateProviderRegistry.register(ItemsRegistry.SPREAD, Util.id("loaf_shape"), (stack, world, entity) -> {
            if(stack.getOrCreateTag().contains("onLoaf")) return stack.getOrCreateTag().getBoolean("onLoaf") ? 1 : 0;
            return 0;
        });

        FabricModelPredicateProviderRegistry.register(Util.id("sandwich_state"), (stack, world, entity) -> {
            if(entity == null) return RenderFlags.RENDERING_SANDWICH_ITEM * 0.25f;
            return 0;
        });

        setupPickleBrine();

        Particles.init();
    }

    private static void setupPickleBrine() {
        Fluid still = FluidsRegistry.PICKLE_BRINE;
        Fluid flowing = FluidsRegistry.PICKLE_BRINE_FLOWING;
        Identifier fluidTexture = Util.id("pickle_brine");
        Identifier stillId = new Identifier(fluidTexture.getNamespace(), "block/" + fluidTexture.getPath());
        Identifier flowingId = new Identifier(fluidTexture.getNamespace(), "block/" + fluidTexture.getPath() + "_flow");
        ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).register((atlasTexture, registry) -> {
            registry.register(stillId);
            registry.register(flowingId);
        });
        Identifier fluidId = Registry.FLUID.getId(still);
        Identifier listenerId = new Identifier(fluidId.getNamespace(), fluidId.getPath() + "_reload_listener");

        Sprite[] sprites = { null, null };

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public void reload(ResourceManager manager) {
                final Function<Identifier, Sprite> atlas = MinecraftClient.getInstance().getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                sprites[0] = atlas.apply(stillId);
                sprites[1] = atlas.apply(flowingId);
            }

            @Override
            public Identifier getFabricId() {
                return listenerId;
            }
        });

        FluidRenderHandler renderHandler = new FluidRenderHandler() {
            @Override
            public Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state) {
                return sprites;
            }

            @Override
            public int getFluidColor(BlockRenderView view, BlockPos pos, FluidState state) {
                return 0x65ff6e;
            }
        };

        FluidRenderHandlerRegistry.INSTANCE.register(still, renderHandler);
        FluidRenderHandlerRegistry.INSTANCE.register(flowing, renderHandler);

        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), FluidsRegistry.PICKLE_BRINE, FluidsRegistry.PICKLE_BRINE_FLOWING);
    }
}
