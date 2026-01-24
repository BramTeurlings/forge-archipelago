package forge.adventure.data;

import forge.adventure.scene.TileMapScene;
import forge.adventure.util.AdventureQuestEvent;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.*;

// This class will keep track of data relevant for the Archipelago implementation
// Persists and loads data inside/from the user's save file.
// Todo: Persist (mini-)boss encounter victories.
public class ArchipelagoData implements SaveFileContent {
    private static ArchipelagoData instance = null;
    private final Map<String, Long> completedTownInnEvents = new HashMap<>();
    private final Map<String, Long> completedTownQuests = new HashMap<>();
    private final Map<String, Long> cardsEarnedByRarity = new HashMap<>();
    private final Map<String, Long> itemsGainedById = new HashMap<>();
    private final Map<String, Long> packsEarnedBySet = new HashMap<>();
    private final Set<String> bossesDefeatedByName = new HashSet<>();
    private final Set<String> miniBossesDefeatedByName = new HashSet<>();
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
        completedTownInnEvents.merge(townName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: INN EVENT COMPLETION DETECTED: " + townName + " - " + completedTownInnEvents.get(townName));
    }

    public void addCompletedQuests(AdventureQuestEvent event) {
        String townName = event.poi.getDisplayName();
        completedTownQuests.merge(townName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + completedTownQuests.get(townName));
    }

    public void addCardByRarity(String rarity) {
        cardsEarnedByRarity.merge(rarity, 1L, Long::sum);
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    public void addItem(String itemName) {
        itemsGainedById.merge(itemName, 1L, Long::sum);
    }

    public void addPack(String setName) {
        packsEarnedBySet.merge(setName, 1L, Long::sum);
    }

    public void addMaxLife(int amount) {
        totalExtraMaxLifeEarned += amount;
    }

    public void addShards(int amount) {
        totalShardsEarned += amount;
    }

    // Note that the name of a boss is not unique so we'll need to filter from all enemies which have a `boss` value of `true`.
    // Returns `true` if the boss was not already defeated before.
    public boolean addMiniBossDefeated(String miniBossName) {
        return miniBossesDefeatedByName.add(miniBossName);
    }
    public boolean addBossDefeated(String bossName) {
        return bossesDefeatedByName.add(bossName);
    }

    // Helper functions for saving and loading
    private static void saveStringSet(SaveFileData parent, String key, Set<String> set) {
        parent.storeObject(key, set.toArray(new String[0]));
    }

    private static void loadStringSet(SaveFileData parent, String key, Set<String> set) {
        set.clear();

        if (!parent.containsKey(key)) return;

        String[] values = (String[]) parent.readObject(key);
        Collections.addAll(set, values);
    }

    private static void saveStringLongMap(SaveFileData parent, String prefix, Map<String, Long> map) {
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

    private static void loadStringLongMap(SaveFileData parent, String prefix, Map<String, Long> map) {
        map.clear();

        if (!parent.containsKey(prefix + "_keys")) return;

        String[] keys = (String[]) parent.readObject(prefix + "_keys");

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = parent.readSubData(prefix + "_value_" + i);
            if (valueData != null) {
                map.put(keys[i], valueData.readLong("value"));
            }
        }
    }

    @Override
    public void load(SaveFileData data) {
        if (data == null) {
            return;
        }

        loadStringLongMap(data, "townEvents", completedTownInnEvents);
        loadStringLongMap(data, "townQuests", completedTownQuests);
        loadStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        loadStringLongMap(data, "items", itemsGainedById);
        loadStringLongMap(data, "packs", packsEarnedBySet);
        loadStringSet(data, "bossesDefeated", bossesDefeatedByName);
        loadStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);

        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();

        saveStringLongMap(data, "townEvents", completedTownInnEvents);
        saveStringLongMap(data, "townQuests", completedTownQuests);
        saveStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        saveStringLongMap(data, "items", itemsGainedById);
        saveStringLongMap(data, "packs", packsEarnedBySet);
        saveStringSet(data, "bossesDefeated", bossesDefeatedByName);
        saveStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);

        data.store("totalGold", totalGoldEarned);
        data.store("extraLife", totalExtraMaxLifeEarned);
        data.store("shards", totalShardsEarned);

        return data;
    }
}
