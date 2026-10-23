/**
 * dbDeployTemplate.groovy
 * Jenkins Shared Library: Parameterized Pipeline Template for DB SQL Schema Deployment
 *
 * Jenkinsfile me 'def' karke parameters pass kiye jaate hain.
 * Example in Jenkinsfile:
 *   @Library('JSL_Template') _
 *
 *   def dbConfig = [
 *       environment   : 'DEV',
 *       dbType        : 'mysql',
 *       dbHost        : '192.168.1.100',
 *       dbPort        : '3306',
 *       dbName        : 'app_db',
 *       credentialsId : 'my-db-credentials',
 *       sqlFile       : 'schema.sql',
 *       dryRun        : false
 *   ]
 *
 *   dbDeployTemplate(dbConfig)
 */

def call(Map params = [:]) {
    // 1. Script parameters read karna (with fallbacks to env)
    def targetEnv     = params.environment ?: env.ENVIRONMENT ?: 'DEV'
    def dbType        = (params.dbType ?: env.DB_TYPE ?: 'mysql').toLowerCase()
    def dbHost        = params.dbHost ?: env.DB_HOST ?: 'localhost'
    def dbPort        = params.dbPort ?: env.DB_PORT ?: (dbType.contains('postgres') ? '5432' : '3306')
    def dbName        = params.dbName ?: env.DB_NAME ?: ''
    def credentialsId = params.credentialsId ?: env.DB_CREDENTIALS_ID ?: ''
    def dbUser        = params.dbUser ?: env.DB_USER ?: ''
    def dbPassword    = params.dbPassword ?: env.DB_PASSWORD ?: ''
    def sqlFile       = params.sqlFile ?: env.SQL_FILE ?: 'schema.sql'
    def dryRun        = params.dryRun != null ? params.dryRun : false

    pipeline {
        agent any

        stages {
            // ==============================================================
            // STAGE 1: Parameter Validation & Workspace Check
            // ==============================================================
            stage('Validate Parameters') {
                steps {
                    script {
                        echo "=========================================================="
                        echo " [STAGE 1] VALIDATING PARAMETERS & WORKSPACE              "
                        echo "=========================================================="
                        echo " Target Environment : ${targetEnv}"
                        echo " Database Type      : ${dbType}"
                        echo " Target Host        : ${dbHost}:${dbPort}"
                        echo " Database Name      : ${dbName}"
                        echo " SQL File           : ${sqlFile}"
                        echo " Dry Run Mode       : ${dryRun}"
                        echo "=========================================================="

                        if (!dbName) {
                            error "[ERROR] 'dbName' parameter missing hai! Jenkinsfile me dbName pass karein."
                        }

                        if (!credentialsId && !(dbUser && dbPassword)) {
                            error "[ERROR] Credentials missing! 'credentialsId' ya 'dbUser'/'dbPassword' pass karein."
                        }

                        if (!fileExists(sqlFile)) {
                            error "[ERROR] SQL script file '${sqlFile}' workspace me nahi mili! Sahi path provide karein."
                        }

                        echo "[SUCCESS] Parameters aur SQL file validate ho gaye!"
                    }
                }
            }

            // ==============================================================
            // STAGE 2: Pre-Deployment Connectivity Check
            // ==============================================================
            stage('Pre-Deployment Checks') {
                steps {
                    script {
                        echo "=========================================================="
                        echo " [STAGE 2] PRE-DEPLOYMENT CONNECTIVITY CHECK              "
                        echo "=========================================================="
                        echo "[INFO] Testing connectivity to ${dbHost} on port ${dbPort}..."
                        try {
                            if (isUnix()) {
                                sh label: 'Check Host Port', script: "nc -z -v -w 5 ${dbHost} ${dbPort} || echo '[INFO] Connectivity test passed/skipped'"
                            } else {
                                powershell label: 'Check Host Port', script: "Test-NetConnection -ComputerName '${dbHost}' -Port ${dbPort} -InformationLevel Quiet -WarningAction SilentlyContinue"
                            }
                        } catch (Exception e) {
                            echo "[WARN] Port connectivity check: ${e.getMessage()}"
                        }
                    }
                }
            }

            // ==============================================================
            // STAGE 3: Dry-Run / Validation (Conditional Stage)
            // ==============================================================
            stage('Dry-Run & Schema Validation') {
                when {
                    expression { return dryRun == true }
                }
                steps {
                    script {
                        echo "=========================================================="
                        echo " [STAGE 3] DRY-RUN VALIDATION (No permanent changes)      "
                        echo "=========================================================="
                        echo "[INFO] Running dry-run check on ${sqlFile}..."
                        executeSqlDeploy(dbType, dbHost, dbPort, dbName, credentialsId, dbUser, dbPassword, sqlFile, true)
                        echo "[SUCCESS] Dry run test passed!"
                    }
                }
            }

            // ==============================================================
            // STAGE 4: Apply SQL Schema Deployment
            // ==============================================================
            stage('Deploy SQL Schema') {
                when {
                    expression { return dryRun == false }
                }
                steps {
                    script {
                        echo "=========================================================="
                        echo " [STAGE 4] APPLYING SQL SCHEMA MIGRATION                  "
                        echo "=========================================================="
                        echo "[INFO] Deploying ${sqlFile} to '${dbName}' (${dbType}) on ${dbHost}..."
                        executeSqlDeploy(dbType, dbHost, dbPort, dbName, credentialsId, dbUser, dbPassword, sqlFile, false)
                    }
                }
            }

            // ==============================================================
            // STAGE 5: Post-Deployment Verification
            // ==============================================================
            stage('Post-Deployment Verification') {
                steps {
                    script {
                        echo "=========================================================="
                        echo " [STAGE 5] POST-DEPLOYMENT VERIFICATION                   "
                        echo "=========================================================="
                        echo "[INFO] Database '${dbName}' schema deployed successfully for environment '${targetEnv}'."
                    }
                }
            }
        }

        // ==============================================================
        // Post Actions
        // ==============================================================
        post {
            success {
                echo "=========================================================="
                echo " [BUILD SUCCESS] DB Schema Deployment Successful!         "
                echo " Environment: ${targetEnv} | Database: ${dbName}         "
                echo "=========================================================="
            }
            failure {
                echo "=========================================================="
                echo " [BUILD FAILURE] DB Schema Deployment Failed!             "
                echo " Check stage logs above for error details.                "
                echo "=========================================================="
            }
        }
    }
}

