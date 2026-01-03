package com.roomelephant.porthole.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
public abstract class IntegrationTestBase {
    static final String TEST_APP_CONTAINER_NAME = "porthole-test-app";
    static final String TEST_ALPINE_CONTAINER_NAME = "porthole-test-alpine";
    static final RestTemplate restTemplate = createRestTemplate();

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().notifier(new ConsoleNotifier(true)))
            .build();

    static GenericContainer<?> docker = new GenericContainer<>("docker:dind")
            .withPrivilegedMode(true)
            .withExposedPorts(2375)
            .withEnv("DOCKER_TLS_CERTDIR", "")
            .waitingFor(forLogMessage(".*API listen on \\[::\\]:2375.*", 1));

    static String testAppContainerId;
    static String alpineContainerId;

    static {
        docker.start();
    }

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void configureProperties(@NotNull DynamicPropertyRegistry registry) {
        registry.add("docker.host", () -> "tcp://" + docker.getHost() + ":" + docker.getMappedPort(2375));

        String wireMockUrl = "http://localhost:" + wireMock.getPort();
        registry.add("registry.urls.registry", () -> wireMockUrl + "/v2/");
        registry.add("registry.urls.auth", () -> wireMockUrl + "/auth?service=registry.docker.io&scope=repository:");
        registry.add("registry.urls.repositories", () -> wireMockUrl + "/v2/repositories/");
    }

    @BeforeAll
    static void setupTestContainers() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<String> appFuture =
                    executor.submit(() -> startContainer(TEST_APP_CONTAINER_NAME, "busybox", "8080:8080"));

            Future<String> alpineFuture =
                    executor.submit(() -> startContainer(TEST_ALPINE_CONTAINER_NAME, "busybox:1.35.0"));

            try {
                testAppContainerId = appFuture.get(30, TimeUnit.SECONDS);
                alpineContainerId = alpineFuture.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                appFuture.cancel(true);
                alpineFuture.cancel(true);

                stopQuietly(TEST_APP_CONTAINER_NAME);
                stopQuietly(TEST_ALPINE_CONTAINER_NAME);
                throw e;
            }
        }
    }

    @AfterAll
    static void cleanupTestContainers() {
        stopQuietly(TEST_APP_CONTAINER_NAME);
        stopQuietly(TEST_ALPINE_CONTAINER_NAME);
    }

    private static String startContainer(String name, String image, String... ports) {
        try {
            Container.ExecResult result = docker.execInContainer(containerExecStart(name, image, ports));

            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Failed to start container " + name + ": " + result.getStderr());
            }

            return result.getStdout().trim();
        } catch (Exception e) {
            throw new RuntimeException("Error starting container " + name, e);
        }
    }

    private static void stopQuietly(String name) {
        try {
            docker.execInContainer(containerExecStop(name));
        } catch (Exception _) {
            // container may not exist
        }
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

    private static String[] containerExecStart(String containerName, String image, String... ports) {
        List<String> command = new ArrayList<>();

        command.add("docker");
        command.add("run");
        command.add("-d");

        for (String port : ports) {
            command.add("-p");
            command.add(port);
        }

        command.add("--name");
        command.add(containerName);
        command.add(image);
        command.add("sh");
        command.add("-c");
        command.add("while true; do sleep 3600; done");

        return command.toArray(String[]::new);
    }

    private static String[] containerExecStop(String containerName) {
        return new String[] {"docker", "rm", "-f", containerName};
    }

    @NotNull
    String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

    // WireMock helper methods to reduce verbosity

    /**
     * Stub Docker registry tags endpoint with given versions
     *
     * @param image    Image name (e.g., "busybox")
     * @param versions Array of version strings (e.g., "1.35.0", "1.36.1")
     */
    void stubRegistryTags(String image, String... versions) {
        StringBuilder tagsJson = new StringBuilder("{\"results\": [");
        for (int i = 0; i < versions.length; i++) {
            if (i > 0) tagsJson.append(", ");
            tagsJson.append("{\"name\": \"").append(versions[i]).append("\"}");
        }
        tagsJson.append("]}");

        wireMock.stubFor(get(urlEqualTo("/v2/repositories/library/" + image + "/tags?page_size=100"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(tagsJson.toString())));
    }

    /**
     * Stub Docker registry manifest HEAD request with digest
     *
     * @param image   Image name (e.g., "busybox")
     * @param version Version tag (e.g., "1.36.1")
     * @param digest  Digest value (e.g., "sha256:newdigest")
     */
    void stubManifestDigest(String image, String version, String digest) {
        wireMock.stubFor(head(urlEqualTo("/v2/library/" + image + "/manifests/" + version))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/vnd.docker.distribution.manifest.v2+json")
                        .withHeader("Docker-Content-Digest", digest)
                        .withBody("{}")));
    }
}
