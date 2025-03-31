# Branch Changes Finder

Assume there is a GitHub.com repository and it's local clone.
The branch branchA is present in both: remote and local repositories. Local branchA is not necessarily synchronized with the remote branchA.
The local branch branchB is created from branchA locally.

This library can find all files with the same path that were changed in both branchA (remotely) and branchB (locally) independently since the merge base commit (latest common commit).