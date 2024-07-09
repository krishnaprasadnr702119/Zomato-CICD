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
    }
    
    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }
        
        stage('Checkout from Git') {
            when {
                expression {
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  // Skip checkout for develop and main branches
                }
            }
            steps {
                git branch: "${params.BRANCH_NAME}", url: 'https://github.com/krishnaprasadnr702119/Zomato-CICD.git'
            }
        }
        
        stage("Sonarqube Analysis") {
            when {
                expression {
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  // Skip SonarQube analysis for develop and main branches
                }
            }
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh """$SCANNER_HOME/bin/sonar-scanner \
                        -Dsonar.projectName=zomato \
                        -Dsonar.projectKey=zomato \
                        -Dsonar.sources=src \
                        -Dsonar.exclusions=public"""
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
            when {
                allOf {
                    expression {
                        params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  // Skip Docker build for develop and main branches
                    }
                    expression {
                        currentBuild.rawBuild.getLog(Integer.MAX_VALUE).contains("Files already committed")
                    }
                }
            }
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    def dockerTag = "${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    
                    // Building Docker image
                    sh "docker build -t zomato:${version} ."
                    
                    // Tagging Docker image
                    sh "docker tag zomato:${version} krishnaprasadnr/zomato:${dockerTag}"
                    
                    // Pushing Docker image
                    withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {
                        sh "docker push krishnaprasadnr/zomato:${dockerTag}"
                    }
                }
            }
        }
        
        stage("Trivy Image Scan") {
            when {
                expression {
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  // Skip Trivy scan for develop and main branches
                }
            }
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    def dockerTag = "${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    
                    // Scanning Docker image
                    sh "trivy image krishnaprasadnr/zomato:${dockerTag} > trivy.txt"
                }
            }
        }
        
        stage('Deploy to Container') {
            when {
                allOf {
                    expression {
                        params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  // Skip deployment for develop and main branches
                    }
                    expression {
                        currentBuild.rawBuild.getLog(Integer.MAX_VALUE).contains("Files already committed")
                    }
                    expression {
                        params.DEPLOY_ENV in ['dev', 'staging', 'uat', 'production']  // Only deploy for selected environments
                    }
                }
            }
            steps {
                script {
                    def version = versioning(params.DEPLOY_ENV)
                    def dockerTag = "${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    def server
                    
                    // Determine server based on DEPLOY_ENV
                    switch (params.DEPLOY_ENV) {
                        case 'dev':
                            server = '192.168.1.71'
                            break
                        case 'staging':
                            server = '192.168.1.71'
                            break
                        case 'uat':
                            server = '192.168.1.71'
                            break
                        case 'production':
                            server = '192.168.1.71'
                            break
                        default:
                            echo "Unknown DEPLOY_ENV"
                            currentBuild.result = 'FAILURE'
                            return
                    }
                    
                    // Deploy to server using SSH credentials
                    withCredentials([usernamePassword(credentialsId: "inapp${params.DEPLOY_ENV}", usernameVariable: 'SSH_USER', passwordVariable: 'SSH_PASSWORD')]) {
                        sh """
                        sshpass -p '${SSH_PASSWORD}' ssh ${SSH_USER}@${server} 'docker stop zomato && docker rm zomato && docker pull krishnaprasadnr/zomato:${dockerTag} && docker run -d --name zomato -p 3000:3000 krishnaprasadnr/zomato:${dockerTag}'
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo "Cleaning up old Docker containers and images."
                sh "docker container prune -f"
                sh "docker image prune -f"
            }
        }
    }
}
