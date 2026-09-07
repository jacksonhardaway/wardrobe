package dev.hardaway.wardrobe.impl.cosmetic.builtin;

import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;
import dev.hardaway.wardrobe.api.cosmetic.Cosmetic;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeContext;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class HytaleCosmetic implements Cosmetic {

    private final CosmeticType type;
    private final PlayerSkinPart part;
    protected final Map<String, PlayerSkinPart.Variant> variantMap;

    public HytaleCosmetic(CosmeticType type, PlayerSkinPart part) {
        this.type = type;
        this.part = part;

        if (this.part.getVariants() != null) {
            this.variantMap = Collections.unmodifiableMap(this.part.getVariants());
        } else {
            this.variantMap = Collections.emptyMap();
        }
    }

    @Override
    public String getId() {
        return "Hytale:" + this.type.name() + "." + part.getId();
    }

    public CosmeticType getType() {
        return type;
    }

    public PlayerSkinPart getPart() {
        return part;
    }

    @Nullable
    protected ModelAttachment createAttachment(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        String model;
        String texture;

        if (!this.variantMap.isEmpty()) {
            PlayerSkinPart.Variant variant = this.variantMap.get(playerCosmetic.getOptionId());
            if (variant == null) return null; // Likely using a pre-release cosmetic?

            model = variant.getModel();

            if (variant.getTextures() != null) {
                PlayerSkinPartTexture partTexture = variant.getTextures().get(playerCosmetic.getVariantId());
                if (partTexture == null) return null; // Likely using a pre-release cosmetic?

                texture = partTexture.getTexture();
            } else {
                texture = variant.getGreyscaleTexture();
                gradientSet = gradientSet == null ? this.part.getGradientSet() : gradientSet;
                gradientId = gradientId == null ? playerCosmetic.getVariantId() : gradientId;
            }
        } else {
            model = this.part.getModel();

            if (this.part.getTextures() != null) {
                PlayerSkinPartTexture partTexture = this.part.getTextures().get(playerCosmetic.getVariantId());
                if (partTexture == null) return null; // Likely using a pre-release cosmetic?

                texture = partTexture.getTexture();
            } else {
                texture = this.part.getGreyscaleTexture();
                gradientSet = gradientSet == null ? this.part.getGradientSet() : gradientSet;
                gradientId = gradientId == null ? playerCosmetic.getVariantId() : gradientId;
            }
        }

        if (this.part.getHeadAccessoryType() == PlayerSkinPart.HeadAccessoryType.FullyCovering) {
            context.hideSlots("Haircut");
        }

        return new ModelAttachment(
                model,
                texture,
                gradientSet,
                gradientId,
                1.0
        );
    }

    @Override
    public void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        ModelAttachment attachment = this.createAttachment(context, slot, playerCosmetic, gradientSet, gradientId);
        if (attachment == null) return;
        // TODO: warn?
        context.addAttachment(slot.getId(), attachment);
    }
}
