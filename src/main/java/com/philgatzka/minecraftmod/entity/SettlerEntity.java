package com.philgatzka.minecraftmod.entity;

import com.philgatzka.minecraftmod.settlement.Settlements;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SettlerEntity extends PathfinderMob
{
    // Synced so the client can render the name above the settler's head
    private static final EntityDataAccessor<String> FIRST_NAME = SynchedEntityData.defineId(SettlerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> LAST_NAME = SynchedEntityData.defineId(SettlerEntity.class, EntityDataSerializers.STRING);

    // The settlement this settler belongs to, if any (spawn-egg settlers have none); server side only
    @Nullable
    private UUID settlementId;

    public SettlerEntity(EntityType<? extends SettlerEntity> type, Level level)
    {
        super(type, level);
        // Every server-side construction gets a name (spawn egg, /summon, ...); saved settlers overwrite it in readAdditionalSaveData
        if (!level.isClientSide)
        {
            setFirstName(SettlerNames.randomFirstName(random));
            setLastName(SettlerNames.randomLastName(random));
        }
    }

    // Same max health as a player (20 = 10 hearts)
    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(FIRST_NAME, "");
        entityData.define(LAST_NAME, "");
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("FirstName", getFirstName());
        tag.putString("LastName", getLastName());
        if (settlementId != null)
            tag.putUUID("Settlement", settlementId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FirstName"))
            setFirstName(tag.getString("FirstName"));
        if (tag.contains("LastName"))
            setLastName(tag.getString("LastName"));
        settlementId = tag.hasUUID("Settlement") ? tag.getUUID("Settlement") : null;
    }

    public String getFirstName()
    {
        return entityData.get(FIRST_NAME);
    }

    public void setFirstName(String firstName)
    {
        entityData.set(FIRST_NAME, firstName);
    }

    public String getLastName()
    {
        return entityData.get(LAST_NAME);
    }

    public void setLastName(String lastName)
    {
        entityData.set(LAST_NAME, lastName);
    }

    @Nullable
    public UUID getSettlementId()
    {
        return settlementId;
    }

    public void setSettlementId(@Nullable UUID settlementId)
    {
        this.settlementId = settlementId;
    }

    // A dead settler leaves its settlement
    @Override
    public void die(DamageSource damageSource)
    {
        super.die(damageSource);
        if (level() instanceof ServerLevel serverLevel)
            Settlements.onSettlerDied(serverLevel, this);
    }

    public String getFullName()
    {
        return getFirstName() + " " + getLastName();
    }

    // Used by getName() whenever there's no custom name, so chat and death messages show the settler's name
    @Override
    protected Component getTypeName()
    {
        return getFirstName().isEmpty() ? super.getTypeName() : Component.literal(getFullName());
    }

    // Always show the name above the head, like a player's name tag
    @Override
    public boolean shouldShowName()
    {
        return true;
    }

    // Settlers are residents, not wildlife: don't despawn them when the player walks away
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer)
    {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource)
    {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound()
    {
        return SoundEvents.PLAYER_DEATH;
    }
}
