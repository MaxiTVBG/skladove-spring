package bg.skladove.warehouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Данните, които собственикът попълва във формата за склад.
 *
 * Защо отделен клас, а не директно Warehouse: формата съдържа само полетата,
 * които собственикът има право да променя. Така никой не може да подаде
 * чужд owner, id или статус през формата (защита от "mass assignment").
 */
public class WarehouseForm {

    @NotBlank(message = "Името е задължително.")
    @Size(max = 100, message = "Името е най-много 100 символа.")
    private String name;

    @NotBlank(message = "Адресът е задължителен.")
    @Size(max = 255, message = "Адресът е най-много 255 символа.")
    private String address;

    @NotNull(message = "Площта е задължителна.")
    @Positive(message = "Площта трябва да е положително число.")
    private Double area;

    @Positive(message = "Височината трябва да е положително число.")
    private Double height;

    @Size(max = 255)
    private String climateConditions;

    @Size(max = 255)
    private String goodsType;

    @NotBlank(message = "Изберете категория.")
    private String category;

    @NotNull(message = "Цената е задължителна.")
    @DecimalMin(value = "0.01", message = "Цената трябва да е по-голяма от 0.")
    private BigDecimal monthlyPrice;

    /** Попълва формата от съществуващ склад (при редакция). */
    public static WarehouseForm from(Warehouse w) {
        WarehouseForm form = new WarehouseForm();
        form.name = w.getName();
        form.address = w.getAddress();
        form.area = w.getArea();
        form.height = w.getHeight();
        form.climateConditions = w.getClimateConditions();
        form.goodsType = w.getGoodsType();
        form.category = w.getCategory();
        form.monthlyPrice = w.getMonthlyPrice();
        return form;
    }

    /** Прехвърля данните от формата върху склада. Не пипа owner, status и агентите. */
    public void applyTo(Warehouse w) {
        w.setName(name.trim());
        w.setAddress(address.trim());
        w.setArea(area);
        w.setHeight(height);
        w.setClimateConditions(climateConditions);
        w.setGoodsType(goodsType);
        w.setCategory(category);
        w.setMonthlyPrice(monthlyPrice);
    }

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
}
