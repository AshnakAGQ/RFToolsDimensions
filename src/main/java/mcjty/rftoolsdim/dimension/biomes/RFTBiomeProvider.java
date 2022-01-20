package mcjty.rftoolsdim.dimension.biomes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.rftoolsdim.dimension.data.DimensionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static mcjty.rftoolsdim.dimension.data.DimensionSettings.SETTINGS_CODEC;

public class RFTBiomeProvider extends BiomeSource {

    public static final Codec<RFTBiomeProvider> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryLookupCodec.create(Registry.BIOME_REGISTRY).forGetter(RFTBiomeProvider::getBiomeRegistry),
                    SETTINGS_CODEC.fieldOf("settings").forGetter(RFTBiomeProvider::getSettings)
            ).apply(instance, RFTBiomeProvider::new));

    private final List<Biome> biomes;
    private final Map<ResourceLocation, Biome> biomeMapping = new HashMap<>();
    private final Registry<Biome> biomeRegistry;
    private final DimensionSettings settings;
    private final MultiNoiseBiomeSource multiNoiseBiomeSource;
    private final boolean defaultBiomes;

    public RFTBiomeProvider(Registry<Biome> biomeRegistry, DimensionSettings settings) {
        super(Collections.emptyList());
        this.settings = settings;
        this.biomeRegistry = biomeRegistry;
        multiNoiseBiomeSource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(biomeRegistry, true);
        biomes = getBiomes(biomeRegistry, settings);
        defaultBiomes = biomes.isEmpty();
    }

    public DimensionSettings getSettings() {
        return settings;
    }

    private Biome getMappedBiome(Biome biome) {
        if (defaultBiomes) {
            return biome;
        }
        return biomeMapping.computeIfAbsent(biome.getRegistryName(), resourceLocation -> {
            List<Biome> biomes = getBiomes(biomeRegistry, settings);
            float minDist = 1000000000;
            Biome desired = biome;
            for (Biome b : biomes) {
                float dist = distance(b, biome);
                if (dist < minDist) {
                    desired = b;
                    minDist = dist;
                }
            }
            return desired;
        });
    }

    private static float distance(Biome biome1, Biome biome2) {
        float d1 = biome1.getBiomeCategory() == biome2.getBiomeCategory() ? 0 : 1;
        float d2 = Math.abs(biome1.getBaseTemperature() - biome2.getBaseTemperature());
        float d3 = Math.abs(biome1.getDownfall() - biome2.getDownfall());
        float d4 = biome1.isHumid() == biome2.isHumid() ? 0 : 1;
        return d1 + d2*d2 + d3*d3 + d4;
    }

    private List<Biome> getBiomes(Registry<Biome> biomeRegistry, DimensionSettings settings) {
        if (defaultBiomes) {
            return Collections.emptyList();
        }
        List<ResourceLocation> biomes = settings.getCompiledDescriptor().getBiomes();
        return biomes.stream().map(biomeRegistry::get).collect(Collectors.toList());
    }

    public Registry<Biome> getBiomeRegistry() {
        return biomeRegistry;
    }

    @Nonnull
    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Nonnull
    @Override
    public BiomeSource withSeed(long seed) {
        return new RFTBiomeProvider(getBiomeRegistry(), settings);
    }

    @Override
    public Set<Biome> possibleBiomes() {
        if (defaultBiomes) {
            return multiNoiseBiomeSource.possibleBiomes();
        } else {
            return multiNoiseBiomeSource.possibleBiomes().stream()
                    .map(this::getMappedBiome)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public List<StepFeatureData> featuresPerStep() {
        return multiNoiseBiomeSource.featuresPerStep();
    }

    @Override
    public void addMultinoiseDebugInfo(List<String> list, BlockPos pos, Climate.Sampler climate) {
        multiNoiseBiomeSource.addMultinoiseDebugInfo(list, pos, climate);
    }

    @Nullable
    @Override
    public BlockPos findBiomeHorizontal(int x, int y, int z, int radius, Predicate<Biome> predicate, Random random, Climate.Sampler climate) {
        return multiNoiseBiomeSource.findBiomeHorizontal(x, y, z, radius, predicate, random, climate);
    }

    @Nullable
    @Override
    public BlockPos findBiomeHorizontal(int x, int y, int z, int p_186699_, int p_186700_, Predicate<Biome> predicate, Random random, boolean p_186703_, Climate.Sampler climate) {
        return multiNoiseBiomeSource.findBiomeHorizontal(x, y, z, p_186699_, p_186700_, predicate, random, p_186703_, climate);
    }

    @Override
    public Set<Biome> getBiomesWithin(int x, int y, int z, int radius, Climate.Sampler climate) {
        if (defaultBiomes) {
            return multiNoiseBiomeSource.getBiomesWithin(x, y, z, radius, climate);
        } else {
            return multiNoiseBiomeSource.getBiomesWithin(x, y, z, radius, climate).stream()
                    .map(this::getMappedBiome)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z, Climate.Sampler climate) {
        switch (settings.getCompiledDescriptor().getBiomeControllerType()) {
            case CHECKER -> {
                if ((x+y)%2 == 0 || biomes.size() <= 1) {
                    return getMappedBiome(biomes.get(0));
                } else {
                    return getMappedBiome(biomes.get(1));
                }
            }
            case SINGLE -> {
                if (biomes.isEmpty()) {
                    if (defaultBiomes) {
                        return multiNoiseBiomeSource.getNoiseBiome(x, y, z, climate);
                    } else {
                        return getMappedBiome(multiNoiseBiomeSource.getNoiseBiome(x, y, z, climate));
                    }
                } else {
                    return getMappedBiome(biomes.get(0));
                }
            }
            default -> {
                if (defaultBiomes) {
                    return multiNoiseBiomeSource.getNoiseBiome(x, y, z, climate);
                } else {
                    return getMappedBiome(multiNoiseBiomeSource.getNoiseBiome(x, y, z, climate));
                }
            }
        }
    }
}
