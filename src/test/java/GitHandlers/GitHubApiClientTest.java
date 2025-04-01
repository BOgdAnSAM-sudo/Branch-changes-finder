package GitHandlers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class GitHubApiClientTest {
    private static final String TEST_TOKEN = "test_token";
    private static final String TEST_OWNER = "test_owner";
    private static final String TEST_REPO = "test_repo";
    private static final String TEST_BASE = "base_commit";
    private static final String TEST_BRANCH = "test_branch";

    private static WireMockServer wireMockServer;

    private GitHubApiClient githubApiClient;


    @AfterEach
    public void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        githubApiClient = new GitHubApiClient(TEST_TOKEN);
        try {
            java.lang.reflect.Field basicURL = GitHubApiClient.class.getDeclaredField("GITHUB_API_URL");
            basicURL.setAccessible(true);
            basicURL.set(githubApiClient, wireMockServer.baseUrl());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getChangedFilesShouldReturnFileListTest() throws Exception {
        wireMockServer.stubFor(get(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH)))
                .withHeader("Authorization", equalTo("token " + TEST_TOKEN))
                .withHeader("Accept", equalTo("application/vnd.github.v3+json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                        "files": [
                            {"filename": "file1.txt"},
                            {"filename": "file2.txt"}
                        ]
                    }
                    """)));

        List<String> changedFiles = githubApiClient.getChangedFiles(
                TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH);

        assertEquals(2, changedFiles.size());
        assertTrue(changedFiles.contains("file1.txt"));
        assertTrue(changedFiles.contains("file2.txt"));

        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH))));
    }

    @Test
    void getChangedFilesShouldHandleEmptyFileListTest() throws Exception {
        wireMockServer.stubFor(get(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("""
                    {
                        "files": []
                    }
                    """)));

        List<String> changedFiles = githubApiClient.getChangedFiles(
                TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH);

        assertTrue(changedFiles.isEmpty());
    }

    @Test
    void getChangedFilesShouldHandleErrorResponsesTest() {
        wireMockServer.stubFor(get(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH)))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("""
                    {
                        "message": "Not Found"
                    }
                    """)));

        assertThrows(GitHubApiException.class, () ->
                githubApiClient.getChangedFiles(TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH));
    }

    @Test
    void getChangedFilesShouldHandleMalformedJsonTest() {
        wireMockServer.stubFor(get(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Not JSON")));

        assertThrows(IOException.class, () ->
                githubApiClient.getChangedFiles(TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH));
    }

    @Test
    void getChangedFilesShouldHandleNetworkErrorsTest() {
        wireMockServer.stubFor(get(urlPathEqualTo(
                String.format("/repos/%s/%s/compare/%s...%s",
                        TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH)))
                .willReturn(aResponse()
                        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThrows(IOException.class, () ->
                githubApiClient.getChangedFiles(TEST_OWNER, TEST_REPO, TEST_BASE, TEST_BRANCH));
    }

}