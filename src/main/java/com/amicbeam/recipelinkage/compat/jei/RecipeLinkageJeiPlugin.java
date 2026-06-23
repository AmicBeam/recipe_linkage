package com.amicbeam.recipelinkage.compat.jei;

import com.amicbeam.recipelinkage.RecipeLinkage;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@JeiPlugin
public class RecipeLinkageJeiPlugin implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return RecipeLinkage.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static boolean tryShowRecipes(ItemStack stack, int keyCode, int scanCode) {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null || stack.isEmpty()) {
            return false;
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        boolean matchesJei = jeiRuntime.getKeyMappings().getShowRecipe().isActiveAndMatches(key);
        if (!matchesJei && keyCode != GLFW.GLFW_KEY_R) {
            return false;
        }
        var focus = jeiRuntime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack.copy());
        jeiRuntime.getRecipesGui().show(focus);
        return true;
    }
}
