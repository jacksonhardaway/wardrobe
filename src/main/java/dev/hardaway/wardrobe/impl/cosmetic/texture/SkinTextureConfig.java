package dev.hardaway.wardrobe.impl.cosmetic.texture;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import dev.hardaway.wardrobe.api.cosmetic.appearance.TextureConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SkinTextureConfig implements TextureConfig {

    public static final BuilderCodec<SkinTextureConfig> CODEC = BuilderCodec.builder(SkinTextureConfig.class, SkinTextureConfig::new)
            .append(new KeyedCodec<>("GrayscaleTexture", Codec.STRING, true),
                    (t, value) -> t.grayscaleTexture = value,
                    t -> t.grayscaleTexture
            )
            .addValidator(CommonAssetValidator.TEXTURE_CHARACTER_ATTACHMENT)
            .metadata(new UIPropertyTitle("Grayscale Texture")).documentation("The Grayscale Texture to use. The Texture will be colored according to the Skin Tone.")
            .add()

            .build();

    private String grayscaleTexture;

    private SkinTextureConfig() {
    }

    public SkinTextureConfig(String grayscaleTexture) {
        this.grayscaleTexture = grayscaleTexture;
    }

    @Nonnull
    @Override
    public String getTexture(@Nullable String variantId) {
        return grayscaleTexture;
    }

    @Nonnull
    @Override
    public String getGradientSet() {
        return "Skin";
    }

    @Override
    public String[] collectVariants() {
        return new String[0];
    }
}
