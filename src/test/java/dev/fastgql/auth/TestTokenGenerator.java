package dev.fastgql.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestTokenGenerator {

  public static void main(String[] args) throws IOException {
    System.out.println(generateTestToken());
  }

  public static final String TEST_PUBLIC_KEY_FILEPATH = "src/test/resources/keys/public_key.pem";
  public static final String TEST_PRIVATE_KEY_FILEPATH = "src/test/resources/keys/private_key.pem";
  public static final String TEST_ALGORITHM = "RSA";

  public static String generateTestToken() throws IOException {
    return generateTokenFromKeyFiles(TEST_PUBLIC_KEY_FILEPATH, TEST_PRIVATE_KEY_FILEPATH,
        TEST_ALGORITHM);
  }

  private static String generateTokenFromKeyFiles(String publicKeyFilepath, String privateKeyFilepath,
      String algorithm)
      throws IOException {
    RSAPublicKey publicKey = (RSAPublicKey) PemUtils
        .readPublicKeyFromFile(publicKeyFilepath, algorithm);
    RSAPrivateKey privateKey = (RSAPrivateKey) PemUtils
        .readPrivateKeyFromFile(privateKeyFilepath, algorithm);
    Algorithm algorithmRS = Algorithm.RSA256(publicKey, privateKey);
    Map<String, Object> claims = new HashMap<>() {{
      put(DefaultKeys.DEFAULT_ROLE_KEY, "admin");
      put(DefaultKeys.USER_ID_KEY, "test_user_id");
    }};
    return JWT.create()
        .withClaim(DefaultKeys.NAMESPACE, claims)
        .withIssuer(TestTokenGenerator.class.getName())
        .withSubject("test")
        .withIssuedAt(new Date())
        .sign(algorithmRS);
  }

}
