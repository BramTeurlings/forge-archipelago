package forge.adventure.data;

import forge.adventure.scene.TileMapScene;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.HashMap;
import java.util.Map;

// This class will keep track of data relevant for the Archipelago implementation
// Persists and loads data inside/from the user's save file.
// Todo: Persist (mini-)boss encounter victories.
public class ArchipelagoData implements SaveFileContent {
    private static ArchipelagoData instance = null;
    private final Map<String, Integer> completedTownInnEvents = new HashMap<>();
    private final Map<String, Integer> cardsEarnedByRarity = new HashMap<>();
    private final Map<String, Integer> itemsGainedById = new HashMap<>();
    private final Map<String, Integer> packsEarnedBySet = new HashMap<>();
    private int totalGoldEarned = 0;
    private int totalExtraMaxLifeEarned = 0;
    private int totalShardsEarned = 0;

    public ArchipelagoData() {
        // Todo: Get randomizer related adventure progress data from save file here.
        // Todo: Get archipelago progress data from save file here.
        instance = this;
    }

    public static ArchipelagoData getInstance() {
        return instance == null ? instance = new ArchipelagoData() : instance;
    }

    public void addCompletedTownInnEvents() {
        String townName = TileMapScene.instance().rootPoint.getDisplayName();
        completedTownInnEvents.merge(townName, 1, Integer::sum);
        System.out.println("FORGE_ARCHIPELAGO: INN EVENT COMPLETION DETECTED: " + townName + " - " + completedTownInnEvents.get(townName));
    }

    public void addCardByRarity(String rarity) {
        cardsEarnedByRarity.merge(rarity, 1, Integer::sum);
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
    }

    public void addItem(String longId) {
        itemsGainedById.merge(longId, 1, Integer::sum);
    }

    public void addPack(String setName) {
        packsEarnedBySet.merge(setName, 1, Integer::sum);
    }

    public void addMaxLife(int amount) {
        totalExtraMaxLifeEarned += amount;
    }

    public void addShards(int amount) {
        totalShardsEarned += amount;
    }

    // Helper functions for saving and loading
    private static void saveStringIntMap(SaveFileData parent, String prefix, Map<String, Integer> map) {
        String[] keys = map.keySet().toArray(new String[0]);
        parent.storeObject(prefix + "_keys", keys);

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = new SaveFileData();
            valueData.store("value", map.get(keys[i]));
            parent.store(prefix + "_value_" + i, valueData);
            System.out.println(
                    "Saving " + prefix + ": " + map.size() + " entries"
            );
        }
    }

    private static void loadStringIntMap(SaveFileData parent, String prefix, Map<String, Integer> map) {
        map.clear();

        if (!parent.containsKey(prefix + "_keys")) return;

        String[] keys = (String[]) parent.readObject(prefix + "_keys");

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = parent.readSubData(prefix + "_value_" + i);
            if (valueData != null) {
                map.put(keys[i], valueData.readInt("value"));
            }
        }
    }

    @Override
    public void load(SaveFileData data) {
        if (data == null) {
            return;
        }

        loadStringIntMap(data, "townEvents", completedTownInnEvents);
        loadStringIntMap(data, "cardsByRarity", cardsEarnedByRarity);
        loadStringIntMap(data, "items", itemsGainedById);
        loadStringIntMap(data, "packs", packsEarnedBySet);

        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();

        saveStringIntMap(data, "townEvents", completedTownInnEvents);
        saveStringIntMap(data, "cardsByRarity", cardsEarnedByRarity);
        saveStringIntMap(data, "items", itemsGainedById);
        saveStringIntMap(data, "packs", packsEarnedBySet);

        data.store("totalGold", totalGoldEarned);
        data.store("extraLife", totalExtraMaxLifeEarned);
        data.store("shards", totalShardsEarned);

        return data;
    }
}
