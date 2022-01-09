package mcjty.rftoolsdim.modules.dimlets.recipes;

import mcjty.lib.crafting.AbstractRecipeAdaptor;
import mcjty.rftoolsdim.modules.dimlets.DimletModule;
import mcjty.rftoolsdim.modules.dimlets.data.DimletKey;
import mcjty.rftoolsdim.modules.dimlets.data.DimletTools;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nonnull;

public class DimletRecipe extends AbstractRecipeAdaptor {

    private final DimletKey key;

    public DimletRecipe(ShapedRecipe recipe, DimletKey key) {
        super(recipe);
        this.key = key;
    }

    public DimletKey getKey() {
        return key;
    }

    @Nonnull
    @Override
    public ItemStack getResultItem() {
        return DimletTools.getDimletStack(key);
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingContainer inv) {
        return DimletTools.getDimletStack(key);
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return DimletModule.DIMLET_RECIPE_SERIALIZER.get();
    }
}
