package me.dancedog.rewardclaim.model;

import com.google.gson.JsonObject;
import lombok.Getter;

public class RewardDailyStreak {
    @Getter
    private final int value;

    @Getter
    private final int score;

    @Getter
    private final int highScore;

    @Getter
    private final boolean keeps;

    @Getter
    private final boolean token;

    RewardDailyStreak(JsonObject raw) {
        value = raw.get("value").getAsInt();
        score = raw.get("score").getAsInt();
        highScore = raw.get("highScore").getAsInt();
        keeps = raw.get("keeps").getAsBoolean();
        token = raw.get("token").getAsBoolean();
    }
}
