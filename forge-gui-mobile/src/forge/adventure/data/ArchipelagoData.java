package forge.adventure.data;

import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;
import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import io.sentry.util.CollectionUtils;

import java.util.*;

// This class will keep track of data relevant for the Archipelago implementation
// Persists and loads data inside/from the user's save file
public class ArchipelagoData implements SaveFileContent {
    private static ArchipelagoData instance = null;
    // Ask Forge for the full list of all sets
    private final CardEdition.Collection allEditions = StaticData.instance().getEditions();
    private final Iterable<CardEdition> allOrderedEditions = allEditions.getOrderedEditions();
    // Todo: This works fine for singleplayer even when updates come out but the fact that the list of all sets can grow will cause problems in Archipelago due to a variable amount of checks.
    private final Set<String> allCardSets = new HashSet<>();
    // Actual user data we want to store
    private final Map<String, Long> completedTownInnEvents = new HashMap<>();
    private final Map<String, Long> completedTownQuests = new HashMap<>();
    private final Map<String, Long> cardsEarnedByRarity = new HashMap<>();
    private final Map<String, Long> itemsGainedById = new HashMap<>();
    private final Map<String, Long> packsEarnedBySet = new HashMap<>();
    private final Set<String> cardsUnlockedByName = new HashSet<>();
    private final Set<String> setsUnlockedByCode = new HashSet<>();
    private final Set<String> bossesDefeatedByName = new HashSet<>();
    private final Set<String> miniBossesDefeatedByName = new HashSet<>();
    private int totalGoldEarned = 0;
    private int totalExtraMaxLifeEarned = 0;
    private int totalShardsEarned = 0;
    // List of unlockable checks
    // Todo: Fill list based on archipelago xml contents
    // Todo: Implement logic to divide the total amount of sets by the amountOfSetUnlockChecks and give the player an amount of checks equal to the result. Also keep track of the rest amount by using a float for the next check.
    private final int amountOfSetUnlockChecks = 100;
    private final Set<String> listOfUnlockableItems = new HashSet<>();

    public ArchipelagoData() {
        instance = this;
    }

    public static ArchipelagoData getInstance() {
        return instance == null ? instance = new ArchipelagoData() : instance;
    }

    public void unlockRandomSet() {
        // Subtract unlocked sets from full list.
        Set<String> lockedSets = new HashSet<>(allCardSets);
        lockedSets.removeAll(setsUnlockedByCode);

        // Nothing left to unlock
        if (lockedSets.isEmpty()) {
            return;
        }

        // Todo: After picking a random set, check if boosters can be generated from this set, if not, add it to the unlocked sets and generate another one.
        // Pick a random locked set
        int index = new Random().nextInt(lockedSets.size());
        String setToUnlock = null;

        int i = 0;
        for (String set : lockedSets) {
            if (i++ == index) {
                setToUnlock = set;
                break;
            }
        }

        if (setToUnlock != null) {
            addSetUnlockedByCode(setToUnlock);
            String setUnlockedText = "FORGE_ARCHIPELAGO: CARD SET REWARD: " + setToUnlock;
            // Some sets don't have booster packs such as full-art land sets (P23).
            var booster = StaticData.instance().getBoosters().get(setToUnlock);
            if (booster != null) {
                Current.player().addBooster(AdventureEventController.instance().generateBooster(setToUnlock));
                setUnlockedText = "FORGE_ARCHIPELAGO: CARD SET REWARD + BOOSTER DETECTED: " + setToUnlock;
            }
            System.out.println(setUnlockedText);
            GameHUD.getInstance().addNotification(setUnlockedText, 0.5f, 3f, 0.5f);
        }
    }

    public boolean checkCardUnlocked(PaperCard card) {
        String cardName = card.getName();

        // Card explicitly unlocked
        if (cardsUnlockedByName.contains(cardName)) {
            return true;
        }

        // Card sets unlocked
        String setCode = card.getEdition();
        if (setCode != null && setsUnlockedByCode.contains(setCode)) {
            return true;
        }

        // Neither card nor set is unlocked
        return false;
    }

    public boolean checkDeckUnlocked(Deck selectedDeck) {
        // Todo: Uncomment this once our set unlock logic is in place
        if (selectedDeck == null) {
            return true;
        }

        CardPool pool = selectedDeck.getAllCardsInASinglePool(true, true);
        for (PaperCard card : pool.toFlatList()) {
            if (!checkCardUnlocked(card)) return false;
        }

        return true;
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

        // Todo: Temporary unlock of new set when player gains a card, remove later
        unlockRandomSet();
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: ITEM REWARD DETECTED: " + amount);
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    // Todo: Verify that item names are unique.
    public void addItem(String itemName) {
        itemsGainedById.merge(itemName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: ITEM REWARD DETECTED: " + itemName);
    }

    public void addPack(String boosterPackName) {
        packsEarnedBySet.merge(boosterPackName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: CARD PACK REWARD DETECTED: +" + boosterPackName);
    }

    public void addMaxLife(int amount) {
        totalExtraMaxLifeEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: MAX LIFE REWARD DETECTED: +" + amount);
    }

    public void addShards(int amount) {
        totalShardsEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: SHARD REWARD DETECTED: +" + amount);
    }

    // Note that the name of a boss is not unique so we'll need to filter from all enemies which have a `boss` value of `true`.
    // Returns `true` if the boss was not already defeated before.
    public boolean addMiniBossDefeated(String miniBossName) {
        return miniBossesDefeatedByName.add(miniBossName);
    }
    public boolean addBossDefeated(String bossName) {
        return bossesDefeatedByName.add(bossName);
    }

    public boolean addCardUnlockedByName(String cardName) {
        return cardsUnlockedByName.add(cardName);
    }
    public boolean addSetUnlockedByCode(String setCode) {
        return setsUnlockedByCode.add(setCode);
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

        Set<String> newSetCodes = new HashSet<>();
        for (CardEdition edition : allOrderedEditions) {
            newSetCodes.add(edition.getCode());
        }
        if (!newSetCodes.equals(allCardSets)) {
            allCardSets.clear();
            allCardSets.addAll(newSetCodes);
        }

        // Load save data
        loadStringLongMap(data, "townEvents", completedTownInnEvents);
        loadStringLongMap(data, "townQuests", completedTownQuests);
        loadStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        loadStringLongMap(data, "items", itemsGainedById);
        loadStringLongMap(data, "packs", packsEarnedBySet);
        loadStringSet(data, "bossesDefeated", bossesDefeatedByName);
        loadStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);
        loadStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        loadStringSet(data, "setsUnlocked", setsUnlockedByCode);

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
        saveStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        saveStringSet(data, "setsUnlocked", setsUnlockedByCode);

        data.store("totalGold", totalGoldEarned);
        data.store("extraLife", totalExtraMaxLifeEarned);
        data.store("shards", totalShardsEarned);

        return data;
    }
}
