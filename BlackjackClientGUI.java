import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BlackjackClientGUI extends JFrame {
    // Network Components
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    
    // Server Info
    private String serverIP = "127.0.0.1";
    private int serverPort = 6789;
    
    // UI Components
    private JTextArea messageLog;
    private JLabel dealerScoreLabel;
    private JLabel playerScoreLabel;
    private JLabel balanceLabel;
    private JLabel currentBetLabel;
    private JLabel gameStatusLabel;
    
    private JPanel dealerCardsPanel;
    private JPanel playerCardsPanel;
    
    private JButton hitButton;
    private JButton standButton;
    private JButton doubleDownButton;
    private JButton surrenderButton;
    private JButton startGameButton;
    private JButton betButton;
    private JButton connectButton;
    
    private JTextField betAmountField;
    private JTextField serverIPField;
    private JTextField serverPortField;
    
    // Game State
    private int playerScore = 0;
    private int dealerScore = 0;
    private int balance = 1000;
    private int currentBet = 0;
    private boolean isConnected = false;
    private boolean isMyTurn = false;
    private boolean isBettingPhase = false;
    
    private List<JLabel> dealerCardLabels = new ArrayList<>();
    private List<JLabel> playerCardLabels = new ArrayList<>();

    public BlackjackClientGUI() {
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("블랙잭 게임 클라이언트");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 상단: 서버 연결 패널
        JPanel topPanel = createConnectionPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // 중앙: 게임 보드
        JPanel gameBoard = createGameBoard();
        add(gameBoard, BorderLayout.CENTER);
        
        // 왼쪽: 정보 패널
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.WEST);
        
        // 오른쪽: 액션 버튼 패널
        JPanel actionPanel = createActionPanel();
        add(actionPanel, BorderLayout.EAST);
        
        // 하단: 메시지 로그
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);
        
        pack();
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        
        updateUIState();
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("서버 연결"));
        
        panel.add(new JLabel("서버 IP:"));
        serverIPField = new JTextField("127.0.0.1", 12);
        panel.add(serverIPField);
        
        panel.add(new JLabel("포트:"));
        serverPortField = new JTextField("6789", 6);
        panel.add(serverPortField);
        
        connectButton = new JButton("연결");
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        gameStatusLabel = new JLabel("연결 안됨");
        gameStatusLabel.setForeground(Color.RED);
        panel.add(gameStatusLabel);
        
        return panel;
    }

    private JPanel createGameBoard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("게임 보드"));
        panel.setBackground(new Color(34, 139, 34)); // 카지노 그린
        
        // 딜러 영역
        JPanel dealerSection = new JPanel(new BorderLayout());
        dealerSection.setBackground(new Color(34, 139, 34));
        dealerSection.setBorder(BorderFactory.createTitledBorder("딜러"));
        
        dealerScoreLabel = new JLabel("점수: -", JLabel.CENTER);
        dealerScoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        dealerScoreLabel.setForeground(Color.WHITE);
        dealerSection.add(dealerScoreLabel, BorderLayout.NORTH);
        
        dealerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        dealerCardsPanel.setBackground(new Color(34, 139, 34));
        dealerSection.add(dealerCardsPanel, BorderLayout.CENTER);
        
        // 플레이어 영역
        JPanel playerSection = new JPanel(new BorderLayout());
        playerSection.setBackground(new Color(34, 139, 34));
        playerSection.setBorder(BorderFactory.createTitledBorder("플레이어"));
        
        playerScoreLabel = new JLabel("점수: -", JLabel.CENTER);
        playerScoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        playerScoreLabel.setForeground(Color.WHITE);
        playerSection.add(playerScoreLabel, BorderLayout.NORTH);
        
        playerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        playerCardsPanel.setBackground(new Color(34, 139, 34));
        playerSection.add(playerCardsPanel, BorderLayout.CENTER);
        
        panel.add(dealerSection, BorderLayout.NORTH);
        panel.add(playerSection, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("게임 정보"));
        panel.setPreferredSize(new Dimension(200, 200));
        
        balanceLabel = new JLabel("잔액: " + balance);
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(balanceLabel);
        
        currentBetLabel = new JLabel("현재 베팅: " + currentBet);
        currentBetLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(currentBetLabel);
        
        panel.add(new JLabel("")); // 공간
        
        panel.add(new JLabel("베팅 금액:"));
        betAmountField = new JTextField("100");
        panel.add(betAmountField);
        
        betButton = new JButton("베팅");
        betButton.addActionListener(e -> placeBet());
        panel.add(betButton);
        
        startGameButton = new JButton("게임 시작");
        startGameButton.addActionListener(e -> startGame());
        panel.add(startGameButton);
        
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("게임 액션"));
        panel.setPreferredSize(new Dimension(150, 200));
        
        hitButton = new JButton("HIT (히트)");
        hitButton.setFont(new Font("Arial", Font.BOLD, 14));
        hitButton.setBackground(new Color(0, 150, 0));
        hitButton.setForeground(Color.WHITE);
        hitButton.addActionListener(e -> sendAction("Hit"));
        
        standButton = new JButton("STAND (스탠드)");
        standButton.setFont(new Font("Arial", Font.BOLD, 14));
        standButton.setBackground(new Color(200, 0, 0));
        standButton.setForeground(Color.WHITE);
        standButton.addActionListener(e -> sendAction("Stand"));
        
        doubleDownButton = new JButton("DOUBLE DOWN");
        doubleDownButton.setFont(new Font("Arial", Font.BOLD, 12));
        doubleDownButton.setBackground(new Color(200, 150, 0));
        doubleDownButton.setForeground(Color.WHITE);
        doubleDownButton.addActionListener(e -> sendAction("DoubleDown"));
        
        surrenderButton = new JButton("SURRENDER");
        surrenderButton.setFont(new Font("Arial", Font.BOLD, 12));
        surrenderButton.setBackground(new Color(100, 100, 100));
        surrenderButton.setForeground(Color.WHITE);
        surrenderButton.addActionListener(e -> sendAction("Surrender"));
        
        panel.add(hitButton);
        panel.add(standButton);
        panel.add(doubleDownButton);
        panel.add(surrenderButton);
        
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("서버 메시지"));
        
        messageLog = new JTextArea(8, 50);
        messageLog.setEditable(false);
        messageLog.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        messageLog.setBackground(new Color(240, 240, 240));
        
        JScrollPane scrollPane = new JScrollPane(messageLog);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private void connectToServer() {
        if (isConnected) {
            appendLog("이미 연결되어 있습니다.");
            return;
        }
        
        try {
            serverIP = serverIPField.getText().trim();
            serverPort = Integer.parseInt(serverPortField.getText().trim());
            
            socket = new Socket(serverIP, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            isConnected = true;
            gameStatusLabel.setText("연결됨");
            gameStatusLabel.setForeground(Color.GREEN);
            connectButton.setEnabled(false);
            appendLog("서버에 연결되었습니다: " + serverIP + ":" + serverPort);
            
            // Start listener thread
            listenerThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        final String message = msg;
                        SwingUtilities.invokeLater(() -> processServerMessage(message));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        appendLog("서버 연결이 끊어졌습니다.");
                        disconnect();
                    });
                }
            });
            listenerThread.start();
            
        } catch (Exception e) {
            appendLog("연결 실패: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage(), 
                "연결 오류", JOptionPane.ERROR_MESSAGE);
            isConnected = false;
        }
    }

    private void disconnect() {
        isConnected = false;
        gameStatusLabel.setText("연결 안됨");
        gameStatusLabel.setForeground(Color.RED);
        connectButton.setEnabled(true);
        
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
        
        updateUIState();
    }

    private void processServerMessage(String msg) {
        appendLog("[서버] " + msg);
        
        // Parse server messages
        if (msg.contains("YOUR_TURN")) {
            isMyTurn = true;
            appendLog(">>> 당신의 턴입니다! 액션을 선택하세요.");
            updateUIState();
        } else if (msg.contains("TURN:") && !msg.contains("YOUR_TURN")) {
            isMyTurn = false;
            updateUIState();
        } else if (msg.contains("GAME_PHASE: Betting Phase")) {
            isBettingPhase = true;
            isMyTurn = false;
            updateUIState();
        } else if (msg.contains("ROUND_START")) {
            isBettingPhase = false;
            clearCards();
            updateUIState();
        } else if (msg.contains("INITIAL_DEAL")) {
            parseInitialDeal(msg);
        } else if (msg.contains("Dealer drew") || msg.contains("DEALER_DRAW")) {
            parseDealerCard(msg);
        } else if (msg.contains("Hit!") || msg.contains("Drew card")) {
            parsePlayerCard(msg);
        } else if (msg.contains("Your current balance") || msg.contains("Balance after")) {
            parseBalance(msg);
        } else if (msg.contains("placed a bet")) {
            parseBet(msg);
        } else if (msg.contains("GAME_END")) {
            isMyTurn = false;
            isBettingPhase = false;
            updateUIState();
        }
    }

    private void parseInitialDeal(String msg) {
        // Format: "INITIAL_DEAL: Dealer=[X], Your Score=[Y]"
        try {
            String[] parts = msg.split(", ");
            if (parts.length >= 2) {
                String dealerPart = parts[0].split("=")[1].replace("]", "");
                String playerPart = parts[1].split("=")[1].replace("]", "");
                
                dealerScore = Integer.parseInt(dealerPart);
                playerScore = Integer.parseInt(playerPart);
                
                addDealerCard(dealerScore);
                // Initial deal gives 2 cards, but we only see total score
                // Split into two cards (simplified)
                int card1 = playerScore / 2;
                int card2 = playerScore - card1;
                addPlayerCard(card1);
                addPlayerCard(card2);
                
                updateScores();
            }
        } catch (Exception e) {
            appendLog("카드 정보 파싱 오류: " + e.getMessage());
        }
    }

    private void parseDealerCard(String msg) {
        // Format: "DEALER_DRAW: Dealer drew a card. (Dealer Score: X)"
        try {
            int startIdx = msg.lastIndexOf("Score: ") + 7;
            int endIdx = msg.indexOf(")", startIdx);
            if (endIdx == -1) endIdx = msg.length();
            
            int newScore = Integer.parseInt(msg.substring(startIdx, endIdx).trim());
            int cardValue = newScore - dealerScore;
            dealerScore = newScore;
            addDealerCard(cardValue);
            updateScores();
        } catch (Exception e) {
            // Try alternative parsing
            try {
                String[] parts = msg.split(":");
                for (String part : parts) {
                    if (part.contains("Score")) {
                        String[] scoreParts = part.split("Score");
                        if (scoreParts.length > 1) {
                            String scoreStr = scoreParts[1].trim().replaceAll("[^0-9]", "");
                            dealerScore = Integer.parseInt(scoreStr);
                            updateScores();
                            break;
                        }
                    }
                }
            } catch (Exception e2) {}
        }
    }

    private void parsePlayerCard(String msg) {
        // Format: "Hit! (Current Score: X)" or "Drew card: Y -> Final Score: X"
        try {
            if (msg.contains("->")) {
                String[] parts = msg.split("->");
                String cardPart = parts[0].split(":")[1].trim();
                String scorePart = parts[1].split(":")[1].trim();
                
                int cardValue = Integer.parseInt(cardPart.replaceAll("[^0-9]", ""));
                playerScore = Integer.parseInt(scorePart.replaceAll("[^0-9]", ""));
                addPlayerCard(cardValue);
            } else {
                int startIdx = msg.indexOf("Score: ") + 7;
                int endIdx = msg.indexOf(")", startIdx);
                if (endIdx == -1) endIdx = msg.length();
                
                int newScore = Integer.parseInt(msg.substring(startIdx, endIdx).trim());
                int cardValue = newScore - playerScore;
                playerScore = newScore;
                addPlayerCard(cardValue);
            }
            updateScores();
        } catch (Exception e) {
            appendLog("플레이어 카드 파싱 오류: " + e.getMessage());
        }
    }

    private void parseBalance(String msg) {
        try {
            int startIdx = msg.indexOf("[") + 1;
            int endIdx = msg.indexOf("]", startIdx);
            if (endIdx > startIdx) {
                balance = Integer.parseInt(msg.substring(startIdx, endIdx));
                balanceLabel.setText("잔액: " + balance);
            }
        } catch (Exception e) {}
    }

    private void parseBet(String msg) {
        try {
            if (msg.contains("placed a bet of")) {
                int startIdx = msg.indexOf("bet of ") + 7;
                int endIdx = msg.indexOf(".", startIdx);
                if (endIdx == -1) endIdx = msg.length();
                currentBet = Integer.parseInt(msg.substring(startIdx, endIdx).trim());
                currentBetLabel.setText("현재 베팅: " + currentBet);
            }
        } catch (Exception e) {}
    }

    private void addDealerCard(int value) {
        JLabel cardLabel = createCardLabel(value, Color.WHITE, Color.BLACK);
        dealerCardLabels.add(cardLabel);
        dealerCardsPanel.add(cardLabel);
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
    }

    private void addPlayerCard(int value) {
        JLabel cardLabel = createCardLabel(value, Color.WHITE, Color.BLUE);
        playerCardLabels.add(cardLabel);
        playerCardsPanel.add(cardLabel);
        playerCardsPanel.revalidate();
        playerCardsPanel.repaint();
    }

    private JLabel createCardLabel(int value, Color bgColor, Color textColor) {
        JLabel label = new JLabel(String.valueOf(value), JLabel.CENTER);
        label.setPreferredSize(new Dimension(60, 90));
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(textColor);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        return label;
    }

    private void clearCards() {
        dealerCardLabels.clear();
        playerCardLabels.clear();
        dealerCardsPanel.removeAll();
        playerCardsPanel.removeAll();
        dealerCardsPanel.revalidate();
        playerCardsPanel.repaint();
        dealerScore = 0;
        playerScore = 0;
        updateScores();
    }

    private void updateScores() {
        dealerScoreLabel.setText("점수: " + (dealerScore > 0 ? dealerScore : "-"));
        playerScoreLabel.setText("점수: " + (playerScore > 0 ? playerScore : "-"));
    }

    private void startGame() {
        if (!isConnected) {
            appendLog("먼저 서버에 연결하세요.");
            return;
        }
        if (out != null) {
            out.println("START");
            appendLog("게임 시작 요청을 보냈습니다.");
        }
    }

    private void placeBet() {
        if (!isConnected) {
            appendLog("먼저 서버에 연결하세요.");
            return;
        }
        
        try {
            int amount = Integer.parseInt(betAmountField.getText().trim());
            if (out != null) {
                out.println("PLACE_BET:" + amount);
                appendLog("베팅 요청: " + amount);
            }
        } catch (NumberFormatException e) {
            appendLog("올바른 숫자를 입력하세요.");
            JOptionPane.showMessageDialog(this, "올바른 베팅 금액을 입력하세요.", 
                "입력 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAction(String action) {
        if (!isConnected) {
            appendLog("먼저 서버에 연결하세요.");
            return;
        }
        if (!isMyTurn) {
            appendLog("당신의 턴이 아닙니다.");
            return;
        }
        if (out != null) {
            out.println("PLAYER_ACTION:" + action);
            appendLog("액션 전송: " + action);
            isMyTurn = false;
            updateUIState();
        }
    }

    private void updateUIState() {
        // Enable/disable buttons based on game state
        boolean canAct = isConnected && isMyTurn && !isBettingPhase;
        
        hitButton.setEnabled(canAct);
        standButton.setEnabled(canAct);
        doubleDownButton.setEnabled(canAct);
        surrenderButton.setEnabled(canAct);
        
        startGameButton.setEnabled(isConnected && !isBettingPhase && !isMyTurn);
        betButton.setEnabled(isConnected && isBettingPhase);
        
        // Visual feedback
        if (isMyTurn) {
            gameStatusLabel.setText("당신의 턴");
            gameStatusLabel.setForeground(Color.YELLOW);
        } else if (isBettingPhase) {
            gameStatusLabel.setText("베팅 단계");
            gameStatusLabel.setForeground(Color.ORANGE);
        } else if (isConnected) {
            gameStatusLabel.setText("연결됨");
            gameStatusLabel.setForeground(Color.GREEN);
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            messageLog.append(message + "\n");
            messageLog.setCaretPosition(messageLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            new BlackjackClientGUI().setVisible(true);
        });
    }
}
