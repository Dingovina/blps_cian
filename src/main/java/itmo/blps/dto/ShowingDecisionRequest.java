package itmo.blps.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ShowingDecisionRequest {

    public enum Decision { CONFIRM, REJECT }

    @NotNull
    private Decision decision;

    // used when decision = CONFIRM
    private Instant scheduledAt;
    private String contactInfo;

    // used when decision = REJECT
    private String reason;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
