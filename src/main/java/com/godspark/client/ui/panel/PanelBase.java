package com.godspark.client.ui.panel;

import com.godspark.client.ui.GodsparkScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PanelBase {

    protected int scrollOffset = 0;
    protected int contentHeight = 0;

    public abstract void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                               int mouseX, int mouseY);

    public boolean mouseScrolled(double mouseX, double mouseY, double delta,
                                 int x, int y, int width, int height) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        int step = 12;
        scrollOffset -= (int) (delta * step);
        int max = Math.max(0, contentHeight - height);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > max) scrollOffset = max;
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button,
                               int x, int y, int width, int height) {
        return false;
    }

    protected void drawHeader(GuiGraphics gg, Font font, String text, int x, int y) {
        gg.drawString(font, text, x, y, GodsparkScreen.COLOR_ACCENT, false);
    }

    protected void drawDim(GuiGraphics gg, Font font, String text, int x, int y) {
        gg.drawString(font, text, x, y, GodsparkScreen.COLOR_DIM, false);
    }

    protected void drawText(GuiGraphics gg, Font font, String text, int x, int y) {
        gg.drawString(font, text, x, y, GodsparkScreen.COLOR_TEXT, false);
    }

    protected void enableScissor(GuiGraphics gg, int x, int y, int width, int height) {
        gg.enableScissor(x, y, x + width, y + height);
    }

    protected void disableScissor(GuiGraphics gg) {
        gg.disableScissor();
    }
}