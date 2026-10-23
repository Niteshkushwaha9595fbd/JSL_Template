# Jenkins Shared Library (JSL_Template)

Parameterized Pipeline Template for **Database SQL Schema Deployment**. 

Aapko Jenkinsfile me sirf `def` karke database parameters define karne hain aur `dbDeployTemplate(dbConfig)` call karna hai. Poori pipeline automatically parameterized stages ke sath execute hogi.

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

1. **Stage 1: Validate Parameters**:
   - `dbName`, `credentialsId`, aur `sqlFile` existence validate karta hai.
2. **Stage 2: Pre-Deployment Checks**:
   - Target database host aur port connectivity test karta hai.
3. **Stage 3: Dry-Run & Schema Validation** *(Conditional: Runs only if `dryRun: true`)*:
   - Schema syntax validate karta hai bina real commit kiye.
4. **Stage 4: Deploy SQL Schema** *(Runs if `dryRun: false`)*:
   - Credentials securely inject karke script ko target DB par deploy karta hai (`mysql` / `psql`).
5. **Stage 5: Post-Deployment Verification**:
   - Deployment status verify karta hai.
6. **Post Actions**:
   - Success aur failure alerts print karta hai.

---

## 💻 Usage in Jenkinsfile

Aap apne Jenkinsfile me `def` karke parameters pass kar sakte hain:

```groovy
@Library('JSL_Template') _

def dbConfig = [
    environment   : 'DEV',                     // Target Env: DEV, QA, UAT, PROD
    dbType        : 'mysql',                   // 'mysql' ya 'postgres'
    dbHost        : '192.168.1.100',           // Database Host / IP
    dbPort        : '3306',                    // Database Port
    dbName        : 'app_production',          // Database Name (Required)
    credentialsId : 'mysql-creds-id',          // Jenkins Credentials ID
    sqlFile       : 'schema.sql',              // SQL Script path
    dryRun        : false                      // true (test only) ya false (apply)
]

dbDeployTemplate(dbConfig)
```
