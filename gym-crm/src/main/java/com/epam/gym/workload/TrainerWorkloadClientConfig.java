package com.epam.gym.workload;

import com.epam.gym.web.logging.TransactionIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * Wiring of the Trainer Workload integration. The base URL holds the Eureka service name
 * ({@code http://trainer-workload-service}); the load balancer resolves it to a registered instance,
 * so no host or port of the service is configured here.
 */
@Configuration
public class TrainerWorkloadClientConfig {

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ServiceTokenProvider serviceTokenProvider(@Value("${gym.workload.jwt.secret}") String secret,
                                                     @Value("${spring.application.name}") String issuer,
                                                     @Value("${gym.workload.service-id}") String audience,
                                                     @Value("${gym.workload.jwt.expiration}") Duration validity,
                                                     Clock clock) {
        return new ServiceTokenProvider(secret, issuer, audience, validity, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TrainerWorkloadClient restTrainerWorkloadClient(@LoadBalanced RestClient.Builder builder,
                                                           ServiceTokenProvider tokenProvider,
                                                           @Value("${gym.workload.service-id}") String serviceId,
                                                           @Value("${gym.workload.connect-timeout}") Duration connectTimeout,
                                                           @Value("${gym.workload.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        RestClient restClient = builder
                .baseUrl("http://" + serviceId)
                .requestFactory(requestFactory)
                .requestInterceptor(authorizationAndTransactionId(tokenProvider))
                .build();
        return new RestTrainerWorkloadClient(restClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gym.workload", name = "enabled", havingValue = "false")
    public TrainerWorkloadClient disabledTrainerWorkloadClient() {
        return new DisabledTrainerWorkloadClient();
    }

    /**
     * Adds the service token and passes this request's transaction id on, so the workload service logs the
     * operation under the same id.
     */
    static ClientHttpRequestInterceptor authorizationAndTransactionId(ServiceTokenProvider tokenProvider) {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(tokenProvider.getToken());
            String transactionId = MDC.get(TransactionIdFilter.MDC_KEY);
            if (transactionId != null) {
                request.getHeaders().set(TransactionIdFilter.HEADER, transactionId);
            }
            return execution.execute(request, body);
        };
    }
}
