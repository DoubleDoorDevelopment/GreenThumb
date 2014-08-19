/*
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.greenThumb;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.doubledoordev.d3core.util.ID3Mod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.doubledoordev.greenThumb.util.Constants.MODID;

/**
 * @author Dries007
 */
@Mod(modid = MODID, canBeDeactivated = false)
public class GreenThumb implements ID3Mod
{
    @Mod.Instance(MODID)
    public static GreenThumb instance;

    List<String> seedMethodNames = ImmutableList.of("func_149866_i", "i");
    Method methodSeed;
    private Configuration configuration;
    boolean checkGrowth = true;

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);

        configuration = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();

        for (Method method : BlockCrops.class.getDeclaredMethods())
        {
            // Hackery!
            if (method.getReturnType().isAssignableFrom(Item.class) && method.getParameterTypes().length == 0 && seedMethodNames.contains(method.getName()))
            {
                methodSeed = method;
                methodSeed.setAccessible(true);
            }
        }
    }

    @SubscribeEvent
    public void rightClickEvent(PlayerInteractEvent event) throws InvocationTargetException, IllegalAccessException
    {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) return;
        if (event.entityPlayer.getHeldItem() != null) return;
        if (methodSeed == null) return;

        Block block = event.world.getBlock(event.x, event.y, event.z);

        if (block instanceof BlockCrops)
        {
            BlockCrops crop = ((BlockCrops) block);
            if (checkGrowth && event.world.getBlockMetadata(event.x, event.y, event.z) != 7) return;
            ArrayList<ItemStack> drops = crop.getDrops(event.world, event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z), 0);
            boolean foundSeed = false;
            event.world.setBlockToAir(event.x, event.y, event.z);
            for (ItemStack itemStack : drops)
            {
                Item seed = ((Item) methodSeed.invoke(crop));
                if (itemStack.getItem() == seed && !foundSeed)
                {
                    foundSeed = true;
                    event.world.setBlock(event.x, event.y, event.z, crop);
                    continue;
                }
                event.world.spawnEntityInWorld(new EntityItem(event.world, event.entityPlayer.posX, event.entityPlayer.posY, event.entityPlayer.posZ, itemStack));
            }
        }
        else if (block instanceof BlockNetherWart)
        {
            BlockNetherWart crop = ((BlockNetherWart) block);
            if (checkGrowth && event.world.getBlockMetadata(event.x, event.y, event.z) != 3) return;
            ArrayList<ItemStack> drops = crop.getDrops(event.world, event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z), 0);
            boolean foundSeed = false;
            event.world.setBlockToAir(event.x, event.y, event.z);
            for (ItemStack itemStack : drops)
            {
                if (!foundSeed)
                {
                    foundSeed = true;
                    event.world.setBlock(event.x, event.y, event.z, crop);
                    continue;
                }
                event.world.spawnEntityInWorld(new EntityItem(event.world, event.entityPlayer.posX, event.entityPlayer.posY, event.entityPlayer.posZ, itemStack));
            }
        }
    }

    @Override
    public void syncConfig()
    {
        configuration.setCategoryLanguageKey(MODID, "d3.greenThumb.config.greenThumb");

        checkGrowth = configuration.getBoolean("checkGrowth", MODID, checkGrowth, "Check to see if the crop is grown 100% and only harvest then.", "d3.greenThumb.config.checkGrowth");

        if (configuration.hasChanged()) configuration.save();
    }

    @Override
    public void addConfigElements(List<IConfigElement> configElements)
    {
        configElements.add(new ConfigElement(configuration.getCategory(MODID.toLowerCase())));
    }
}
