import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

public class OpenFinanceAuthGenerator {

    public static void main(String[] args) throws Exception {

        // -------------------------------------------------------
        // Authorization Details Object
        ---------------------------------------------------------
        String consentId = UUID.randomUUID().toString();

        Map<String, Object> consent = new HashMap<>();
        consent.put("ExpirationDateTime", "2025-12-25T00:00:00.000Z");
        consent.put("ConsentId", consentId);
        consent.put("BaseConsentId", "b265ab23-017e-4d86-98d2-bff578e0de08");

        consent.put("Permissions", Arrays.asList(
                "ReadAccountsBasic","ReadAccountsDetail","ReadBalances",
                "ReadBeneficiariesBasic","ReadBeneficiariesDetail",
                "ReadTransactionsBasic","ReadTransactionsDetail",
                "ReadTransactionsCredits","ReadTransactionsDebits",
                "ReadScheduledPaymentsBasic","ReadScheduledPaymentsDetail",
                "ReadDirectDebits","ReadStandingOrdersBasic","ReadStandingOrdersDetail",
                "ReadConsents","ReadPartyUser","ReadPartyUserIdentity","ReadParty"
        ));

        Map<String, Object> billing = new HashMap<>();
        billing.put("UserType", "Retail");
        billing.put("Purpose", "AccountAggregation");

        consent.put("OpenFinanceBilling", billing);

        Map<String, Object> authDetailsItem = new HashMap<>();
        authDetailsItem.put("type", "urn:openfinanceuae:account-access-consent:v1.2");
        authDetailsItem.put("consent", consent);

        List<Object> authorizationDetails = Collections.singletonList(authDetailsItem);

        System.out.println("Authorization Details: " + authorizationDetails);

        // -------------------------------------------------------
        // PKCE Generation
        ---------------------------------------------------------
        String nonce = UUID.randomUUID().toString();
        String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));

        // Base64URL encode (no padding)
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        // -------------------------------------------------------
        // State
        ---------------------------------------------------------
        String state = UUID.randomUUID().toString();

        // -------------------------------------------------------
        // Authorization request payload
        ---------------------------------------------------------
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("scope", "accounts openid");
        requestPayload.put("redirect_uri", System.getenv("REDIRECT_URI"));
        requestPayload.put("client_id", System.getenv("CLIENT_ID"));
        requestPayload.put("nonce", nonce);
        requestPayload.put("state", state);
        requestPayload.put("response_type", "code");
        requestPayload.put("code_challenge_method", "S256");
        requestPayload.put("code_challenge", codeChallenge);
        requestPayload.put("max_age", 3600);
        requestPayload.put("authorization_details", authorizationDetails);

        // -------------------------------------------------------
        // Load PKCS8 Private Key (PEM)
        ---------------------------------------------------------
        String keyPath = ".../certificates/client_signing.key";
        PrivateKey privateKey = loadPrivateKey(keyPath);

        // -------------------------------------------------------
        // Sign JWT using PS256
        ---------------------------------------------------------
        String signedJWT = signJWT(requestPayload, privateKey);

        System.out.println("\nSigned Request JWT:\n" + signedJWT);
    }

    // -------------------------------------------------------
    // Load PEM PKCS8 Private Key
    ---------------------------------------------------------
    private static PrivateKey loadPrivateKey(String filePath) throws Exception {
        String pem = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    // -------------------------------------------------------
    // Nimbus JOSE JWT Signing (PS256)
    ---------------------------------------------------------
    private static String signJWT(Map<String, Object> payload, PrivateKey privateKey) throws Exception {

        long now = System.currentTimeMillis() / 1000;

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS256)
                .keyID(System.getenv("SIGNING_KEY_ID"))
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .claim("scope", payload.get("scope"))
                .claim("redirect_uri", payload.get("redirect_uri"))
                .claim("client_id", payload.get("client_id"))
                .claim("nonce", payload.get("nonce"))
                .claim("state", payload.get("state"))
                .claim("response_type", "code")
                .claim("code_challenge_method", "S256")
                .claim("code_challenge", payload.get("code_challenge"))
                .claim("max_age", 3600)
                .claim("authorization_details", payload.get("authorization_details"))
                .issueTime(new Date(now * 1000))
                .notBeforeTime(new Date((now - 10) * 1000))
                .expirationTime(new Date((now + 300) * 1000))
                .issuer(System.getenv("CLIENT_ID"))
                .audience(System.getenv("ISSUER"))
                .build());

        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}
