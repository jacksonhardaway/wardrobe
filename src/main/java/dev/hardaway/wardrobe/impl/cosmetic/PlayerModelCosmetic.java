package dev.hardaway.wardrobe.impl.cosmetic;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeContext;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.cosmetic.appearance.Appearance;
import dev.hardaway.wardrobe.api.cosmetic.appearance.AppearanceCosmetic;
import dev.hardaway.wardrobe.api.cosmetic.appearance.TextureConfig;
import dev.hardaway.wardrobe.api.menu.variant.CosmeticOptionEntry;
import dev.hardaway.wardrobe.api.menu.variant.CosmeticVariantEntry;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;
import dev.hardaway.wardrobe.api.property.WardrobeProperties;
import dev.hardaway.wardrobe.api.property.WardrobeTranslationProperties;
import dev.hardaway.wardrobe.api.property.WardrobeVisibility;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.VariantAppearance;
import dev.hardaway.wardrobe.impl.cosmetic.appearance.VariantAppearanceEntry;
import dev.hardaway.wardrobe.impl.cosmetic.texture.GradientTextureConfig;
import dev.hardaway.wardrobe.impl.cosmetic.texture.VariantTextureConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayerModelCosmetic extends CosmeticAsset implements AppearanceCosmetic {

    public static final BuilderCodec<PlayerModelCosmetic> CODEC = BuilderCodec.builder(PlayerModelCosmetic.class, PlayerModelCosmetic::new, CosmeticAsset.ABSTRACT_CODEC)
            .append(new KeyedCodec<>("Appearance", Appearance.MODELASSET_CODEC, true),
                    (t, value) -> t.appearance = value,
                    t -> t.appearance
            )
            .addValidator(Validators.nonNull())
            .metadata(new UIPropertyTitle("Appearance")).documentation("The appearance of this Cosmetic.")
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()

            .afterDecode((cosmetic) -> {
                if (cosmetic.appearance == null) return;
                String[] variants = cosmetic.appearance.collectVariants();

                if (variants.length == 0) cosmetic.optionEntries = Map.of();
                else if (cosmetic.appearance instanceof VariantAppearance v) {
                    Map<String, CosmeticOptionEntry> entries = new LinkedHashMap<>();
                    for (String variantId : variants) {
                        VariantAppearanceEntry entry = v.getVariants().get(variantId);
                        entries.put(variantId, new CosmeticOptionEntry(
                                variantId,
                                entry.getProperties(),
                                entry.getIcon()
                        ));
                    }
                    cosmetic.optionEntries = entries;
                } else cosmetic.optionEntries = Map.of();
            }).build();

    private Appearance appearance;
    private Map<String, CosmeticOptionEntry> optionEntries;

    private PlayerModelCosmetic() {
    }

    public PlayerModelCosmetic(String id, String[] hiddenCosmeticSlots, String cosmeticSlotId, WardrobeProperties properties, Appearance appearance) {
        super(id, cosmeticSlotId, hiddenCosmeticSlots, properties);
        this.appearance = appearance;
    }

    @Override
    public Appearance getAppearance() {
        return appearance;
    }

    @Override
    public void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        super.applyCosmetic(context, slot, playerCosmetic, gradientSet, gradientId);
        Appearance appearance = this.getAppearance();

        String option = playerCosmetic.getOptionId();
        if (appearance.getModel(option) == null) {
            option = appearance.collectVariants()[0];
        }

        TextureConfig textureConfig = this.getAppearance().getTextureConfig(option);

        String variant = playerCosmetic.getVariantId();
        if (textureConfig.getTexture(variant) == null) {
            variant = textureConfig.collectVariants()[0];
        }

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(this.getAppearance().getModel(option));
        Model model = Model.createScaledModel(modelAsset, this.getAppearance().getScale(option));
        context.setPlayerModel(new Model(
                model.getModelAssetId(),
                model.getScale(),
                model.getRandomAttachmentIds(),
                model.getAttachments(),
                model.getBoundingBox(),
                model.getModel(),
                textureConfig.getTexture(variant),
                textureConfig.getGradientSet(),
                textureConfig.getGradientSet() != null ? variant : null,
                model.getEyeHeight(),
                model.getCrouchOffset(),
                model.getSittingOffset(),
                model.getSleepingOffset(),
                model.getAnimationSetMap(),
                model.getCamera(),
                model.getLight(),
                model.getParticles(),
                model.getTrails(),
                model.getPhysicsValues(),
                model.getDetailBoxes(),
                model.getPhobia(),
                model.getPhobiaModelAssetId()
        ));
    }

    // TODO: create after decoding
    @Override
    public Map<String, CosmeticOptionEntry> getOptionEntries() {
        return optionEntries;
    }

    @Override
    public List<CosmeticVariantEntry> getVariantEntries(@Nullable String variantId) {
        TextureConfig textureConfig = this.appearance.getTextureConfig(variantId);
        String[] textures = textureConfig.collectVariants();
        if (textures.length == 0) return List.of();

        List<CosmeticVariantEntry> entries = new ArrayList<>();
        for (String textureId : textures) {
            WardrobeProperties properties;
            String[] colors;
            String icon = null;
            if (textureConfig instanceof VariantTextureConfig vt) {
                VariantTextureConfig.Entry entry = vt.getVariants().get(textureId);
                properties = entry.getProperties();
                colors = entry.getColors();
                icon = entry.getIcon();
            } else if (textureConfig instanceof GradientTextureConfig gt) {
                properties = new WardrobeProperties(new WardrobeTranslationProperties(textureId, ""), WardrobeVisibility.ALWAYS, null);
                colors = CosmeticsModule.get().getRegistry().getGradientSets()
                        .get(gt.getGradientSet()).getGradients().get(textureId).getBaseColor();
            } else {
                continue;
            }
            entries.add(new CosmeticVariantEntry(textureId, properties, colors, icon));
        }
        return entries;
    }
}
