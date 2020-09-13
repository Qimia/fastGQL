package dev.fastgql.integration;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

import java.util.Map;

public interface WithSecurity extends WithFastGQL {

  String ALGORITHM = "RS256";
  String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvAOEBHuP9Pie9si3Ok3e/rBOYhyJC6jR+P6Np38biqxUbZRfvmXpS4sdsUIIiMP14C5sfzDtT+oe+FEOyleCjRY2aXCHUmqQHyxmUMe4sBvxn5wLoCDi528dpeDsq3QVvL+X0zgn266ZgCP2xwoXtc0ry8Y397jTonC90/jnawizn4rqm7s14ChPM1CUn5rzfGsd1/8BvyR3H7F2wGcgQlNYpKW8ge1YsvtmIzZswc6u2JWUg6wh1Is1BweBDULvJVONyCzyt97CyYp2Si5ISODrfe+VzYZnPfJ+mtuUQxa3+YivK1Hz0rFotK1cmDCpJ8AqfMKDnyh/urYWqAXFVQIDAQAB";
  String SECRET_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8A4QEe4/0+J72yLc6Td7+sE5iHIkLqNH4/o2nfxuKrFRtlF++ZelLix2xQgiIw/XgLmx/MO1P6h74UQ7KV4KNFjZpcIdSapAfLGZQx7iwG/GfnAugIOLnbx2l4OyrdBW8v5fTOCfbrpmAI/bHChe1zSvLxjf3uNOicL3T+OdrCLOfiuqbuzXgKE8zUJSfmvN8ax3X/wG/JHcfsXbAZyBCU1ikpbyB7Viy+2YjNmzBzq7YlZSDrCHUizUHB4ENQu8lU43ILPK33sLJinZKLkhI4Ot975XNhmc98n6a25RDFrf5iK8rUfPSsWi0rVyYMKknwCp8woOfKH+6thaoBcVVAgMBAAECggEAEwggEvwXBGsuXDjRI1vCmPFr3c6ZqM58AsJxwGTDevQaz8TXNETIqtb4OHtAtedwOjM41F81hoNigyxhXOYi1vlKpL25VQRodl9eNAsjjOAnQ0reemUZ0ueSjroH+wsTMDoMulrS3g/8hdYeIuPIpqSQLF5yR+cztRvIe26UrmbUe1Qg6zjEq2/DdZnNQmys7qs2AEB0uWPAHPn5ti1lXqmYcggacxVRVcDPoImvTgHQ4x4Wz8ZWbIUEzn6v6DYbLaXniNwn2xQDUOMW/4Nr3iU4yA5DlDSytXivGen+MGiEV71bOyyk0SgGR8sDThpCm3OzK57CF2qaRpqRLV5VeQKBgQDw+I0g8kW8CQ0rmVuirf/KAld5HXyhlNRMpwAb2bISCZtRlCKEItEYeWoz+y087qop7Sv9EGRlxBgzxEl/yCxUniUXyYYNxu0UaGBiF8fpOLOCobHUHj1Ydl7CGzRbnmE87oIpR1DgJWE6SEWUDgAfFyrB2UZkejMytLiSaLxjtwKBgQDHvW05NaBkJTGQNzKABSJ67NXMQ7bldTweZ1METSg5DZ2+iApXMm7kfvHYzyc3F2bvY7IQjFGbE3sOtV8Pb5zqzYa1BRHV68c3zJSqmAcAxKezhP1TsAvlttpVDouCpEu3sfk1/aHW3knznug+HcVjfJ8IMS/NtvPYq1nAweQXUwKBgAHPG+AfcX6T4tFs+U8jV0RA+D8khYcBAwcASfPrknaoM85M+mVbjY5Newqg3BoVAJoH+ciQkvBgpH3e/15CNnL8LPMcxDDeSXFZxz91Rj7t+gsFA7y/7V34pYV3htEZQ0md0MRWkLjeDvjNChiucjnJhryl0O14LWI4ERqoRqHJAoGBAL7Ec8odJsgfIdxMa6YLwWfIRVYnIkq7EqUzJ/3Gt3DuSUfNZJrtZy5C9DePejPK3Rwsisf1TIehLnnYzibPAf7cNxky81ruKsJnWWIpex7HtCfoD49bZ7GJV7O/BY3L3yleCNgBGw4+FkFg9w1En96qCrXnTHHcl77LwRgx1uhTAoGBANam8E36bxcoNUk6SbXqL4JJfRsGwZCopb5Kkp9e9Et8jDqyAQ83sk/mkMEORG6kZY7kmjA3M5c6mPynA4IJ2vv29wDaQxTQhxbNNAkSclAcuundPX9KgjJDVBd/z8Srad5EJwVcPlJG57IF/RRVT75r2xoZQhYhbK+uIOZOKxqi";

  @Override
  default boolean isSecurityActive() {
    return true;
  }

  @Override
  default Map<String, Object> createSecurityConfigEntry() {
    return Map.of(
        "algorithm",
        ALGORITHM,
        "publicKey",
        PUBLIC_KEY);
  }

  @Override
  default String getJwtToken(Vertx vertx, Map<String, Object> userParams) {
    JWTAuth jwtAuth =
      JWTAuth.create(
        vertx,
        new JWTAuthOptions()
          .addPubSecKey(
            new PubSecKeyOptions()
              .setAlgorithm(ALGORITHM)
              .setPublicKey(PUBLIC_KEY)
              .setSecretKey(SECRET_KEY)));

    return jwtAuth.generateToken(
      JsonObject.mapFrom(userParams),
      new JWTOptions().setAlgorithm(ALGORITHM));
  }
}
