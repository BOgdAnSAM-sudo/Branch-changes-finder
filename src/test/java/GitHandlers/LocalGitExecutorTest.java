package GitHandlers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalGitExecutorTest {
    @TempDir
    private static Path tempDir;
    private static LocalGitExecutor gitExecutor;

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        executeCommand(tempDir, "git", "init");

        Files.createFile(tempDir.resolve("README.md"));
        executeCommand(tempDir, "git", "add", "README.md");
        executeCommand(tempDir, "git", "commit", "-m", "Initial commit");

        executeCommand(tempDir, "git", "checkout", "-b", "branchA");
        Files.createFile(tempDir.resolve("fileA.txt"));
        executeCommand(tempDir, "git", "add", "fileA.txt");
        executeCommand(tempDir, "git", "commit", "-m", "Add fileA");

        executeCommand(tempDir, "git", "checkout", "-b", "branchB");
        Files.createFile(tempDir.resolve("fileB.txt"));
        executeCommand(tempDir, "git", "add", "fileB.txt");
        executeCommand(tempDir, "git", "commit", "-m", "Add fileB");

        gitExecutor = new LocalGitExecutor(tempDir.toString());
    }

    @Test
    void getMergeBase_shouldReturnCommonAncestor() throws IOException {
        String mergeBase = gitExecutor.getMergeBase("branchA", "branchB");
        assertNotNull(mergeBase);
        assertFalse(mergeBase.isEmpty());
    }

    @Test
    void getChangedFiles_shouldReturnChangedFiles() throws IOException {
        String mergeBase = gitExecutor.getMergeBase("branchA", "branchB");
        List<String> changedFiles = gitExecutor.getChangedFiles(mergeBase, "branchB");

        assertNotNull(changedFiles);
        assertTrue(changedFiles.contains("fileB.txt"));
        assertFalse(changedFiles.contains("fileA.txt"));
    }

    @Test
    void getMergeBase_shouldThrowExceptionForInvalidBranches() {
        assertThrows(NullPointerException.class, () -> gitExecutor.getMergeBase("nonexistent", "branchB"));
    }

    @Test
    void getChangedFiles_shouldReturnEmptyListForNoChanges() throws IOException {
        String mergeBase = gitExecutor.getMergeBase("branchA", "branchB");

        List<String> changedFiles = gitExecutor.getChangedFiles(mergeBase, mergeBase);
        assertTrue(changedFiles.isEmpty());
    }

    private static void executeCommand(Path workingDir, String... command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDir.toFile())
                .start();
        process.waitFor();
    }

}