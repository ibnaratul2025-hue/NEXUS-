# Contributing Documentation

For the primary contribution guide, please refer to the root [CONTRIBUTING.md](../CONTRIBUTING.md).

This sub-document covers repository etiquette, review guidelines, and release verification.

---

## 1. Code Review Criteria

All pull requests are evaluated against:
1. **Receipt Authority**: Does the pull request introduce any logic where the LLM's unverified output alters persistent state? If yes, it will be rejected.
2. **Deterministic Risk**: Are new tools properly assigned an accurate `RiskLevel`?
3. **Sandbox Compliance**: Do file operations strictly utilize `FileSandboxHelper`?
4. **Test Coverage**: Does the PR include Robolectric or unit tests covering success, failure, and permission-denied cases?
5. **No Secrets or Bloat**: Ensure no binary model files (`.gguf`) or secrets are included.

---

## 2. Running Verification Scripts

Before submitting a PR or triggering a release:
```bash
# Verify checksums and release packaging integrity
./scripts/verify-release.sh app/build/outputs/apk/debug/app-debug.apk
```
