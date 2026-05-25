package com.luisdavid.pruebatecnica.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la capa REST.
 *
 * <p>Convierte excepciones de dominio en respuestas HTTP estandarizadas
 * usando {@link ProblemDetail} (RFC 7807 — formato estandar de errores HTTP).
 * Centralizar el mapeo aqui evita try/catch repetitivos en cada controller (DRY)
 * y desacopla las excepciones de negocio del transporte HTTP.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BicicletaNoEncontradaException.class)
    public ProblemDetail handleBicicletaNoEncontrada(BicicletaNoEncontradaException ex) {
        return build(HttpStatus.NOT_FOUND, "Bicicleta no encontrada", ex.getMessage());
    }

    @ExceptionHandler(AlquilerNoEncontradoException.class)
    public ProblemDetail handleAlquilerNoEncontrado(AlquilerNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, "Alquiler no encontrado", ex.getMessage());
    }

    @ExceptionHandler(BicicletaNoDisponibleException.class)
    public ProblemDetail handleBicicletaNoDisponible(BicicletaNoDisponibleException ex) {
        return build(HttpStatus.CONFLICT, "Bicicleta no disponible", ex.getMessage());
    }

    @ExceptionHandler(AlquilerYaFinalizadoException.class)
    public ProblemDetail handleAlquilerYaFinalizado(AlquilerYaFinalizadoException ex) {
        return build(HttpStatus.CONFLICT, "Alquiler ya finalizado", ex.getMessage());
    }

    @ExceptionHandler(CodigoBicicletaDuplicadoException.class)
    public ProblemDetail handleDuplicado(CodigoBicicletaDuplicadoException ex) {
        return build(HttpStatus.CONFLICT, "Codigo duplicado", ex.getMessage());
    }

    /**
     * Fallo el bloqueo optimista — dos transacciones intentaron modificar
     * la misma bicicleta simultaneamente.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "La bicicleta fue modificada por otra operacion. Intente nuevamente.");
    }

    /** Validacion de Bean Validation (anotaciones en records de request). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errores.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Datos invalidos",
                "Uno o mas campos no superaron la validacion");
        pd.setProperty("errores", errores);
        return pd;
    }

    /** JSON malformado o con tipos incorrectos. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "JSON invalido",
                "El cuerpo de la peticion no se pudo deserializar correctamente");
    }

    /** Parametro de tipo incorrecto en path o query (ej. enum no valido). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "El parametro '" + ex.getName() + "' con valor '" + ex.getValue()
                + "' no es valido";
        return build(HttpStatus.BAD_REQUEST, "Parametro invalido", msg);
    }

    /** Ruta inexistente. Devuelve 404 limpio en lugar de filtrar al handler generico. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ruta no encontrada",
                "La ruta '/" + ex.getResourcePath() + "' no existe en esta API. "
                        + "Ver Swagger UI en /swagger-ui/index.html");
    }

    /** Cualquier excepcion de estado ilegal proveniente del dominio. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "Estado invalido", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Argumento invalido", ex.getMessage());
    }

    /** Captura por defecto — evita filtrar stack traces internos. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrio un error inesperado. Contacte al administrador.");
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }
}
