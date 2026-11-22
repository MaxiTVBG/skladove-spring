package bg.skladove.api;

import bg.skladove.api.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Грешките от валидацията в услугите (IllegalArgumentException — невалиден период, липсващ склад и т.н.)
 * се връщат като 400 с JSON {"error": "..."}, вместо като HTML страница за грешка.
 *
 * Важи само за REST контролерите от този пакет. AccessDeniedException не се хваща тук:
 * Spring Security я превръща в 403.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(IllegalArgumentException e) {
        return new ApiError(e.getMessage());
    }
}
