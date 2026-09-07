package dev.hardaway.wardrobe.impl.player;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hardaway.wardrobe.WardrobePlugin;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;
import dev.hardaway.wardrobe.api.player.PlayerWardrobe;
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset;
import dev.hardaway.wardrobe.impl.cosmetic.PlayerModelCosmetic;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerWardrobeComponent implements PlayerWardrobe, Component<EntityStore> {

    public static final BuilderCodec<PlayerWardrobeComponent> CODEC = BuilderCodec.builder(PlayerWardrobeComponent.class, PlayerWardrobeComponent::new)
            .append(new KeyedCodec<>("Cosmetics", new MapCodec<>(CosmeticSaveData.CODEC, HashMap::new, false), true),
                    PlayerWardrobeComponent::setCosmetics,
                    t -> t.cosmetics
            ).add()
            .append(new KeyedCodec<>("HiddenCosmeticTypes", new SetCodec<>(new EnumCodec<>(CosmeticType.class), HashSet::new, false), false),
                    (t, value) -> t.hiddenCosmeticTypes = value,
                    t -> t.hiddenCosmeticTypes
            ).add()
            .build();

    private Map<String, CosmeticSaveData> cosmetics;
    private Set<String> cosmeticIdSet;
    private Set<CosmeticType> hiddenCosmeticTypes;
    private boolean dirty;

    public PlayerWardrobeComponent() {
        this(new HashMap<>(), new HashSet<>());
    }

    protected PlayerWardrobeComponent(Map<String, CosmeticSaveData> cosmetics, Set<CosmeticType> hiddenCosmeticTypes) {
        this.setCosmetics(cosmetics);
        this.hiddenCosmeticTypes = hiddenCosmeticTypes;
    }

    @Override
    public Map<String, PlayerCosmetic> getCosmeticMap() {
        return Collections.unmodifiableMap(this.cosmetics);
    }

    @Override
    public Collection<PlayerCosmetic> getCosmetics() {
        return Collections.unmodifiableCollection(this.cosmetics.values());
    }

    private void setCosmetics(Map<String, CosmeticSaveData> cosmeticMap) {
        this.cosmetics = cosmeticMap;
        this.cosmeticIdSet = this.cosmetics.values().stream().map(CosmeticSaveData::getCosmeticId).collect(Collectors.toSet());
    }

    @Override
    public boolean hasCosmetic(String id) {
        return cosmeticIdSet.contains(id);
    }

    @Override
    public PlayerCosmetic getCosmetic(String slot) {
        return cosmetics.get(slot);
    }

    @Override
    public void setCosmetic(String slot, PlayerCosmetic cosmetic) {
        if (cosmetic == null) {
            PlayerCosmetic lastCosmetic = this.cosmetics.remove(slot);
            if (lastCosmetic != null) {
                this.cosmeticIdSet.remove(lastCosmetic.getCosmeticId());
            }
            return;
        }

        if (CosmeticAsset.getAssetMap().getAsset(cosmetic.getCosmeticId()) instanceof PlayerModelCosmetic) {
            Iterator<Map.Entry<String, CosmeticSaveData>> iterator = this.cosmetics.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, CosmeticSaveData> entry = iterator.next();
                CosmeticSaveData existingCosmetic = entry.getValue();

                if (CosmeticAsset.getAssetMap().getAsset(existingCosmetic.getCosmeticId()) instanceof PlayerModelCosmetic) {
                    iterator.remove();
                    this.cosmeticIdSet.remove(existingCosmetic.getCosmeticId());
                }
            }
        }

        // TODO: fix api so i dont have to do this
        CosmeticSaveData cosmeticSaveData = new CosmeticSaveData(cosmetic.getCosmeticId(), cosmetic.getOptionId(), cosmetic.getVariantId());
        PlayerCosmetic lastCosmetic = this.cosmetics.put(slot, cosmeticSaveData);
        if (lastCosmetic != null) {
            this.cosmeticIdSet.remove(lastCosmetic.getCosmeticId());
        }

        this.cosmeticIdSet.add(cosmetic.getCosmeticId());
    }

    @Override
    public void clearCosmetics() {
        this.cosmetics.clear();
    }

    @Override
    public void toggleCosmeticType(CosmeticType type) {
        if (hiddenCosmeticTypes.contains(type)) hiddenCosmeticTypes.remove(type);
        else hiddenCosmeticTypes.add(type);
    }

    @Override
    public Collection<CosmeticType> getHiddenCosmeticTypes() {
        return hiddenCosmeticTypes;
    }

    @Override
    public void rebuild() {
        this.dirty = true;
    }

    protected boolean consumeDirty() {
        boolean dirty = this.dirty;
        this.dirty = false;
        return dirty;
    }

    @Nullable
    @Override
    public PlayerWardrobeComponent clone() {
        return new PlayerWardrobeComponent(new HashMap<>(this.cosmetics), new HashSet<>(this.hiddenCosmeticTypes));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerWardrobeComponent that)) return false;
        return cosmetics.equals(that.cosmetics) && this.hiddenCosmeticTypes.equals(that.hiddenCosmeticTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cosmetics, hiddenCosmeticTypes);
    }

    public static ComponentType<EntityStore, PlayerWardrobeComponent> getComponentType() {
        return WardrobePlugin.get().getPlayerWardrobeComponentType();
    }
}