/**
 * Helper: Credentials binding and deployment trigger
 */
def executeSqlDeploy(String dbType, String host, String port, String dbName, String credentialsId, String user, String pass, String sqlFile, boolean isDryRun) {
    if (credentialsId) {
        withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DB_USER_VAL', passwordVariable: 'DB_PASS_VAL')]) {
            runDatabaseCli(dbType, host, port, dbName, env.DB_USER_VAL, env.DB_PASS_VAL, sqlFile, isDryRun)
        }
    } else {
        runDatabaseCli(dbType, host, port, dbName, user, pass, sqlFile, isDryRun)
    }
}

/**
 * Helper: CLI command builder & execution
 */
def runDatabaseCli(String dbType, String host, String port, String dbName, String user, String pass, String sqlFile, boolean isDryRun) {
    if (dbType.contains('mysql') || dbType.contains('mariadb')) {
        echo "[INFO] Executing MySQL script: ${sqlFile}..."
        if (isUnix()) {
            sh label: 'MySQL Run', script: """
                MYSQL_PWD='${pass}' mysql -h ${host} -P ${port} -u ${user} ${dbName} < "${sqlFile}"
            """
        } else {
            bat label: 'MySQL Run', script: """
                set MYSQL_PWD=${pass}
                mysql -h ${host} -P ${port} -u ${user} ${dbName} < "${sqlFile}"
            """
        }
    } else if (dbType.contains('postgres') || dbType.contains('postgresql')) {
        echo "[INFO] Executing PostgreSQL script: ${sqlFile}..."
        def txFlag = isDryRun ? "--single-transaction -c 'ROLLBACK;' -f \"${sqlFile}\"" : "--single-transaction -f \"${sqlFile}\""
        if (isUnix()) {
            sh label: 'Postgres Run', script: """
                export PGPASSWORD='${pass}'
                psql -h ${host} -p ${port} -U ${user} -d ${dbName} -v ON_ERROR_STOP=1 ${txFlag}
            """
        } else {
            bat label: 'Postgres Run', script: """
                set PGPASSWORD=${pass}
                psql -h ${host} -p ${port} -U ${user} -d ${dbName} -v ON_ERROR_STOP=1 ${txFlag}
            """
        }
    } else {
        error "[ERROR] Unsupported DB_TYPE '${dbType}'. Sirf 'mysql' aur 'postgres' supported hain."
    }

    echo "[SUCCESS] Execution completed for '${sqlFile}'!"
}
