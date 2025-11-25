import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    private List<ClientHandlerB> players = new ArrayList<>();
    private int currentPlayerIndex = 0; // Current player's turn index (starts from 0)
    private boolean isGameStarted = false;
    private boolean isBettingPhase = false; // Is currently in "Betting Phase"

    // Dealer Information
    private int dealerScore = 0;

    // Player Join
    // Synchronized to handle multi-threaded access
    public synchronized void join(ClientHandlerB player) {
        players.add(player);
        broadcast("SERVER_MSG: Player [" + player.getPlayerId() + "] joined. (Total: " + players.size() + ")");
    }

    // Player Leave
    // Synchronized to handle multi-threaded access
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
        if (isBettingPhase || isGameStarted) return; // Ignore if game is already running

        isBettingPhase = true; // Start betting phase
        isGameStarted = false;

        // Reset all players' status
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
        // Check player's balance (ClientHandler must have getBalance method)
        if (player.getBalance() >= amount) {
            player.decreaseBalance(amount); // Deduct money
            player.setCurrentBet(amount);   // Set bet amount
            player.setBetPlaced(true);      // Mark as bet placed   

            broadcast("SERVER_MSG: [" + player.getPlayerId() + "] placed a bet of " + amount + ".");

            // ★ Check if everyone has placed a bet
            checkAllBets();
            return true; // Success
        } else {
            player.sendMessage("ERROR: Insufficient balance.");
            return false; // Failed (Not enough money)
        }
    }

    private void checkAllBets() {
        for (ClientHandlerB p : players) {
            if (!p.isBetPlaced()) {
                return; // Someone hasn't bet yet
            }
        }

        // All bets placed -> Start actual game logic
        startRound();
    }

    // ★ [New] Deal cards and start the round
    private void startRound() {
        isBettingPhase = false;
        isGameStarted = true;
        currentPlayerIndex = 0;
        dealerScore = 0;

        broadcast("------------------------------------------------");
        broadcast("ROUND_START: Betting closed! The game begins.");

        // 1. Dealer draws a card (1 card open)
        dealerScore = drawCard();
        broadcast("INFO: Dealer's open card: [" + dealerScore + "]");

        // 2. ★ Deal 2 cards to every player
        for (ClientHandlerB p : players) {
            // Reset state (ensure score is 0)
            p.resetScoreOnly(); 

            // Draw 2 cards
            int c1 = drawCard();
            int c2 = drawCard();
            
            // Apply score
            p.addCardScore(c1 + c2);

            // ★ Send 'Dealer Card' and 'My Card' info to each player individually
            p.sendMessage("INITIAL_DEAL: Dealer=[" + dealerScore + "], Your Score=[" + p.getScore() + "]");
            
            // Check for Blackjack (21) immediately (Optional)
             if (p.getScore() == 21) {
                 p.sendMessage("INFO: Blackjack! (21 points)");
             }
        }
        
        broadcast("------------------------------------------------");

        // 3. Start the first player's turn after dealing is complete
        notifyCurrentPlayer();
    }

    // ★ Handle Player Actions (HIT / STAND / DOUBLE DOWN / SURRENDER)
    public synchronized void handlePlayerAction(ClientHandlerB player, String action) {
        if (!isGameStarted) {
            player.sendMessage("ERROR: Game is not in progress.");
            return;
        }

        // 1. Check Turn (Reject if it's not my turn)
        ClientHandlerB currentTurnPlayer = players.get(currentPlayerIndex);
        if (!player.equals(currentTurnPlayer)) {
            player.sendMessage("ERROR: It is not your turn. (Current turn: " + currentTurnPlayer.getPlayerId() + ")");
            return;
        }

        // 2. Process Action
        if (action.equalsIgnoreCase("Hit")) {
            int card = drawCard();
            player.addCardScore(card);
            broadcast("ACTION: [" + player.getPlayerId() + "] Hit! (Current Score: " + player.getScore() + ")");

            if (player.getScore() > 21) {
                broadcast("RESULT: [" + player.getPlayerId() + "] BUST! (Over 21)");
                nextTurn(); // Busted, move to next player
            }
        } else if (action.equalsIgnoreCase("Stand")) {
            broadcast("ACTION: [" + player.getPlayerId() + "] Stand. (Turn Ended)");
            nextTurn(); // Move to next player
        } else if (action.equalsIgnoreCase("DoubleDown")) { // ★ Added
            // Check balance
            if (player.getBalance() < player.getCurrentBet()) {
                player.sendMessage("ERROR: Insufficient balance for Double Down.");
                return;
            }
            // Double the bet
            int additionalBet = player.getCurrentBet(); // Bet same amount again
            player.decreaseBalance(additionalBet);
            player.setCurrentBet(player.getCurrentBet() + additionalBet);
            
            // Draw only 1 card and end turn
            int card = drawCard();
            player.addCardScore(card);
            
            broadcast("ACTION: [" + player.getPlayerId() + "] Double Down! (Bet Doubled: " + player.getCurrentBet() + ")");
            broadcast("INFO: [" + player.getPlayerId() + "] Drew card: " + card + " -> Final Score: " + player.getScore());

            if (player.getScore() > 21) {
                broadcast("RESULT: [" + player.getPlayerId() + "] BUST! (Lost)");
            }
            nextTurn(); // Force end turn

        } else if (action.equalsIgnoreCase("Surrender")) { // ★ Added
            player.setSurrender(true);
            broadcast("ACTION: [" + player.getPlayerId() + "] Surrender. (Given up)");
            nextTurn(); // End turn
        }
    }

    // Move to next turn
    private void nextTurn() {
        currentPlayerIndex++;

        // If all players are done -> Start Dealer's Turn
        if (currentPlayerIndex >= players.size()) {
            playDealerTurn();
        } else {
            notifyCurrentPlayer();
        }
    }

    // Notify the current player
    private void notifyCurrentPlayer() {
        ClientHandlerB p = players.get(currentPlayerIndex);
        broadcast("TURN: It is [" + p.getPlayerId() + "]'s turn.");
        p.sendMessage("YOUR_TURN: Choose action (HIT, STAND, DOUBLEDOWN, SURRENDER).");
    }

    // ★ Dealer Turn Logic (Rule: Stand on 17)
    private void playDealerTurn() {
        broadcast("DEALER_TURN: All player turns ended. Dealer draws cards. (Current: " + dealerScore + ")");
        
        try { Thread.sleep(1000); } catch (Exception e) {} // Delay for suspense

        // Dealer must draw until 17 or higher
        while (dealerScore < 17) {
            int card = drawCard();
            dealerScore += card;
            broadcast("DEALER_DRAW: Dealer drew a card. (Dealer Score: " + dealerScore + ")");
            try { Thread.sleep(1000); } catch (Exception e) {}
        }

        calculateResults(); // Calculate results
    }

    // Calculate Results
    private void calculateResults() {
        broadcast("--- [Final Results] ---");
        broadcast("Dealer Final Score: " + dealerScore);

        for (ClientHandlerB p : players) {
            String resultMsg;
            int pScore = p.getScore();
            int bet = p.getCurrentBet();

            // 1. Surrender (Already processed - half refund)
            if (p.isSurrender()) {
                int returnMoney = bet / 2;
                p.increaseBalance(returnMoney); // Refund half
                resultMsg = "Surrender (Given up: " + returnMoney + " returned)";
            
            // 2. Player Bust (Lose)
            } else if (pScore > 21) {
                resultMsg = "Lose (Bust)";
            
            // 3. Dealer Bust (Win)
            } else if (dealerScore > 21) {
                int prize = bet * 2;
                p.increaseBalance(prize); // Pay 2x bet
                resultMsg = "Win! (Dealer Bust) - Prize: " + prize;
            
            // 4. Player Higher Score (Win)
            } else if (pScore > dealerScore) {
                int prize = bet * 2;
                p.increaseBalance(prize); // Pay 2x bet
                resultMsg = "Win! (" + pScore + " vs " + dealerScore + ") - Prize: " + prize;

            // 5. Tie (Push)
            } else if (pScore == dealerScore) {
                p.increaseBalance(bet); // Return bet
                resultMsg = "Tie (Push)";

            // 6. Dealer Higher Score (Lose)
            } else {
                resultMsg = "Lose (" + pScore + " vs " + dealerScore + ") - Bet lost.";
            }
            broadcast(p.getPlayerId() + ": " + resultMsg);
            p.sendMessage("INFO: Balance after settlement: [" + p.getBalance() + "]");
        }
        
        isGameStarted = false; // Game Ended
        isBettingPhase = false; // Reset phase
        broadcast("GAME_END: Round ended. Type START to play again.");
    }

    // Card Draw Utility (Temp: Random 2~11)
    private int drawCard() {
        return (int)(Math.random() * 10) + 2; // Random between 2~11 (Simplified)
    }
}
