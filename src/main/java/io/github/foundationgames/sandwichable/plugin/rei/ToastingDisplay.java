package io.github.foundationgames.sandwichable.plugin.rei;

import com.google.common.collect.ImmutableList;
import io.github.foundationgames.sandwichable.recipe.ToastingRecipe;
import me.shedaniel.rei.api.EntryStack;
import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;

public class ToastingDisplay implements RecipeDisplay {

    private List<List<EntryStack>> inputs;
    private List<EntryStack> results;
    private ToastingRecipe display;

    public ToastingDisplay(ToastingRecipe recipe) {
        this(recipe.getInput(), recipe.getOutput());
        this.display = recipe;
    }

    public ToastingDisplay(Ingredient input, ItemStack result) {
        ImmutableList.Builder<EntryStack> b = new ImmutableList.Builder<>();
        for(ItemStack i : input.getMatchingStacksClient()) b.add(EntryStack.create(i));
        this.inputs = ImmutableList.of(b.build());
        this.results = Collections.singletonList(EntryStack.create(result));
    }

    @Override
    public List<List<EntryStack>> getInputEntries() {
        return this.inputs;
    }

    @Override
    public List<EntryStack> getOutputEntries() {
        return this.results;
    }

    @Override
    public Identifier getRecipeCategory() {
        return SandwichableREI.TOASTING_CATEGORY;
    }
}
