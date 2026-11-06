package pl.tpahere.auto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("tpahere_auto.json");

    public boolean enabled = false;
    /** Komenda; {player} = nick gracza, ktory napisal na czacie. Bez ukosnika na poczatku. */
    public String command = "tpahere {player}";
    /** Ile sekund czekac zanim ten sam gracz dostanie komende ponownie. */
    public int cooldownSeconds = 60;
    /** Odstep miedzy kolejnymi komendami (w tickach, 20 = 1 s). */
    public int delayTicks = 40;

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) {
                    if (c.command == null || !c.command.contains("{player}")) c.command = "tpahere {player}";
                    return c;
                }
            }
        } catch (Exception ignored) {}
        return new Config();
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception ignored) {}
    }
}
