public class Card {
    public enum Rank {
        ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING
    }
    
    private Rank rank;
    
    public Card(Rank rank) {
        this.rank = rank;
    }
    
    public Rank getRank() {
        return rank;
    }
    
    // 카드의 표시 이름 반환 (A, 2-10, J, Q, K)
    public String getDisplayName() {
        switch (rank) {
            case ACE: return "A";
            case JACK: return "J";
            case QUEEN: return "Q";
            case KING: return "K";
            default: return String.valueOf(getValue());
        }
    }
    
    // 카드의 기본 값 반환 (에이스는 1로, J/Q/K는 10으로)
    public int getValue() {
        switch (rank) {
            case ACE: return 1;
            case JACK:
            case QUEEN:
            case KING: return 10;
            default: return rank.ordinal() + 1; // TWO=2, THREE=3, ..., TEN=10
        }
    }
    
    // 에이스인지 확인
    public boolean isAce() {
        return rank == Rank.ACE;
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    // 랜덤 카드 생성
    public static Card drawRandom() {
        Rank[] ranks = Rank.values();
        Rank randomRank = ranks[(int)(Math.random() * ranks.length)];
        return new Card(randomRank);
    }
}
