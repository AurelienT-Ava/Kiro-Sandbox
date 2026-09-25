# EMG Kiro sandbox

Training repository for the Kiro hands-on labs. It deliberately mixes the two
stacks you work with every day: a modern Java service and a legacy Oracle Pro\*C
module.

## What is inside

```
java-service/    Spring Boot 3 / Java 21 billing service. Builds, runs, tests pass.
legacy-proc/     Oracle Pro*C billing batch. Read only, no build environment.
docs/            Business rules for billing. Deliberately incomplete.
data/            Reference data and environment configuration.
.kiro/           Empty. You will fill it during Lab 1.
```

## Prerequisites

- JDK 21 (`java -version` must report 21)
- Kiro IDE and Kiro CLI installed and authenticated
- The 1-hour Kiro Getting Started module on AWS Skill Builder, completed

## Check your setup before the lab

Run this from the repository root. All three commands must succeed.

```bash
cd java-service
./mvnw clean test          # mvnw.cmd on Windows
./mvnw spring-boot:run     # then Ctrl+C
```

**You do not need Maven installed.** The `mvnw` wrapper downloads its own copy
on first use, into `~/.m2/wrapper/`. That first download takes a few minutes;
everything afterwards is fast. If you already have Maven, plain `mvn` works too.

### If the wrapper cannot download (corporate proxy)

Install Maven once, by hand, then use `mvn` instead of `./mvnw`:

```powershell
# Windows, PowerShell
$v = "3.9.9"
$dest = "$env:USERPROFILE\.m2\wrapper"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Invoke-WebRequest -UseBasicParsing `
  -Uri "https://archive.apache.org/dist/maven/maven-3/$v/binaries/apache-maven-$v-bin.zip" `
  -OutFile "$dest\maven.zip"
Expand-Archive "$dest\maven.zip" -DestinationPath $dest -Force
$env:PATH = "$dest\apache-maven-$v\bin;$env:PATH"
mvn -v
```

Or simply `winget install EclipseAdoptium.Temurin.21.JDK` for the JDK and
download Maven from maven.apache.org. Raise it in the Champions channel if the
proxy blocks both.

```bash
kiro-cli --version
```

If any of these fails, raise it in the Solutions Champions channel **before**
the session rather than during it.

## Try the API

### macOS / Linux, or Windows `cmd.exe`

```bash
curl http://localhost:8080/api/invoices/customers

curl -X POST http://localhost:8080/api/invoices/C-1003 \
  -H "Content-Type: application/json" \
  -d "[{\"reference\":\"REF-1\",\"description\":\"Support pack\",\"quantity\":2,\"unitPrice\":149.99}]"
```

On Windows use `curl.exe`, not `curl`, and keep it in a **cmd.exe** window.

### Windows PowerShell

PowerShell aliases `curl` to `Invoke-WebRequest` and rewrites escaped quotes, so
the command above will fail there. Use the native cmdlet instead:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/invoices/customers

Invoke-RestMethod -Uri http://localhost:8080/api/invoices/C-1003 `
  -Method Post -ContentType 'application/json' `
  -Body '[{"reference":"REF-1","description":"Support pack","quantity":2,"unitPrice":149.99}]'
```

Single quotes around `-Body` are what keeps the inner double quotes intact.

To inspect an error response, catch it:

```powershell
try {
  Invoke-RestMethod -Uri http://localhost:8080/api/invoices/C-9999 `
    -Method Post -ContentType 'application/json' `
    -Body '[{"reference":"R1","description":"Support","quantity":1,"unitPrice":10.00}]'
} catch {
  "HTTP " + $_.Exception.Response.StatusCode.value__
}
```

## The reference

The business rules in `docs/billing-rules.md` are the reference. Where the code
and the documentation disagree, **the documentation wins**.

A green test suite is not evidence that the code matches the rules. The tests
cover what someone chose to cover, which is never everything.

## Ground rule

Everything Kiro proposes is a proposal. You read it, you understand it, you
decide. Whatever ends up in a commit is yours.
