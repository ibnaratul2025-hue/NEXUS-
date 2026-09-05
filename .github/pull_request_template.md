## Description
Briefly describe the change, its motivation, and the problem it solves.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Security fix / anti-hallucination enhancement
- [ ] Documentation update
- [ ] CI/CD or build system enhancement

## Architectural Compliance
- [ ] Receipt Authority: Model is NOT the source of truth for execution states.
- [ ] Risk Classification: New tools explicitly declare an accurate `RiskLevel`.
- [ ] Path Sandboxing: File access is strictly contained via `FileSandboxHelper`.
- [ ] No Hardcoded Secrets: No API keys, tokens, or keystores included.

## Testing Performed
- [ ] Local JVM unit tests executed (`./gradlew test`)
- [ ] Android Lint checks passed (`./gradlew lint`)
- [ ] Verified on real device or Android emulator (if UI/JNI changes made)

## Checklist
- [ ] My code follows the code style and formatting of this project.
- [ ] I have performed a self-review of my code.
- [ ] I have commented my code where necessary, particularly in hard-to-understand areas.
- [ ] I have updated corresponding documentation in `docs/` or `README.md`.
- [ ] My changes generate no new warnings or build errors.
