package com.godspark.client.ui.widget;

import com.godspark.client.ui.GodsparkScreen;
import com.godspark.client.ui.GodsparkTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class GodsparkTabButtonWidget extends AbstractButton {

    private final GodsparkTab tab;
    private final Supplier<GodsparkTab> activeSupplier;
    private final Consumer<GodsparkTab> onSelect;

    public GodsparkTabButtonWidget(int x, int y, int w, int h, GodsparkTab tab,
                                   Supplier<GodsparkTab> activeSupplier,
                                   Consumer<GodsparkTab> onSelect) {
        super(x, y, w, h, Component.literal(tab.getDisplayName()));
        this.tab = tab;
        this.activeSupplier = activeSupplier;
        this.onSelect = onSelect;
    }

    @Override
    public void onPress() {
        onSelect.accept(tab);
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        boolean active = activeSupplier.get() == tab;
        boolean hovered = isHovered();

        int bg = active ? GodsparkScreen.COLOR_ACCENT
               : hovered ? 0xFF35355A : GodsparkScreen.COLOR_PANEL;
        int border = active ? 0xFF7DB6E8 : GodsparkScreen.COLOR_BORDER;

        gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bg);
        gg.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(), border);

        int textColor = active ? 0xFFFFFFFF : GodsparkScreen.COLOR_TEXT;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textW = font.width(getMessage());
        gg.drawString(font, getMessage(),
            getX() + (getWidth() - textW) / 2,
            getY() + (getHeight() - 8) / 2,
            textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}