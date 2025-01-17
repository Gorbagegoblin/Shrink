package net.gigabit101.shrink.items;

import net.gigabit101.shrink.utils.MathHelper;
import net.gigabit101.shrink.ShrinkContainer;
import net.gigabit101.shrink.api.ShrinkAPI;
import net.gigabit101.shrink.cap.EnergyStorageItemImpl;
import net.gigabit101.shrink.config.ShrinkConfig;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class ItemShrinkingDevice extends Item implements MenuProvider
{
    public ItemShrinkingDevice(Item.Properties properties)
    {
        super(properties.rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if(!player.isCrouching())
        {
            player.getCapability(ShrinkAPI.SHRINK_CAPABILITY).ifPresent(iShrinkProvider ->
            {
               if(!iShrinkProvider.isShrunk())
               {
                   if (!level.isClientSide()) NetworkHooks.openGui((ServerPlayer) player, this);
               }
               else
               {
                   if (!level.isClientSide()) player.displayClientMessage(new TranslatableComponent("Can't open while shrunk"), false);
               }
            });
        }

        if (!level.isClientSide() && player.isCrouching())
        {
            player.getCapability(ShrinkAPI.SHRINK_CAPABILITY).ifPresent(iShrinkProvider ->
            {
                if (!iShrinkProvider.isShrunk() && canUse(stack, player))
                {
                    iShrinkProvider.shrink((ServerPlayer) player);
                }
                else if(iShrinkProvider.isShrunk() && canUse(stack, player))
                {
                    iShrinkProvider.deShrink((ServerPlayer) player);
                }
                else if(!canUse(stack, player) && ShrinkConfig.POWER_REQUIREMENT.get())
                {
                    player.displayClientMessage(new TranslatableComponent("Not enough power in device"), false);
                }
            });
        }

        if(level.isClientSide() && player.isCrouching())
        {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 5F, 0F);
            spawnParticle(level, player.getX(), player.getY(), player.getZ() -1, level.random);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity)
    {
        AtomicReference<Float> scale = new AtomicReference<>(0.1F);
        player.getCapability(ShrinkAPI.SHRINK_CAPABILITY).ifPresent(iShrinkProvider -> scale.set(iShrinkProvider.scale()));

        if(entity instanceof LivingEntity && !entity.level.isClientSide)
        {
            entity.getCapability(ShrinkAPI.SHRINK_CAPABILITY).ifPresent(iShrinkProvider ->
            {
                iShrinkProvider.setScale(scale.get());

                if (!iShrinkProvider.isShrunk() && canUse(stack, player))
                {
                    iShrinkProvider.shrink((LivingEntity) entity);
                }
                else if (iShrinkProvider.isShrunk() && canUse(stack, player))
                {
                    iShrinkProvider.deShrink((LivingEntity) entity);
                }
            });
            return true;
        }
        return false;
    }

    public void spawnParticle(Level worldIn, double posX, double posY, double posZ, Random rand)
    {
        for (int i = 0; i < 16; ++i)
        {
            int j = rand.nextInt(2) * 2 - 1;
            int k = rand.nextInt(2) * 2 - 1;
            double d0 = posX + 0.5D + 0.25D * (double) j;
            double d1 = ((float) posY + rand.nextFloat());
            double d2 = posZ + 0.5D + 0.25D * (double) k;
            double d3 = (rand.nextFloat() * (float) j);
            double d4 = (rand.nextFloat() - 0.5D) * 0.125D;
            double d5 = (rand.nextFloat() * (float) k);
            worldIn.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
        }
    }

    public boolean canUse(ItemStack stack, Player playerEntity)
    {
        if(!ShrinkConfig.POWER_REQUIREMENT.get()) return true;
        if(playerEntity.isCreative()) return true;

        LazyOptional<IEnergyStorage> optional = stack.getCapability(CapabilityEnergy.ENERGY);
        if(optional.isPresent())
        {
            IEnergyStorage energyStorage = optional.orElseThrow(IllegalStateException::new);
            if(energyStorage.getEnergyStored() >= ShrinkConfig.POWER_COST.get())
            {
                energyStorage.extractEnergy(ShrinkConfig.POWER_COST.get(), false);
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt)
    {
        return new ICapabilityProvider()
        {
            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
            {
                if (cap == CapabilityEnergy.ENERGY) return LazyOptional.of(() -> new EnergyStorageItemImpl(stack, ShrinkConfig.POWER_CAPACITY.get(), ShrinkConfig.POWER_CAPACITY.get(), ShrinkConfig.POWER_CAPACITY.get())).cast();
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        return MathHelper.hsvToRgb((1 + getChargeRatio(stack)) / 3.0F, 1.0F, 1.0F);
    }

    //TODO
//    @Override
//    public boolean showDurabilityBar(ItemStack stack)
//    {
//        return ShrinkConfig.POWER_REQUIREMENT.get();
//    }
//
//    @Override
//    public double getDurabilityForDisplay(ItemStack stack)
//    {
//        return 1 - getChargeRatio(stack);
//    }

    public static float getChargeRatio(ItemStack stack)
    {
        LazyOptional<IEnergyStorage> optional = stack.getCapability(CapabilityEnergy.ENERGY);
        if (optional.isPresent())
        {
            IEnergyStorage energyStorage = optional.orElseThrow(IllegalStateException::new);
            return (float) energyStorage.getEnergyStored() / energyStorage.getMaxEnergyStored();
        }
        return 0;
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn)
    {
        //TODO
//        if(KeyBindings.shrink != null) tooltip.add(new TextComponent(TextFormatting.DARK_PURPLE + "Sneak-Click " + TextFormatting.WHITE + "or press " + TextFormatting.DARK_PURPLE + KeyBindings.shrink.getKey().getDisplayName().getString() + TextFormatting.WHITE + " to active"));
        LazyOptional<IEnergyStorage> optional = stack.getCapability(CapabilityEnergy.ENERGY);
        if (optional.isPresent())
        {
            IEnergyStorage energyStorage = optional.orElseThrow(IllegalStateException::new);
            tooltip.add(new TextComponent(energyStorage.getEnergyStored() + " FE / " + energyStorage.getMaxEnergyStored() + " FE"));
        }
    }

    @Override
    public Component getDisplayName()
    {
        return new TextComponent(this.getOrCreateDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player)
    {
        return new ShrinkContainer(id, inventory);
    }
}
