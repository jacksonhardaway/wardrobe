package dev.hardaway.wardrobe.impl.cosmetic.builtin;

import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import dev.hardaway.wardrobe.api.cosmetic.Cosmetic;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeContext;
import dev.hardaway.wardrobe.api.cosmetic.WardrobeCosmeticSlot;
import dev.hardaway.wardrobe.api.player.PlayerCosmetic;
import dev.hardaway.wardrobe.impl.cosmetic.ModelAttachmentCosmetic;

import javax.annotation.Nullable;
import java.util.Objects;

public class HytaleHaircutCosmetic extends HytaleCosmetic {
    public HytaleHaircutCosmetic(CosmeticType type, PlayerSkinPart part) {
        super(type, part);
    }

    @Override
    protected ModelAttachment createAttachment(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId) {
        ModelAttachment attachment = super.createAttachment(context, slot, playerCosmetic, gradientSet, gradientId);
        if (attachment == null) return null;

        CosmeticRegistry cosmeticRegistry = CosmeticsModule.get().getRegistry();

        String model = attachment.getModel();
        String texture = attachment.getTexture();
        if (this.getPart().doesRequireGenericHaircut() && this.getPart().getHairType() != null) {
            boolean needsGeneric = false;
            for (Cosmetic cosmetic : context.getCosmeticMap().values()) {
                if (cosmetic instanceof HytaleCosmetic hytaleCosmetic) {
                    if (hytaleCosmetic.getPart().getHeadAccessoryType() == PlayerSkinPart.HeadAccessoryType.HalfCovering) {
                        needsGeneric = true;
                        break;
                    }
                } else if (cosmetic instanceof ModelAttachmentCosmetic modelAttachmentCosmetic) { // TODO: unhardcode this
                    for (String overlapSlot : modelAttachmentCosmetic.getOverlapCosmeticSlotIds()) {
                        if (slot.getId().equals(overlapSlot)) {
                            needsGeneric = true;
                            break;
                        }
                    }
                }
            }

            if (needsGeneric) {
                PlayerSkinPart genericHaircut = cosmeticRegistry.getHaircuts().get("Generic" + this.getPart().getHairType());
                if (genericHaircut != null) {
                    model = genericHaircut.getModel();
                    texture = genericHaircut.getGreyscaleTexture();
                }
            }
        }

        if (!Objects.equals(attachment.getModel(), model) || !Objects.equals(attachment.getTexture(), texture)) {
            return new ModelAttachment(
                    model,
                    texture,
                    attachment.getGradientSet(),
                    attachment.getGradientId(),
                    1.0
            );
        } else return attachment;
    }
}
