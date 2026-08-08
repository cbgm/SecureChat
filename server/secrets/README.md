# Production secrets

Do not place real secret values in source control. Run `scripts/New-ProductionSecrets.ps1` from the
repository root to generate the ignored `*.txt` files and `server/.env.production`.

Every file contains one random secret without a trailing newline. Docker Compose grants each file
only to the services that need it and mounts it under `/run/secrets`.
