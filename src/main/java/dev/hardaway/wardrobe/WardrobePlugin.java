package dev.hardaway.wardrobe;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.lookup.Priority;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hardaway.wardrobe.api.cosmetic.appearance.Appearance;
import dev.hardaway.wardrobe.api.cosmetic.appearance.TextureConfig;
import dev.hardaway.wardrobe.api.property.WardrobeCamera;
import dev.hardaway.wardrobe.impl.command.WardrobeCommand;
import dev.hardaway.wardrobe.impl.cosmetic.*;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.ModelAppearance;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.ModelAssetModelAppearance;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.ModelAssetVariantAppearance;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.VariantAppearance;
import dev.hardaway.wardrobe.impl.cosmetic.texture.GradientTextureConfig;
import dev.hardaway.wardrobe.impl.cosmetic.texture.SkinTextureConfig;
import dev.hardaway.wardrobe.impl.cosmetic.texture.StaticTextureConfig;
import dev.hardaway.wardrobe.impl.cosmetic.texture.VariantTextureConfig;
import dev.hardaway.wardrobe.impl.menu.WardrobePage;
import dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent;
import dev.hardaway.wardrobe.impl.player.PlayerWardrobeSystems;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class WardrobePlugin extends JavaPlugin {

    private static WardrobePlugin instance;
    private ComponentType<EntityStore, PlayerWardrobeComponent> playerWardrobeComponentType;

    public WardrobePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static WardrobePlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        OpenCustomUIInteraction.registerCustomPageSupplier(this, WardrobePage.class, "Wardrobe", (ref, componentAccessor, playerRef, context) -> {
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
            PlayerWardrobeComponent wardrobe = commandBuffer.ensureAndGetComponent(ref, PlayerWardrobeComponent.getComponentType());
            BlockPosition blockPosition = context.getTargetBlock();
            ServerCameraSettings camera = WardrobeCamera.DEFAULT_CAMERA.toServerSettings(ref, componentAccessor);

            if (blockPosition != null) {
                World world = commandBuffer.getExternalData().getWorld();
                Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReference(ChunkUtil.chunkCoordinate(blockPosition.x), ChunkUtil.indexSection(blockPosition.y), ChunkUtil.chunkCoordinate(blockPosition.z));
                BlockSection blockSection = world.getChunkStore().getStore().getComponent(sectionRef, BlockSection.getComponentType());
                int blockIndex = ChunkUtil.indexBlock(blockPosition.x, blockPosition.y, blockPosition.z);
                int rotationIndex = blockSection.getRotationIndex(blockIndex) + 2;
                float rotation = (float) ((Math.PI / 2) * rotationIndex);

                Transform transform = commandBuffer.ensureAndGetComponent(ref, TransformComponent.getComponentType()).getTransform();
                world.execute(() -> {
                    transform.getRotation().setY((float) (rotation + Math.PI));
                    Teleport teleport = Teleport.createForPlayer(transform);
                    commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);
                });

                camera.eyeOffset = false;

                Position position = new Position(blockPosition.x, blockPosition.y, blockPosition.z);
                position.x += 0.5;
                position.y += 1.5;
                position.z += 0.5;

                camera.positionType = PositionType.Custom;
                camera.position = position;
                camera.rotation = new Direction(rotation, camera.rotation.pitch, camera.rotation.roll);
            }

            return new WardrobePage(playerRef, wardrobe, camera, true);
        });

        this.getCodecRegistry(TextureConfig.CODEC)
                .register(Priority.DEFAULT, "Static", StaticTextureConfig.class, StaticTextureConfig.CODEC)
                .register(Priority.NORMAL, "Gradient", GradientTextureConfig.class, GradientTextureConfig.CODEC)
                .register(Priority.NORMAL, "Variant", VariantTextureConfig.class, VariantTextureConfig.CODEC)
                .register(Priority.NORMAL, "Skin", SkinTextureConfig.class, SkinTextureConfig.CODEC);

        this.getCodecRegistry(CosmeticAsset.CODEC)
                .register(Priority.DEFAULT, "ModelAttachment", ModelAttachmentCosmetic.class, ModelAttachmentCosmetic.CODEC)
                .register(Priority.NORMAL, "PlayerModel", PlayerModelCosmetic.class, PlayerModelCosmetic.CODEC);

        this.getCodecRegistry(Appearance.MODELASSET_CODEC)
                .register(Priority.DEFAULT, "Model", ModelAssetModelAppearance.class, ModelAssetModelAppearance.CODEC)
                .register(Priority.NORMAL, "Variant", ModelAssetVariantAppearance.class, ModelAssetVariantAppearance.CODEC);

        this.getCodecRegistry(Appearance.CODEC)
                .register(Priority.DEFAULT, "Model", ModelAppearance.class, ModelAppearance.CODEC)
                .register(Priority.NORMAL, "Variant", VariantAppearance.class, VariantAppearance.CODEC);

        AssetRegistry.register(HytaleAssetStore.builder(CosmeticCategoryAsset.class, new DefaultAssetMap<>())
                .setPath("Wardrobe/Categories")
                .setCodec(CosmeticCategoryAsset.CODEC)
                .setKeyFunction(CosmeticCategoryAsset::getId)
                .build()
        );
        AssetRegistry.register(HytaleAssetStore.builder(CosmeticSlotAsset.class, new DefaultAssetMap<>())
                .setPath("Wardrobe/Slots")
                .setCodec(CosmeticSlotAsset.CODEC)
                .setKeyFunction(CosmeticSlotAsset::getId)
                .loadsAfter(CosmeticCategoryAsset.class)
                .build()
        );
        AssetRegistry.register(HytaleAssetStore.builder(CosmeticAsset.class, new DefaultAssetMap<>())
                .setPath("Wardrobe/Cosmetics")
                .setCodec(CosmeticAsset.CODEC)
                .setKeyFunction(CosmeticAsset::getId)
                .loadsAfter(ModelAsset.class, CosmeticSlotAsset.class)
                .build()
        );


        this.playerWardrobeComponentType = this.getEntityStoreRegistry().registerComponent(
                PlayerWardrobeComponent.class,
                "PlayerWardrobe",
                PlayerWardrobeComponent.CODEC
        );

        // TODO: groups
        PlayerWardrobeSystems.registerSystems(this);
        WardrobeEvents.registerEvents(this);

        this.getCommandRegistry().registerCommand(new WardrobeCommand());
    }

    public ComponentType<EntityStore, PlayerWardrobeComponent> getPlayerWardrobeComponentType() {
        return playerWardrobeComponentType;
    }

    public static <T extends JsonAssetWithMap<String, DefaultAssetMap<String, T>>> Supplier<AssetStore<String, T, DefaultAssetMap<String, T>>> createAssetStore(Class<T> clazz) {
        return new Supplier<>() {
            AssetStore<String, T, DefaultAssetMap<String, T>> value;

            @Override
            public AssetStore<String, T, DefaultAssetMap<String, T>> get() {
                if (value == null)
                    value = AssetRegistry.getAssetStore(clazz);
                return value;
            }
        };
    }
}