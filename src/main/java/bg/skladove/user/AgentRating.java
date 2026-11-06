
package bg.skladove.user;

import bg.skladove.rental.RentalContract;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "agent_rating",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agent_rating_contract",
                        columnNames = "rental_contract_id"
                )
        }
)
public class AgentRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AppUser agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_contract_id", nullable = false)
    private RentalContract rentalContract;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer score;

    @Size(max = 500)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AgentRating() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AppUser getAgent() {
        return agent;
    }

    public void setAgent(AppUser agent) {
        this.agent = agent;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public RentalContract getRentalContract() {
        return rentalContract;
    }

    public void setRentalContract(RentalContract rentalContract) {
        this.rentalContract = rentalContract;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
