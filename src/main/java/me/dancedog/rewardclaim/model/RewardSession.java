package me.dancedog.rewardclaim.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import me.dancedog.rewardclaim.RewardClaim;
import net.minecraft.client.Minecraft;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DanceDog / Ben on 3/22/20 @ 8:52 PM
 */
public class RewardSession {

    private static final Gson gson = new Gson();

    @Getter
    String error;

    @Getter
    private String id;

    @Getter
    private List<RewardCard> cards;

    @Getter
    private boolean skippable;

    @Getter
    private RewardDailyStreak dailyStreak;

    @Getter
    private RewardAd ad;

    @Getter
    private int activeAd;

    @Getter
    private String csrfToken;

    @Getter
    private List<String> cookies;

    /**
     * Create a new reward session object from the session json (rewards, ad, streak, etc), the
     * session's csrf token and the session's cookies
     *
     * @param raw     The session's raw json representation
     * @param cookies The cookies received from the original reward request
     */
    public RewardSession(JsonObject raw, List<String> cookies) {
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
            this.cards.add(new RewardCard(rewardElement != null ? rewardElement.getAsJsonObject() : null));
        }
        this.skippable = raw.get("skippable").getAsBoolean();
        this.dailyStreak = gson.fromJson(raw.get("dailyStreak"), RewardDailyStreak.class);
        this.ad = gson.fromJson(raw.get("ad"), RewardAd.class);
        this.activeAd = raw.get("activeAd").getAsInt();
        this.csrfToken = raw.get("_csrf").getAsString();
        this.cookies = cookies;
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
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("rewards.hypixel.net")
                .addPathSegments("claim-reward/claim")
                .addQueryParameter("id", this.id)
                .addQueryParameter("option", String.valueOf(option))
                .addQueryParameter("_csrf", this.csrfToken)
                .addQueryParameter("activeAd", String.valueOf(this.activeAd))
                .addQueryParameter("watchedFallback", "false")
                .addQueryParameter("skipped", "0")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", RewardClaim.MOD_NAME + "/" + RewardClaim.VERSION)
                .header("Cookie", String.join("; ", this.cookies))
                .post(Util.EMPTY_REQUEST)
                .build();

        RewardClaim.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                RewardClaim.printWarning("Failed to claim the reward", e, true);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    RewardClaim.printWarning("Failed to claim reward. Server sent back a " + response.code() + " status code", true);
                    RewardClaim.printWarning("Received the following body:\n" + response.body(), false);
                    return;
                }

                ResponseBody body = response.body();

                if (body == null) {
                    RewardClaim.printWarning("Server sent back an empty body", true);
                    return;
                }

                RewardClaim.getLogger().info("Successfully claimed reward");
            }
        });
    }
}
