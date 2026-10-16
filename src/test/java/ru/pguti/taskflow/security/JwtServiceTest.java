package ru.pguti.taskflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
            "VGhpc0lzQVN1cGVyU2VjcmV0S2V5Rm9yVGFza0Zsb3cxMjM0NTY3ODkw";

    private static final long EXPIRATION =
            86400000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(
                SECRET,
                EXPIRATION
        );
    }

    @Test
    void shouldGenerateValidToken() {

        String token = jwtService.generateToken(
                "max@test.ru"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.isValid(token));
    }

    @Test
    void shouldExtractEmailFromToken() {

        String email = "max@test.ru";

        String token = jwtService.generateToken(email);

        String extractedEmail =
                jwtService.extractEmail(token);

        assertEquals(
                email,
                extractedEmail
        );
    }

    @Test
    void shouldValidateGeneratedToken() {

        String token = jwtService.generateToken(
                "max@test.ru"
        );

        assertTrue(
                jwtService.isValid(token)
        );
    }

    @Test
    void shouldRejectInvalidToken() {

        String invalidToken =
                "this.is.not.a.valid.jwt.token";

        assertFalse(
                jwtService.isValid(invalidToken)
        );
    }

    @Test
    void shouldRejectModifiedToken() {

        String token = jwtService.generateToken(
                "max@test.ru"
        );

        String modifiedToken =
                token.substring(0, token.length() - 1)
                        + "X";

        assertFalse(
                jwtService.isValid(modifiedToken)
        );
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsers() {

        String firstToken =
                jwtService.generateToken(
                        "max@test.ru"
                );

        String secondToken =
                jwtService.generateToken(
                        "other@test.ru"
                );

        assertNotNull(firstToken);
        assertNotNull(secondToken);

        assertFalse(
                firstToken.equals(secondToken)
        );

        assertEquals(
                "max@test.ru",
                jwtService.extractEmail(firstToken)
        );

        assertEquals(
                "other@test.ru",
                jwtService.extractEmail(secondToken)
        );
    }
}