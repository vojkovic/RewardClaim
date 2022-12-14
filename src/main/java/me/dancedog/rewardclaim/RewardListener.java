package me.dancedog.rewardclaim;

import lombok.NonNull;
import me.dancedog.rewardclaim.model.RewardSession;
import me.dancedog.rewardclaim.ui.GuiScreenRewardSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DanceDog / Ben on 3/29/20 @ 11:38 AM
 */
public class RewardListener {

    private static final Pattern REWARD_MESSAGE_PATTERN = Pattern.compile("§r§6Click the link to visit our website and claim your reward: §r§bhttps://rewards\\.hypixel\\.net/claim-reward/([A-Za-z0-9]+)§r");

    private long lastRewardOpenedMs;
    private final AtomicReference<RewardSession> sessionData = new AtomicReference<>();

    /**
     * Fetches & scrapes the reward page in a separate thread. The resulting json is then stored in
     * this class's rawRewardSessionData
     *
     * @param sessionId Session ID to scrape reward data from
     */
    private void fetchRewardSession(String sessionId) {
        Request request = new Request.Builder()
                .url("https://rewards.hypixel.net/claim-reward/" + sessionId)
                .header("User-Agent", RewardClaim.MOD_NAME + "/" + RewardClaim.VERSION)
                .build();

        RewardClaim.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                RewardClaim.printWarning("Failed to fetch the reward page", e, true);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    RewardClaim.printWarning("Failed to fetch the reward page. Server sent back a " + response.code() + " status code", true);
                    RewardClaim.printWarning("Received the following body:\n" + response.body(), false);
                    return;
                }

                ResponseBody body = response.body();

                if (body == null) {
                    RewardClaim.printWarning("Server sent back an empty body", true);
                    return;
                }

                Document document = Jsoup.parse(body.string());
                RewardSession session = RewardScraper.parseSessionFromRewardPage(document, response.headers("Set-Cookie"));
                sessionData.set(session);
            }
        });
    }

    /**
     * Checks every tick to see if there is a reward session to display
     */
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }

        RewardSession currentSessionData = sessionData.getAndSet(null);
        if (currentSessionData != null) {
            if (currentSessionData.getError() != null) {
                RewardClaim.printWarning("Failed to get reward: " + currentSessionData.getError(), true);
                return;
            }

            // TODO implement ads
            if (!currentSessionData.isSkippable()) {
                RewardClaim.printWarning("Ads are not yet implemented - go get a rank ;)", true);
                return;
            }

            Minecraft.getMinecraft().displayGuiScreen(new GuiScreenRewardSession(currentSessionData));
        }
    }

    /**
     * Check for link to daily reward in chat
     */
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        Matcher chatMatcher = REWARD_MESSAGE_PATTERN.matcher(event.message.getFormattedText());
        if (chatMatcher.find()) {
            event.setCanceled(true);
            lastRewardOpenedMs = System.currentTimeMillis();

            String sessionId = chatMatcher.group(1);
            RewardClaim.getLogger().info("Triggered fetch for reward session #{}", sessionId);
            this.fetchRewardSession(sessionId);
        }
    }

    /**
     * Checks for the daily reward book (which Hypixel normally displays when clicking the reward
     * token) and cancels it completely
     */
    @SubscribeEvent
    public void onGuiInit(GuiOpenEvent event) {
        // Check for the reward book notification up to 10 seconds after the reward's chat link was received
        if (Minecraft.getMinecraft().thePlayer != null
                && event.gui instanceof GuiScreenBook
                && (System.currentTimeMillis() - lastRewardOpenedMs) <= 10000) {
            event.setCanceled(true);
            lastRewardOpenedMs = 0;
        }
    }
}
