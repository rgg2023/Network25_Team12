import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private List<ClientHandlerB> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private boolean isGameStarted = false;
    private boolean isBettingPhase = false;

    // Dealer Information
    private int dealerScore = 0;

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

    // ★ 수정됨: 카드 2장을 각각 알려주는 로직 적용
    private void startRound() {
        isBettingPhase = false;
        isGameStarted = true;
        currentPlayerIndex = 0;
        dealerScore = 0;

        broadcast("------------------------------------------------");
        broadcast("ROUND_START: Betting closed! The game begins.");

        dealerScore = drawCard();
        broadcast("INFO: Dealer's open card: [" + dealerScore + "]");

        for (ClientHandlerB p : players) {
            p.resetScoreOnly();
            
            int c1 = drawCard();
            int c2 = drawCard();
            
            p.addCardScore(c1 + c2);

            // ★ 수정됨: 카드 각각 전송
            p.sendMessage("INITIAL_DEAL: Dealer=[" + dealerScore + "], Cards=[" + c1 + "," + c2 + "], Total=[" + p.getScore() + "]");
            
            if (p.getScore() == 21) {
                p.sendMessage("INFO: Blackjack! (21 points)");
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
            int card = drawCard();
            player.addCardScore(card);
            
            // 1. 카드를 뽑았다고 알림
            broadcast("ACTION: [" + player.getPlayerId() + "] Hit! (Draw: " + card + ", Score: " + player.getScore() + ")");

            // 2. 점수 체크
            if (player.getScore() > 21) {
                broadcast("RESULT: [" + player.getPlayerId() + "] BUST! (Over 21)");
                nextTurn(); // 21점 넘으면 다음 사람 턴으로
            } else {
                // ★★★ 21점을 안 넘었으면 다시 턴을 줌 (버튼 활성화용)
                player.sendMessage("YOUR_TURN: Choose action (HIT, STAND, DOUBLEDOWN, SURRENDER).");
            }
            
        } else if (action.equalsIgnoreCase("Stand")) { // ★ 중괄호 오류 수정됨
            broadcast("ACTION: [" + player.getPlayerId() + "] Stand. (Turn Ended)");
            nextTurn();
        } else if (action.equalsIgnoreCase("DoubleDown")) {
            if (player.getBalance() < player.getCurrentBet()) {
                player.sendMessage("ERROR: Insufficient balance for Double Down.");
                return;
            }
            int additionalBet = player.getCurrentBet();
            player.decreaseBalance(additionalBet);
            player.setCurrentBet(player.getCurrentBet() + additionalBet);
            
            int card = drawCard();
            player.addCardScore(card);
            
            // ★ 수정됨: 뽑은 카드(Draw)를 보여줌
            broadcast("ACTION: [" + player.getPlayerId() + "] Double Down! (Draw: " + card + ", Final Score: " + player.getScore() + ")");

            if (player.getScore() > 21) {
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
        broadcast("DEALER_TURN: All player turns ended. Dealer draws cards. (Current: " + dealerScore + ")");
        try { Thread.sleep(1000); } catch (Exception e) {}

        while (dealerScore < 17) {
            int card = drawCard();
            dealerScore += card;
            broadcast("DEALER_DRAW: Dealer drew a card. (Dealer Score: " + dealerScore + ")");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        calculateResults();
    }

    private void calculateResults() {
        broadcast("--- [Final Results] ---");
        broadcast("Dealer Final Score: " + dealerScore);

        for (ClientHandlerB p : players) {
            String resultMsg;
            int pScore = p.getScore();
            int bet = p.getCurrentBet();
            String status = ""; // 승패 상태 (WIN, LOSE, TIE)

            if (p.isSurrender()) {
                int returnMoney = bet / 2;
                p.increaseBalance(returnMoney);
                resultMsg = "Surrender (Given up: " + returnMoney + " returned)";
                status = "LOSE"; // 서렌더는 패배 처리
            } else if (pScore > 21) {
                resultMsg = "Lose (Bust)";
                status = "LOSE";
            } else if (dealerScore > 21) {
                int prize = bet * 2;
                p.increaseBalance(prize);
                resultMsg = "Win! (Dealer Bust) - Prize: " + prize;
                status = "WIN";
            } else if (pScore > dealerScore) {
                int prize = bet * 2;
                p.increaseBalance(prize);
                resultMsg = "Win! (" + pScore + " vs " + dealerScore + ") - Prize: " + prize;
                status = "WIN";
            } else if (pScore == dealerScore) {
                p.increaseBalance(bet);
                resultMsg = "Tie (Push)";
                status = "TIE";
            } else {
                resultMsg = "Lose (" + pScore + " vs " + dealerScore + ") - Bet lost.";
                status = "LOSE";
            }
            
            broadcast(p.getPlayerId() + ": " + resultMsg);
            p.sendMessage("INFO: Balance after settlement: [" + p.getBalance() + "]");
            
            // ★ 핵심: 승패 결과를 명확한 신호로 따로 보내줍니다!
            p.sendMessage("GAME_RESULT:" + status);
        }
        
        isGameStarted = false;
        isBettingPhase = false;
        broadcast("GAME_END: Round ended. Type START to play again.");
    }

    private int drawCard() {
        return (int)(Math.random() * 10) + 2;
    }
}