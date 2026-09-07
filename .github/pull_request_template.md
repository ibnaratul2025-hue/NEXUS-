## Description
Briefly describe the change, its motivation, and the problem it solves.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Security fix / anti-hallucination enhancement
- [ ] Documentation update
- [ ] World Model / Action Fabric enhancement
- [ ] JARVIS Operating Layer enhancement
- [ ] CI/CD or build system enhancement

## Architectural Compliance
- [ ] **Receipt Authority**: Model is NOT the source of truth for execution states.
- [ ] **Epistemic Invariant**: Predictions/assumptions are never treated as facts without verified ToolReceipts.
- [ ] **Risk Classification**: New actions/tools explicitly declare an accurate `RiskLevel` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
- [ ] **Hard Security Boundary**: No bypass of permission policies, security policies, confirmation dialogs, sandboxes, or the Emergency Stop kill switch.
- [ ] **Path Sandboxing**: File access is strictly contained via `FileSandboxHelper`.
- [ ] **No Hardcoded Secrets**: No API keys, tokens, or keystores included.

## Testing Performed
- [ ] Local JVM unit tests executed (`gradle :app:testDebugUnitTest`)
- [ ] Android Lint checks passed (`gradle :app:lintDebug`)
- [ ] Verified on real device or Android emulator (if UI/JNI changes made)

## Checklist
- [ ] My code follows the code style and formatting of this project.
- [ ] I have performed a self-review of my code.
- [ ] I have commented my code where necessary, particularly in hard-to-understand areas.
- [ ] I have updated corresponding documentation in `docs/` or `README.md`.
- [ ] My changes generate no new warnings or build errors.
