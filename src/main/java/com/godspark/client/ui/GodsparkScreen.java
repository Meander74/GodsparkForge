package com.godspark.client.ui;

import com.godspark.client.GodsparkClientState;
import com.godspark.client.ui.panel.*;
import com.godspark.client.ui.widget.GodsparkTabButtonWidget;
import com.godspark.network.GodsparkNetwork;
import com.godspark.network.packet.RequestUiSnapshotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class GodsparkScreen extends Screen {

    public static final int COLOR_BG = 0xFF1A1A2E;
    public static final int COLOR_PANEL = 0xFF252540;
    public static final int COLOR_ACCENT = 0xFF4A90D9;
    public static final int COLOR_TEXT = 0xFFE0E0E0;
    public static final int COLOR_DIM = 0xFFA0A0B0;
    public static final int COLOR_BORDER = 0xFF3A3A55;

    private static final long AUTO_REFRESH_MILLIS = 3000L;

    private final Map<GodsparkTab, PanelBase> panels = new EnumMap<>(GodsparkTab.class);
    private GodsparkTab activeTab = GodsparkTab.STATUS;

    private int panelX, panelY, panelWidth, panelHeight;
    private int contentX, contentY, contentWidth, contentHeight;
    private long lastAutoRefreshMillis = 0L;

    public GodsparkScreen() {
        super(Component.literal("Godspark Dashboard"));
    }

    @Override
    protected void init() {
        super.init();

        panelWidth = Math.min(420, this.width - 40);
        panelHeight = Math.min(260, this.height - 40);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        contentX = panelX + 8;
        contentY = panelY + 36;
        contentWidth = panelWidth - 16;
        contentHeight = panelHeight - 44;

        int tabX = panelX + 8;
        int tabY = panelY + 8;
        int tabW = (panelWidth - 16) / GodsparkTab.values().length;
        int tabH = 22;

        for (GodsparkTab tab : GodsparkTab.values()) {
            GodsparkTabButtonWidget btn = new GodsparkTabButtonWidget(
                tabX, tabY, tabW - 2, tabH, tab,
                () -> activeTab,
                t -> activeTab = t
            );
            this.addRenderableWidget(btn);
            tabX += tabW;
        }

        panels.put(GodsparkTab.STATUS, new StatusPanel());
        panels.put(GodsparkTab.COLONIES, new ColoniesPanel());
        panels.put(GodsparkTab.PRESSURES, new PressuresPanel());
        panels.put(GodsparkTab.EVENTS, new EventsPanel());
        panels.put(GodsparkTab.MEMORIES, new MemoriesPanel());
        panels.put(GodsparkTab.PRAYERS, new PrayersPanel());

        requestSnapshotIfNeeded(true);
    }

    @Override
    public void tick() {
        super.tick();
        requestSnapshotIfNeeded(false);
    }

    private void requestSnapshotIfNeeded(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (now - lastAutoRefreshMillis) < AUTO_REFRESH_MILLIS) {
            return;
        }
        lastAutoRefreshMillis = now;
        int requestId = GodsparkClientState.nextRequestId();
        GodsparkNetwork.CHANNEL.sendToServer(new RequestUiSnapshotPacket(requestId));
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        gg.fill(0, 0, this.width, this.height, 0xC0000000);

        gg.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG);
        gg.fill(panelX, panelY, panelX + panelWidth, panelY + 1, COLOR_BORDER);
        gg.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, COLOR_BORDER);
        gg.fill(panelX, panelY, panelX + 1, panelY + panelHeight, COLOR_BORDER);
        gg.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BORDER);

        gg.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_PANEL);

        super.render(gg, mouseX, mouseY, partialTick);

        PanelBase panel = panels.get(activeTab);
        if (panel != null) {
            panel.render(gg, this.font, contentX + 6, contentY + 6,
                contentWidth - 12, contentHeight - 12, mouseX, mouseY);
        }

        String footer;
        if (GodsparkClientState.getSnapshot() == null) {
            footer = "Loading snapshot...";
        } else {
            long age = (System.currentTimeMillis() - GodsparkClientState.getLastSnapshotMillis()) / 1000L;
            footer = "Snapshot age: " + age + "s  |  Auto-refresh ~3s  |  ESC to close";
        }
        gg.drawString(this.font, footer, panelX + 8,
            panelY + panelHeight - 12, COLOR_DIM, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        PanelBase panel = panels.get(activeTab);
        if (panel != null && panel.mouseScrolled(mouseX, mouseY, delta,
            contentX + 6, contentY + 6, contentWidth - 12, contentHeight - 12)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PanelBase panel = panels.get(activeTab);
        if (panel != null && panel.mouseClicked(mouseX, mouseY, button,
            contentX + 6, contentY + 6, contentWidth - 12, contentHeight - 12)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}