import GitHandlers.GitException;
import GitHandlers.GitHubApiClient;
import GitHandlers.GitHubApiException;
import GitHandlers.LocalGitExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ChangedFilesAnalyzer {
    private final GitHubApiClient githubClient;
    private final LocalGitExecutor gitExecutor;
    private final String owner;
    private final String repo;
    private final String branchA;
    private final String branchB;

    public ChangedFilesAnalyzer(@NotNull String owner, @NotNull String repo, @NotNull String accessToken,
                                @NotNull String localRepoPath, @NotNull String branchA, @NotNull String branchB) {
        this.githubClient = new GitHubApiClient(accessToken);
        this.gitExecutor = new LocalGitExecutor(localRepoPath);
        this.owner = owner;
        this.repo = repo;
        this.branchA = branchA;
        this.branchB = branchB;
    }

    public ChangedFilesAnalyzer(@NotNull String owner, @NotNull String repo,
                                @NotNull String branchA, @NotNull String branchB,
                                @NotNull LocalGitExecutor gitExecutor, @NotNull GitHubApiClient githubClient) {
        this.owner = owner;
        this.repo = repo;
        this.branchA = branchA;
        this.branchB = branchB;
        this.gitExecutor = gitExecutor;
        this.githubClient = githubClient;
    }

    public List<String> findOverlappingChangedFiles() throws GitException, GitHubApiException {
        try {
            String mergeBase = gitExecutor.getMergeBase(branchA, branchB);
            if (mergeBase == null || mergeBase.isEmpty()) {
                throw new GitException("Could not find merge base between branches");
            }

            List<String> remoteChangedFiles = githubClient.getChangedFiles(owner, repo, mergeBase, branchA);
            List<String> localChangedFiles = gitExecutor.getChangedFiles(mergeBase, branchB);

            return remoteChangedFiles.stream()
                    .filter(localChangedFiles::contains)
                    .collect(Collectors.toList());
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitException("Git operation was interrupted", e);
        } catch (GitException e) {
            throw e;
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitException("Error executing git command", e);
        }
    }
}