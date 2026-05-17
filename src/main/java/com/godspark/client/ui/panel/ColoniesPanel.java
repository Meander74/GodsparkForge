package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.client.ui.GodsparkScreen;
import com.godspark.network.payload.UiColonyEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ColoniesPanel extends PanelBase {

    private static final int CARD_HEIGHT = 56;
    private static final int CARD_GAP = 4;

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                       int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        drawHeader(gg, font, "Colonies", x, y);

        if (snap == null || snap.colonies().isEmpty()) {
            drawDim(gg, font, "No colonies observed.", x, y + 14);
            return;
        }

        int listX = x;
        int listY = y + 16;
        int listH = height - 16;

        contentHeight = snap.colonies().size() * (CARD_HEIGHT + CARD_GAP);

        enableScissor(gg, listX, listY, width, listH);
        int row = listY - scrollOffset;

        int selected = GodsparkClientState.getSelectedColonyId();

        for (UiColonyEntry c : snap.colonies()) {
            boolean isSelected = selected == c.colonyId();
            boolean hovered = mouseX >= listX && mouseX <= listX + width
                && mouseY >= row && mouseY <= row + CARD_HEIGHT;

            int bg = isSelected ? 0xFF34537A
                   : hovered ? 0xFF2C2C4A
                   : GodsparkScreen.COLOR_PANEL;
            gg.fill(listX, row, listX + width, row + CARD_HEIGHT, bg);
            gg.fill(listX, row + CARD_HEIGHT - 1, listX + width, row + CARD_HEIGHT, GodsparkScreen.COLOR_BORDER);

            String title = c.name() + "  #" + c.colonyId() + (c.hasActiveRaid() ? "  [RAID]" : "");
            drawText(gg, font, title, listX + 6, row + 4);

            String line2 = String.format("citizens %d / %d   guards %d   happiness %.1f",
                c.citizenCount(), c.housingCapacity(), c.guardCount(), c.happiness());
            drawDim(gg, font, line2, listX + 6, row + 18);

            String line3 = String.format("food %d   industry %d   sacred %d   warehouse %d",
                c.foodBuildingCount(), c.industryBuildingCount(),
                c.sacredBuildingCount(), c.warehouseCount());
            drawDim(gg, font, line3, listX + 6, row + 32);

            row += CARD_HEIGHT + CARD_GAP;
        }

        disableScissor(gg);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                               int x, int y, int width, int height) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return false;
        }
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        if (snap == null || snap.colonies().isEmpty()) {
            return false;
        }
        int listY = y + 16;
        int listH = height - 16;
        if (mouseY < listY || mouseY > listY + listH) {
            return false;
        }
        int row = listY - scrollOffset;
        for (UiColonyEntry c : snap.colonies()) {
            if (mouseY >= row && mouseY <= row + CARD_HEIGHT) {
                GodsparkClientState.setSelectedColonyId(c.colonyId());
                return true;
            }
            row += CARD_HEIGHT + CARD_GAP;
        }
        return false;
    }
}