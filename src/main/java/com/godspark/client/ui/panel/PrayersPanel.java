package com.godspark.client.ui.panel;

import com.godspark.client.GodsparkClientState;
import com.godspark.network.payload.UiPrayerEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class PrayersPanel extends PanelBase {

    @Override
    public void render(GuiGraphics gg, Font font, int x, int y, int width, int height,
                        int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        drawHeader(gg, font, "Prayers", x, y);

        if (snap == null || snap.prayers().isEmpty()) {
            drawDim(gg, font, "No active prayers.", x, y + 14);
            return;
        }

        int selectedColonyId = ensureValidSelection(snap);

        var entries = snap.prayers();
        if (selectedColonyId != -1) {
            entries = entries.stream().filter(p -> p.colonyId() == selectedColonyId).toList();
        }
        if (entries.isEmpty()) {
            drawDim(gg, font, "No prayers for this colony.", x, y + 14);
            return;
        }

        int row = y + 16 - scrollOffset;
        contentHeight = entries.size() * 30;

        enableScissor(gg, x, y + 16, width, height - 16);

        for (UiPrayerEntry p : entries) {
            String head = "#" + p.colonyId() + "  [" + p.channel() + " "
                + p.prayerType() + " " + p.intensity() + "][" + p.pressureType() + "]";
            drawText(gg, font, head, x, row);
            drawDim(gg, font, truncate(p.content(), 90), x, row + 10);
            row += 30;
        }

        disableScissor(gg);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "...";
    }
}