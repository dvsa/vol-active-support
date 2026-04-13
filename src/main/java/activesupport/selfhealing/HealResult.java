package activesupport.selfhealing;

public class HealResult {

    private final String selector;
    private final double confidence;
    private final String reason;

    public HealResult(String selector, double confidence, String reason) {
        this.selector = selector;
        this.confidence = confidence;
        this.reason = reason;
    }

    public String getSelector() {
        return selector;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("HealResult{selector='%s', confidence=%.2f, reason='%s'}", selector, confidence, reason);
    }
}
