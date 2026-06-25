pipeline {
  agent {label 'xretorch-agent'}
  environment {
    SELENOID_PRESENT = "TRUE"
    SUT_LOCATION = "$WORKSPACE/"
    SCRIPTS_FOLDER = "$WORKSPACE/.retorch/scripts"
  } // EndEnvironment
  options {
    disableConcurrentBuilds()
  } // EndPipOptions
  stages {
    stage('Clean Workspace') {
        steps {
            cleanWs()
        } // EndStepsCleanWS
    } // EndStageCleanWS
    stage('Clone Project') {
        steps {
            checkout scm
        } // EndStepsCloneProject
    } // EndStageCloneProject
    stage('SETUP-Infrastructure') {
        steps {
            sh 'chmod +x -R $SCRIPTS_FOLDER'
            sh '$SCRIPTS_FOLDER/coilifecycles/coi-setup.sh'
        } // EndStepsSETUPINF
    } // EndStageSETUPInf
    stage('Stage 0') {
      failFast false
      parallel {
        stage('tjoba IdResource: home-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjoba 0 http://tjoba-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjoba 0 http://tjoba-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineBadRequest"'
            }// EndExecutionStageErrortjoba
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjoba 0'
          }// EndStepstjoba
        }// EndStagetjoba
        stage('tjobb IdResource: home-timeline post social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobb 0 http://tjobb-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobb 0 http://tjobb-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineShowsFollowedUserPost"'
            }// EndExecutionStageErrortjobb
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobb 0'
          }// EndStepstjobb
        }// EndStagetjobb
        stage('tjobc IdResource: user-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobc 0 http://tjobc-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobc 0 http://tjobc-nginx-thrift:8080 "TestApiPosts#testAPIReadUserTimelineBadRequest"'
            }// EndExecutionStageErrortjobc
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobc 0'
          }// EndStepstjobc
        }// EndStagetjobc
        stage('tjobd IdResource: frontend post user user-timeline web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobd 0 http://tjobd-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobd 0 http://tjobd-nginx-thrift:8080 "TestPosts#testComposePostAppearsInTimeline"'
            }// EndExecutionStageErrortjobd
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobd 0'
          }// EndStepstjobd
        }// EndStagetjobd
        stage('tjobe IdResource: frontend social-graph user web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobe 0 http://tjobe-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobe 0 http://tjobe-nginx-thrift:8080 "TestSocialGraph#testFollowAndUnfollow"'
            }// EndExecutionStageErrortjobe
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobe 0'
          }// EndStepstjobe
        }// EndStagetjobe
        stage('tjobf IdResource: frontend user web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobf 0 http://tjobf-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobf 0 http://tjobf-nginx-thrift:8080 "TestLogin#testLogin,TestNavigation#testNavigation,TestLogin#testRegister,TestPosts#testVisibilityComposeForm"'
            }// EndExecutionStageErrortjobf
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobf 0'
          }// EndStepstjobf
        }// EndStagetjobf
} // End Parallel
} // End Stage
    stage('Stage 1') {
      failFast false
      parallel {
        stage('tjobg IdResource: post user user-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobg 1 http://tjobg-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobg 1 http://tjobg-nginx-thrift:8080 "TestApiPosts#testAPIComposePostWithMentionPopulatesUserMentions,TestApiPosts#testAPIReadUserTimeline"'
            }// EndExecutionStageErrortjobg
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobg 1'
          }// EndStepstjobg
        }// EndStagetjobg
        stage('tjobh IdResource: post social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobh 1 http://tjobh-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobh 1 http://tjobh-nginx-thrift:8080 "TestApiPosts#testAPIComposePost"'
            }// EndExecutionStageErrortjobh
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobh 1'
          }// EndStepstjobh
        }// EndStagetjobh
        stage('tjobi IdResource: social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobi 1 http://tjobi-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobi 1 http://tjobi-nginx-thrift:8080 "TestApiSocialGraph#testAPIFollowUser,TestApiSocialGraph#testAPIGetFolloweesWithToken,TestApiSocialGraph#testAPIGetFollowers,TestApiSocialGraph#testAPIUnfollowRemovesFollowee"'
            }// EndExecutionStageErrortjobi
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobi 1'
          }// EndStepstjobi
        }// EndStagetjobi
        stage('tjobj IdResource: home-timeline user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobj 1 http://tjobj-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobj 1 http://tjobj-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineReturnsArray"'
            }// EndExecutionStageErrortjobj
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobj 1'
          }// EndStepstjobj
        }// EndStagetjobj
} // End Parallel
} // End Stage
    stage('Stage 2') {
      failFast false
      parallel {
        stage('tjobk IdResource: user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobk 2 http://tjobk-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobk 2 http://tjobk-nginx-thrift:8080 "TestApiUsers#testAPILoginUser,TestApiUsers#testAPIRegisterUser,TestApiUsers#testLoginWrongPassword"'
            }// EndExecutionStageErrortjobk
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobk 2'
          }// EndStepstjobk
        }// EndStagetjobk
        stage('tjobl IdResource: user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobl 2 http://tjobl-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobl 2 http://tjobl-nginx-thrift:8080 "TestApiUsers#testAPIRegisterMissingField"'
            }// EndExecutionStageErrortjobl
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobl 2'
          }// EndStepstjobl
        }// EndStagetjobl
} // End Parallel
} // End Stage
    stage('TEARDOWN-Infrastructure') {
      steps {
          sh '$SCRIPTS_FOLDER/coilifecycles/coi-teardown.sh'
      } // EndStepsTearDownInf
    } // EndStageTearDown
} // EndStagesPipeline
post {
    always {
        archiveArtifacts artifacts: 'artifacts/*.csv', onlyIfSuccessful: true
        archiveArtifacts artifacts: 'target/testlogs/**/*.*', onlyIfSuccessful: false
        archiveArtifacts artifacts: 'target/containerlogs/**/*.*', onlyIfSuccessful: false
    }// EndAlways
} // EndPostActions
} // EndPipeline
