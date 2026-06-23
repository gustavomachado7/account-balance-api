package com.bank.balance.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Validacao falhou: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "O identificador da conta nao pode estar vazio"
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Rota nao encontrada: {}", ex.getResourcePath());
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Nao ha informacoes para serem exibidas"
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException ex) {
        log.warn("Conta nao encontrada: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RepositoryException.class)
    public ProblemDetail handleRepositoryError(RepositoryException ex) {
        log.error("Erro de persistencia", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Servico temporariamente indisponivel"
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericError(Exception ex) {
        log.error("Erro inesperado", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
    }
}