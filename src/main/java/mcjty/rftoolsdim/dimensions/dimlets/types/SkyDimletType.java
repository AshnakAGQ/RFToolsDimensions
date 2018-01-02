package mcjty.rftoolsdim.dimensions.dimlets.types;

import mcjty.rftoolsdim.config.WorldgenConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.description.SkyDescriptor;
import mcjty.rftoolsdim.dimensions.dimlets.DimletKey;
import mcjty.rftoolsdim.dimensions.dimlets.DimletObjectMapping;
import mcjty.rftoolsdim.dimensions.dimlets.DimletRandomizer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SkyDimletType implements IDimletType {

    @Override
    public String getName() {
        return "Sky";
    }

    @Override
    public String getOpcode() {
        return "s";
    }

    @Override
    public String getTextureName() {
        return "skyDimlet";
    }

    @Override
    public void setupFromConfig(Configuration cfg) {
    }

    @Override
    public boolean isModifier() {
        return false;
    }

    @Override
    public boolean isModifiedBy(DimletType type) {
        return false;
    }

    @Override
    public float getModifierCreateCostFactor(DimletType modifierType, DimletKey key) {
        return 1.0f;
    }

    @Override
    public float getModifierMaintainCostFactor(DimletType modifierType, DimletKey key) {
        return 1.0f;
    }

    @Override
    public float getModifierTickCostFactor(DimletType modifierType, DimletKey key) {
        return 1.0f;
    }

    @Override
    public boolean isInjectable(DimletKey key) {
        return true;
    }

    @Override
    public void inject(DimletKey key, DimensionInformation dimensionInformation) {
        SkyDescriptor.Builder builder = new SkyDescriptor.Builder();
        builder.combine(dimensionInformation.getSkyDescriptor());
        SkyDescriptor newDescriptor = DimletObjectMapping.getSky(key);
        if (newDescriptor.specifiesFogColor()) {
            builder.resetFogColor();
        }
        if (newDescriptor.specifiesSkyColor()) {
            builder.resetSkyColor();
        }
        if (newDescriptor.isCloudColorGiven()) {
            builder.resetCloudColor();
        }
        builder.combine(newDescriptor);
        dimensionInformation.setSkyDescriptor(builder.build());
    }

    @Override
    public void constructDimension(List<Pair<DimletKey, List<DimletKey>>> dimlets, Random random, DimensionInformation dimensionInformation) {
        dimlets = DimensionInformation.extractType(DimletType.DIMLET_SKY, dimlets);
        if (dimlets.isEmpty()) {
            if (random.nextFloat() < WorldgenConfiguration.randomSpecialSkyChance) {
                // If nothing was specified then there is random chance we get random sky stuff.
                for (int i = 0 ; i < 1+random.nextInt(3) ; i++) {
                    DimletKey key = DimletRandomizer.getRandomSky(random);
                    if (key != null) {
                        List<DimletKey> modifiers = Collections.emptyList();
                        dimlets.add(Pair.of(key, modifiers));
                    }
                }
            }

            if (random.nextFloat() < WorldgenConfiguration.randomSpecialSkyChance) {
                for (int i = 0 ; i < random.nextInt(3) ; i++) {
                    DimletKey key = DimletRandomizer.getRandomSkyBody(random);
                    if (key != null) {
                        List<DimletKey> modifiers = Collections.emptyList();
                        dimlets.add(Pair.of(key, modifiers));
                    }
                }
            }
        }

        SkyDescriptor.Builder builder = new SkyDescriptor.Builder();
        for (Pair<DimletKey, List<DimletKey>> dimletWithModifiers : dimlets) {
            DimletKey key = dimletWithModifiers.getKey();
            builder.combine(DimletObjectMapping.getSky(key));
        }
        dimensionInformation.setSkyDescriptor(builder.build());
    }

    @Override
    public String[] getInformation() {
        return new String[] { "Control various features of the sky", "like sky color, fog color, celestial bodies, ..." };
    }

    @Override
    public DimletKey attemptDimletCrafting(ItemStack stackController, ItemStack stackMemory, ItemStack stackEnergy, ItemStack stackEssence) {
        return null;
    }
}
