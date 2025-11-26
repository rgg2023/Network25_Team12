public class Card {
    public enum Rank {
        ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN,
        EIGHT, NINE, TEN, JACK, QUEEN, KING
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
            case TEN: return "10";
            default: return String.valueOf(getValue());
        }
    }
    
    // 기본 값 반환 (ACE=1, J/Q/K/TEN=10)
    public int getValue() {
        switch (rank) {
            case ACE: return 1;
            case TEN:
            case JACK:
            case QUEEN:
            case KING: return 10;
            default: 
                // TWO(1)=2, THREE(2)=3, ... NINE(8)=9
                return rank.ordinal() + 1;
        }
    }
    
    public boolean isAce() {
        return rank == Rank.ACE;
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    // 랜덤 카드 생성 (1장 뽑기)
    public static Card drawRandom() {
        Rank[] ranks = Rank.values();
        Rank randomRank = ranks[(int)(Math.random() * ranks.length)];
        return new Card(randomRank);
    }
}
