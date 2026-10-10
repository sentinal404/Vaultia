# Security Policy

At **Vaultia**, data sovereignty, zero-trust cloud privacy, and cryptographic rigor are foundational. Because Vaultia turns Telegram cloud infrastructure into an encrypted personal vault with direct MTProto binary streaming, safeguarding credentials, encryption keys, and client integrity is our highest priority.

---

## Supported Versions

Only the latest release versions receive security patches and updates. We recommend all users and developers upgrade promptly when new releases are published.

| Version | Supported | Status |
| :--- | :---: | :--- |
| `v2.0.x` | :white_check_mark: | Current Stable Release (TDLib MTProto Engine) |
| `v1.0.x` | :warning: | Maintenance / Critical Security Only |
| `< 1.0.0` | :x: | End of Life (Unsupported) |

---

## Reporting a Security Vulnerability

We follow a strict **Responsible Disclosure Policy**. Please review the guidelines below before submitting a report:

### :lock: Private Security Reports (CRITICAL — DO NOT OPEN PUBLIC ISSUES)

If you discover a vulnerability that could compromise user security, credentials, or privacy, **DO NOT open a public GitHub Issue, pull request, or discussion**. Publicly disclosing an unpatched vulnerability exposes users to active exploits.

Please report the following categories exclusively through **GitHub Private Vulnerability Reporting**:

- **Remote Code Execution (RCE)** or arbitrary code execution via native components / JNI (`libtdjni.so`)
- **Local File Inclusion (LFI)**, arbitrary path traversal (`../`) during file extraction or chunk reassembly
- **Credential & Token Theft**: Telegram Bot Token, API ID, API Hash, or session leakage via IPC, logs, or unencrypted storage
- **Cryptographic Flaws**: AES-256-GCM key derivation issues, nonce/IV reuse, or ciphertext tampering
- **Biometric / App Lock Bypass**: Circumventing fingerprint/PIN verification via Intent manipulation or state tampering
- **Insecure IPC / Exported Components**: Unauthorized access to ContentProviders, BroadcastReceivers, or Services
- **Data Exfiltration**: Sensitive file metadata or cleartext transfers leaking to unintended endpoints

#### How to Submit:
1. Navigate to the repository's **[Security Advisories](https://github.com/sentinal404/Vaultia/security/advisories/new)** tab.
2. Click **"Report a vulnerability"** to open a confidential report.
3. Provide a clear description, proof of concept (PoC) or reproduction steps, and the affected version/environment.
4. If GitHub Private Reporting is temporarily unavailable, you may contact the maintainer directly at: `sentinal404@users.noreply.github.com` with the subject line `[SECURITY] Vaultia Vulnerability Report`.

---

### :beetle: Public Bug Reports (Functional & Non-Security Issues)

If you encounter general software bugs, UI glitches, or performance issues that **do not present a security or privacy threat**, please submit a standard issue:

- Visual artifacts, layout padding, or theme color contrast issues
- Media player buffering, codec compatibility, or playback controls
- Slow download/upload speeds or network retry failures
- Folder scanning or metadata indexing anomalies
- Feature suggestions, design critiques, and documentation improvements

:link: **Submit public bugs here:** [Open GitHub Issue](https://github.com/sentinal404/Vaultia/issues/new)

---

## Response Timeline & SLA

When you submit a private security disclosure, our response workflow follows these target commitments:

1. **Initial Acknowledgment**: Within **24 to 48 hours**, confirming receipt of the report.
2. **Assessment & Triage**: Within **72 hours**, assessing CVSS severity, reproducing the vulnerability, and determining affected versions.
3. **Remediation & Patching**: A security fix is engineered in a private advisory fork and verified against regression tests.
4. **Coordinated Disclosure**: A public release containing the patch is deployed alongside an advisory credit acknowledging the security researcher.

---

## Cryptographic Architecture Notes

To assist security researchers during code reviews:

- **Client-Side Encryption**: Files may be encrypted using hardware-backed **AES-256-GCM** via the Android Jetpack Security / Android KeyStore provider before chunking and transmission.
- **Credential Storage**: Sensitive user parameters (`bot_token`, `api_id`, `api_hash`) are stored in `EncryptedSharedPreferences` backed by `MasterKeys.AES256_GCM_SPEC`.
- **Zero-Logging Policy**: Cleartext bot tokens, session hashes, and decryption keys are strictly omitted from logcat (`android.util.Log`) in release builds.
- **Local Proxy Security**: The internal HTTP media streaming server binds strictly to the loopback interface (`127.0.0.1`) on a dynamic local port, rejecting external network interfaces.

---

## Safe Harbor Commitment

We support security research conducted in good faith. If you:
- Make a good faith effort to avoid privacy violations, destruction of data, and interruption of services.
- Keep the vulnerability details confidential until a patch has been published and released.
- Do not exploit the vulnerability beyond what is strictly necessary to establish proof of concept.

We commit to:
- Not pursuing legal action against you or initiating law enforcement investigations for your research.
- Working cooperatively with you to understand, remediate, and credit your discovery.

Thank you for helping keep Vaultia and its users safe!
