package mcjty.rftoolsdim.modules.dimlets.data;

import mcjty.lib.varia.Tools;
import mcjty.rftoolsdim.compat.LostCityCompat;
import mcjty.rftoolsdim.dimension.AdminDimletType;
import mcjty.rftoolsdim.modules.dimlets.DimletModule;
import mcjty.rftoolsdim.modules.dimlets.items.DimletItem;
import mcjty.rftoolsdim.modules.essences.EssencesModule;
import mcjty.rftoolsdim.modules.essences.blocks.BiomeAbsorberTileEntity;
import mcjty.rftoolsdim.modules.essences.blocks.BlockAbsorberTileEntity;
import mcjty.rftoolsdim.modules.essences.blocks.FluidAbsorberTileEntity;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class DimletTools {

    // Use client side!
    public static String getDimletDescription(ItemStack stack) {
        DimletKey key = getDimletKey(stack);
        if (key == null) {
            return "<Unknown>";
        } else {
            return getReadableName(key);
        }
    }

    // Use client side!
    public static String getDimletRarity(ItemStack stack) {
        DimletKey key = getDimletKey(stack);
        if (key == null) {
            return "<Unknown>";
        } else {
            DimletSettings settings = DimletDictionary.get().getSettings(key);
            if (settings != null) {
                DimletRarity rarity = settings.getRarity();
                return rarity.name();
            } else {
                return "<Unknown>";
            }
        }
    }

    // Use client side!
    public static String getDimletCost(ItemStack stack) {
        DimletKey key = getDimletKey(stack);
        if (key == null) {
            return "<Unknown>";
        } else {
            DimletSettings settings = DimletDictionary.get().getSettings(key);
            if (settings != null) {
                int createCost = settings.getCreateCost();
                int maintainCost = settings.getMaintainCost();
                int tickCost = settings.getTickCost();
                return "C " + createCost + ", M " + maintainCost + ", T " + tickCost;
            } else {
                return "<Unknown>";
            }
        }
    }

    private static DimletItem getDimletItem(DimletType type) {
        return switch (type) {
            case TERRAIN -> DimletModule.TERRAIN_DIMLET.get();
            case ATTRIBUTE -> DimletModule.ATTRIBUTE_DIMLET.get();
            case BIOME_CONTROLLER -> DimletModule.BIOME_CONTROLLER_DIMLET.get();
            case BIOME_CATEGORY -> DimletModule.BIOME_CATEGORY_DIMLET.get();
            case BIOME -> DimletModule.BIOME_DIMLET.get();
            case FEATURE -> DimletModule.FEATURE_DIMLET.get();
            case STRUCTURE -> DimletModule.STRUCTURE_DIMLET.get();
            case TIME -> DimletModule.TIME_DIMLET.get();
            case BLOCK -> DimletModule.BLOCK_DIMLET.get();
            case TAG -> DimletModule.TAG_DIMLET.get();
            case FLUID -> DimletModule.FLUID_DIMLET.get();
            case DIGIT -> DimletModule.DIGIT_DIMLET.get();
            case ADMIN -> DimletModule.ADMIN_DIMLET.get();
            case SKY -> DimletModule.SKY_DIMLET.get();
        };
    }

    private static DimletItem getEmptyDimletItem(DimletType type) {
        return switch (type) {
            case TERRAIN -> DimletModule.EMPTY_TERRAIN_DIMLET.get();
            case ATTRIBUTE -> DimletModule.EMPTY_ATTRIBUTE_DIMLET.get();
            case BIOME_CONTROLLER -> DimletModule.EMPTY_BIOME_CONTROLLER_DIMLET.get();
            case BIOME_CATEGORY -> DimletModule.EMPTY_BIOME_CATEGORY_DIMLET.get();
            case BIOME -> DimletModule.EMPTY_BIOME_DIMLET.get();
            case FEATURE -> DimletModule.EMPTY_FEATURE_DIMLET.get();
            case STRUCTURE -> DimletModule.EMPTY_STRUCTURE_DIMLET.get();
            case TIME -> DimletModule.EMPTY_TIME_DIMLET.get();
            case BLOCK -> DimletModule.EMPTY_BLOCK_DIMLET.get();
            case TAG -> DimletModule.EMPTY_TAG_DIMLET.get();
            case SKY -> DimletModule.EMPTY_SKY_DIMLET.get();
            case FLUID -> DimletModule.EMPTY_FLUID_DIMLET.get();
            case DIGIT -> null;
            case ADMIN -> null;
        };
    }

    @Nullable
    public static DimletKey getDimletKey(ItemStack stack) {
        if (stack.getItem() instanceof DimletItem) {
            DimletType type = ((DimletItem) stack.getItem()).getType();
            if (type != null) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    String name = tag.getString("name");
                    return new DimletKey(type, name);
                }
            }
        }
        return null;
    }

    @Nonnull
    public static ItemStack getDimletStack(DimletKey key) {
        DimletItem item = getDimletItem(key.type());
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("name", key.key());
        return stack;
    }

    @Nonnull
    public static ItemStack getEmptyDimletStack(DimletType type) {
        DimletItem item = getEmptyDimletItem(type);
        return new ItemStack(item);
    }

    public static ItemStack getNeededMemoryPart(DimletKey key) {
        DimletSettings settings = DimletDictionary.get().getSettings(key);
        if (settings == null) {
            return ItemStack.EMPTY;
        }
        DimletRarity rarity = settings.getRarity();
        if (rarity == null) {
            return ItemStack.EMPTY;
        }
        return switch (rarity) {
            case COMMON -> new ItemStack(DimletModule.PART_MEMORY_0.get());
            case UNCOMMON -> new ItemStack(DimletModule.PART_MEMORY_1.get());
            case RARE -> new ItemStack(DimletModule.PART_MEMORY_2.get());
            case LEGENDARY -> new ItemStack(DimletModule.PART_MEMORY_3.get());
        };
    }

    public static ItemStack getNeededEnergyPart(DimletKey key) {
        DimletSettings settings = DimletDictionary.get().getSettings(key);
        if (settings == null) {
            return ItemStack.EMPTY;
        }
        DimletRarity rarity = settings.getRarity();
        if (rarity == null) {
            return ItemStack.EMPTY;
        }
        return switch (rarity) {
            case COMMON -> new ItemStack(DimletModule.PART_ENERGY_0.get());
            case UNCOMMON -> new ItemStack(DimletModule.PART_ENERGY_1.get());
            case RARE -> new ItemStack(DimletModule.PART_ENERGY_2.get());
            case LEGENDARY -> new ItemStack(DimletModule.PART_ENERGY_3.get());
        };
    }

    public static ItemStack getNeededEssence(DimletKey key, @Nonnull DimletSettings settings) {
        if (!settings.getEssence().isEmpty()) {
            return settings.getEssence();
        }
        return switch (key.type()) {
            case TERRAIN -> ItemStack.EMPTY;
            case ATTRIBUTE -> ItemStack.EMPTY;
            case BIOME_CONTROLLER -> ItemStack.EMPTY;
            case BIOME_CATEGORY -> ItemStack.EMPTY;
            case BIOME -> new ItemStack(EssencesModule.BIOME_ABSORBER_ITEM.get());
            case FEATURE -> ItemStack.EMPTY;
            case STRUCTURE -> new ItemStack(EssencesModule.STRUCTURE_ABSORBER_ITEM.get());
            case TIME -> ItemStack.EMPTY;
            case BLOCK -> new ItemStack(EssencesModule.BLOCK_ABSORBER_ITEM.get());
            case TAG -> ItemStack.EMPTY;
            case FLUID -> new ItemStack(EssencesModule.FLUID_ABSORBER_ITEM.get());
            case DIGIT -> ItemStack.EMPTY;
            case ADMIN -> ItemStack.EMPTY;
            case SKY -> ItemStack.EMPTY;
        };
    }


    // Use client side!
    public static String getReadableName(DimletKey dimletKey) {
        return switch (dimletKey.type()) {
            case TERRAIN -> dimletKey.key().toLowerCase();
            case ATTRIBUTE -> dimletKey.key().toLowerCase();
            case BIOME_CONTROLLER -> dimletKey.key().toLowerCase();
            case BIOME_CATEGORY -> dimletKey.key().toLowerCase();
            case BIOME -> getReadableNameBiome(dimletKey);
            case SKY -> dimletKey.key().toLowerCase();
            case FEATURE -> dimletKey.key().toLowerCase();
            case STRUCTURE -> new ResourceLocation(dimletKey.key()).getPath();
            case TIME -> dimletKey.key().toLowerCase();
            case BLOCK -> getReadableNameBlock(dimletKey);
            case TAG -> dimletKey.key().toLowerCase();
            case FLUID -> getReadableNameFluid(dimletKey);
            case DIGIT -> dimletKey.key();
            case ADMIN -> dimletKey.key();
        };
    }

    @NotNull
    private static String getReadableNameFluid(DimletKey dimletKey) {
        Fluid fluid = Tools.getFluid(new ResourceLocation(dimletKey.key()));
        if (fluid != null) {
            String modName = Tools.getModName(fluid);
            if ("minecraft".equalsIgnoreCase(modName)) {
                return I18n.get(fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
            } else {
                return I18n.get(fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId())
                        + " (" + modName + ")";
            }
        }
        return "<Invalid " + dimletKey.key() + ">";
    }

    @NotNull
    private static String getReadableNameBlock(DimletKey dimletKey) {
        Block block = Tools.getBlock(new ResourceLocation(dimletKey.key()));
        if (block != null) {
            String modName = Tools.getModName(block);
            if ("minecraft".equalsIgnoreCase(modName)) {
                return I18n.get(block.getDescriptionId());
            } else {
                return I18n.get(block.getDescriptionId()) + " (" + modName + ")";
            }
        }
        return "<Invalid " + dimletKey.key() + ">";
    }

    @NotNull
    private static String getReadableNameBiome(DimletKey dimletKey) {
        ResourceLocation id = new ResourceLocation(dimletKey.key());
        String trans = "biome." + id.getNamespace() + "." + id.getPath();
        return I18n.get(trans);
    }

    public static boolean isFullEssence(ItemStack stack, ItemStack desired, String desiredKey) {
        if (ItemStack.isSameItem(stack, desired)) {
            if (stack.getItem() == EssencesModule.BIOME_ABSORBER_ITEM.get()) {
                String biome = BiomeAbsorberTileEntity.getBiome(stack);
                if (Objects.equals(desiredKey, biome)) {
                    return BiomeAbsorberTileEntity.getProgress(stack) >= 100;
                }
            } else if (stack.getItem() == EssencesModule.BLOCK_ABSORBER_ITEM.get()) {
                String block = BlockAbsorberTileEntity.getBlock(stack);
                if (Objects.equals(desiredKey, block)) {
                    return BlockAbsorberTileEntity.getProgress(stack) >= 100;
                }
            } else if (stack.getItem() == EssencesModule.FLUID_ABSORBER_ITEM.get()) {
                String fluid = FluidAbsorberTileEntity.getFluid(stack);
                if (Objects.equals(desiredKey, fluid)) {
                    return FluidAbsorberTileEntity.getProgress(stack) >= 100;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean isOwnerDimlet(DimletKey dimletKey) {
        return dimletKey != null && dimletKey.type() == DimletType.ADMIN && dimletKey.key().equals(AdminDimletType.OWNER.name().toLowerCase());
    }

    /// Return true if this dimlet can exist (refers to an existing block/biome/...)
    public static boolean isValidDimlet(RegistryAccess access, DimletKey key) {
        return switch (key.type()) {
            case BIOME -> isValidBiome(access, key);
            case BLOCK -> isValidBlock(key);
            case FLUID -> isValidFluid(key);
            case ADMIN -> isValidAttribute(key);
            default -> true;
        };
    }

    private static boolean isValidAttribute(DimletKey key) {
        if ("cities".equals(key.key())) {
            return LostCityCompat.hasLostCities();
        }
        return true;
    }

    private static boolean isValidBiome(RegistryAccess access, DimletKey key) {
        return access.registry(Registries.BIOME).get().containsKey(new ResourceLocation(key.key()));
//        return ForgeRegistries.BIOMES.getValue(new ResourceLocation(key.key())) != null;
    }

    private static boolean isValidBlock(DimletKey key) {
        Block value = Tools.getBlock(new ResourceLocation(key.key()));
        return value != null && value != Blocks.AIR;
    }

    private static boolean isValidFluid(DimletKey key) {
        Fluid fluid = Tools.getFluid(new ResourceLocation(key.key()));
        return fluid != null && fluid.defaultFluidState().createLegacyBlock().getBlock() != Blocks.AIR;
    }
}
