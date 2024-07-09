pipeline {
    agent any
    tools {
        nodejs 'node16'
    }
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }
    parameters {
        choice(
            name: 'BRANCH_NAME',
            choices: ['main', 'develop', 'feature', 'bugfix'],
            description: 'Select the branch to build'
        )
        choice(
            name: 'DEPLOY_ENV',
            choices: ['dev', 'staging', 'uat', 'production'],
            description: 'Select the deployment environment'
        )
        choice(
            name: 'ROLL_BACK',
            choices: ['specific', 'previous','none'],
            description: 'Select rollback strategy'
        )
    }
    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout from Git') {
            steps {
                git branch: "${params.BRANCH_NAME}", url: 'https://github.com/krishnaprasadnr702119/Zomato-CICD.git'
            }
        }
        stage("Sonarqube Analysis") {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh '''$SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=zomato \
                    -Dsonar.projectKey=zomato'''
                }
            }
        }
        // stage("Quality Gate") {
        //     steps {
        //         script {
        //             waitForQualityGate abortPipeline: false, credentialsId: 'Sonar-token'
        //         }
        //     }
        // }
        stage('Install Dependencies') {
            steps {
                sh "npm install"
            }
        }
        stage('OWASP FS Scan') {
            steps {
                dependencyCheck additionalArguments: '--scan ./ --disableYarnAudit --disableNodeAudit', odcInstallation: 'DP-Check'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('Trivy FS Scan') {
            steps {
                sh "trivy fs . > trivyfs.txt"
            }
        }
        stage("Docker Build & Push") {
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh "docker build -t zomato:${version} ."
                        sh "docker tag zomato:${version} krishnaprasadnr/zomato:${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                        sh "docker push krishnaprasadnr/zomato:${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    }
                }
            }
        }
        stage("Trivy Image Scan") {
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    sh "trivy image krishnaprasadnr/zomato:${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version} > trivy.txt"
                }
            }
        }
        stage('Deploy to Container') {
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    sh "docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                }
            }
        }
    }
    post {
        failure {
            script {
                echo "Deployment failed. Rolling back to the previous version."
                if (params.ROLL_BACK == 'previous') {
                    sh "docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:previous"
                } else if (params.ROLL_BACK == 'specific') {
                    input message: 'Please provide the version to roll back to', parameters: [string(name: 'ROLLBACK_VERSION', defaultValue: '', description: 'Version to roll back to')]
                    sh "docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:${ROLLBACK_VERSION}"
                }
            }
        }
        always {
            script {
                echo "Cleaning up old Docker containers and images."
                sh "docker container prune -f"
                sh "docker image prune -f"
            }
        }
    }
}
