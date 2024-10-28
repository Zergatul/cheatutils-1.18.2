package com.zergatul.cheatutils.mixins.common;

import com.zergatul.cheatutils.controllers.EnchantmentScreenController;
import com.zergatul.cheatutils.utils.HumanReadableFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(EnchantmentScreen.class)
public abstract class MixinEnchantmentScreen extends AbstractContainerScreen<EnchantmentMenu> {

    private Button addDataButton;
    private Button scanButton;
    private Button powerButton;
    private Button setLastSeedButton;
    private Button setCurrentSeedButton;
    private Button crackPlayerSeedButton;
    private EditBox threadsEditBox;
    private EditBox lastEnchSeedEditBox;
    private EditBox currEnchSeedEditBox;
    private EditBox playerSeedEditBox;
    private Button findDropsButton;

    private MixinEnchantmentScreen(EnchantmentMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
    }

    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentScreen;init()V")
    private void onInit(CallbackInfo info) {
        int space = 4;
        int right = this.leftPos + this.imageWidth + space;
        int cursor = this.topPos;
        int btnWidth = 100;
        int btnHeight = 20;

        /*addRenderableWidget(new Button(right, cursor, btnWidth, btnHeight, Component.literal("Reset"), this::onResetClick));
        cursor += btnHeight + space;
        addRenderableWidget(addDataButton = new Button(right, cursor, btnWidth, btnHeight, Component.literal("Add State"), this::onAddDataClick));
        cursor += btnHeight + space;
        addRenderableWidget(scanButton = new Button(right, cursor, btnWidth, btnHeight, Component.literal("Scan"), this::onScanClick));
        cursor += btnHeight + space;
        addRenderableWidget(setLastSeedButton = new Button(right, cursor, btnWidth, btnHeight, Component.literal("Set Last Seed"), this::onSetLastSeedClick));
        cursor += btnHeight + space;
        addRenderableWidget(setCurrentSeedButton = new Button(right, cursor, btnWidth, btnHeight, Component.literal("Set Current Seed"), this::onSetCurrentSeedClick));
        cursor += btnHeight + space;
        addRenderableWidget(crackPlayerSeedButton = new Button(right, cursor, btnWidth, btnHeight, Component.literal("Crack Player Seed"), this::onCrackPlayerSeedClick));
        cursor += btnHeight + space;
        addRenderableWidget(new TextWidget(right, cursor, btnWidth, btnHeight, "Threads:"));
        cursor += btnHeight + space;
        addRenderableWidget(threadsEditBox = new EditBox(font, right, cursor, btnWidth, btnHeight, Component.literal("")));
        threadsEditBox.setValue(Integer.toString(EnchantmentScreenController.instance.getLastUsedThreads()));

        int left = this.leftPos - btnWidth - space;
        cursor = this.topPos;

        addRenderableWidget(powerButton = new Button(left, cursor, btnWidth, btnHeight, Component.literal("Power"), this::onPowerClick));
        onPowerClick(powerButton);
        cursor += btnHeight + space;
        addRenderableWidget(lastEnchSeedEditBox = new EditBox(font, left, cursor, btnWidth, btnHeight, Component.literal("")));
        cursor += btnHeight + space;
        addRenderableWidget(currEnchSeedEditBox = new EditBox(font, left, cursor, btnWidth, btnHeight, Component.literal("")));
        cursor += btnHeight + space;
        addRenderableWidget(playerSeedEditBox = new EditBox(font, left, cursor, btnWidth, btnHeight, Component.literal("")));
        cursor += btnHeight + space;
        addRenderableWidget(findDropsButton = new Button(left, cursor, btnWidth, btnHeight, Component.literal("Find drops"), this::onFindDropsClick));*/
    }

