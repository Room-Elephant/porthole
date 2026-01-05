package com.roomelephant.porthole.it.infra;

import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class DockerInfrastructure implements AutoCloseable {

    @Override
    public void close() {
        docker.stop();
    }

    public static final int DIND_PORT = 2375;
    public static final String[] CONTAINER_CMD = {"sh", "-c", "echo started; while true; do sleep 3600; done"};
    public static final String TEST_LOCAL_IMAGE_TAG = "my-local-image:1.0";
    public static final String TEST_APP_CONTAINER_NAME = "porthole-test-app";
    public static final String TEST_NO_PORTS_CONTAINER_NAME = "porthole-test-no-ports";
    public static final String TEST_STOPPED_CONTAINER_NAME = "porthole-test-stopped";
    public static final String TEST_LOCAL_CONTAINER_NAME = "porthole-test-local";
    public static final String BUSYBOX_IMAGE = "busybox:1.37.0-uclibc";

    private final GenericContainer<?> docker;
    private final DockerClient sharedDockerClient;

    private GenericContainer<?> testAppContainer;
    private GenericContainer<?> localContainer;
    private GenericContainer<?> noPortsContainer;
    private String stoppedContainerId;

    public DockerInfrastructure() {
        docker = new GenericContainer<>("docker:dind")
                .withPrivilegedMode(true)
                .withExposedPorts(DIND_PORT)
                .withCreateContainerCmdModifier(cmd -> {
                    var hostConfig = cmd.getHostConfig()
                            .withPortBindings(
                                    new PortBinding(Ports.Binding.bindPort(DIND_PORT), ExposedPort.tcp(DIND_PORT)));
                    if (!"true".equalsIgnoreCase(System.getenv("CI"))) {
                        hostConfig.withBinds(new Bind("porthole-dind-data", new Volume("/var/lib/docker")));
                    }
                    cmd.withHostConfig(hostConfig);
                })
                .withEnv("DOCKER_TLS_CERTDIR", "")
                .withSharedMemorySize(512L * 1024 * 1024) // 512MB
                .waitingFor(forLogMessage(".*API listen on \\[::\\]:2375.*", 1));

        docker.start();
        sharedDockerClient = createClient(getDinDHost());
    }

    private String getDinDHost() {
        return "tcp://" + docker.getHost() + ":" + docker.getMappedPort(DIND_PORT);
    }

    private DockerClient createClient(String dockerHost) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();

        ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        return DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }

    public void cleanup() {
        if (testAppContainer != null) {
            testAppContainer.stop();
        }
        if (noPortsContainer != null) {
            noPortsContainer.stop();
        }
        if (localContainer != null) {
            localContainer.stop();
        }

        DockerClient dockerClient = getDetailsDockerClient();
        removeContainerQuietly(dockerClient, TEST_APP_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_STOPPED_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_NO_PORTS_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_LOCAL_CONTAINER_NAME);
    }

    private void removeContainerQuietly(DockerClient client, String containerName) {
        try {
            client.removeContainerCmd(containerName).withForce(true).exec();
        } catch (Exception e) {
            // Ignore
        }
    }

    public void ensureContainersRunning() throws Exception {
        if (testAppContainer != null
                && testAppContainer.isRunning()
                && noPortsContainer != null
                && noPortsContainer.isRunning()
                && localContainer != null
                && localContainer.isRunning()) {
            return;
        }
        createAndStartContainers();
    }

    private void createAndStartContainers() throws Exception {
        DockerClient dockerClient = getDetailsDockerClient();

        // Cleanup any existing containers from previous runs (due to persistent volume)
        removeContainerQuietly(dockerClient, TEST_APP_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_STOPPED_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_NO_PORTS_CONTAINER_NAME);
        removeContainerQuietly(dockerClient, TEST_LOCAL_CONTAINER_NAME);

        // Ensure BusyBox image is available
        dockerClient.pullImageCmd(BUSYBOX_IMAGE).start().awaitCompletion();

        // App Container (Running with ports)
        testAppContainer = new DinDContainer<>(DockerImageName.parse(BUSYBOX_IMAGE), sharedDockerClient)
                .withCommand(CONTAINER_CMD)
                .withExposedPorts(8080)
                .waitingFor(new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // No-op
                    }
                })
                .withStartupTimeout(Duration.ofSeconds(120))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(TEST_APP_CONTAINER_NAME));
        testAppContainer.start();

        // Stopped Container
        var stoppedResponse = dockerClient
                .createContainerCmd(BUSYBOX_IMAGE)
                .withName(TEST_STOPPED_CONTAINER_NAME)
                .withCmd(CONTAINER_CMD)
                .withExposedPorts(ExposedPort.tcp(8081))
                .exec();
        stoppedContainerId = stoppedResponse.getId();

        // No Ports Container
        noPortsContainer = new DinDContainer<>(DockerImageName.parse(BUSYBOX_IMAGE), sharedDockerClient)
                .withCommand(CONTAINER_CMD)
                // No withExposedPorts
                .waitingFor(new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // No-op
                    }
                })
                .withStartupTimeout(Duration.ofSeconds(120))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(TEST_NO_PORTS_CONTAINER_NAME));
        noPortsContainer.start();

        // Local Image Container
        Path tempDir = Files.createTempDirectory("porthole-test-context");
        Files.writeString(tempDir.resolve("Dockerfile"), "FROM " + BUSYBOX_IMAGE);

        dockerClient
                .buildImageCmd(tempDir.toFile())
                .withTags(java.util.Set.of(TEST_LOCAL_IMAGE_TAG))
                .start()
                .awaitImageId();

        localContainer = new DinDContainer<>(DockerImageName.parse(TEST_LOCAL_IMAGE_TAG), sharedDockerClient)
                .withCommand(CONTAINER_CMD)
                .withImagePullPolicy(imageName -> false)
                .withExposedPorts(8082)
                .waitingFor(new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // No-op
                    }
                })
                .withStartupTimeout(Duration.ofSeconds(120))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(TEST_LOCAL_CONTAINER_NAME));
        localContainer.start();
    }

    public DockerClient getDetailsDockerClient() {
        return sharedDockerClient;
    }

    public GenericContainer<?> getTestAppContainer() {
        return testAppContainer;
    }

    public GenericContainer<?> getLocalContainer() {
        return localContainer;
    }

    public GenericContainer<?> getNoPortsContainer() {
        return noPortsContainer;
    }

    public String getStoppedContainerId() {
        return stoppedContainerId;
    }

    public GenericContainer<?> getDocker() {
        return docker;
    }
}
