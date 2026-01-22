package forge.adventure.data;

import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.TileMapScene;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// This class will keep track of data relevant for the Archipelago implementation
// Todo: Persist and load this data inside/from the user's save file.
public class ArchipelagoData implements SaveFileContent {
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
        String townName = TileMapScene.instance().rootPoint.getDisplayName();
        completedTownInnEvents.merge(townName, 1, Integer::sum);
        System.out.println("FORGE_ARCHIPELAGO: INN EVENT COMPLETION DETECTED: " + townName + " - " + completedTownInnEvents.get(townName));
    }

    @Override
    public void load(SaveFileData data) {
        completedTownInnEvents.clear();

        if (data == null || !data.containsKey("keys")) {
            return;
        }

        String[] keys = (String[]) data.readObject("keys");

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = data.readSubData("value_" + i);
            if (valueData != null) {
                int count = valueData.readInt("count");
                completedTownInnEvents.put(keys[i], count);
            }
        }
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();

        String[] keys = completedTownInnEvents.keySet().toArray(new String[0]);
        data.storeObject("keys", keys);

        int index = 0;
        for (String key : keys) {
            SaveFileData valueData = new SaveFileData();
            valueData.store("count", completedTownInnEvents.get(key));
            data.store("value_" + index, valueData);
            index++;
        }

        return data;
    }
}
