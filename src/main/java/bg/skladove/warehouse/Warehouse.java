package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Складово помещение. Създава се от собственик, който избира
 * един или повече складови агенти, които да го отдават под наем.
 */
@Entity
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotBlank
    private String name;

    @NotBlank
    private String address;

    /** Площ в кв. м. */
    @Positive
    private Double area;

    /** Височина в метри. */
    private Double height;

    /** Климатични условия: температура, влажност, хладилен и т.н. */
    private String climateConditions;

    /** Вид на съхраняваната стока. */
    private String goodsType;

    /** Категория склад: A, B, C и т.н. */
    private String category;

    /** Цена на месец. */
    private BigDecimal monthlyPrice;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.AVAILABLE;

    @ManyToOne(optional = false)
    private AppUser owner;

    @ManyToMany
    @JoinTable(name = "warehouse_agent",
            joinColumns = @JoinColumn(name = "warehouse_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id"))
    private Set<AppUser> agents = new HashSet<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    public String getClimateConditions() { return climateConditions; }
    public void setClimateConditions(String climateConditions) { this.climateConditions = climateConditions; }
    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(BigDecimal monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    public WarehouseStatus getStatus() { return status; }
    public void setStatus(WarehouseStatus status) { this.status = status; }
    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }
    public Set<AppUser> getAgents() { return agents; }
    public void setAgents(Set<AppUser> agents) { this.agents = agents; }
}
