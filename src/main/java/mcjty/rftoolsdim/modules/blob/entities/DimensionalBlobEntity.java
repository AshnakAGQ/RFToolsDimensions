package mcjty.rftoolsdim.modules.blob.entities;

import mcjty.rftoolsdim.dimension.data.DimensionData;
import mcjty.rftoolsdim.dimension.data.PersistantDimensionManager;
import mcjty.rftoolsdim.modules.blob.BlobConfig;
import mcjty.rftoolsdim.modules.dimlets.data.DimletRarity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DimensionalBlobEntity extends Monster {

    private float squishAmount;
    public float squishFactor;
    public float prevSquishFactor;

    private final DimletRarity rarity;
    private AABB targetBox = null;

    private int tickCounter = 5;

    private static final TargetingConditions PREDICATE = new TargetingConditions()
            .allowUnseeable();

    public DimensionalBlobEntity(EntityType<? extends Monster> type, Level worldIn, DimletRarity rarity) {
        super(type, worldIn);
        this.rarity = rarity;
        calculateTargetBox(getBoundingBox());
    }

    private static int getDefaultMaxHealth(DimletRarity rarity) {
        // There is no UNCOMMON mob
        switch (rarity) {
            case COMMON:
                return BlobConfig.BLOB_COMMON_HEALTH.get();
            case RARE:
                return BlobConfig.BLOB_RARE_HEALTH.get();
            case LEGENDARY:
                return BlobConfig.BLOB_LEGENDARY_HEALTH.get();
            case UNCOMMON:
                throw new IllegalStateException("There is no uncommon blob!");
        }
        throw new IllegalStateException("Unknown blob type!");
    }

    private int getRegenLevel() {
        switch (rarity) {
            case COMMON:
                return BlobConfig.BLOB_COMMON_REGEN.get();
            case UNCOMMON:
                throw new IllegalStateException("There is no uncommon blob!");
            case RARE:
                return BlobConfig.BLOB_RARE_REGEN.get();
            case LEGENDARY:
                return BlobConfig.BLOB_LEGENDARY_REGEN.get();
        }
        throw new IllegalStateException("Unknown blob type!");
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level.isClientSide) {
            DimensionData data = PersistantDimensionManager.get(level).getData(level.dimension().location());
            if (data != null) {
                if (data.getEnergy() >= BlobConfig.BLOB_REGENERATION_LEVEL.get()) {
                    tickCounter--;
                    if (tickCounter <= 0) {
                        tickCounter = 5;
                        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, getRegenLevel()));
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor worldIn, @Nonnull DifficultyInstance difficultyIn, @Nonnull MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        AttributeInstance attr = getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(getDefaultMaxHealth(rarity));
            setHealth(getDefaultMaxHealth(rarity));
        }
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public static AttributeSupplier.Builder registerAttributes(DimletRarity rarity) {
        // Note, since this is called before config is init the default max health here will be
        // the default from the config. In onInitialSpawn() this is later corrected
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.MAX_HEALTH, getDefaultMaxHealth(rarity));
    }

    @Override
    public float getScale() {
        switch (rarity) {
            case COMMON:
            case UNCOMMON:
                return 1.5f;
            case RARE:
                return 2.2f;
            case LEGENDARY:
                return 4.7f;
        }
        return 1.0f;
    }

    @Override
    public void setBoundingBox(@Nonnull AABB bb) {
        super.setBoundingBox(bb);
        calculateTargetBox(bb);
    }

    private void calculateTargetBox(AABB bb) {
        if (rarity != null) {
            double radius = 1.0;
            switch (rarity) {
                case COMMON:
                case UNCOMMON:
                    radius = 5.0;
                    break;
                case RARE:
                    radius = 9.0;
                    break;
                case LEGENDARY:
                    radius = 15.0;
                    break;
            }
            targetBox = bb.inflate(radius);
        }
    }


    private void infectPlayer(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100));
        switch (rarity) {
            case COMMON:
            case UNCOMMON:
                break;
            case RARE:
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                break;
            case LEGENDARY:
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
                break;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5F;
            this.prevSquishFactor = this.squishFactor;

            if (random.nextFloat() < 0.03f) {
                this.squishAmount = -0.5F;
            } else if (random.nextFloat() < 0.03f) {
                this.squishAmount = 1.0f;
            }

            this.squishAmount *= 0.6F;
        } else {
            if (random.nextFloat() < .05) {
                List<Player> players = level.getNearbyPlayers(PREDICATE, this, targetBox);
                for (Player player : players) {
                    infectPlayer(player);
                }

            }
        }
    }

    public DimletRarity getRarity() {
        return rarity;
    }

    @Nonnull
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }


}
