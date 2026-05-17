package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.network.payload.UiSnapshot;
import com.godspark.network.payload.UiStatusInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class StatusPanel extends PanelBase {

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                       int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        int row = y;

        drawHeader(gg, font, "Godspark Status", x, row);
        row += 14;

        if (snap == null) {
            drawDim(gg, font, "Waiting for server snapshot...", x, row);
            return;
        }

        UiStatusInfo s = snap.status();

        drawText(gg, font, "Mod active:           " + bool(s.modActive()), x, row);
        row += 12;
        drawText(gg, font, "MineColonies loaded:  " + bool(s.mineColoniesDetected()), x, row);
        row += 12;
        drawText(gg, font, "AI enabled:           " + bool(s.aiEnabled()), x, row);
        row += 12;
        row += 6;
        drawText(gg, font, "Colonies observed:    " + s.colonyCount(), x, row);
        row += 12;
        drawText(gg, font, "Active events:        " + s.activeEventCount(), x, row);
        row += 12;
        drawText(gg, font, "Total memories:       " + s.totalMemories(), x, row);
        row += 12;
        drawText(gg, font, "Total prayers:        " + s.totalPrayers(), x, row);
        row += 12;
        row += 6;
        drawDim(gg, font, "Server tick: " + snap.serverTick(), x, row);
    }

    private String bool(boolean v) {
        return v ? "yes" : "no";
    }
}