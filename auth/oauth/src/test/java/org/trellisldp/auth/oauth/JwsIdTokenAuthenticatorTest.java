/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.auth.oauth;

import static java.util.Base64.getUrlDecoder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.spec.RSAPrivateKeySpec;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JwsIdTokenAuthenticatorTest {

    private static final String base64PrivateExponent = "VRPRBm9dCoAJfBbEz5oAHEz7Tnm" +
        "0i0O6m5yj7NwqAZOj9i4ZwgZ8VZQo88oxZQWNaYd1yKeoQhUsJija_vxQEPXO1Q2q6OqMcwTBH0wyGhIFp--z2dAyRlDVLUTQbJUXyq" +
        "ammdh7b16-igH-BB67jfolM-cw-O7YaN7GrxCCYX5bI38IipeYfcroeIUXdLYmmUdNy7c8P2_K4O-iHQ6A4AUtQRUOzt2FGOdmlGZih" +
        "upI9YprshIy9CZq_iA3BcOl4Gcc-ljwwUzT0M_4jt53DCV7oxqWVt9WRdYDNoD62g2FzQ-1nYUqsz4YChk1MuOPV1xFpRklwSpt5dfh" +
        "uldnbQ";
    private static final BigInteger exponent = new BigInteger(1, getUrlDecoder().decode(base64PrivateExponent));

    private static final String base64PublicExponent = "AQAB";

    private static final String base64Modulus = "oMyjaeUbmnqojRpMBDbWbfBFitd_" +
        "dQcFJ96CDWwzsVcyAK3_kp4dEvhc2KLBjrmE69gJ-4HRuPF-kulDEmpC-MVx9eOihdUG9XV0ZA_eYWj9RoI_Gt3TUqTxlQH_nJRADTf" +
        "y82fOCCboKpaQ2idZH55Vb0FDbau2b2462tYRmcnxTFjClP4fDTTubI-3oFJ4tKMjynvUT34mCrZPiM8Q4noxVoyRYpzUTL1USxdUf5" +
        "6IKSB8NduH438zhMXE5VLC6PzhR3i_4KKpe4nq2otsrJ3KlEc7Me6UeiMXxPYz8rrPovW5L3LFWDmntGs5q923fBZFLFg8yBgMdTine" +
        "aahEQ";
    private static final BigInteger modulus = new BigInteger(1, getUrlDecoder().decode(base64Modulus));

    @Test
    void testAuthenticateNoIdToken() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        final Key privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder()
            .setHeader(headers)
            .claim("iss", "example.com")
            .signWith(privateKey)
            .compact();

        final JwsIdTokenAuthenticator authenticator = new JwsIdTokenAuthenticator();
        assertThrows(MalformedJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoKeyCnf() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        final String internalJws = Jwts.builder().setHeader(headers).claim("foo", "bar").compact();
        final Key privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder()
            .setHeader(headers)
            .claim("id_token", internalJws)
            .signWith(privateKey)
            .compact();

        final JwsIdTokenAuthenticator authenticator = new JwsIdTokenAuthenticator();
        assertThrows(MalformedJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testAuthenticateNoInternalBody() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        final String idToken = "eyJhbGciOiJSUzI1NiJ9";
        final Key privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String token = Jwts.builder()
            .setHeader(headers)
            .claim("id_token", idToken)
            .signWith(privateKey)
            .compact();

        final JwsIdTokenAuthenticator authenticator = new JwsIdTokenAuthenticator();
        assertThrows(MalformedJwtException.class, () -> authenticator.authenticate(token));
    }

    @Test
    void testGreenPath() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        final HashMap<String, Object> cnf = new HashMap<>();
        final HashMap<String, Object> jwk = new HashMap<>();
        cnf.put("jwk", jwk);
        jwk.put("alg", "RSA256");
        jwk.put("n", base64Modulus);
        jwk.put("e", base64PublicExponent);
        final Key privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));
        final String internalJws = Jwts.builder()
            .setHeader(headers)
            .claim("sub", "https://bob.solid.community/profile/card#me")
            .claim("cnf", cnf)
            .signWith(privateKey)
            .compact();
        final String token = Jwts.builder()
            .setHeader(headers)
            .claim("id_token", internalJws)
            .signWith(privateKey)
            .compact();

        final JwsIdTokenAuthenticator authenticator = new JwsIdTokenAuthenticator();
        final Principal principal = authenticator.authenticate(token);
        assertNotNull(principal, "Principal was null!!!");
    }
}