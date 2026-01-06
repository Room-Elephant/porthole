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
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@Import(DockerConfiguration.class)
public abstract class IntegrationTestBase {
    static final int WIREMOCK_PORT = 8089;

    protected static final String TEST_LOCAL_IMAGE_TAG = "my-local-image:1.0";
    protected static final String TEST_APP_CONTAINER_NAME = "porthole-test-app";
    protected static final String TEST_NO_PORTS_CONTAINER_NAME = "porthole-test-no-ports";
    protected static final String TEST_STOPPED_CONTAINER_NAME = "porthole-test-stopped";
    protected static final String TEST_LOCAL_CONTAINER_NAME = "porthole-test-local";
    protected static final String BUSYBOX_IMAGE = "busybox:1.37.0-uclibc";

    protected static GenericContainer<?> testAppContainer;
    protected static GenericContainer<?> localContainer;
    protected static GenericContainer<?> noPortsContainer;

    @RegisterExtension
    protected static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(WIREMOCK_PORT).notifier(new ConsoleNotifier(true)))
            .build();

    private static DockerInfrastructure dockerInfra;
    private final RestTemplate restTemplate = createRestTemplate();

    @LocalServerPort
    private int port;

    @Autowired
    protected void setDockerInfra(DockerInfrastructure dockerInfra) {
        IntegrationTestBase.dockerInfra = dockerInfra;
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

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTestMethod().isPresent()
                && testInfo.getTestMethod().get().isAnnotationPresent(RunWithoutContainers.class)) {
            cleanupDocker();
            return;
        }
        if (areContainersRunning()) {
            return;
        }

        createContainers();
    }

    protected void pauseDocker() {
        dockerInfra.pauseDocker();
    }

    protected void unpauseDocker() {
        dockerInfra.unpauseDocker();
    }

    protected @NotNull ResponseEntity<String> fetch(String url) {
        return fetch(url, String.class);
    }

    protected <T> @NotNull ResponseEntity<T> fetch(String url, Class<T> responseType) {
        return restTemplate.getForEntity(createURLWithPort(url), responseType);
    }

    private @NotNull String createURLWithPort(@NotNull String uri) {
        return "http://localhost:" + port + uri;
    }

    private boolean areContainersRunning() {
        return testAppContainer != null
                && testAppContainer.isRunning()
                && noPortsContainer != null
                && noPortsContainer.isRunning()
                && localContainer != null
                && localContainer.isRunning();
    }

    private void cleanupDocker() {
        testAppContainer = null;
        dockerInfra.removeContainerQuietly(TEST_APP_CONTAINER_NAME);

        noPortsContainer = null;
        dockerInfra.removeContainerQuietly(TEST_NO_PORTS_CONTAINER_NAME);

        localContainer = null;
        dockerInfra.removeContainerQuietly(TEST_LOCAL_CONTAINER_NAME);

        dockerInfra.removeContainerQuietly(TEST_STOPPED_CONTAINER_NAME);
    }

    private void createContainers() {
        dockerInfra.pullImage(BUSYBOX_IMAGE);

        // App Container (Running with ports)
        dockerInfra.removeContainerQuietly(TEST_APP_CONTAINER_NAME);
        testAppContainer = dockerInfra.buildImage(BUSYBOX_IMAGE, 8080, TEST_APP_CONTAINER_NAME);
        testAppContainer.start();

        // Stopped Container
        dockerInfra.removeContainerQuietly(TEST_STOPPED_CONTAINER_NAME);
        dockerInfra.buildStoppedImage(BUSYBOX_IMAGE, 8081, TEST_STOPPED_CONTAINER_NAME);

        // Local Image Container
        dockerInfra.removeContainerQuietly(TEST_LOCAL_CONTAINER_NAME);
        localContainer =
                dockerInfra.buildLocalImage(BUSYBOX_IMAGE, 8082, TEST_LOCAL_CONTAINER_NAME, TEST_LOCAL_IMAGE_TAG);
        localContainer.start();

        // No Ports Container
        dockerInfra.removeContainerQuietly(TEST_NO_PORTS_CONTAINER_NAME);
        noPortsContainer = dockerInfra.buildImage(BUSYBOX_IMAGE, null, TEST_NO_PORTS_CONTAINER_NAME);
        noPortsContainer.start();
    }
}
