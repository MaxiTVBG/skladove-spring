package bg.skladove.rental;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Формулярът за наем, който попълва складовият агент
 * (по заданието: данни за наемателя, период и цена).
 */
public class RentalRequestForm {

    @NotNull(message = "Изберете склад.")
    private Long warehouseId;

    @NotBlank(message = "Името на наемателя е задължително.")
    @Size(max = 150, message = "Името е най-много 150 символа.")
    private String tenantName;

    @Size(max = 30, message = "Телефонът е най-много 30 символа.")
    private String tenantPhone;

    @Email(message = "Невалиден имейл.")
    @Size(max = 150)
    private String tenantEmail;

    // <input type="date"> изпраща датата като 2026-11-07 (ISO формат)
    @NotNull(message = "Изберете начална дата.")
    @FutureOrPresent(message = "Началната дата не може да е в миналото.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Изберете крайна дата.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "Цената е задължителна.")
    @DecimalMin(value = "0.01", message = "Цената трябва да е по-голяма от 0.")
    private BigDecimal price;

    /**
     * Проверка между две полета: крайната дата не може да е преди началната.
     * Bean Validation извиква всеки метод isXxx() с @AssertTrue;
     * грешката се показва под името "periodValid".
     */
    @AssertTrue(message = "Крайната дата трябва да е след началната.")
    public boolean isPeriodValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
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
}
