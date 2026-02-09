package forge.card;

import forge.item.PaperCard;

public interface ICardLockProvider {
    boolean isCardLocked(PaperCard card);
}
