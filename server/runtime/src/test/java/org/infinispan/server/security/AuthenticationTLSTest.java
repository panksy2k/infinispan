package org.infinispan.server.security;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.server.test.InfinispanServerRule;
import org.infinispan.server.test.InfinispanServerTestConfiguration;
import org.infinispan.server.test.InfinispanServerTestMethodRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/

@RunWith(Parameterized.class)
public class AuthenticationTLSTest {

   @ClassRule
   public static InfinispanServerRule SERVERS = new InfinispanServerRule(new InfinispanServerTestConfiguration("configuration/AuthenticationServerTLSTest.xml"));

   @Rule
   public InfinispanServerTestMethodRule SERVER_TEST = new InfinispanServerTestMethodRule(SERVERS);

   private final String mechanism;

   @Parameterized.Parameters(name = "{0}")
   public static Collection<Object[]> data() {
      return Common.SASL_MECHS;
   }

   public AuthenticationTLSTest(String mechanism) {
      this.mechanism = mechanism;
   }

   @Test
   public void testReadWrite() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.security().ssl().trustStoreFileName(SERVERS.getServerDriver().getCertificateFile("ca").getAbsolutePath()).trustStorePassword("secret".toCharArray());
      if (!mechanism.isEmpty()) {
         builder.security().authentication()
               .saslMechanism(mechanism)
               .serverName("infinispan")
               .realm("default")
               .username("all_user")
               .password("all");
      }

      try {
         RemoteCache<String, String> cache = SERVER_TEST.getHotRodCache(builder, CacheMode.DIST_SYNC);
         cache.put("k1", "v1");
         assertEquals(1, cache.size());
         assertEquals("v1", cache.get("k1"));
      } catch (HotRodClientException e) {
         // Rethrow if unexpected
         if (!mechanism.isEmpty()) throw e;
      }
   }
}
