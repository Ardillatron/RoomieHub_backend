package roomiehub.vivienda.application.usecase.cuenta;

import java.time.Instant;

import roomiehub.vivienda.domain.model.cuenta.Usuario;

public record Sesion(String accessToken, Instant accesoExpiraEn, String refreshToken, Usuario usuario) {}
