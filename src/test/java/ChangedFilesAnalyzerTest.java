import GitHandlers.GitException;
import GitHandlers.GitHubApiClient;
import GitHandlers.GitHubApiException;
import GitHandlers.LocalGitExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChangedFilesAnalyzerTest {
    private static final String TEST_OWNER = "testOwner";
    private static final String TEST_REPO = "testRepo";
    private static final String BRANCH_A = "branchA";
    private static final String BRANCH_B = "branchB";
    private static final String MERGE_BASE = "mergeBaseSha";

    private GitHubApiClient mockGitHubClient;

    private LocalGitExecutor mockGitExecutor;

    private ChangedFilesAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        mockGitHubClient = Mockito.mock(GitHubApiClient.class);
        mockGitExecutor = Mockito.mock(LocalGitExecutor.class);
        analyzer = new ChangedFilesAnalyzer(TEST_OWNER, TEST_REPO, BRANCH_A, BRANCH_B, mockGitExecutor, mockGitHubClient);
    }

    @Test
    void findOverlappingChangedFilesShouldReturnCommonFiles() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn(MERGE_BASE);

        List<String> remoteFiles = Arrays.asList("file1.txt", "file2.txt", "common.txt");
        List<String> localFiles = Arrays.asList("common.txt", "file3.txt", "file4.txt");

        when(mockGitHubClient.getChangedFiles(TEST_OWNER, TEST_REPO, MERGE_BASE, BRANCH_A))
                .thenReturn(remoteFiles);
        when(mockGitExecutor.getChangedFiles(MERGE_BASE, BRANCH_B))
                .thenReturn(localFiles);

        List<String> result = analyzer.findOverlappingChangedFiles();

        assertEquals(1, result.size());
        assertTrue(result.contains("common.txt"));
    }

    @Test
    void findOverlappingChangedFilesShouldReturnEmptyList_WhenNoCommonFiles() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn(MERGE_BASE);

        List<String> remoteFiles = Arrays.asList("file1.txt", "file2.txt");
        List<String> localFiles = Arrays.asList("file3.txt", "file4.txt");

        when(mockGitHubClient.getChangedFiles(TEST_OWNER, TEST_REPO, MERGE_BASE, BRANCH_A))
                .thenReturn(remoteFiles);
        when(mockGitExecutor.getChangedFiles(MERGE_BASE, BRANCH_B))
                .thenReturn(localFiles);

        List<String> result = analyzer.findOverlappingChangedFiles();

        assertTrue(result.isEmpty());
    }

    @Test
    void findOverlappingChangedFilesShouldThrowGitExceptionWhenNoMergeBase() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn("");

        GitException exception = assertThrows(GitException.class,
                () -> analyzer.findOverlappingChangedFiles());

        assertEquals("Could not find merge base between branches", exception.getMessage());
    }

    @Test
    void findOverlappingChangedFilesShouldThrowGitHubApiExceptionWhenApiFails() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn(MERGE_BASE);
        when(mockGitHubClient.getChangedFiles(TEST_OWNER, TEST_REPO, MERGE_BASE, BRANCH_A))
                .thenThrow(new GitHubApiException("API error"));

        assertThrows(GitHubApiException.class,
                () -> analyzer.findOverlappingChangedFiles());
    }

    @Test
    void findOverlappingChangedFilesShouldThrowGitExceptionWhenGitFails() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn(MERGE_BASE);
        when(mockGitHubClient.getChangedFiles(TEST_OWNER, TEST_REPO, MERGE_BASE, BRANCH_A)).thenReturn(null);
        when(mockGitExecutor.getChangedFiles(MERGE_BASE, BRANCH_B))
                .thenThrow(new IOException("Git error"));

        GitException exception = assertThrows(GitException.class,
                () -> analyzer.findOverlappingChangedFiles());

        assertEquals("Error executing git command", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void findOverlappingChangedFiles_ShouldPreserveInterruptedStatus_WhenInterrupted() throws Exception {
        when(mockGitExecutor.getMergeBase(BRANCH_A, BRANCH_B)).thenReturn(MERGE_BASE);
        when(mockGitExecutor.getChangedFiles(MERGE_BASE, BRANCH_B))
                .thenAnswer(invocation -> {
                    throw new InterruptedException();
                });

        assertThrows(GitException.class,
                () -> analyzer.findOverlappingChangedFiles());

        assertTrue(Thread.interrupted()); // Clears the interrupted status
    }
}