    /*@Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentScreen;containerTick()V")
    private void onContainerTick(CallbackInfo info) {
        EnchantmentScreenController controller = EnchantmentScreenController.instance;

        addDataButton.active = this.menu.costs[0] != 0;
        addDataButton.setMessage(Component.literal("Add State [" + controller.getDataCount() + "]"));

        scanButton.active = controller.getDataCount() > 0 && !controller.isFilterInProgress();
        if (controller.isFilterInProgress()) {
            scanButton.setMessage(Component.literal(String.format(Locale.ROOT, "%.3f", 100 * controller.getProgress()) + "% [" + HumanReadableFormatter.formatLong(controller.getNewSeedsCount()) + "]"));
        } else {
            scanButton.setMessage(Component.literal("Filter [" + HumanReadableFormatter.formatLong(controller.getSeedsCount()) + "]"));
        }

        setLastSeedButton.active = controller.getSeedsCount() == 1;
        setCurrentSeedButton.active = controller.getSeedsCount() == 1;
        crackPlayerSeedButton.active = controller.getLastEnchSeed() != null && controller.getCurrEnchSeed() != null;

        String value = controller.getLastEnchSeed() == null ? "" : String.format("%08X", controller.getLastEnchSeed());
        if (!lastEnchSeedEditBox.getValue().equals(value)) {
            lastEnchSeedEditBox.setValue(value);
        }

        value = controller.getCurrEnchSeed() == null ? "" : String.format("%08X", controller.getCurrEnchSeed());
        if (!currEnchSeedEditBox.getValue().equals(value)) {
            currEnchSeedEditBox.setValue(value);
        }

        value = controller.getPlayerSeed() == null ? "" : String.format("%12X", controller.getPlayerSeed());
        if (!playerSeedEditBox.getValue().equals(value)) {
            playerSeedEditBox.setValue(value);
        }
    }

    private void onResetClick(Button button) {
        EnchantmentScreenController.instance.reset();
    }

    private void onAddDataClick(Button button) {
        Integer enchantPowerBonus = getEnchantPowerBonus();
        if (enchantPowerBonus == null) {
            return;
        }

        EnchantmentScreenController.instance.addState(menu.getSlot(0).getItem(), enchantPowerBonus, menu.costs, menu.enchantClue, menu.levelClue);
    }

    private void onScanClick(Button button) {
        int threads;
        try {
            threads = Integer.parseInt(threadsEditBox.getValue());
        }
        catch (Throwable e) {
            threads = 1;
        }
        EnchantmentScreenController.instance.scan(threads);
    }

    private void onSetLastSeedClick(Button button) {
        EnchantmentScreenController.instance.setLastEnchSeed();
    }

    private void onSetCurrentSeedClick(Button button) {
        EnchantmentScreenController.instance.setCurrEnchSeed();
    }

    private void onCrackPlayerSeedClick(Button button) {
        EnchantmentScreenController.instance.crackPlayerSeed();
    }

    private void onPowerClick(Button button) {
        button.setMessage(Component.literal("Power: " + getEnchantPowerBonus()));
    }

    private void onFindDropsClick(Button button) {
        EnchantmentScreenController.instance.findDrops();
    }

    private Integer getEnchantPowerBonus() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }

        // find enchanting table in 5 block radius
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos pos = null;
        for (int dx = -5; dx <= 5 && pos == null; dx++) {
            for (int dy = -5; dy <= 5 && pos == null; dy++) {
                for (int dz = -5; dz <= 5 && pos == null; dz++) {
                    if (mc.level.getBlockState(playerPos.offset(dx, dy, dz)).getBlock() == Blocks.ENCHANTING_TABLE) {
                        pos = playerPos.offset(dx, dy, dz);
                    }
                }
            }
        }

        if (pos == null) {
            return null;
        }

        float enchantPowerBonus = 0;
        for (BlockPos delta : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(mc.level, pos, delta)) {
                //enchantPowerBonus += mc.level.getBlockState(pos.offset(delta)).getEnchantPowerBonus(mc.level, pos.offset(delta));
                throw new IllegalStateException("disabled");
            }
        }

        if (enchantPowerBonus > 15) {
            enchantPowerBonus = 15;
        }

        return (int)enchantPowerBonus;
    }*/
}