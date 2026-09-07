package dev.hardaway.wardrobe.api.cosmetic.appearance;

import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;

import javax.annotation.Nullable;

public interface TextureConfig {

    BuilderCodecMapCodec<TextureConfig> CODEC = new BuilderCodecMapCodec<>("Type", true);

    String getTexture(@Nullable String variantId);

    @Nullable
    default String getGradientSet() {
        return null;
    }

    @Nullable
    default String getGradientFrom() {
        return null;
    }

    String[] collectVariants();
}
