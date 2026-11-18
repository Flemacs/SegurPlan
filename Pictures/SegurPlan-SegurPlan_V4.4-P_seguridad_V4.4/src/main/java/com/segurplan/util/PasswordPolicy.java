package com.segurplan.util;

/** Política de contraseñas (RNF04): mínimo 8 caracteres, letras y números. */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validar(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres.");
        }
        boolean letra = password.chars().anyMatch(Character::isLetter);
        boolean numero = password.chars().anyMatch(Character::isDigit);
        if (!letra || !numero) {
            throw new IllegalArgumentException(
                    "La contraseña debe combinar letras y números.");
        }
    }
}
