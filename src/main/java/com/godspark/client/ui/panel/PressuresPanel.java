package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.client.ui.GodsparkScreen;
import com.godspark.network.payload.UiColonyEntry;
import com.godspark.network.payload.UiPressureEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PressuresPanel extends PanelBase {

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                       int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        drawHeader(gg, font, "Pressures (base -> effective)", x, y);

        if (snap == null || snap.pressures().isEmpty()) {
            drawDim(gg, font, "No pressure data.", x, y + 14);
            return;
        }

        int selectedColonyId = ensureValidSelection(snap);

        Map<Integer, List<UiPressureEntry>> grouped = new TreeMap<>();
        for (UiPressureEntry p : snap.pressures()) {
            if (selectedColonyId != -1 && p.colonyId() != selectedColonyId) continue;
            grouped.computeIfAbsent(p.colonyId(), k -> new ArrayList<>()).add(p);
        }
        if (grouped.isEmpty()) {
            drawDim(gg, font, "No pressure data for this colony.", x, y + 14);
            return;
        }

        int row = y + 16;
        contentHeight = 0;

        enableScissor(gg, x, y + 16, width, height - 16);
        row -= scrollOffset;

        for (Map.Entry<Integer, List<UiPressureEntry>> entry : grouped.entrySet()) {
            String colonyName = findColonyName(snap, entry.getKey());
            drawText(gg, font, colonyName + " (#" + entry.getKey() + ")", x, row);
            row += 12;

            for (UiPressureEntry p : entry.getValue()) {
                drawPressureBar(gg, font, p, x, row, width);
                row += 14;
                contentHeight += 14;
            }
            row += 6;
            contentHeight += 18;
        }

        disableScissor(gg);
    }

    private String findColonyName(UiSnapshot snap, int colonyId) {
        return snap.colonies().stream()
            .filter(c -> c.colonyId() == colonyId)
            .map(UiColonyEntry::name)
            .findFirst().orElse("Colony");
    }

    private void drawPressureBar(GuiGraphics gg, Font font, UiPressureEntry p,
                                int x, int y, int width) {
        int labelW = 70;
        int barX = x + labelW;
        int barW = Math.max(40, width - labelW - 70);
        int barH = 8;

        drawText(gg, font, p.pressureType(), x, y);

        gg.fill(barX, y, barX + barW, y + barH, GodsparkScreen.COLOR_PANEL);
        int basePx = (int) (barW * (p.baseValue() / 100.0));
        int effPx = (int) (barW * (p.effectiveValue() / 100.0));

        gg.fill(barX, y, barX + basePx, y + barH, 0xFF555577);
        int color = p.effectiveValue() >= 70 ? 0xFFD96B6B
                  : p.effectiveValue() >= 40 ? 0xFFD9C76B
                  : 0xFF6BD98E;
        gg.fill(barX, y, barX + effPx, y + barH, color);

        String value = p.baseValue() + " -> " + p.effectiveValue();
        gg.drawString(font, value, barX + barW + 6, y, GodsparkScreen.COLOR_TEXT, false);
    }
}