/*
 * Copyright 2020, Google LLC.
 *
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

package anthos.samples.bankofanthos.userservice;

import com.google.cloud.MetadataConfig;
import io.micrometer.stackdriver.StackdriverConfig;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceConfiguration {

  @Value("${user-service.private-key}")
  private String privateKeyString;

  @Value("${user-service.token-expiry-seconds}")
  private int tokenExpirySeconds;

  /**
   * Provides the configuration needed by metrics collection via Micrometer.
   */
  @Bean
  public StackdriverConfig stackdriverConfig() {
    return new StackdriverConfig() {
      @Override
      public String projectId() {
        return MetadataConfig.getProjectId();
      }

      @Override
      public String get(String key) {
        return null;
      }
      @Override
      public String resourceType() {
        return "k8s_container";
      }

      @Override
      public Map<String, String> resourceLabels() {
        Map<String, String> map = new HashMap<>();

        String podName = System.getenv("HOSTNAME");
        String containerName = podName.substring(0,podName.indexOf("-"));

        map.put("namespace_name", "default");
        map.put("container_name", containerName);
        map.put("pod_name", podName);
        map.put("cluster_name", MetadataConfig.getClusterName());
        map.put("location", MetadataConfig.getZone());

        return map;
      }
    };
  }

  @Bean
  public JwtTokenProvider jwtTokenProvider() throws Exception {
    return new JwtTokenProvider(parsePrivateKey(privateKeyString), tokenExpirySeconds);
  }

  private static PrivateKey parsePrivateKey(String key)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    key = key.replace("-----BEGIN PRIVATE KEY-----\n", "");
    key = key.replace("-----END PRIVATE KEY-----", "");
    key = key.replace("\n", "");
    key = key.trim();

    byte[] encoded = Base64.getDecoder().decode(key);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    return keyFactory.generatePrivate(keySpec);
  }
}
