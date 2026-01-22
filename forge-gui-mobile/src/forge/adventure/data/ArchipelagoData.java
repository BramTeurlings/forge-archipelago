package forge.adventure.data;

import forge.adventure.scene.TileMapScene;

import java.util.HashMap;
import java.util.Map;

// This class will keep track of data relevant for the Archipelago implementation
// Todo: Persist and load this data inside/from the user's save file.
public class ArchipelagoData {
    private static ArchipelagoData instance = null;
    private Map<String, Integer> completedTownInnEvents = new HashMap<>();

    public ArchipelagoData() {
        // Todo: Get randomizer related adventure progress data from save file here.
        // Todo: Get archipelago progress data from save file here.
    }

    public static ArchipelagoData getInstance() {
        return instance == null ? instance = new ArchipelagoData() : instance;
    }

    public void incrementCompletedTownInnEvents() {
        completedTownInnEvents.merge(TileMapScene.instance().rootPoint.getDisplayName(), 1, Integer::sum);
    }
}
