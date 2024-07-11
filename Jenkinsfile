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
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  
                }
            }
            steps {
                git branch: "${params.BRANCH_NAME}", url: 'https://github.com/krishnaprasadnr702119/Zomato-CICD.git'
            }
        }
        
        stage("Sonarqube Analysis") {
            when {
                expression {
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main' 
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
                        params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  
                    }
                }
            }
            steps {
                script {
                    def version = env.BUILD_NUMBER 
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
                    params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main' 
                }
            }
            steps {
                script {
                    def version = env.BUILD_NUMBER
                    def dockerTag = "${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    
                    sh "trivy image krishnaprasadnr/zomato:${dockerTag} > trivy.txt"
                }
            }
        }
        
        stage('Deploy to Container') {
            when {
                allOf {
                    expression {
                        params.BRANCH_NAME != 'develop' && params.BRANCH_NAME != 'main'  
                    }
                    expression {
                        params.DEPLOY_ENV in ['dev', 'staging', 'uat', 'production']  
                    }
                }
            }
            steps {
                script {
                    def version = env.BUILD_NUMBER
                    def dockerTag = "${params.BRANCH_NAME}-${params.DEPLOY_ENV}-${version}"
                    
                    sh "docker run -d --name zomato-${params.DEPLOY_ENV} -p 8080:80 krishnaprasadnr/zomato:${dockerTag}"
                }
            }
        }
    }
}
