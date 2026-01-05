package com.roomelephant.porthole.it.infra;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(DockerConfiguration.class)
public abstract class IntegrationTestBase {
    static final int WIREMOCK_PORT = 8089;

    static final RestTemplate restTemplate = createRestTemplate();

    protected static DockerInfrastructure dockerInfra;

    @Autowired
    protected void setDockerInfra(DockerInfrastructure dockerInfra) {
        IntegrationTestBase.dockerInfra = dockerInfra;
    }

    @RegisterExtension
    protected static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(WIREMOCK_PORT).notifier(new ConsoleNotifier(true)))
            .build();

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        if (testInfo.getTestMethod().isPresent()
                && testInfo.getTestMethod().get().isAnnotationPresent(RunWithoutContainers.class)) {
            dockerInfra.cleanup();
            return;
        }

        dockerInfra.ensureContainersRunning();
    }

    static void cleanupTestContainers() {
        // No-op, cleanup handled by DockerInfrastructure lifecycle or manually if
        // needed
    }

    private static RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new ResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().is5xxServerError();
            }

            public void handleError(ClientHttpResponse response) throws IOException {}
        });
        return template;
    }

    protected @NotNull String createURLWithPort(@NotNull String uri) {
        return "http://localhost:" + port + uri;
    }

    protected void pauseDocker() {
        dockerInfra
                .getDocker()
                .getDockerClient()
                .pauseContainerCmd(dockerInfra.getDocker().getContainerId())
                .exec();
    }

    protected void unpauseDocker() {
        dockerInfra
                .getDocker()
                .getDockerClient()
                .unpauseContainerCmd(dockerInfra.getDocker().getContainerId())
                .exec();
    }

    protected @NotNull ResponseEntity<String> fetch(String url) {
        return fetch(url, String.class);
    }

    protected <T> @NotNull ResponseEntity<T> fetch(String url, Class<T> responseType) {
        return restTemplate.getForEntity(createURLWithPort(url), responseType);
    }
}
