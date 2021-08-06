package me.dancedog.rewardclaim;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = RewardClaim.MOD_ID, useMetadata = true)
public class RewardClaim {

    public static final String MOD_ID = "rewardclaim";
    public static final String MOD_NAME = "RewardClaim";
    public static final String VERSION = "@VERSION@";

    @Getter
    private final static OkHttpClient httpClient = new OkHttpClient();

    @Getter
    private static Logger logger = LogManager.getLogger(MOD_ID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new RewardListener());
    }

    public static ResourceLocation getGuiTexture(String... path) {
        return new ResourceLocation(MOD_ID, "textures/gui/" + String.join("/", path));
    }

    public static ResourceLocation getSound(String... path) {
        return new ResourceLocation(MOD_ID, String.join(".", path));
    }

    public static void printWarning(String message, Throwable t, boolean inChat) {
        logger.warn(message, t);

        if (inChat && Minecraft.getMinecraft().thePlayer != null) {
            ChatComponentText chatMessage = new ChatComponentText("[" + MOD_NAME + "] " + message);
            chatMessage.getChatStyle().setBold(true).setColor(EnumChatFormatting.RED);
            Minecraft.getMinecraft().thePlayer.addChatMessage(chatMessage);
        }
    }

    public static void printWarning(String message, boolean inChat) {
        printWarning(message, null, inChat);
    }
}
