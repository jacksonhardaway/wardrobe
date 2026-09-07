package dev.hardaway.wardrobe.impl.player;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkin;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSettings;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hardaway.wardrobe.WardrobeUtil;
import dev.hardaway.wardrobe.api.cosmetic.Cosmetic;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;
import dev.hardaway.wardrobe.api.player.PlayerWardrobe;
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset;
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticSlotAsset;
import dev.hardaway.wardrobe.impl.cosmetic.ModelAttachmentCosmetic;
import dev.hardaway.wardrobe.impl.cosmetic.builtin.HytaleCosmetic;
import dev.hardaway.wardrobe.impl.cosmetic.builtin.HytalePlayerCosmetic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PlayerWardrobeSystems {
    public static void registerSystems(JavaPlugin plugin) {
        plugin.getEntityStoreRegistry().registerSystem(new Tick());
        plugin.getEntityStoreRegistry().registerSystem(new EntityAdded());
        plugin.getEntityStoreRegistry().registerSystem(new WardrobeChanged());
        plugin.getEntityStoreRegistry().registerSystem(new ArmorVisibilityChanged());
        plugin.getEntityStoreRegistry().registerSystem(new ArmorInventoryChanged());
    }

    public static class Tick extends EntityTickingSystem<EntityStore> {

        @Override
        public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            PlayerWardrobeComponent wardrobe = chunk.getComponent(i, PlayerWardrobeComponent.getComponentType());
            if (wardrobe == null || !wardrobe.consumeDirty()) // TODO: use events instead of ticking
                return;

            PlayerSkinComponent playerSkinComponent = chunk.getComponent(i, PlayerSkinComponent.getComponentType());
            if (wardrobe.getCosmetics().isEmpty() && wardrobe.getHiddenCosmeticTypes().isEmpty()) {
                if (playerSkinComponent != null) {
                    playerSkinComponent.setNetworkOutdated();
                    Model newModel = CosmeticsModule.get().createModel(playerSkinComponent.getPlayerSkin());
                    if (newModel != null) {
                        chunk.setComponent(i, ModelComponent.getComponentType(), new ModelComponent(newModel));
                    }
                }
                return;
            }

            Model model = buildWardrobeModel(
                    chunk.getComponent(i, Player.getComponentType()),
                    chunk.getComponent(i, PlayerSettings.getComponentType()),
                    playerSkinComponent.getPlayerSkin(),
                    wardrobe,
                    chunk.getComponent(i, PlayerRef.getComponentType())
            );
            chunk.setComponent(i, ModelComponent.getComponentType(), new ModelComponent(model));
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Query.and(PlayerWardrobeComponent.getComponentType(), PlayerSettings.getComponentType(), PlayerSkinComponent.getComponentType());
        }

        public static Model buildWardrobeModel(Player player, PlayerSettings playerSettings, com.hypixel.hytale.protocol.PlayerSkin skin, PlayerWardrobe wardrobeComponent, PlayerRef playerRef) {
            // Build context
            Model playerModel = CosmeticsModule.get().createModel(skin);
            PlayerWardrobeContext context = new PlayerWardrobeContext(
                    player,
                    playerSettings,
                    wardrobeComponent,
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashSet<>(),
                    playerModel
            );

            // Populate the applied cosmetics map
            PlayerSkin cosmeticSkin = WardrobeUtil.skinFromProtocol(skin);
            Map<String, ? extends WardrobeCosmeticSlot> slots = CosmeticSlotAsset.getAssetMap().getAssetMap();
            for (WardrobeCosmeticSlot group : slots.values()) {
                PlayerCosmetic playerCosmetic = wardrobeComponent.getCosmetic(group);
                if (playerCosmetic != null) {
                    dev.hardaway.wardrobe.api.cosmetic.Cosmetic cosmetic = CosmeticAsset.getAssetMap().getAsset(playerCosmetic.getCosmeticId()); // TODO: replace with registry
                    if (cosmetic != null) {
                        context.getCosmeticMap().put(group.getId(), cosmetic);
                    }
                } else if (group.getHytaleCosmeticType() != null && !context.getHiddenTypes().contains(group.getHytaleCosmeticType())) {
                    HytaleCosmetic hytaleCosmetic = WardrobeUtil.createHytaleCosmetic(group.getHytaleCosmeticType(), cosmeticSkin);
                    if (hytaleCosmetic != null && !context.getHiddenTypes().contains(hytaleCosmetic.getType())) {
                        context.getCosmeticMap().put(group.getId(), hytaleCosmetic);
                    }
                }
            }

            Map<String, ModelAttachmentCosmetic> applyAfter = new HashMap<>();

            // Apply custom cosmetics
            context.getCosmeticMap().forEach((slotId, cosmetic) -> {
                WardrobeCosmeticSlot slot = slots.get(slotId); // TODO: replace with registry
                if (slot != null) {
                    // TODO: validate & warn
                    PlayerCosmetic cosmeticData = wardrobeComponent.getCosmetic(slotId);
                    if (cosmeticData == null && cosmetic instanceof HytaleCosmetic) {
                        PlayerSkin.PlayerSkinPartId part = WardrobeUtil.getSkinPartForType(slot.getHytaleCosmeticType(), cosmeticSkin);
                        if (part == null) return;

                        cosmeticData = new HytalePlayerCosmetic(part);
                    }
                    if (cosmetic instanceof ModelAttachmentCosmetic modelAttachmentCosmetic) {
                        if (modelAttachmentCosmetic.getAppearance().getTextureConfig(null).getGradientFrom() != null) {
                            applyAfter.put(slotId, modelAttachmentCosmetic);
                            return;
                        }
                    }
                    cosmetic.applyCosmetic(context, slot, cosmeticData, null, null);
                }
            });

            for (String slotId : applyAfter.keySet()) {
                ModelAttachmentCosmetic cosmetic = applyAfter.get(slotId);
                String gradientFrom = cosmetic.getAppearance().getTextureConfig(null).getGradientFrom();
                Cosmetic gradientFromCosmetic = context.getCosmeticMap().get(gradientFrom);
                PlayerCosmetic gradientFromPlayerCosmetic = context.getCosmetic(gradientFrom);
                if (gradientFromCosmetic == null || gradientFromPlayerCosmetic == null) continue;
                WardrobeCosmeticSlot slot = slots.get(slotId); // TODO: replace with registry
                if (slot != null) {
                    // TODO: validate & warn
                    String gradientId = gradientFromPlayerCosmetic.getVariantId();
                    PlayerCosmetic cosmeticData = wardrobeComponent.getCosmetic(slotId);
                    cosmetic.applyCosmetic(context, slot, cosmeticData, null, gradientId);
                }
            }

            // Handle armor hiding
            Set<CosmeticType> hiddenHytaleSlots = new HashSet<>();
            ItemContainer armorContainer = player.getInventory().getArmor(); // TODO
            if (armorContainer != null) {
                for (short armorIndex = 0; armorIndex < armorContainer.getCapacity(); armorIndex++) {
                    ItemStack stack = armorContainer.getItemStack(armorIndex);
                    if (stack == null || stack.isEmpty()) continue;

                    ItemArmor armor = stack.getItem().getArmor();
                    if (armor == null)
                        break;

                    ItemArmorSlot slot = armor.getArmorSlot();
                    boolean shouldHide = switch (slot) {
                        case Head -> playerSettings.hideHelmet();
                        case Chest -> playerSettings.hideCuirass();
                        case Hands -> playerSettings.hideGauntlets();
                        case Legs -> playerSettings.hidePants();
                    };

                    if (shouldHide)
                        continue;

                    com.hypixel.hytale.protocol.ItemArmor protocolArmor = armor.toPacket();
                    if (protocolArmor.cosmeticsToHide == null)
                        break;

                    for (com.hypixel.hytale.protocol.Cosmetic cosmetic : protocolArmor.cosmeticsToHide) {
                        CosmeticType type = WardrobeUtil.protocolCosmeticToCosmeticType(cosmetic);
                        if (type == null)
                            continue;

                        hiddenHytaleSlots.add(type);
                    }
                }
            }

            // Build the attachment set
            Set<ModelAttachment> modelAttachments = new HashSet<>();
            Map<String, ModelAttachment> attachmentsMap = context.getModelAttachments();
            for (Map.Entry<String, ModelAttachment> entry : attachmentsMap.entrySet()) {
                String slotId = entry.getKey();
                if (entry.getValue() == null || context.getHiddenSlots().contains(slotId)) {
                    continue;
                }
                WardrobeCosmeticSlot slot = slots.get(slotId);
                if (slot != null && slot.getHytaleCosmeticType() != null && hiddenHytaleSlots.contains(slot.getHytaleCosmeticType())) {
                    continue;
                }

                modelAttachments.add(entry.getValue());
            }

            Model contextModel = context.getPlayerModel();
            return new Model(
                    "Wardrobe_" + playerRef.getUuid() + "_" + contextModel.getModelAssetId(),
                    contextModel.getScale(),
                    contextModel.getRandomAttachmentIds(),
                    modelAttachments.toArray(ModelAttachment[]::new), // Skin attachments
                    contextModel.getBoundingBox(),
                    contextModel.getModel(), // Model
                    contextModel.getTexture(), // Skin texture
                    contextModel.getGradientSet(), // Skin gradient set
                    contextModel.getGradientId(), // Skin gradient id
                    contextModel.getEyeHeight(),
                    contextModel.getCrouchOffset(),
                    contextModel.getSittingOffset(),
                    contextModel.getSleepingOffset(),
                    contextModel.getAnimationSetMap(),
                    contextModel.getCamera(),
                    contextModel.getLight(),
                    contextModel.getParticles(),
                    contextModel.getTrails(),
                    contextModel.getPhysicsValues(),
                    contextModel.getDetailBoxes(),
                    contextModel.getPhobia(),
                    contextModel.getPhobiaModelAssetId()
            );
        }
    }

    public static class EntityAdded extends RefSystem<EntityStore> {

        @Override
        public Query<EntityStore> getQuery() {
            return PlayerWardrobeComponent.getComponentType();
        }

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            PlayerWardrobe wardrobe = store.getComponent(ref, PlayerWardrobeComponent.getComponentType());
            wardrobe.rebuild();
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        }
    }

    public static class WardrobeChanged extends RefChangeSystem<EntityStore, PlayerWardrobeComponent> {

        private static final Query<EntityStore> QUERY = Query.and(Player.getComponentType(), PlayerSkinComponent.getComponentType(), ModelComponent.getComponentType());

        @Nonnull
        @Override
        public ComponentType<EntityStore, PlayerWardrobeComponent> componentType() {
            return PlayerWardrobeComponent.getComponentType();
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerWardrobeComponent wardrobe, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            wardrobe.rebuild();
        }

        @Override
        public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable PlayerWardrobeComponent oldWardrobe, @Nonnull PlayerWardrobeComponent newWardrobe, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            newWardrobe.rebuild();
        }

        @Override
        public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerWardrobeComponent wardrobe, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            PlayerSkinComponent playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (playerSkinComponent != null) {
                playerSkinComponent.setNetworkOutdated();
                Model newModel = CosmeticsModule.get().createModel(playerSkinComponent.getPlayerSkin());
                if (newModel != null) {
                    commandBuffer.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(newModel));
                }
            }
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return QUERY;
        }
    }

    public static class ArmorVisibilityChanged extends RefChangeSystem<EntityStore, PlayerSettings> {

        private static final Query<EntityStore> QUERY = Query.and(Player.getComponentType(), PlayerWardrobeComponent.getComponentType());

        @Nonnull
        @Override
        public ComponentType<EntityStore, PlayerSettings> componentType() {
            return PlayerSettings.getComponentType();
        }

        @Override
        public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerSettings settings, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        }

        @Override
        public void onComponentSet(@Nonnull Ref<EntityStore> ref, @Nullable PlayerSettings oldSettings, @Nonnull PlayerSettings newSettings, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            // If there are no armor visibility changes, then don't rebuild the wardrobe
            if (oldSettings != null && (
                    Objects.equals(oldSettings.hideHelmet(), newSettings.hideHelmet())
                            && Objects.equals(oldSettings.hideCuirass(), newSettings.hideCuirass())
                            && Objects.equals(oldSettings.hidePants(), newSettings.hidePants())
                            && Objects.equals(oldSettings.hideGauntlets(), newSettings.hideGauntlets())
            )) return;

            store.getComponent(ref, PlayerWardrobeComponent.getComponentType()).rebuild();
        }

        @Override
        public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull PlayerSettings settings, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        }

        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return QUERY;
        }
    }

    public static class ArmorInventoryChanged extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

        private static final Query<EntityStore> QUERY = Query.and(Player.getComponentType(), InventoryComponent.Armor.getComponentType(), PlayerWardrobeComponent.getComponentType());

        public ArmorInventoryChanged() {
            super(InventoryChangeEvent.class);
        }

        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
            Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
            assert playerComponent != null;


            InventoryComponent.Armor armorComponent = archetypeChunk.getComponent(index, InventoryComponent.Armor.getComponentType());
            assert armorComponent != null;

            ItemContainer armorInventory = armorComponent.getInventory();
            if (event.getItemContainer().equals(armorInventory)) {
                Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
                PlayerWardrobe wardrobe = store.getComponent(ref, PlayerWardrobeComponent.getComponentType());
                if (wardrobe != null) wardrobe.rebuild();
            }

        }

        @Nullable
        public Query<EntityStore> getQuery() {
            return QUERY;
        }
    }
}
