package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.network.payload.UiMemoryEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class MemoriesPanel extends PanelBase {

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                       int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        drawHeader(gg, font, "Colony Memories", x, y);

        if (snap == null || snap.memories().isEmpty()) {
            drawDim(gg, font, "No memories yet.", x, y + 14);
            return;
        }

        int selectedColonyId = ensureValidSelection(snap);

        var entries = snap.memories();
        if (selectedColonyId != -1) {
            entries = entries.stream().filter(m -> m.colonyId() == selectedColonyId).toList();
        }
        if (entries.isEmpty()) {
            drawDim(gg, font, "No memories for this colony.", x, y + 14);
            return;
        }

        int row = y + 16 - scrollOffset;
        contentHeight = entries.size() * 22;

        enableScissor(gg, x, y + 16, width, height - 16);

        for (UiMemoryEntry m : entries) {
            String title = "#" + m.colonyId() + "  " + m.memoryType()
                + " | " + m.pressureType() + " | int " + m.intensity();
            drawText(gg, font, title, x, row);
            drawDim(gg, font, "tick " + m.createdAtTick(), x, row + 10);
            row += 22;
        }

        disableScissor(gg);
    }
}