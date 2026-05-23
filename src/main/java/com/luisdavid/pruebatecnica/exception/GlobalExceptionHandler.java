package com.luisdavid.pruebatecnica.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la capa REST.
 *
 * <p>Convierte excepciones de dominio en respuestas HTTP estandarizadas
 * usando {@link ProblemDetail} (RFC 7807 — formato estándar de errores HTTP).
 * Centralizar el mapeo aquí evita try/catch repetitivos en cada controller (DRY)
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
        return build(HttpStatus.CONFLICT, "Código duplicado", ex.getMessage());
    }

    /**
     * Falló el bloqueo optimista — dos transacciones intentaron modificar
     * la misma bicicleta simultáneamente.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "La bicicleta fue modificada por otra operación. Intente nuevamente.");
    }

    /** Validación de Bean Validation (anotaciones en records de request). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errores.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "Uno o más campos no superaron la validación");
        pd.setProperty("errores", errores);
        return pd;
    }

    /** JSON malformado o con tipos incorrectos. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "JSON inválido",
                "El cuerpo de la petición no se pudo deserializar correctamente");
    }

    /** Parámetro de tipo incorrecto en path o query (ej. enum no válido). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "El parámetro '" + ex.getName() + "' con valor '" + ex.getValue()
                + "' no es válido";
        return build(HttpStatus.BAD_REQUEST, "Parámetro inválido", msg);
    }

    /** Cualquier excepción de estado ilegal proveniente del dominio. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "Estado inválido", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "Argumento inválido", ex.getMessage());
    }

    /** Captura por defecto — evita filtrar stack traces internos. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Contacte al administrador.");
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        return pd;
    }
}
