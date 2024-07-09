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
        stage("Quality Gate") {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'Sonar-token'
                }
            }
        }
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
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh "docker build -t zomato ."
                        sh "docker tag zomato krishnaprasadnr/zomato:${params.BRANCH_NAME}"
                        sh "docker push krishnaprasadnr/zomato:${params.BRANCH_NAME}"
                    }
                }
            }
        }
        stage("Trivy Image Scan") {
            steps {
                sh "trivy image krishnaprasadnr/zomato:${params.BRANCH_NAME} > trivy.txt"
            }
        }
        stage('Deploy to Container') {
            steps {
                sh "docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:${params.BRANCH_NAME}"
            }
        }
    }
    post {
        failure {
            script {
                echo "Deployment failed. Rolling back to the previous version."
                sh "docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:previous"
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
