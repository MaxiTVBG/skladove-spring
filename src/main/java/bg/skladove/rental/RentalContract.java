package bg.skladove.rental;

import bg.skladove.user.AppUser;
import bg.skladove.warehouse.Warehouse;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Формуляр (договор) за наем, създаден от складов агент:
 * информация за наемателя, период и цена.
 */
@Entity
public class RentalContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Warehouse warehouse;

    @ManyToOne(optional = false)
    private AppUser agent;

    @NotBlank
    private String tenantName;

    private String tenantPhone;

    private String tenantEmail;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private BigDecimal price;

    private LocalDate createdAt = LocalDate.now();

    public Long getId() { return id; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public AppUser getAgent() { return agent; }
    public void setAgent(AppUser agent) { this.agent = agent; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }
    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public LocalDate getCreatedAt() { return createdAt; }
}
