#!/usr/bin/env groovy
@Library('velo-pipeline-library@master') _

import com.velopayments.pipeline.MavenUtils
import com.velopayments.pipeline.Utils

pipeline {

    agent { label 'ec2' }

    tools {
        maven MavenUtils.DEFAULT_MVN
        jdk 'jdk-11'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr:'30'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
        ansiColor('xterm')
    }

    environment {
        LIBRARY = 'velochain-sdk'
        LIBRARY_MODULE = 'sdk'
        HOSTNAME = 'velochain.sandbox.velopayments.com'
    }

    stages {
        stage ('Build') {
            steps {
                mvnBuildLibrary("${env.LIBRARY_MODULE}/target/${env.LIBRARY}.jar")
                dir('docker/sentinel-container/') {
                    withDefaultMaven([options: [jacocoPublisher(disabled: true)]]) {
                        script {
                            if (isReleaseBuild()) {
                                sh "./build.sh 1.0.${env.BUILD_NUMBER}"
                            } else {
                                sh "./build.sh"
                            }
                        }
                    }
                }
            }
        }

        stage ('Code Quality') {
            steps {
                parallel(sonar: {
                    mvnSonar()
                },
                 owasp: {
                    mvnDependencyCheck()
                })
            }
        }

        stage ('Release') {
            when { expression { isReleaseBuild() }}
            steps {
                mvnReleaseArtifacts()

                // TODO send the distrib to S3 instead
                //archiveArtifacts artifacts: 'distrib/target/*-bin.zip', fingerprint: true, onlyIfSuccessful: false
                withDefaultDockerRegistry {
                    sh "docker push velopayments/velochain-sentinels:latest"
                }
            }
        }

        stage ('Deploy') {
            when { expression {  false && isReleaseBuild() }} // TODO: deploy

            steps {
                sshagent(['dev2-deployer']) {
                    sh "ssh  -o StrictHostKeyChecking=no centos@${env.HOSTNAME} \'./update.sh\'"
                }

                timeout(3) {
                    sh '''
                        while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' https://${HOSTNAME}/manage/health)" != "200" ]]
                        do
                          sleep 5
                        done
                    '''
                }
            }
        }
    }

    post {
        changed {
            changeNotification()
        }
    }
}
