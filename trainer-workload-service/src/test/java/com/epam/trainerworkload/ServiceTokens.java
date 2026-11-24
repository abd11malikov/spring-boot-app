package com.epam.trainerworkload;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Builds service tokens the way Gym CRM does, for calling the API in tests.
 */
public final class ServiceTokens {

    /** The development key from application.properties. */
    public static final String SECRET = "ZGV2LW9ubHktZ3ltLXNlcnZpY2UtdG8tc2VydmljZS1qd3Qta2V5LTI1Ng==";
    public static final String OTHER_SECRET = "YW5vdGhlci1neW0tc2VydmljZS10by1zZXJ2aWNlLWp3dC1rZXktMjU2LWJpdHM=";

    private ServiceTokens() {
    }

    public static String valid() {
        return bearer(SECRET, "gym-crm", "trainer-workload-service", Duration.ofMinutes(5));
    }

    public static String bearer(String secret, String issuer, String audience, Duration validFor) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("gym-crm")
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now.minusSeconds(120)))
                .expirationTime(Date.from(now.plus(validFor)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(Base64.getDecoder().decode(secret)));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return "Bearer " + jwt.serialize();
    }
}
