package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.network.payload.UiEventEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class EventsPanel extends PanelBase {

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                       int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        drawHeader(gg, font, "Active Events", x, y);

        if (snap == null || snap.events().isEmpty()) {
            drawDim(gg, font, "No active events.", x, y + 14);
            return;
        }

        int selectedColonyId = ensureValidSelection(snap);

        var entries = snap.events();
        if (selectedColonyId != -1) {
            entries = entries.stream().filter(e -> e.colonyId() == selectedColonyId).toList();
        }
        if (entries.isEmpty()) {
            drawDim(gg, font, "No events for this colony.", x, y + 14);
            return;
        }

        int row = y + 16 - scrollOffset;
        contentHeight = entries.size() * 28;

        enableScissor(gg, x, y + 16, width, height - 16);

        for (UiEventEntry e : entries) {
            int badgeColor = switch (e.severity()) {
                case "HIGH" -> 0xFFD96B6B;
                case "MEDIUM" -> 0xFFD9C76B;
                default -> 0xFF6BD98E;
            };
            gg.fill(x, row, x + 4, row + 24, badgeColor);

            drawText(gg, font, e.storyEventType() + "  [#" + e.colonyId() + "]", x + 8, row);
            String line2 = e.pressureType() + " | " + e.severity()
                + " | " + e.state() + " | persist " + e.persistenceCount();
            drawDim(gg, font, line2, x + 8, row + 12);

            row += 28;
        }

        disableScissor(gg);
    }
}