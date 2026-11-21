package internal.org.springframework.content.jpa.support.models;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Claim {

    @Id
    @org.springframework.data.annotation.Id
    private String claimId = UuidCreator.getTimeOrdered().toString();

    private String lastName;
    private String firstName;

    @Embedded
    private ClaimForm claimForm = new ClaimForm();

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public ClaimForm getClaimForm() {
        return claimForm;
    }

    public void setClaimForm(ClaimForm claimForm) {
        this.claimForm = claimForm;
    }
}
