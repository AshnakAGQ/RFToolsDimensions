package mcjty.rftoolsdim.dimensions.dimlets.types;

import mcjty.rftoolsdim.config.WorldgenConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.description.WeatherDescriptor;
import mcjty.rftoolsdim.dimensions.dimlets.DimletKey;
import mcjty.rftoolsdim.dimensions.dimlets.DimletObjectMapping;
import mcjty.rftoolsdim.dimensions.dimlets.DimletRandomizer;
import mcjty.rftoolsdim.dimensions.dimlets.WeatherRegistry;
import mcjty.rftoolsdim.dimensions.types.WeatherType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Random;

public class WeatherDimletType implements IDimletType {

    @Override
    public String getName() {
        return "Weather";
    }

    @Override
    public String getOpcode() {
        return "W";
    }

    @Override
    public String getTextureName() {
        return "weatherDimlet";
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
        WeatherDescriptor.Builder builder = new WeatherDescriptor.Builder();
        builder.combine(dimensionInformation.getWeatherDescriptor());
        WeatherType newType = DimletObjectMapping.getWeather(key);
        builder.weatherType(newType);
        dimensionInformation.setWeatherDescriptor(builder.build());
    }

    @Override
    public void constructDimension(List<Pair<DimletKey, List<DimletKey>>> dimlets, Random random, DimensionInformation dimensionInformation) {
        dimlets = DimensionInformation.extractType(DimletType.DIMLET_WEATHER, dimlets);
        WeatherDescriptor.Builder builder = new WeatherDescriptor.Builder();
        if (dimlets.isEmpty()) {
            while (random.nextFloat() > WorldgenConfiguration.randomWeatherChance) {
                DimletKey key = DimletRandomizer.getRandomWeather(random);
                if (key != null) {
                    dimensionInformation.updateCostFactor(key);
                    builder.weatherType(WeatherRegistry.getWeatherType(key));
                }
            }
        } else {
            for (Pair<DimletKey, List<DimletKey>> dimlet : dimlets) {
                DimletKey key = dimlet.getKey();
                builder.weatherType(WeatherRegistry.getWeatherType(key));
            }
        }
        dimensionInformation.setWeatherDescriptor(builder.build());
    }

    @Override
    public String[] getInformation() {
        return new String[] { "A weather dimlet affects the weather", "in a dimension." };
    }

    @Override
    public DimletKey attemptDimletCrafting(ItemStack stackController, ItemStack stackMemory, ItemStack stackEnergy, ItemStack stackEssence) {
        return null;
    }
}
