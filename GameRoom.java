import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private List<ClientHandlerB> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private boolean isGameStarted = false;
    private boolean isBettingPhase = false;

    // Dealer Information
    private List<Card> dealerCards = new ArrayList<>();

    public synchronized void join(ClientHandlerB player) {
        players.add(player);
        broadcast("SERVER_MSG: Player [" + player.getPlayerId() + "] joined. (Total: " + players.size() + ")");
    }

    public synchronized void leave(ClientHandlerB player) {
        players.remove(player);
        broadcast("SERVER_MSG: Player [" + player.getPlayerId() + "] left.");
    }

    public synchronized void broadcast(String msg) {
        for (ClientHandlerB p : players) {
            p.sendMessage(msg);
        }
    }

    public synchronized void startGame() {
        if (players.size() < 1) return;
        if (isBettingPhase || isGameStarted) return;

        isBettingPhase = true;
        isGameStarted = false;

        for (ClientHandlerB p : players) {
            p.resetRound();
        }

        broadcast("------------------------------------------------");
        broadcast("GAME_PHASE: Betting Phase Started!");
        for (ClientHandlerB p : players) {
            p.resetRound();
            p.sendMessage("INFO: Your current balance is [" + p.getBalance() + "].");
            p.sendMessage("INFO: Please place your bet. (e.g., BET 100)");
        }
        broadcast("------------------------------------------------");
    }

    public synchronized boolean placeBet(ClientHandlerB player, int amount) {
        if (!isBettingPhase) {
            player.sendMessage("ERROR: It is not betting time.");
            return false;
        }
        if (player.isBetPlaced()) {
            player.sendMessage("ERROR: You have already placed a bet.");
            return false;
        }
        if (player.getBalance() >= amount) {
            player.decreaseBalance(amount);
            player.setCurrentBet(amount);
            player.setBetPlaced(true);

            broadcast("SERVER_MSG: [" + player.getPlayerId() + "] placed a bet of " + amount + ".");
            checkAllBets();
            return true;
        } else {
            player.sendMessage("ERROR: Insufficient balance.");
            return false;
        }
    }

    private void checkAllBets() {
        for (ClientHandlerB p : players) {
            if (!p.isBetPlaced()) {
                return;
            }
        }
        startRound();
    }

    private void startRound() {
        isBettingPhase = false;
        isGameStarted = true;
        currentPlayerIndex = 0;
        dealerCards.clear();

        broadcast("------------------------------------------------");
        broadcast("ROUND_START: Betting closed! The game begins.");

        // 딜러 카드 1장 뽑기
        Card dealerCard = Card.drawRandom();
        dealerCards.add(dealerCard);
        int dealerScore = BlackjackScoreCalculator.calculateScore(dealerCards);
        broadcast("INFO: Dealer's open card: [" + dealerCard.getDisplayName() + "] (Score: " + dealerScore + ")");

        // 각 플레이어에게 카드 2장씩 나눠주기
        for (ClientHandlerB p : players) {
            p.resetScoreOnly();
            
            Card c1 = Card.drawRandom();
            Card c2 = Card.drawRandom();
            
            p.addCard(c1);
            p.addCard(c2);
            
            int playerScore = p.getScore();
            
            // 카드 정보 전송 (A, J, Q, K 표시 포함)
            String cardsStr = c1.getDisplayName() + "," + c2.getDisplayName();
            p.sendMessage("INITIAL_DEAL: Dealer=[" + dealerCard.getDisplayName() + "], Cards=[" + cardsStr + "], Total=[" + playerScore + "]");
            
            if (p.isBlackjack()) {
                p.sendMessage("INFO: Blackjack! (21 points)");
            } else if (p.isSoftHand()) {
                p.sendMessage("INFO: Soft Hand (Ace can be 11)");
            }
        }
        
        broadcast("------------------------------------------------");
        notifyCurrentPlayer();
    }

    public synchronized void handlePlayerAction(ClientHandlerB player, String action) {
        if (!isGameStarted) {
            player.sendMessage("ERROR: Game is not in progress.");
            return;
        }

        ClientHandlerB currentTurnPlayer = players.get(currentPlayerIndex);
        if (!player.equals(currentTurnPlayer)) {
            player.sendMessage("ERROR: It is not your turn. (Current turn: " + currentTurnPlayer.getPlayerId() + ")");
            return;
        }

        if (action.equalsIgnoreCase("Hit")) {
            Card card = Card.drawRandom();
            player.addCard(card);
            
            int playerScore = player.getScore();
            
            // 카드를 뽑았다고 알림
            broadcast("ACTION: [" + player.getPlayerId() + "] Hit! (Draw: " + card.getDisplayName() + ", Score: " + playerScore + ")");

            // 점수 체크
            if (player.isBust()) {
                broadcast("RESULT: [" + player.getPlayerId() + "] BUST! (Over 21)");
                nextTurn();
            } else {
                // 21점을 안 넘었으면 다시 턴을 줌
                if (player.isSoftHand()) {
                    player.sendMessage("INFO: Soft Hand - You can hit safely!");
                }
                player.sendMessage("YOUR_TURN: Choose action (HIT, STAND, DOUBLEDOWN, SURRENDER).");
            }
            
        } else if (action.equalsIgnoreCase("Stand")) {
            broadcast("ACTION: [" + player.getPlayerId() + "] Stand. (Final Score: " + player.getScore() + ")");
            nextTurn();
        } else if (action.equalsIgnoreCase("DoubleDown")) {
            if (player.getBalance() < player.getCurrentBet()) {
                player.sendMessage("ERROR: Insufficient balance for Double Down.");
                return;
            }
            int additionalBet = player.getCurrentBet();
            player.decreaseBalance(additionalBet);
            player.setCurrentBet(player.getCurrentBet() + additionalBet);
            
            Card card = Card.drawRandom();
            player.addCard(card);
            
            int finalScore = player.getScore();
            broadcast("ACTION: [" + player.getPlayerId() + "] Double Down! (Draw: " + card.getDisplayName() + ", Final Score: " + finalScore + ")");

            if (player.isBust()) {
                broadcast("RESULT: [" + player.getPlayerId() + "] BUST! (Lost)");
            }
            nextTurn();

        } else if (action.equalsIgnoreCase("Surrender")) {
            player.setSurrender(true);
            broadcast("ACTION: [" + player.getPlayerId() + "] Surrender. (Given up)");
            nextTurn();
        }
    }

    private void nextTurn() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            playDealerTurn();
        } else {
            notifyCurrentPlayer();
        }
    }

    private void notifyCurrentPlayer() {
        ClientHandlerB p = players.get(currentPlayerIndex);
        broadcast("TURN: It is [" + p.getPlayerId() + "]'s turn.");
        p.sendMessage("YOUR_TURN: Choose action (HIT, STAND, DOUBLEDOWN, SURRENDER).");
    }

    private void playDealerTurn() {
        int dealerScore = BlackjackScoreCalculator.calculateScore(dealerCards);
        broadcast("DEALER_TURN: All player turns ended. Dealer draws cards. (Current: " + dealerScore + ")");
        try { Thread.sleep(1000); } catch (Exception e) {}

        // 딜러는 17 이상이 될 때까지 카드를 받음 (Soft 17 포함)
        while (dealerScore < 17) {
            Card card = Card.drawRandom();
            dealerCards.add(card);
            dealerScore = BlackjackScoreCalculator.calculateScore(dealerCards);
            broadcast("DEALER_DRAW: Dealer drew [" + card.getDisplayName() + "]. (Dealer Score: " + dealerScore + ")");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        calculateResults();
    }

    private void calculateResults() {
        broadcast("--- [Final Results] ---");
        int dealerFinalScore = BlackjackScoreCalculator.calculateScore(dealerCards);
        broadcast("Dealer Final Score: " + dealerFinalScore);

        for (ClientHandlerB p : players) {
            String resultMsg;
            int pScore = p.getScore();
            int bet = p.getCurrentBet();
            String status = ""; // 승패 상태 (WIN, LOSE, TIE)

            if (p.isSurrender()) {
                int returnMoney = bet / 2;
                p.increaseBalance(returnMoney);
                resultMsg = "Surrender (Given up: " + returnMoney + " returned)";
                status = "LOSE";
            } else if (p.isBust()) {
                resultMsg = "Lose (Bust)";
                status = "LOSE";
            } else if (dealerFinalScore > 21) {
                int prize = bet * 2;
                p.increaseBalance(prize);
                resultMsg = "Win! (Dealer Bust) - Prize: " + prize;
                status = "WIN";
            } else if (p.isBlackjack() && dealerFinalScore != 21) {
                // 블랙잭 승리 (보통 2.5배 지급, 여기서는 2배로)
                int prize = (int)(bet * 2.5);
                p.increaseBalance(prize);
                resultMsg = "Blackjack Win! (" + pScore + " vs " + dealerFinalScore + ") - Prize: " + prize;
                status = "WIN";
            } else if (pScore > dealerFinalScore) {
                int prize = bet * 2;
                p.increaseBalance(prize);
                resultMsg = "Win! (" + pScore + " vs " + dealerFinalScore + ") - Prize: " + prize;
                status = "WIN";
            } else if (pScore == dealerFinalScore) {
                p.increaseBalance(bet);
                resultMsg = "Tie (Push)";
                status = "TIE";
            } else {
                resultMsg = "Lose (" + pScore + " vs " + dealerFinalScore + ") - Bet lost.";
                status = "LOSE";
            }
            
            broadcast(p.getPlayerId() + ": " + resultMsg);
            p.sendMessage("INFO: Balance after settlement: [" + p.getBalance() + "]");
            p.sendMessage("GAME_RESULT:" + status);
        }
        
        isGameStarted = false;
        isBettingPhase = false;
        broadcast("GAME_END: Round ended. Type START to play again.");
    }
}