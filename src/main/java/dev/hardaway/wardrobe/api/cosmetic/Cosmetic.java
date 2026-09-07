package dev.hardaway.wardrobe.api.cosmetic;

import dev.hardaway.wardrobe.api.player.PlayerCosmetic;

import javax.annotation.Nullable;

public interface Cosmetic {

    String getId();

    void applyCosmetic(WardrobeContext context, WardrobeCosmeticSlot slot, PlayerCosmetic playerCosmetic, @Nullable String gradientSet, @Nullable String gradientId);

}
