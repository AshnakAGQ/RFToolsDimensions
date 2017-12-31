package mcjty.rftoolsdim.dimensions.dimlets.types;

import mcjty.lib.varia.BlockTools;
import mcjty.rftoolsdim.blocks.ModBlocks;
import mcjty.rftoolsdim.config.Settings;
import mcjty.rftoolsdim.config.WorldgenConfiguration;
import mcjty.rftoolsdim.dimensions.DimensionInformation;
import mcjty.rftoolsdim.dimensions.dimlets.DimletKey;
import mcjty.rftoolsdim.dimensions.dimlets.DimletObjectMapping;
import mcjty.rftoolsdim.dimensions.dimlets.DimletRandomizer;
import mcjty.rftoolsdim.dimensions.dimlets.KnownDimletConfiguration;
import mcjty.rftoolsdim.dimensions.types.PatreonType;
import mcjty.rftoolsdim.dimensions.types.TerrainType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TerrainDimletType implements IDimletType {
    private static final String CATEGORY_TYPE = "type_terrain";

    private static float materialCreationCostFactor = 5.0f;
    private static float liquidCreationCostFactor = 5.0f;
    private static float materialMaintenanceCostFactor = 5.0f;
    private static float liquidMaintenanceCostFactor = 5.0f;
    private static float materialTickCostFactor = 2.0f;
    private static float liquidTickCostFactor = 2.0f;


    @Override
    public String getName() {
        return "Terrain";
    }

    @Override
    public String getOpcode() {
        return "T";
    }

    @Override
    public String getTextureName() {
        return "terrainDimlet";
    }

    @Override
    public void setupFromConfig(Configuration cfg) {
        cfg.addCustomCategoryComment(CATEGORY_TYPE, "Settings for the terrain dimlet type");
        materialCreationCostFactor = (float) cfg.get(CATEGORY_TYPE, "material.creation.factor", materialCreationCostFactor, "The cost factor for a material dimlet modifier when used in combination with this terrain").getDouble();
        liquidCreationCostFactor = (float) cfg.get(CATEGORY_TYPE, "liquid.creation.factor", liquidCreationCostFactor, "The cost factor for a liquid dimlet modifier when used in combination with this terrain").getDouble();
        materialMaintenanceCostFactor = (float) cfg.get(CATEGORY_TYPE, "material.maintenance.factor", materialMaintenanceCostFactor, "The cost factor for a material dimlet modifier when used in combination with this terrain").getDouble();
        liquidMaintenanceCostFactor = (float) cfg.get(CATEGORY_TYPE, "liquid.maintenance.factor", liquidMaintenanceCostFactor, "The cost factor for a liquid dimlet modifier when used in combination with this terrain").getDouble();
        materialTickCostFactor = (float) cfg.get(CATEGORY_TYPE, "material.tick.factor", materialTickCostFactor, "The cost factor for a material dimlet modifier when used in combination with this terrain").getDouble();
        liquidTickCostFactor = (float) cfg.get(CATEGORY_TYPE, "liquid.tick.factor", liquidTickCostFactor, "The cost factor for a liquid dimlet modifier when used in combination with this terrain").getDouble();
    }

    @Override
    public boolean isModifier() {
        return false;
    }

    @Override
    public boolean isModifiedBy(DimletType type) {
        return type == DimletType.DIMLET_MATERIAL || type == DimletType.DIMLET_LIQUID;
    }

    @Override
    public float getModifierCreateCostFactor(DimletType modifierType, DimletKey key) {
        TerrainType terrainType = DimletObjectMapping.getTerrain(key);

        if (modifierType == DimletType.DIMLET_MATERIAL) {
            return materialCreationCostFactor * terrainType.getMaterialCostFactor();
        } else if (modifierType == DimletType.DIMLET_LIQUID) {
            return liquidCreationCostFactor * terrainType.getLiquidCostFactor();
        } else {
            return 1.0f;
        }
    }

    @Override
    public float getModifierMaintainCostFactor(DimletType modifierType, DimletKey key) {
        TerrainType terrainType = DimletObjectMapping.getTerrain(key);

        if (modifierType == DimletType.DIMLET_MATERIAL) {
            return materialMaintenanceCostFactor * terrainType.getMaterialCostFactor();
        } else if (modifierType == DimletType.DIMLET_LIQUID) {
            return liquidMaintenanceCostFactor * terrainType.getLiquidCostFactor();
        } else {
            return 1.0f;
        }
    }

    @Override
    public float getModifierTickCostFactor(DimletType modifierType, DimletKey key) {
        TerrainType terrainType = DimletObjectMapping.getTerrain(key);

        if (modifierType == DimletType.DIMLET_MATERIAL) {
            return materialTickCostFactor * terrainType.getMaterialCostFactor();
        } else if (modifierType == DimletType.DIMLET_LIQUID) {
            return liquidTickCostFactor * terrainType.getLiquidCostFactor();
        } else {
            return 1.0f;
        }
    }

    @Override
    public boolean isInjectable(DimletKey key) {
        return false;
    }

    @Override
    public void inject(DimletKey key, DimensionInformation dimensionInformation) {

    }

    @Override
    public void constructDimension(List<Pair<DimletKey, List<DimletKey>>> dimlets, Random random, DimensionInformation dimensionInformation) {
        dimlets = DimensionInformation.extractType(DimletType.DIMLET_TERRAIN, dimlets);
        List<DimletKey> modifiers;
        TerrainType terrainType = TerrainType.TERRAIN_VOID;
        if (dimlets.isEmpty()) {
            // Pick a random terrain type with a seed that is generated from all the
            // dimlets so we always get the same random value for these dimlets.
            DimletKey key = DimletRandomizer.getRandomTerrain(random);
            if (key != null) {
                dimensionInformation.updateCostFactor(key);
                terrainType = DimletObjectMapping.getTerrain(key);
            }
            modifiers = Collections.emptyList();
        } else {
            int index = random.nextInt(dimlets.size());
            DimletKey key = dimlets.get(index).getLeft();
            terrainType = DimletObjectMapping.getTerrain(key);
            modifiers = dimlets.get(index).getRight();
        }

        List<IBlockState> blocks = new ArrayList<>();
        List<Block> fluids = new ArrayList<>();
        DimensionInformation.getMaterialAndFluidModifiers(modifiers, blocks, fluids);
        dimensionInformation.setTerrainType(terrainType);

        IBlockState baseBlockForTerrain;
        if (dimensionInformation.isPatreonBitSet(PatreonType.PATREON_LAYEREDMETA)) {
            baseBlockForTerrain = Blocks.STONE.getDefaultState();
//            baseBlockForTerrain = new BlockMeta(Blocks.WOOL, 127);
            // @todo
        } else {
            if (!blocks.isEmpty()) {
                baseBlockForTerrain = blocks.get(random.nextInt(blocks.size()));
                if (baseBlockForTerrain == null) {
                    baseBlockForTerrain = Blocks.STONE.getDefaultState();     // This is the default in case None was specified.
                }
            } else {
                // Nothing was specified. With a relatively big chance we use stone. But there is also a chance that the material will be something else.
                // Note that in this particular case we disallow randomly selecting 'expensive' blocks like glass.
                // If the terrain type is void we always pick stone as a random material.
                if (terrainType == TerrainType.TERRAIN_VOID) {
                    baseBlockForTerrain = Blocks.STONE.getDefaultState();
                } else if (random.nextFloat() < WorldgenConfiguration.randomBaseBlockChance) {
                    DimletKey key = DimletRandomizer.getRandomMaterialBlock(random);
                    if (key != null) {
                        dimensionInformation.updateCostFactor(key);
                        baseBlockForTerrain = DimletObjectMapping.getBlock(key);
                    } else {
                        baseBlockForTerrain = Blocks.STONE.getDefaultState();
                    }
                } else {
                    baseBlockForTerrain = Blocks.STONE.getDefaultState();
                }
            }
        }
        dimensionInformation.setBaseBlockForTerrain(baseBlockForTerrain);

        Block fluidForTerrain;
        if (!fluids.isEmpty()) {
            fluidForTerrain = fluids.get(random.nextInt(fluids.size()));
            if (fluidForTerrain == null) {
                fluidForTerrain = Blocks.WATER;         // This is the default.
            }
        } else {
            // If the terrain type is void we always pick water as the random liquid.
            if (terrainType == TerrainType.TERRAIN_VOID) {
                fluidForTerrain = Blocks.WATER;
            } else if (random.nextFloat() < WorldgenConfiguration.randomOceanLiquidChance) {
                DimletKey key = DimletRandomizer.getRandomFluidBlock(random);
                if (key != null) {
                    dimensionInformation.updateCostFactor(key);
                    fluidForTerrain = DimletObjectMapping.getFluid(key);
                } else {
                    fluidForTerrain = Blocks.WATER;
                }
            } else {
                fluidForTerrain = Blocks.WATER;
            }
        }
        dimensionInformation.setFluidForTerrain(fluidForTerrain);
    }

    @Override
    public String[] getInformation() {
        return new String[] { "This affects the type of terrain", "that you will get in a dimension", "This dimlet can receive liquid and material", "modifiers which have to come in front of the terrain." };
    }


    @Override
    public DimletKey isValidEssence(ItemStack stackEssence) {
        Block essenceBlock = BlockTools.getBlock(stackEssence);

        if (essenceBlock != ModBlocks.terrainAbsorberBlock) {
            return null;
        }
        NBTTagCompound essenceCompound = stackEssence.getTagCompound();
        if (essenceCompound == null) {
            return null;
        }
        int absorbing = essenceCompound.getInteger("absorbing");
        String terrain = essenceCompound.getString("terrain");
        if (absorbing > 0 || terrain == null) {
            return null;
        }
        return findTerrainDimlet(essenceCompound);
    }

    @Override
    public ItemStack getDefaultEssence(DimletKey key) {
        return new ItemStack(ModBlocks.terrainAbsorberBlock);
    }

    private static DimletKey findTerrainDimlet(NBTTagCompound essenceCompound) {
        String terrain = essenceCompound.getString("terrain");
        DimletKey key = new DimletKey(DimletType.DIMLET_TERRAIN, terrain);
        Settings settings = KnownDimletConfiguration.getSettings(key);
        if (settings == null || !settings.isDimlet()) {
            return null;
        }
        return key;
    }

    @Override
    public DimletKey attemptDimletCrafting(ItemStack stackController, ItemStack stackMemory, ItemStack stackEnergy, ItemStack stackEssence) {
        DimletKey terrainDimlet = isValidEssence(stackEssence);
        if (terrainDimlet == null) {
            return null;
        }
        if (!DimletCraftingTools.matchDimletRecipe(terrainDimlet, stackController, stackMemory, stackEnergy)) {
            return null;
        }
        return terrainDimlet;
    }
}
