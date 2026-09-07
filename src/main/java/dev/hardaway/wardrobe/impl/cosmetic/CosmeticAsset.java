package dev.hardaway.wardrobe.impl.cosmetic;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.*;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import dev.hardaway.wardrobe.WardrobePlugin;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeContext;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmetic;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;
import dev.hardaway.wardrobe.api.property.WardrobeProperties;
import dev.hardaway.wardrobe.api.property.validator.WardrobeValidators;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

public abstract class CosmeticAsset implements WardrobeCosmetic, JsonAssetWithMap<String, DefaultAssetMap<String, CosmeticAsset>> {

    public static final BuilderCodec<CosmeticAsset> ABSTRACT_CODEC = BuilderCodec.abstractBuilder(CosmeticAsset.class)
            .append(
                    new KeyedCodec<>("Icon", Codec.STRING),
                    (t, value) -> t.icon = value,
                    t -> t.icon
            )
            .addValidator(WardrobeValidators.ICON)
            .metadata(new UIPropertyTitle("Icon")).documentation("A preview icon of the Cosmetic to display in the Wardrobe Menu.")
            .metadata(new UIRebuildCaches(UIRebuildCaches.ClientCache.MODELS))
            .metadata(new UIEditor(new UIEditor.Icon(
                    "Icons/Wardrobe/CosmeticsGenerated/{assetId}.png", 64, 64
            )))
            .add()

            .append(
                    new KeyedCodec<>("IconProperties", AssetIconProperties.CODEC),
                    (p, i) -> p.iconProperties = i,
                    (item) -> item.iconProperties
            )
            .metadata(UIDisplayMode.HIDDEN)
            .add()

            .appendInherited(new KeyedCodec<>("Properties", WardrobeProperties.CODEC, true),
                    (c, value) -> c.properties = value,
                    c -> c.properties,
                    (c, p) -> c.properties = p.properties
            )
            .addValidator(Validators.nonNull())
            .metadata(new UIPropertyTitle("Wardrobe Properties")).documentation("Properties for the Cosmetic to display in the Wardrobe Menu.")
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()

            .appendInherited(new KeyedCodec<>("CosmeticSlot", Codec.STRING, true),
                    (c, value) -> c.cosmeticSlotId = value,
                    c -> c.cosmeticSlotId,
                    (c, p) -> c.cosmeticSlotId = p.cosmeticSlotId
            )
            .addValidator(CosmeticSlotAsset.VALIDATOR_CACHE.getValidator().late())
            .metadata(new UIPropertyTitle("Cosmetic Slot")).documentation("The Cosmetic Slot this Cosmetic will be applied to and show up under in the Wardrobe Menu.")
            .add()

            .appendInherited(new KeyedCodec<>("HiddenCosmeticSlots", Codec.STRING_ARRAY),
                    (c, value) -> c.hiddenCosmeticSlots = value,
                    c -> c.hiddenCosmeticSlots,
                    (c, p) -> c.hiddenCosmeticSlots = p.hiddenCosmeticSlots
            )
            .addValidator(CosmeticSlotAsset.VALIDATOR_CACHE.getArrayValidator().late())
            .metadata(new UIPropertyTitle("Cosmetic Slots to Hide")).documentation("An array of Cosmetic Slot ids to hide when this cosmetic is applied.")
            .add()
            .afterDecode(asset -> {
                if (asset.getIconPath() == null && asset.properties != null && asset.properties.getIcon() != null) { // DEPRECATED
                    asset.icon = asset.properties.getIcon();
                }
            })
            .build();

    public static final AssetCodecMapCodec<String, CosmeticAsset> CODEC = new AssetCodecMapCodec<>(
            Codec.STRING, (t, k) -> t.id = k, t -> t.id, (t, data) -> t.data = data, t -> t.data, true
    );

    public static final Supplier<AssetStore<String, CosmeticAsset, DefaultAssetMap<String, CosmeticAsset>>> ASSET_STORE = WardrobePlugin.createAssetStore(CosmeticAsset.class);

    public static DefaultAssetMap<String, CosmeticAsset> getAssetMap() {
        return ASSET_STORE.get().getAssetMap();
    }

    private String id;
    private AssetExtraInfo.Data data;

    private String icon;
    private AssetIconProperties iconProperties;

    private String cosmeticSlotId;
    private String[] hiddenCosmeticSlots = new String[0];
    private WardrobeProperties properties;

    protected CosmeticAsset() {
    }

    public CosmeticAsset(String id, String cosmeticSlotId, String[] hiddenCosmeticSlots, WardrobeProperties properties) {
        this.id = id;
        this.cosmeticSlotId = cosmeticSlotId;
        this.hiddenCosmeticSlots = hiddenCosmeticSlots;
        this.properties = properties;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public WardrobeProperties getProperties() {
        return properties;
    }

    @Override
    public String getIconPath() {
        return icon;
    }

    public AssetIconProperties getIconProperties() {
        return iconProperties;
    }

    @Nonnull
    public String getCosmeticSlotId() {
        return cosmeticSlotId;
    }

    public String[] getHiddenCosmeticSlotIds() {
        return hiddenCosmeticSlots;
    }

    @Override
    public void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        context.hideSlots(this.getHiddenCosmeticSlotIds());
    }
}