import java.util.List;

public class BlackjackScoreCalculator {

    /**
     * 블랙잭 점수를 계산합니다.
     * 에이스는 21을 넘지 않는 최대값(11 또는 1)으로 자동 계산됩니다.
     */
    public static int calculateScore(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return 0;

        int baseScore = 0;
        int aceCount = 0;

        // 에이스 제외 점수 먼저 계산
        for (Card card : cards) {
            if (card.isAce()) aceCount++;
            else baseScore += card.getValue();
        }

        // 에이스 없으면 점수 반환
        if (aceCount == 0) {
            return baseScore;
        }

        // 에이스 처리: 가장 큰 값(11)을 먼저 시도
        // softScore = A 하나를 11로 썼을 때의 점수
        int softScore = baseScore + 11 + (aceCount - 1);

        // softScore가 21 이하라면 그대로 사용
        if (softScore <= 21) {
            return softScore;
        }

        // softScore가 21을 넘으면 모든 에이스를 1로 계산 (hard score)
        return baseScore + aceCount;
    }

    /**
     * Soft Hand인지 확인 (에이스를 11로 사용할 수 있는 상태).
     */
    public static boolean isSoftHand(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return false;

        int baseScore = 0;
        int aceCount = 0;

        for (Card card : cards) {
            if (card.isAce()) aceCount++;
            else baseScore += card.getValue();
        }

        if (aceCount == 0) return false;

        int softScore = baseScore + 11 + (aceCount - 1); // A 하나는 11, 나머지 1
        int hardScore = baseScore + aceCount;            // 모든 A는 1

        // softScore가 21 이하이고, hardScore와 다를 때 Soft
        return softScore <= 21 && softScore != hardScore;
    }

    /**
     * 버스트인지 확인 (21 초과).
     */
    public static boolean isBust(List<Card> cards) {
        return calculateScore(cards) > 21;
    }

    /**
     * 블랙잭인지 확인 (2장이고 정확히 21점).
     */
    public static boolean isBlackjack(List<Card> cards) {
        return cards.size() == 2 && calculateScore(cards) == 21;
    }
}
