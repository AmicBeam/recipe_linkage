package com.amicbeam.recipelinkage.client;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

public final class JeiBridge {
    private JeiBridge() {
    }

    public static boolean tryShowRecipes(ItemStack stack, int keyCode, int scanCode) {
        if (stack.isEmpty() || !ModList.get().isLoaded("jei")) {
            return false;
        }
        try {
            return com.amicbeam.recipelinkage.compat.jei.RecipeLinkageJeiPlugin.tryShowRecipes(stack, keyCode, scanCode);
        } catch (LinkageError ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return keyCode == GLFW.GLFW_KEY_R && false;
        }
    }
}

