package dev.hardaway.wardrobe.impl.cosmetic.builtin;

import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeContext;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;

import javax.annotation.Nullable;

public class HytaleBodyCharacteristicCosmetic extends HytaleCosmetic {
    public HytaleBodyCharacteristicCosmetic(CosmeticType type, PlayerSkinPart part) {
        super(type, part);
    }

    @Override
    public void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        Model model = context.getPlayerModel();
        context.setPlayerModel(new Model(
                model.getModelAssetId(),
                model.getScale(),
                model.getRandomAttachmentIds(),
                model.getAttachments(),
                model.getBoundingBox(),
                this.getPart().getModel(),
                this.getPart().getGreyscaleTexture(),
                gradientSet == null ? this.getPart().getGradientSet() : gradientSet,
                gradientId == null ? playerCosmetic.getVariantId() : gradientId,
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
}
