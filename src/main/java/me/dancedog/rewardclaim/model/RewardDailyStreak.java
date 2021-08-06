package me.dancedog.rewardclaim.model;

import lombok.Data;

@Data
public class RewardDailyStreak {
    private final int value;
    private final int score;
    private final int highScore;
    private final boolean keeps;
    private final boolean token;
}
