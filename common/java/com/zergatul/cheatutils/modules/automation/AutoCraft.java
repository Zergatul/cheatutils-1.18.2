package com.zergatul.cheatutils.modules.automation;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.AutoCraftConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.mixins.common.accessors.AbstractRecipeBookScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.core.Holder;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class AutoCraft {

    public static final AutoCraft instance = new AutoCraft();

    private final Minecraft mc = Minecraft.getInstance();
    private final Logger logger = LogManager.getLogger(AutoCraft.class);
    private State state = State.NONE;

    private AutoCraft() {
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    private void onClientTickEnd() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            state = State.NONE;
            return;
        }

        AutoCraftConfig config = ConfigStore.instance.getConfig().autoCraftConfig;
        if (!config.enabled || config.items.length == 0) {
            state = State.NONE;
            return;
        }

        if (!(mc.screen instanceof CraftingScreen craftingScreen)) {
            state = State.NONE;
            return;
        }

        if (state == State.NONE) {
            state = State.START;
        }

        try {
            switch (state) {
                case START:
                    ContextMap context = SlotDisplayContext.fromLevel(mc.level);
                    StackedItemContents stackedContents = new StackedItemContents();
                    mc.player.getInventory().fillStackedContents(stackedContents);

                    RecipeDisplayEntry recipe = findRecipe(config, context, stackedContents);
                    if (recipe != null) {
                        boolean shift = recipe.resultItems(context).get(0).getItem().getDefaultMaxStackSize() > 1;
                        mc.gameMode.handlePlaceRecipe(craftingScreen.getMenu().containerId, recipe.id(), shift);
                        state = State.RECIPE_CLICKED;
                    }
                    break;

                case RECIPE_CLICKED:
                    Slot slot = craftingScreen.getMenu().slots.get(CraftingMenu.RESULT_SLOT);
                    if (slot.hasItem()) {
                        ((AbstractRecipeBookScreenAccessor) craftingScreen).slotClicked_CU(slot, 0, 0, ClickType.QUICK_MOVE);
                        state = State.RESULT_CLICKED;
                    }
                    break;

                case RESULT_CLICKED:
                    slot = craftingScreen.getMenu().slots.get(CraftingMenu.RESULT_SLOT);
                    if (!slot.hasItem()) {
                        state = State.START;
                    }
                    break;
            }
        }
        catch (Exception e) {
            logger.error(e);
            config.enabled = false;
        }
    }

    private RecipeDisplayEntry findRecipe(AutoCraftConfig config, ContextMap context, StackedItemContents stackedContents) {
        assert mc.level != null;
        assert mc.player != null;

        ImmutableList<ItemStack> inventory = new ImmutableList<>(mc.player.getInventory().items.stream().map(ItemStack::copy).toList());

        List<RecipeDisplayEntry> recipes = mc.player.getRecipeBook().getCollections().stream()
                .flatMap(c -> c.getRecipes().stream())
                .filter(r -> r.display() instanceof ShapedCraftingRecipeDisplay || r.display() instanceof ShapelessCraftingRecipeDisplay)
                .toList();

        for (Item baseItem : config.items) {
            // queue for next recipes to process
            Queue<CraftingTreeEntry> queue = new LinkedList<>();

            // add base recipes to queue
            for (RecipeDisplayEntry recipe : recipes) {
                List<ItemStack> results = recipe.resultItems(context);
                if (results.stream().anyMatch(stack -> stack.is(baseItem))) {
                    queue.add(new CraftingTreeEntry(recipe, null));
                }
            }

            while (!queue.isEmpty()) {
                CraftingTreeEntry entry = queue.poll();
                List<Item> missing = getMissingIngredients(entry, inventory);
                if (missing.isEmpty()) {
                    // found recipe we can craft
                    return entry.recipe;
                }

                // add missing items to queue
                for (Item item : missing) {
                    // find recipes
                    recipesLoop:
                    for (RecipeDisplayEntry recipe : recipes) {
                        List<ItemStack> results = recipe.resultItems(context);
                        if (results.stream().noneMatch(stack -> stack.is(item))) {
                            continue;
                        }

                        // don't use the same recipe to prevent loops
                        if (entry.has(recipe)) {
                            continue;
                        }

                        // don't use the same items to prevent loops
                        Optional<List<Ingredient>> ingredients = recipe.craftingRequirements();
                        if (ingredients.isPresent()) {
                            for (Ingredient ingredient : ingredients.get()) {
                                for (Holder<Item> holder : ingredient.items()) {
                                    if (entry.has(context, holder.get())) {
                                        continue recipesLoop;
                                    }
                                }
                            }
                        }

                        queue.add(new CraftingTreeEntry(recipe, entry));
                    }
                }
            }
        }

        return null;
    }

    private List<Item> getMissingIngredients(CraftingTreeEntry entry, ImmutableList<ItemStack> inventory) {
        Optional<List<Ingredient>> ingredients = entry.recipe.craftingRequirements();
        if (ingredients.isEmpty()) {
            return List.of();
        }

        List<Item> list = new ArrayList<>();
        for (Ingredient ingredient : ingredients.get()) {
            boolean has = false;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack itemStack = inventory.get(i);
                if (ingredient.test(itemStack)) {
                    ItemStack newItemStack = itemStack.copy();
                    newItemStack.setCount(itemStack.getCount() - 1);
                    inventory = inventory.set(i, newItemStack);
                    has = true;
                    break;
                }
            }
            if (!has) {
                for (Holder<Item> holder : ingredient.items()) {
                    if (!list.contains(holder.get())) {
                        list.add(holder.get());
                    }
                }
            }
        }
        return list;
    }

    private enum State {
        NONE,
        START,
        RECIPE_CLICKED,
        RESULT_CLICKED,
        INVALID
    }

    private record CraftingTreeEntry(RecipeDisplayEntry recipe, AutoCraft.CraftingTreeEntry parent) {

        public boolean has(RecipeDisplayEntry recipe) {
            for (CraftingTreeEntry current = this; current != null; current = current.parent) {
                if (current.recipe == recipe) {
                    return true;
                }
            }

            return false;
        }

        public boolean has(ContextMap context, Item item) {
            for (CraftingTreeEntry current = this; current != null; current = current.parent) {
                if (current.recipe.resultItems(context).stream().anyMatch(stack -> stack.is(item))) {
                    return true;
                }
            }

            return false;
        }
    }
}