package dev.fastgql.integration;

import java.util.Map;

public interface WithSecurity extends WithFastGQL {

  @Override
  default boolean isSecurityActive() {
    return true;
  }

  @Override
  default Map<String, Object> createSecurityConfigEntry() {
    return Map.of(
        "algorithm",
        "RS256",
        "publicKey",
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvAOEBHuP9Pie9si3Ok3e/rBOYhyJC6jR+P6Np38biqxUbZRfvmXpS4sdsUIIiMP14C5sfzDtT+oe+FEOyleCjRY2aXCHUmqQHyxmUMe4sBvxn5wLoCDi528dpeDsq3QVvL+X0zgn266ZgCP2xwoXtc0ry8Y397jTonC90/jnawizn4rqm7s14ChPM1CUn5rzfGsd1/8BvyR3H7F2wGcgQlNYpKW8ge1YsvtmIzZswc6u2JWUg6wh1Is1BweBDULvJVONyCzyt97CyYp2Si5ISODrfe+VzYZnPfJ+mtuUQxa3+YivK1Hz0rFotK1cmDCpJ8AqfMKDnyh/urYWqAXFVQIDAQAB");
  }

  @Override
  default String getJwtToken() {
    return "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9."
        + "eyJpYXQiOjE1OTQ3MTk2NDZ9."
        + "Y88rOApF1jYtB8Gs7iqE3Bp5Jgriyqr3B7bs7RKgiUzFYcAz-"
        + "KGTmrhn8e-CbM7eqNAEN7r8AxRw5jkgXxoE1A7zQ7YXK1y9xP"
        + "iMo9VqpLMSSl-u4ujFJvijIDsYfyrmTJr3bnOctmd2Lq2LlNO"
        + "QQoarVBVZkrCa5jA654l6rIKls5DiX8-Ya9gp2TFDJ-ADG2iv"
        + "36b4XykZqZeES7qGEAm8ZCvMQ9AUawbTjIa74CIBqgZbeCWb-"
        + "o884vxFOr1qnC9_U139hIeXX2p71Q_5v0Kb7ggBgCydMmPtKT"
        + "-JEkWTBcVXfzGHP-wNKHkKzkPKu2_e1O560SWGLgfB9L-9DA";
  }
}
