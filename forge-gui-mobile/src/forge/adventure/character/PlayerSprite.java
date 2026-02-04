package forge.adventure.character;

import com.badlogic.gdx.math.Vector2;
import forge.Forge;
import forge.adventure.data.ArchipelagoData;
import forge.adventure.data.BiomeData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.Scene;
import forge.adventure.stage.GameStage;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;

/**
 * Class that will represent the player sprite on the map
 */
public class PlayerSprite extends CharacterSprite {
    private final float playerSpeed;
    private final Vector2 direction = Vector2.Zero.cpy();
    private final Vector2 lastLegalPosition = new Vector2();
    private float playerSpeedModifier = 1f;
    private float playerSpeedEquipmentModifier = 1f;
    GameStage gameStage;

    public PlayerSprite(GameStage gameStage) {
        super(AdventurePlayer.current().spriteName());
        this.gameStage = gameStage;
        setOriginX(getWidth() / 2);
        Current.player().onPlayerChanged(PlayerSprite.this::updatePlayer);

        playerSpeed = Config.instance().getConfigData().playerBaseSpeed;

        //Attach signals here.
        Current.player().onBlessing(() -> playerSpeedEquipmentModifier = Current.player().equipmentSpeed());
        Current.player().onEquipmentChanged(() -> playerSpeedEquipmentModifier = Current.player().equipmentSpeed());

        // Set initial last legal position (will be 0,0 here)
        lastLegalPosition.set(getX(), getY());
    }

    private void updatePlayer() {
        load(AdventurePlayer.current().spriteName());
    }

    public void LoadPos() {
        setPosition(AdventurePlayer.current().getWorldPosX(), AdventurePlayer.current().getWorldPosY());
    }

    public void storePos() {
        storePos(getX(), getY());
    }

    public void storePos(final float x, final float y) {
        AdventurePlayer.current().setWorldPosX(x);
        AdventurePlayer.current().setWorldPosY(y);
    }

    public Vector2 getMovementDirection() {
        return direction;
    }

    public void setMovementDirection(final Vector2 dir) {
        direction.set(dir);
    }

    public void setMoveModifier(float speed) {
        playerSpeedModifier = speed;
    }

    private boolean isInUnlockedBiome() {
        World world = WorldSave.getCurrentSave().getWorld();

        int tileX = (int)(getX() / world.getTileSize());
        int tileY = (int)(getY() / world.getTileSize());

        int biomeId = World.highestBiome(world.getBiome(tileX, tileY));
        int maxBiomeIndex = world.getData().GetBiomes().size();
        if (maxBiomeIndex <= biomeId) {
            // If the biome is not one we recognize, we should assume that the player is allowed to be there.
            return true;
        }
        BiomeData biome = world.getData().GetBiomes().get(biomeId);

        return ArchipelagoData.getInstance().isBiomeUnlocked(biome.name);
    }

    // Todo: Pretty sure this is where collision is handled
    @Override
    public void act(float delta) {
        super.act(delta);
        if (Forge.advFreezePlayerControls)
            return;
        direction.setLength(playerSpeed * delta * playerSpeedModifier*playerSpeedEquipmentModifier);
        Vector2 previousDirection = getMovementDirection().cpy();
        Scene previousScene = forge.Forge.getCurrentScene();

        if(!direction.isZero()) {
            gameStage.prepareCollision(pos(),direction,boundingRect);
            direction.set(gameStage.adjustMovement(direction,boundingRect));
            moveBy(direction.x, direction.y);

            // Archipelago: Check if it's a legal biome for the player to be in, if not, refuse to update their position.
            if (!isInUnlockedBiome()) {
                setPosition(lastLegalPosition.x, lastLegalPosition.y);

                // Todo: Show feedback to the user that this region is locked. (Play sound effect or show dialog).
            } else {
                // Update last known legal position
                lastLegalPosition.set(getX(), getY());
            }

            // If the player is blocked by an obstacle, and they haven't changed scenes,
            // they will keep trying to move in that direction
            if (previousScene == forge.Forge.getCurrentScene()) {
                direction.set(previousDirection.cpy());
            }
        }
    }

    public boolean isMoving() {
        return !direction.isZero();
    }

    public void stop() {
        direction.setZero();
        setAnimation(AnimationTypes.Idle);
    }

    public void setPosition(Vector2 oldPosition) {
        setPosition(oldPosition.x, oldPosition.y);
    }
}
