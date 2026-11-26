package bg.skladove.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Грешка")
public record ApiError(@Schema(example = "Крайната дата трябва да е след началната.") String error) {
}
