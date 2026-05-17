package com.godspark.client.ui;

import com.godspark.client.GodsparkClientState;
import com.godspark.client.ui.panel.*;
import com.godspark.client.ui.widget.GodsparkTabButtonWidget;
import com.godspark.network.GodsparkNetwork;
import com.godspark.network.packet.RequestUiSnapshotPacket;
import com.godspark.network.payload.UiColonyEntry;
import com.godspark.network.payload.UiSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
    private int filterY;
    private long lastAutoRefreshMillis = 0L;
    private final List<FilterBtn> filterBtns = new ArrayList<>();

    private record FilterBtn(int colonyId, int x, int y, int w, int h) {}

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
        filterY = panelY + 32;
        contentY = filterY + 14;
        contentWidth = panelWidth - 16;
        contentHeight = panelHeight - (contentY - panelY) - 8;

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

        gg.drawString(this.font, "[EXPERIMENT]",
            panelX + panelWidth - 6 - this.font.width("[EXPERIMENT]"),
            panelY + 6, 0xFFFFD700, false);

        gg.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, COLOR_PANEL);

        super.render(gg, mouseX, mouseY, partialTick);

        renderColonyFilter(gg, mouseX, mouseY);

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

    private void renderColonyFilter(GuiGraphics gg, int mouseX, int mouseY) {
        UiSnapshot snap = GodsparkClientState.getSnapshot();
        filterBtns.clear();
        if (snap == null || snap.colonies().size() <= 1) {
            GodsparkClientState.setSelectedColonyId(-1);
            return;
        }
        int fx = contentX;
        int fy = filterY;
        int sel = GodsparkClientState.getSelectedColonyId();
        int allW = font.width("All") + 10;
        drawFilterBtn(gg, fx, fy, allW, 12, "All", sel == -1, mouseX, mouseY);
        filterBtns.add(new FilterBtn(-1, fx, fy, allW, 12));
        fx += allW + 4;
        for (UiColonyEntry c : snap.colonies()) {
            String name = c.name();
            if (font.width(name) > 80) name = font.plainSubstrByWidth(name, 76) + "...";
            int bw = font.width(name) + 10;
            if (fx + bw > panelX + panelWidth - 8) break;
            drawFilterBtn(gg, fx, fy, bw, 12, name, sel == c.colonyId(), mouseX, mouseY);
            filterBtns.add(new FilterBtn(c.colonyId(), fx, fy, bw, 12));
            fx += bw + 4;
        }
    }

    private void drawFilterBtn(GuiGraphics gg, int x, int y, int w, int h, String label, boolean active, int mx, int my) {
        boolean hovered = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = active ? COLOR_ACCENT : hovered ? 0xFF35355A : COLOR_BORDER;
        gg.fill(x, y, x + w, y + h, bg);
        gg.drawString(font, label, x + 5, y + 2, active ? 0xFFFFFFFF : COLOR_TEXT, false);
    }

    private boolean handleFilterClick(double mx, double my) {
        for (FilterBtn fb : filterBtns) {
            if (mx >= fb.x && mx <= fb.x + fb.w && my >= fb.y && my <= fb.y + fb.h) {
                GodsparkClientState.setSelectedColonyId(fb.colonyId);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (handleFilterClick(mouseX, mouseY)) return true;
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