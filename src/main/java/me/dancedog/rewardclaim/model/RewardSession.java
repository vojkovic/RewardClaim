package me.dancedog.rewardclaim.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import me.dancedog.rewardclaim.Mod;
import me.dancedog.rewardclaim.fetch.Request;
import me.dancedog.rewardclaim.fetch.Request.Method;
import me.dancedog.rewardclaim.fetch.Response;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DanceDog / Ben on 3/22/20 @ 8:52 PM
 */
public class RewardSession {

    @Getter
    String error;
    @Getter
    private String id;
    @Getter
    private List<RewardCard> cards;
    @Getter
    private RewardDailyStreak dailyStreak;
    @Getter
    private String csrfToken;
    @Getter
    private List<String> cookie;

    /**
     * Create a new reward session object from the session json (rewards, ad, streak, etc), the
     * session's csrf token and the session's cookie
     *
     * @param raw    The session's raw json representation
     * @param cookie The cookie received from the original reward request
     */
    public RewardSession(JsonObject raw, List<String> cookie) {
        if (!validateSessionData(raw)) {
            if (raw != null && raw.has("error")) {
                this.error = raw.get("error").getAsString();
            } else {
                this.error = "Invalid reward session data";
            }
            return;
        }

        this.id = raw.get("id").getAsString();
        this.cards = new ArrayList<>();
        for (JsonElement rewardElement : raw.get("rewards").getAsJsonArray()) {
            this.cards
                    .add(new RewardCard(rewardElement != null ? rewardElement.getAsJsonObject() : null));
        }
        this.csrfToken = raw.get("_csrf").getAsString();
        this.dailyStreak = new RewardDailyStreak(raw.getAsJsonObject("dailyStreak"));
        this.cookie = cookie;
    }

    private static boolean validateSessionData(JsonObject raw) {
        return raw != null
                && raw.has("id")
                && raw.has("activeAd")
                && raw.has("ad")
                && raw.has("skippable")
                && raw.has("rewards")
                && raw.get("rewards").getAsJsonArray().size() == 3;
    }

    public void claimReward(int option) {
        new Thread(() -> {
            try {
                // TODO: 3/29/20 Actually parse the activeAd id & send it in request
                String urlStr = "https://rewards.hypixel.net/claim-reward/claim"
                        + "?id=" + this.id
                        + "&option=" + option
                        + "&_csrf=" + this.csrfToken
                        + "&activeAd=" + "0"
                        + "&watchedFallback=false"
                        + "&skipped=0";
                URL url = new URL(urlStr);

                Response response = new Request(url, Method.POST, this.cookie).execute();
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    Mod.getLogger().info("Successfully claimed reward");
                    Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation(Mod.MODID, "reward")));
                } else {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Failed to claim reward, check the logs").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
                    Mod.printWarning("Failed to claim reward. Server sent back a " + response.getStatusCode()
                            + " status code. Received the following body:\n" + response.getBody(), null, false);
                }
            } catch (IOException e) {
                Mod.printWarning("IOException during claim reward request", e, false);
            }
        }).start();
    }
}
