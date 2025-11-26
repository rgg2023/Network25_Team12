import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
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
    private JLabel resultLabel; // ★ 테두리가 적용된 커스텀 라벨
    
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
        
        JPanel topPanel = createConnectionPanel();
        add(topPanel, BorderLayout.NORTH);
        
        JPanel gameBoard = createGameBoard();
        add(gameBoard, BorderLayout.CENTER);
        
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.WEST);
        
        JPanel actionPanel = createActionPanel();
        add(actionPanel, BorderLayout.EAST);
        
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
        
        // ★ 수정됨: 일반 JLabel 대신 커스텀 OutlineLabel 사용
        resultLabel = new OutlineLabel("");
        resultLabel.setHorizontalAlignment(JLabel.CENTER);
        resultLabel.setFont(new Font("맑은 고딕", Font.BOLD, 80));
        panel.add(resultLabel, BorderLayout.CENTER);
        
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
        panel.setPreferredSize(new Dimension(270, 200)); // 너비 수정됨
        
        balanceLabel = new JLabel("잔액: " + balance);
        balanceLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        panel.add(balanceLabel);
        
        currentBetLabel = new JLabel("현재 베팅: " + currentBet);
        currentBetLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        panel.add(currentBetLabel);
        
        panel.add(new JLabel("")); 
        
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

    private void decorateButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        btn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("게임 액션"));
        panel.setPreferredSize(new Dimension(160, 200));
        
        hitButton = new JButton("HIT (히트)");
        decorateButton(hitButton, new Color(46, 204, 113));

        standButton = new JButton("STAND (스탠드)");
        decorateButton(standButton, new Color(231, 76, 60));

        doubleDownButton = new JButton("DOUBLE DOWN");
        decorateButton(doubleDownButton, new Color(241, 196, 15));

        surrenderButton = new JButton("SURRENDER");
        decorateButton(surrenderButton, new Color(149, 165, 166));

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
            
            updateUIState();
            
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
            resultLabel.setText("");
            updateUIState();
        } else if (msg.contains("ROUND_START")) {
            isBettingPhase = false;
            clearCards();
            resultLabel.setText("");
            updateUIState();
        } else if (msg.contains("INFO: Dealer's open card")) {
            parseDealerInitialCard(msg);
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
        } else if (msg.startsWith("GAME_RESULT:")) {
            String result = msg.split(":")[1].trim();
            if (result.equals("WIN")) {
                resultLabel.setText("WIN!");
                resultLabel.setForeground(Color.decode("#e7bb54"));
            } else if (result.equals("LOSE")) {
                resultLabel.setText("LOSE...");
                resultLabel.setForeground(Color.decode("#064abf")); // ★ 요청하신 파란색
            } else if (result.equals("TIE")) {
                resultLabel.setText("TIE");
                resultLabel.setForeground(Color.decode("#a3a3a3"));
            }
            updateUIState();
        }
    }

    private void parseDealerInitialCard(String msg) {
        try {
            // 형식: "INFO: Dealer's open card: [A] (Score: 11)"
            int cardStartIdx = msg.indexOf("[") + 1;
            int cardEndIdx = msg.indexOf("]", cardStartIdx);
            if (cardEndIdx > cardStartIdx) {
                String cardName = msg.substring(cardStartIdx, cardEndIdx).trim();
                
                // 점수도 파싱
                int scoreStartIdx = msg.indexOf("Score:") + 6;
                int scoreEndIdx = msg.indexOf(")", scoreStartIdx);
                if (scoreEndIdx > scoreStartIdx) {
                    String scoreStr = msg.substring(scoreStartIdx, scoreEndIdx).trim();
                    dealerScore = Integer.parseInt(scoreStr);
                }
                
                // 딜러 카드가 이미 초기화되어 있지 않으면 추가
                if (dealerCardLabels.isEmpty()) {
                    clearCards();
                    addDealerCard(cardName);
                    updateScores();
                }
            }
        } catch (Exception e) {}
    }

    private void parseInitialDeal(String msg) {
        try {
            // Cards 파싱 (예: "Cards=[A,7]")
            String cardsPart = msg.substring(msg.indexOf("Cards=[") + 7);
            cardsPart = cardsPart.substring(0, cardsPart.indexOf("]"));
            
            // Total 점수 파싱
            String totalPart = msg.substring(msg.indexOf("Total=[") + 7);
            totalPart = totalPart.substring(0, totalPart.indexOf("]"));
            playerScore = Integer.parseInt(totalPart);

            // 플레이어 카드 추가
            String[] cards = cardsPart.split(",");
            for (String c : cards) {
                addPlayerCard(c.trim()); // 카드 이름을 그대로 전달
            }
            updateScores();
            
        } catch (Exception e) {
            appendLog("카드 파싱 오류: " + e.getMessage());
        }
    }

    private void parseDealerCard(String msg) {
        try {
            // 메시지 형식 예: "DEALER_DRAW: Dealer drew [A]. (Dealer Score: 11)"
            
            // 1. 카드 이름 파싱 ([ ] 사이의 값 추출)
            int cardStartIdx = msg.indexOf("[");
            int cardEndIdx = msg.indexOf("]", cardStartIdx);
            
            if (cardStartIdx != -1 && cardEndIdx > cardStartIdx) {
                String cardName = msg.substring(cardStartIdx + 1, cardEndIdx).trim();
                // 카드 UI 추가
                addDealerCard(cardName);
            }
            
            // 2. 딜러 점수 파싱 ("Score:" 뒤의 숫자 추출)
            if (msg.contains("Score")) {
                // "Score" 키워드가 포함된 부분부터 끝까지 자른 뒤 숫자만 추출
                String scorePart = msg.substring(msg.lastIndexOf("Score"));
                String scoreStr = scorePart.replaceAll("[^0-9]", "");
                
                if (!scoreStr.isEmpty()) {
                    dealerScore = Integer.parseInt(scoreStr);
                    updateScores();
                }
            }
        } catch (Exception e) {
            appendLog("딜러 카드 파싱 오류: " + e.getMessage());
            e.printStackTrace(); // 디버깅을 위해 콘솔에도 출력
        }
    }

    private void parsePlayerCard(String msg) {
        try {
            // 형식: "Hit! (Draw: A, Score: 18)" 또는 "Double Down! (Draw: K, Final Score: 20)"
            if (msg.contains("Draw:")) {
                int drawIndex = msg.indexOf("Draw:") + 5;
                int commaIndex = msg.indexOf(",", drawIndex);
                int scoreIndex = msg.indexOf("Score:");
                
                String cardName;
                if (commaIndex != -1 && commaIndex < scoreIndex) {
                    cardName = msg.substring(drawIndex, commaIndex).trim();
                } else {
                    // Score 전까지가 카드 이름
                    cardName = msg.substring(drawIndex, scoreIndex).trim();
                }
                
                // 점수 파싱
                int scoreStartIdx = msg.indexOf("Score:") + 6;
                int endParenIndex = msg.indexOf(")", scoreStartIdx);
                if (endParenIndex == -1) endParenIndex = msg.length();
                
                String scoreStr = msg.substring(scoreStartIdx, endParenIndex).trim().replaceAll("[^0-9]", "");
                if (!scoreStr.isEmpty()) {
                    playerScore = Integer.parseInt(scoreStr);
                }
                
                addPlayerCard(cardName);
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
    
    private void addDealerCard(String cardName) {
        if (cardName == null || cardName.isEmpty()) return;

        JLabel cardLabel = createCardLabel(cardName, Color.WHITE, Color.BLACK);
        dealerCardLabels.add(cardLabel);
        dealerCardsPanel.add(cardLabel);
        
        // 패널 갱신
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
    }

    private void addPlayerCard(String cardName) {
        if (cardName == null || cardName.isEmpty()) return;
        
        JLabel cardLabel = createCardLabel(cardName, Color.WHITE, Color.BLUE);
        playerCardLabels.add(cardLabel);
        playerCardsPanel.add(cardLabel);
        playerCardsPanel.revalidate();
        playerCardsPanel.repaint();
    }

    private JLabel createCardLabel(String cardName, Color bgColor, Color textColor) {
        JLabel label = new JLabel(cardName, JLabel.CENTER);
        
        label.setPreferredSize(new Dimension(80, 110));
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(textColor);
        
        // 수정됨: J, Q, K만 큰 폰트(32)를 사용하고, A는 숫자와 같이 기본 크기(24)로 설정
        if (cardName.equals("J") || cardName.equals("Q") || cardName.equals("K")) {
            label.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        } else {
            // A와 숫자 카드(2~10)는 이쪽으로 분기됨
            label.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        }
        
        // 모든 카드에 똑같은 테두리 적용
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

    private void updateUIState() {
        startGameButton.setEnabled(isConnected && !isBettingPhase && !isMyTurn);
        betButton.setEnabled(isConnected && isBettingPhase);
        
        boolean canAct = isConnected && isMyTurn && !isBettingPhase;
        
        hitButton.setEnabled(canAct);
        standButton.setEnabled(canAct);
        doubleDownButton.setEnabled(canAct);
        surrenderButton.setEnabled(canAct);
        
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

    // ★ 핵심: 글자 테두리를 그려주는 커스텀 라벨 클래스
    // 이 클래스가 있어야 글씨가 선명하게 보입니다.
    class OutlineLabel extends JLabel {
        private Color outlineColor = Color.BLACK;
        private int outlineThickness = 3; // 테두리 두께

        public OutlineLabel(String text) {
            super(text);
        }

        @Override
        public void paintComponent(Graphics g) {
            String text = getText();
            if (text == null || text.isEmpty()) {
                super.paintComponent(g);
                return;
            }

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontRenderContext frc = g2d.getFontRenderContext();
            TextLayout tl = new TextLayout(text, getFont(), frc);
            Shape shape = tl.getOutline(AffineTransform.getTranslateInstance(0, 0));

            // 글자 중앙 정렬을 위한 좌표 계산
            Rectangle bounds = shape.getBounds();
            int x = (getWidth() - bounds.width) / 2 - bounds.x;
            int y = (getHeight() - bounds.height) / 2 - bounds.y;
            g2d.translate(x, y);

            // 1. 테두리 그리기 (검은색)
            g2d.setColor(outlineColor);
            g2d.setStroke(new BasicStroke(outlineThickness));
            g2d.draw(shape);

            // 2. 글자 내부 채우기 (원래 설정한 색상: WIN=황금색, LOSE=파란색 등)
            g2d.setColor(getForeground());
            g2d.fill(shape);

            g2d.dispose();
        }
    }
}