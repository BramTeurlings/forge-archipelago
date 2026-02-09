package forge.card;

import forge.item.PaperCard;

public final class CardLocking {
    private static ICardLockProvider provider;

    public static void setProvider(ICardLockProvider p) {
        provider = p;
    }

    public static boolean isLocked(PaperCard card) {
        return provider != null && provider.isCardLocked(card);
    }
}