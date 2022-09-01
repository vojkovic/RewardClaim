package me.dancedog.rewardclaim.ui;

import lombok.Getter;
import me.dancedog.rewardclaim.RewardClaim;
import me.dancedog.rewardclaim.model.RewardSession;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * Created by DanceDog / Ben on 3/22/20 @ 10:43 AM
 */
public class GuiScreenRewardSession extends GuiScreen {

    private State guiState = State.CHOOSING;
    private int chosenCard = -1;
    private RewardSession session;

    private GuiRewardCard[] cards = new GuiRewardCard[3];

    public GuiScreenRewardSession(RewardSession session) {
        this.session = session;
        for (int i = 0; i < 3; i++) {
            this.cards[i] = new GuiRewardCard(session.getCards().get(i));
        }
    }

    @Override
    public void initGui() {
        // Move cursor out of center card
        Mouse.setCursorPosition(mc.displayWidth / 2, 25);

        // Determine card position and spacing
        int middleCardX = width / 2 - (GuiRewardCard.CARD_WIDTH / 2);
        int posY = height / 2 - (GuiRewardCard.CARD_HEIGHT / 2);
        int cardSpacing = 20;

        // Reward cards
        for (int i = 0; i < 3; i++) {
            int posX;
            switch (i) {
                case 0:
                    posX = middleCardX - GuiRewardCard.CARD_WIDTH - cardSpacing;
                    break;
                case 1:
                    posX = middleCardX;
                    break;
                case 2:
                    posX = middleCardX + GuiRewardCard.CARD_WIDTH + cardSpacing;
                    break;
                default:
                    posX = 0;
            }
            this.cards[i].initGui(posX, posY);
        }

        refreshState();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw bg & buttons
        this.drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Determine tooltip states
        for (GuiRewardCard rewardCard : cards) {
            rewardCard.setShowTooltip(rewardCard.canShowTooltip(mouseX, mouseY));
        }

        // Draw all 3 cards
        for (GuiRewardCard rewardCard : cards) {
            rewardCard.drawRewardCard(mouseX, mouseY);
        }

        drawStringRight(
                EnumChatFormatting.GOLD + "Current score" + EnumChatFormatting.DARK_GRAY + ": " + EnumChatFormatting.YELLOW + session.getDailyStreak().getScore(),
                1
        );

        drawStringRight(
                EnumChatFormatting.GOLD + "High score" + EnumChatFormatting.DARK_GRAY + ": " + EnumChatFormatting.YELLOW + session.getDailyStreak().getHighScore(),
                0
        );

        StringBuilder infoText = new StringBuilder(EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.ITALIC + "Reward ID: " + session.getId());

        if (session.getDailyStreak().isToken()) {
            infoText.append(" (token)");
        }

        // Clickable text for reward page
        drawString(fontRendererObj,
                infoText.toString(),
                3,
                height - 10,
                0);
    }

    private void drawStringRight(String text, int y) {
        drawString(
                fontRendererObj,
                text,
                width - 3 - fontRendererObj.getStringWidth(text),
                height - 10 - fontRendererObj.FONT_HEIGHT * y,
                0
        );
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Only select card on left-click
        if (mouseButton != 0) {
            return;
        }

        // Open reward link if ID is clicked
        if (mouseX > 0
                && mouseY < height
                && mouseX < fontRendererObj.getStringWidth("Reward ID:" + session.getId())
                && mouseY > height - 10
        ) {
            String rewardUrlStr = "https://rewards.hypixel.net/claim-reward/" + session.getId();

            // Hacky way to open a URL with the player's consent
            ChatComponentText urlOpener = new ChatComponentText("");
            urlOpener.setChatStyle(
                    new ChatStyle().setChatClickEvent(new ClickEvent(Action.OPEN_URL, rewardUrlStr)));
            this.handleComponentClick(urlOpener);
        }

        // Check if card was clicked (claimed)
        for (int i = 0; i < cards.length; i++) {
            if (guiState != State.CHOOSING) {
                break;
            }

            if (cards[i].isHovered(mouseX, mouseY)) {
                guiState = State.FINAL;
                this.chosenCard = i;
                refreshState();

                RewardClaim.getLogger().debug("Card {} was claimed", i);
                this.session.claimReward(i);
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Updates all elements according to the GUI's current state This prevents the GUI from resetting
     * when the window gets resized
     */
    private void refreshState() {
        if (this.guiState == State.FINAL) {
            this.mc.setIngameFocus();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * An enum of states that the RewardClaimGui can be in
     */
    enum State {
        CHOOSING, // Cards are enabled
        FINAL; // A card was selected
    }
}
