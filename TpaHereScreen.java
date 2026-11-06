package pl.tpahere.auto;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Proste okno ustawien (klawisz K). Nie ma tu pola na nick:
 * mod wysyla komende do kazdego, kto napisze na czacie.
 */
public class TpaHereScreen extends Screen {
    private ButtonWidget toggleButton;
    private ButtonWidget cooldownButton;
    private ButtonWidget delayButton;

    public TpaHereScreen() {
        super(Text.literal("TPAHere Auto"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 55;
        Config cfg = TpaHereClient.CONFIG;

        toggleButton = ButtonWidget.builder(Text.empty(), b -> {
            cfg.enabled = !cfg.enabled;
            if (!cfg.enabled) TpaHereClient.clearState();
            cfg.save();
            refresh();
        }).dimensions(cx - 100, y, 200, 20).build();
        this.addDrawableChild(toggleButton);

        cooldownButton = ButtonWidget.builder(Text.empty(), b -> {
            // 30 -> 60 -> 120 -> 300 -> 30 ...
            int c = cfg.cooldownSeconds;
            cfg.cooldownSeconds = c < 60 ? 60 : c < 120 ? 120 : c < 300 ? 300 : 30;
            cfg.save();
            refresh();
        }).dimensions(cx - 100, y + 28, 200, 20).build();
        this.addDrawableChild(cooldownButton);

        delayButton = ButtonWidget.builder(Text.empty(), b -> {
            // 1s -> 2s -> 3s -> 5s -> 1s ...
            int d = cfg.delayTicks;
            cfg.delayTicks = d < 40 ? 40 : d < 60 ? 60 : d < 100 ? 100 : 20;
            cfg.save();
            refresh();
        }).dimensions(cx - 100, y + 56, 200, 20).build();
        this.addDrawableChild(delayButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zamknij"), b -> this.close())
                .dimensions(cx - 100, y + 90, 200, 20).build());

        refresh();
    }

    private void refresh() {
        Config cfg = TpaHereClient.CONFIG;
        toggleButton.setMessage(Text.literal("Wysylanie do wszystkich piszacych: " + (cfg.enabled ? "WLACZONE" : "WYLACZONE")));
        cooldownButton.setMessage(Text.literal("Cooldown na gracza: " + cfg.cooldownSeconds + " s"));
        delayButton.setMessage(Text.literal("Odstep miedzy komendami: " + (cfg.delayTicks / 20.0) + " s"));
    }
}
