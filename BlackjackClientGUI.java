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
    private String lastGameResult = ""; 
    private Color lastResultColor = Color.BLACK;
    
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
        gameStatusLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        panel.add(gameStatusLabel);
        
        return panel;
    }

    private JPanel createGameBoard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("게임 보드"));
        
        // ★ 디자인 개선: 고급스러운 카지노 펠트 색상
        Color tableColor = new Color(0, 102, 51); 
        panel.setBackground(tableColor); 
        
        // 딜러 영역
        JPanel dealerSection = new JPanel(new BorderLayout());
        dealerSection.setBackground(tableColor);
        dealerSection.setBorder(BorderFactory.createTitledBorder(null, "딜러", 
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, 
            javax.swing.border.TitledBorder.DEFAULT_POSITION, 
            new Font("맑은 고딕", Font.BOLD, 12), Color.WHITE));
        
        dealerScoreLabel = new JLabel("점수: -", JLabel.CENTER);
        dealerScoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        dealerScoreLabel.setForeground(Color.WHITE);
        dealerSection.add(dealerScoreLabel, BorderLayout.NORTH);
        
        dealerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        dealerCardsPanel.setBackground(tableColor);
        dealerSection.add(dealerCardsPanel, BorderLayout.CENTER);
        
        // 플레이어 영역
        JPanel playerSection = new JPanel(new BorderLayout());
        playerSection.setBackground(tableColor);
        playerSection.setBorder(BorderFactory.createTitledBorder(null, "플레이어", 
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, 
            javax.swing.border.TitledBorder.DEFAULT_POSITION, 
            new Font("맑은 고딕", Font.BOLD, 12), Color.WHITE));
        
        playerScoreLabel = new JLabel("점수: -", JLabel.CENTER);
        playerScoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        playerScoreLabel.setForeground(Color.WHITE);
        playerSection.add(playerScoreLabel, BorderLayout.NORTH);
        
        playerCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        playerCardsPanel.setBackground(tableColor);
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
        balanceLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        panel.add(balanceLabel);
        
        currentBetLabel = new JLabel("현재 베팅: " + currentBet);
        currentBetLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
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

    // ★ 디자인 개선: 버튼 꾸미기 도우미 함수
    private void decorateButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("게임 액션"));
        panel.setPreferredSize(new Dimension(160, 200));
        
        hitButton = new JButton("HIT (히트)");
        decorateButton(hitButton, new Color(46, 204, 113)); // 밝은 초록

        standButton = new JButton("STAND (스탠드)");
        decorateButton(standButton, new Color(231, 76, 60)); // 붉은색

        doubleDownButton = new JButton("DOUBLE DOWN");
        decorateButton(doubleDownButton, new Color(241, 196, 15)); // 노란색
        doubleDownButton.setForeground(Color.BLACK);

        surrenderButton = new JButton("SURRENDER");
        decorateButton(surrenderButton, new Color(149, 165, 166)); // 회색

        hitButton.addActionListener(e -> sendAction("Hit"));
        standButton.addActionListener(e -> sendAction("Stand"));
        doubleDownButton.addActionListener(e -> sendAction("DoubleDown"));
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
            
            // ★ 수정됨: 연결 직후 버튼 상태 갱신
            updateUIState();
            
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
            lastGameResult = ""; // ★ 새 게임 시작하면 결과 텍스트 초기화
            updateUIState();
        } else if (msg.contains("ROUND_START")) {
            isBettingPhase = false;
            lastGameResult = ""; // ★ 라운드 시작하면 결과 텍스트 초기화
            clearCards();
            updateUIState();
        } else if (msg.contains("INITIAL_DEAL")) {
            parseInitialDeal(msg);
        } else if (msg.contains("Dealer drew") || msg.contains("DEALER_DRAW")) {
            parseDealerCard(msg);
        } else if (msg.contains("Hit!") || msg.contains("Double Down!")) {
            parsePlayerCard(msg);
        } else if (msg.contains("Your current balance") || msg.contains("Balance after")) {
            parseBalance(msg);
        } else if (msg.contains("placed a bet")) {
            parseBet(msg);
        } else if (msg.contains("GAME_END")) {
            isMyTurn = false;
            isBettingPhase = false;
            updateUIState();
            
        // ★ 핵심: 승패 신호를 받으면 변수에 저장하고 화면 갱신
        } else if (msg.startsWith("GAME_RESULT:")) {
            String result = msg.split(":")[1].trim();
            if (result.equals("WIN")) {
                lastGameResult = "WIN!";
                lastResultColor = Color.decode("#e7bb54"); // 황금색
            } else if (result.equals("LOSE")) {
                lastGameResult = "LOSE...";
                lastResultColor = Color.decode("#4d3add"); // 보라색
            } else if (result.equals("TIE")) {
                lastGameResult = "TIE (PUSH)";
                lastResultColor = Color.WHITE;
            }
            updateUIState();
        }
    }

    // ★ 수정됨: 카드 각각 보여주는 로직 적용 (Initial Deal)
    private void parseInitialDeal(String msg) {
        // 형식: "INITIAL_DEAL: Dealer=[3], Cards=[10,6], Total=[16]"
        try {
            // 1. 딜러 카드 파싱
            String dealerPart = msg.substring(msg.indexOf("Dealer=[") + 8);
            dealerPart = dealerPart.substring(0, dealerPart.indexOf("]"));
            dealerScore = Integer.parseInt(dealerPart);
            
            // 2. 내 카드 목록 파싱
            String cardsPart = msg.substring(msg.indexOf("Cards=[") + 7);
            cardsPart = cardsPart.substring(0, cardsPart.indexOf("]"));
            
            // 3. 점수 파싱
            String totalPart = msg.substring(msg.indexOf("Total=[") + 7);
            totalPart = totalPart.substring(0, totalPart.indexOf("]"));
            playerScore = Integer.parseInt(totalPart);

            // 화면 그리기
            clearCards();
            addDealerCard(dealerScore);
            
            String[] cards = cardsPart.split(",");
            for (String c : cards) {
                addPlayerCard(Integer.parseInt(c.trim()));
            }
            
            updateScores();
            
        } catch (Exception e) {
            appendLog("카드 파싱 오류: " + e.getMessage());
        }
    }

    private void parseDealerCard(String msg) {
        try {
            // 다양한 형식 지원 (괄호 안 Score 파싱)
            int startIdx = msg.lastIndexOf("Score: ") + 7;
            if (startIdx < 7) startIdx = msg.lastIndexOf("Score:") + 6;
            
            int endIdx = msg.indexOf(")", startIdx);
            if (endIdx == -1) endIdx = msg.length();
            
            String scoreStr = msg.substring(startIdx, endIdx).trim().replaceAll("[^0-9]", "");
            int newScore = Integer.parseInt(scoreStr);
            
            int cardValue = newScore - dealerScore;
            dealerScore = newScore;
            addDealerCard(cardValue);
            updateScores();
        } catch (Exception e) {
             // 딜러 드로우 파싱 실패 시 조용히 넘어감 (로그만 남김)
             // appendLog("딜러 카드 파싱 오류: " + e.getMessage());
        }
    }

    // ★ 수정됨: 카드 각각 보여주는 로직 적용 (Hit / DoubleDown)
    private void parsePlayerCard(String msg) {
        // 형식: "Hit! (Draw: 5, Score: 21)"
        try {
            if (msg.contains("Draw:")) {
                int drawIndex = msg.indexOf("Draw:") + 5;
                int commaIndex = msg.indexOf(",", drawIndex);
                if (commaIndex == -1) commaIndex = msg.indexOf(")", drawIndex);
                
                String cardStr = msg.substring(drawIndex, commaIndex).trim();
                int cardValue = Integer.parseInt(cardStr);
                
                int scoreIndex = msg.indexOf("Score:") + 6;
                int endParenIndex = msg.indexOf(")", scoreIndex);
                String scoreStr = msg.substring(scoreIndex, endParenIndex).trim();
                
                playerScore = Integer.parseInt(scoreStr);
                addPlayerCard(cardValue);
                updateScores();
            }
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

    // ★ 디자인 개선: 카드 모양 예쁘게
    private JLabel createCardLabel(int value, Color bgColor, Color textColor) {
        JLabel label = new JLabel(String.valueOf(value), JLabel.CENTER);
        label.setPreferredSize(new Dimension(80, 110));
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(textColor);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
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

    // ★ 수정됨: 버튼 활성화 로직 개선 (강제 활성화 포함)
    private void updateUIState() {
        startGameButton.setEnabled(isConnected && !isBettingPhase && !isMyTurn);
        betButton.setEnabled(isConnected && isBettingPhase);
        
        boolean canAct = isConnected && isMyTurn && !isBettingPhase;
        
        hitButton.setEnabled(canAct);
        standButton.setEnabled(canAct);
        doubleDownButton.setEnabled(canAct);
        surrenderButton.setEnabled(canAct);
        
        // 상태 메시지 우선순위 처리
        if (isMyTurn) {
            gameStatusLabel.setText("당신의 턴");
            gameStatusLabel.setForeground(Color.YELLOW);
        } else if (isBettingPhase) {
            gameStatusLabel.setText("베팅 단계");
            gameStatusLabel.setForeground(Color.ORANGE);
        } else if (!lastGameResult.isEmpty()) { 
            // ★ 게임 결과가 있으면 그걸 보여줍니다 (WIN! / LOSE...)
            gameStatusLabel.setText(lastGameResult);
            gameStatusLabel.setForeground(lastResultColor);
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