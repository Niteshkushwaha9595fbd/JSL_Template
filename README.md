# Jenkins Shared Library (JSL_Template)

Parameterized Pipeline Template for **Database SQL Schema Deployment**. 

Aapko Jenkinsfile me sirf `def` karke database aur repository parameters define karne hain aur `dbDeployTemplate(dbConfig)` call karna hai. Poori pipeline automatically Git clone se lekar deployment tak parameterized stages me execute hogi.

---

## 📁 Repository Structure

```text
JSL_Template/
├── vars/
│   ├── dbDeployTemplate.groovy    # Complete Parameterized Pipeline Template
│   └── dbDeployTemplate.txt       # Parameters documentation
├── resources/templates/sql/
│   ├── V1__init_schema.sql        # Sample SQL migration script
│   └── U1__undo_init_schema.sql
├── examples/
│   └── Jenkinsfile                # Clean usage example
└── README.md
```

---

## 🚦 Pipeline Schema (Parameterized Stages)

Jab aap `dbDeployTemplate(dbConfig)` execute karte hain, toh yeh stages run hoti hain:

1. **Stage 1: Checkout Repository**:
   - Agar `repoUrl` diya hai toh specified repo aur `branch` ko clone karta hai (private repo ke liye `gitCredentialsId` use karta hai). Agar nahi diya, toh default `checkout scm` karta hai.
2. **Stage 2: Validate Parameters**:
   - `dbName`, `credentialsId`, aur cloned workspace me `sqlFile` existence validate karta hai.
3. **Stage 3: Pre-Deployment Checks**:
   - Target database host aur port connectivity test karta hai.
4. **Stage 4: Dry-Run & Schema Validation** *(Conditional: Runs only if `dryRun: true`)*:
   - Schema syntax validate karta hai bina real commit kiye.
5. **Stage 5: Deploy SQL Schema** *(Runs if `dryRun: false`)*:
   - Database credentials securely inject karke script ko target DB par apply karta hai (`mysql` / `psql`).
6. **Stage 6: Post-Deployment Verification**:
   - Deployment status verify karta hai.
7. **Post Actions**:
   - Success aur failure alerts print karta hai.

---

## ⚙️ Config Parameters Reference

| Parameter | Type | Default | Description |
|---|---|---|---|
| `repoUrl` | String | `''` | Git repository URL jisme SQL files hain (Optional) |
| `branch` | String | `'main'` | Git branch name |
| `gitCredentialsId` | String | `''` | Jenkins Git credentials ID (agar repo private hai) |
| `environment` | String | `'DEV'` | Target environment name (`DEV`, `QA`, `PROD`) |
| `dbType` | String | `'mysql'` | Database engine (`mysql` ya `postgres`) |
| `dbHost` | String | `'localhost'` | Database host / IP address |
| `dbPort` | String | Auto | Port (MySQL: 3306, Postgres: 5432) |
| `dbName` | String | **Required** | Target database ka naam |
| `credentialsId` | String | **Required** | Jenkins DB Username/Password Credentials ID |
| `sqlFile` | String | `'schema.sql'` | SQL script path (workspace ke andar) |
| `dryRun` | Boolean | `false` | True = Test/Rollback only, False = Apply changes |

---

## 💻 Usage in Jenkinsfile

```groovy
@Library('JSL_Template') _

def dbConfig = [
    // --- Git Repository ---
    repoUrl          : 'https://github.com/my-org/database-repo.git',
    branch           : 'main',
    gitCredentialsId : 'github-creds',

    // --- Database Target ---
    environment      : 'DEV',
    dbType           : 'mysql',
    dbHost           : '192.168.1.100',
    dbPort           : '3306',
    dbName           : 'app_production',
    credentialsId    : 'mysql-creds-id',
    sqlFile          : 'schema.sql',
    dryRun           : false
]

dbDeployTemplate(dbConfig)
```
