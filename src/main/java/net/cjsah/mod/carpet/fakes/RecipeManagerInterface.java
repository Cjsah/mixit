package net.cjsah.mod.carpet.fakes;

import java.util.List;

import net.cjsah.mod.carpet.helpers.HopperCounter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public interface RecipeManagerInterface
{
    /**
     * Gets all the recipes for a given item. Also used for {@link HopperCounter#guessColor} to guess the
     * colour of an item to display it prettily
     */
    List<Recipe<?>> getAllMatching(RecipeType<?> type, ResourceLocation output);
}