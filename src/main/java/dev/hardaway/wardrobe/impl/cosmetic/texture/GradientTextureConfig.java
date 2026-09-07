package dev.hardaway.wardrobe.impl.cosmetic.texture;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.schema.metadata.ui.UIPropertyTitle;
import com.hypixel.hytale.server.core.asset.common.CommonAssetValidator;
import com.hypixel.hytale.server.core.cosmetics.CosmeticAssetValidator;
import com.hypixel.hytale.server.core.cosmetics.CosmeticType;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import dev.hardaway.wardrobe.api.cosmetic.appearance.TextureConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GradientTextureConfig implements TextureConfig {

    public static final BuilderCodec<GradientTextureConfig> CODEC = BuilderCodec.builder(GradientTextureConfig.class, GradientTextureConfig::new)
            .append(new KeyedCodec<>("GradientSet", Codec.STRING, true),
                    (t, value) -> t.gradientSet = value,
                    t -> t.gradientSet
            )
            .addValidator(new CosmeticAssetValidator(CosmeticType.GRADIENT_SETS).late())
            .metadata(new UIEditor(new UIEditor.Dropdown("GradientSets")))
            .metadata(new UIPropertyTitle("Gradient Set")).documentation("The Gradient Set determines which colors appear under the 'Variants' section in the Wardrobe Menu.")
            .add()

            .append(new KeyedCodec<>("GradientFrom", Codec.STRING, true),
                    (t, value) -> t.gradientFrom = value,
                    t -> t.gradientFrom
            )
            // .addValidator(CommonAssetValidator.TEXTURE_CHARACTER_ATTACHMENT) // TODO: Create validator
            .metadata(new UIPropertyTitle("Gradient From")).documentation("The slot to take the gradient and color from.")
            .add()

            .append(new KeyedCodec<>("GrayscaleTexture", Codec.STRING, true),
                    (t, value) -> t.grayscaleTexture = value,
                    t -> t.grayscaleTexture
            )
            .addValidator(CommonAssetValidator.TEXTURE_CHARACTER_ATTACHMENT)
            .metadata(new UIPropertyTitle("Grayscale Texture")).documentation("The Grayscale Texture to use. The Texture will be colored according to the Gradient Set and the selected Variant.")
            .add()

            .build();

    private String gradientSet;
    private String gradientFrom;
    private String grayscaleTexture;

    private GradientTextureConfig() {
    }

    public GradientTextureConfig(String gradientSet, String gradientFrom, String grayscaleTexture) {
        this.gradientSet = gradientSet;
        this.gradientFrom = gradientFrom;
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
        return gradientSet;
    }

    @Nonnull
    @Override
    public String getGradientFrom() {
        return gradientFrom;
    }

    @Override
    public String[] collectVariants() {
        return CosmeticsModule.get().getRegistry().getGradientSets().get(this.getGradientSet()).getGradients().keySet().toArray(String[]::new);
    }
}
