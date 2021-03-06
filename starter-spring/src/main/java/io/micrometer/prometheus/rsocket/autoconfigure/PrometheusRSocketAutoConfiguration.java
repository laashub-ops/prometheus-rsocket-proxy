/**
 * Copyright 2019 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micrometer.prometheus.rsocket.autoconfigure;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.rsocket.PrometheusRSocketClient;
import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;

@Configuration
@AutoConfigureAfter(PrometheusMetricsExportAutoConfiguration.class)
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnProperty(prefix = "management.metrics.export.prometheus.rsocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PrometheusRSocketProperties.class)
public class PrometheusRSocketAutoConfiguration {

  @ConditionalOnMissingBean
  @Bean
  Clock micrometerClock() {
    return Clock.SYSTEM;
  }

  @ConditionalOnMissingBean
  @Bean
  PrometheusConfig prometheusConfig() {
    return PrometheusConfig.DEFAULT;
  }

  @ConditionalOnMissingBean
  @Bean
  CollectorRegistry prometheusCollectorRegistry() {
    return new CollectorRegistry(true);
  }

  @ConditionalOnMissingBean
  @Bean
  PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
    return new PrometheusMeterRegistry(config, collectorRegistry, clock);
  }

  @ConditionalOnMissingBean
  @Bean(destroyMethod = "pushAndClose")
  PrometheusRSocketClient prometheusRSocketClient(PrometheusMeterRegistry meterRegistry, PrometheusRSocketProperties properties) {
    return PrometheusRSocketClient.build(meterRegistry, properties.createClientTransport())
        .retry(Retry.backoff(properties.getMaxRetries(), properties.getFirstBackoff())
            .maxBackoff(properties.getMaxBackoff()))
        .connect();
  }
}
