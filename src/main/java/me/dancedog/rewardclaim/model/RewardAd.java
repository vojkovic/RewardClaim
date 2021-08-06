package me.dancedog.rewardclaim.model;

import lombok.Data;

@Data
public class RewardAd {
    private final String video;
    private final long duration;
    private final String link;
    private final String buttonText;
}
