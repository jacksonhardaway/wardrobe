package dev.hardaway.wardrobe.impl.cosmetic;

import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDefaultCollapsedState;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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
import dev.hardaway.wardrobe.impl.cosmetic.builtin.HytaleCosmetic;
import dev.hardaway.wardrobe.impl.cosmetic.texture.GradientTextureConfig;
import dev.hardaway.wardrobe.impl.cosmetic.texture.VariantTextureConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModelAttachmentCosmetic extends CosmeticAsset implements AppearanceCosmetic {

    public static final BuilderCodec<ModelAttachmentCosmetic> CODEC = AssetBuilderCodec.builder(ModelAttachmentCosmetic.class, ModelAttachmentCosmetic::new, CosmeticAsset.ABSTRACT_CODEC)
            .appendInherited(new KeyedCodec<>("OverlapCosmeticSlots", Codec.STRING_ARRAY),
                    (c, value) -> c.overlapCosmeticSlotIds = value,
                    c -> c.overlapCosmeticSlotIds,
                    (c, p) -> c.overlapCosmeticSlotIds = p.overlapCosmeticSlotIds
            )
            .addValidator(CosmeticSlotAsset.VALIDATOR_CACHE.getArrayValidator().late())
            .metadata(new UIPropertyTitle("Overlapping Cosmetic Slots")).documentation("An array of Cosmetic Slot ids to mark as overlapping. When this Cosmetic is applied on a Cosmetic Slot, any Model Attachment Cosmetics in these Cosmetic Slots will change to their Armor Appearance, if there is one.")
            .add()

            .appendInherited(new KeyedCodec<>("Appearance", Appearance.CODEC, true),
                    (c, value) -> c.appearance = value,
                    c -> c.appearance,
                    (c, p) -> c.appearance = p.appearance
            )
            .addValidator(Validators.nonNull())
            .metadata(new UIPropertyTitle("Appearance")).documentation("The appearance of this Cosmetic.")
            .metadata(UIDefaultCollapsedState.UNCOLLAPSED)
            .add()

            .appendInherited(new KeyedCodec<>("OverlapAppearance", Appearance.CODEC),
                    (c, value) -> c.overlapAppearance = value,
                    c -> c.appearance,
                    (c, p) -> c.overlapAppearance = p.overlapAppearance
            )
            .metadata(new UIPropertyTitle("Overlap Appearance")).documentation("The appearance of this Cosmetic to use when another Cosmetic is overlapping with the Cosmetic Slot this Cosmetic is applied to. See 'Overlapping Cosmetic Slots' above for more information.")
            .add()

            .appendInherited(new KeyedCodec<>("ArmorAppearance", Appearance.CODEC, false),
                    (t, value) -> t.armorAppearance = value,
                    t -> t.armorAppearance,
                    (c, p) -> c.armorAppearance = p.armorAppearance
            )
            .metadata(new UIPropertyTitle("Armor Appearance")).documentation("The appearance of this Cosmetic to use when there is an item equipped in the Armor Slot defined in the Cosmetic Slot this Cosmetic is applied to.")
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

    private String[] overlapCosmeticSlotIds = new String[0];
    private Appearance appearance;
    private @Nullable Appearance overlapAppearance;
    private @Nullable Appearance armorAppearance;
    private Map<String, CosmeticOptionEntry> optionEntries;
    protected ModelAttachmentCosmetic() {
    }

    public ModelAttachmentCosmetic(String id, String[] hiddenCosmeticSlots, String cosmeticSlotId, WardrobeProperties properties, Appearance appearance, @Nullable Appearance overlapAppearance, @Nullable Appearance armorAppearance) {
        super(id, cosmeticSlotId, hiddenCosmeticSlots, properties);
        this.appearance = appearance;
        this.overlapAppearance = overlapAppearance;
        this.armorAppearance = armorAppearance;
    }

    public String[] getOverlapCosmeticSlotIds() {
        return overlapCosmeticSlotIds;
    }

    @Override
    public Appearance getAppearance() {
        return appearance;
    }

    @Nullable
    public Appearance getOverlapAppearance() {
        return overlapAppearance;
    }

    @Nullable
    public Appearance getArmorAppearance() {
        return armorAppearance;
    }

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

    @Override
    public void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        super.applyCosmetic(context, slot, playerCosmetic, gradientSet, gradientId);
        Appearance appearance = this.getAppearance();

        if (slot.getArmorSlot() != null && this.getArmorAppearance() != null) {
            boolean shouldHide = switch (slot.getArmorSlot()) {
                case Head -> context.getPlayerSettings().hideHelmet();
                case Chest -> context.getPlayerSettings().hideCuirass();
                case Hands -> context.getPlayerSettings().hideGauntlets();
                case Legs -> context.getPlayerSettings().hidePants();
            };

            if (!shouldHide) {
                ItemStack armor = context.getPlayer().getInventory().getArmor().getItemStack((short) slot.getArmorSlot().getValue());
                if (armor != null) appearance = this.getArmorAppearance();
            }
        } else if (this.getOverlapAppearance() != null) {
            boolean overlapFound = false;
            for (dev.hardaway.wardrobe.api.cosmetic.Cosmetic cosmetic : context.getCosmeticMap().values()) {
                if (overlapFound)
                    break;

                if (cosmetic instanceof HytaleCosmetic hytaleCosmetic) {
                    if (hytaleCosmetic.getPart().getHeadAccessoryType() == PlayerSkinPart.HeadAccessoryType.HalfCovering) {
                        overlapFound = true;
                        break;
                    }
                }
                if (cosmetic instanceof ModelAttachmentCosmetic modelAttachmentCosmetic) {
                    for (String overlapSlot : modelAttachmentCosmetic.overlapCosmeticSlotIds) {
                        if (slot.getId().equals(overlapSlot)) {
                            overlapFound = true;
                            break;
                        }
                    }
                }
            }

            if (overlapFound) {
                appearance = this.getOverlapAppearance();
            }
        }

        String option = playerCosmetic.getOptionId();
        if (appearance.getModel(option) == null) {
            option = appearance.collectVariants()[0];
        }

        String variant = gradientId == null ? playerCosmetic.getVariantId() : gradientId;
        if (appearance.getTextureConfig(option).getTexture(variant) == null) {
            variant = appearance.getTextureConfig(option).collectVariants()[0];
        }

        TextureConfig textureConfig = appearance.getTextureConfig(option);
        System.out.println("textureConfig: " + textureConfig.getGradientFrom());
        context.addAttachment(slot.getId(), new ModelAttachment(
                appearance.getModel(option),
                textureConfig.getTexture(variant),
                textureConfig.getGradientSet(),
                textureConfig.getGradientSet() != null ? variant : null,
                1.0
        ));
    }

}
