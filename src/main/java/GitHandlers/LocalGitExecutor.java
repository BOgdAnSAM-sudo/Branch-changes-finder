package GitHandlers;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalGitExecutor {
    private final Path repoPath;

    public LocalGitExecutor(@NotNull String localRepoPath) {
        this.repoPath = Path.of(localRepoPath);
    }

    public String getMergeBase(@NotNull String branchA, @NotNull String branchB) throws IOException {
        String[] command = {"git", "merge-base", branchA, branchB};
        Process process = new ProcessBuilder(command)
                .directory(repoPath.toFile())
                .start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            return reader.readLine().trim();
        }
    }

    public List<String> getChangedFiles(@NotNull String baseCommit, @NotNull String branch) throws IOException {
        String[] command = {"git", "diff", "--name-only", baseCommit + ".." + branch};
        Process process = new ProcessBuilder(command)
                .directory(repoPath.toFile())
                .start();

        List<String> changedFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    changedFiles.add(line.trim());
                }
            }
        }
        return changedFiles;
    }
}