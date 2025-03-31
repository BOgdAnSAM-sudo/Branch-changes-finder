package GitHandlers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class GitHubApiClient {
    private String GITHUB_API_URL = "https://api.github.com";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String authHeader;

    public GitHubApiClient(@NotNull String accessToken) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.authHeader = "token " + accessToken;
    }

    public List<String> getChangedFiles(@NotNull String owner, @NotNull String repo, @NotNull String baseCommit, @NotNull String branch) throws IOException, InterruptedException, GitHubApiException {
        String url = String.format("%s/repos/%s/%s/compare/%s...%s",
                GITHUB_API_URL, owner, repo, baseCommit, branch);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new GitHubApiException("Failed to communicate with GitHub API");
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode files = root.path("files");
        List<String> changedFiles = new ArrayList<>();

        for (JsonNode file : files) {
            changedFiles.add(file.path("filename").asText());
        }

        return changedFiles;
    }
}