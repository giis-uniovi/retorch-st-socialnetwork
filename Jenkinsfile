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
        stage('tjoba IdResource: social-graph ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjoba 0 http://tjoba-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjoba 0 http://tjoba-nginx-thrift:8080 "TestApiSocialGraph#testAPIGetFollowersWithoutSessionUnauthorized"'
            }// EndExecutionStageErrortjoba
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjoba 0'
          }// EndStepstjoba
        }// EndStagetjoba
        stage('tjobb IdResource: home-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobb 0 http://tjobb-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobb 0 http://tjobb-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineBadRequest"'
            }// EndExecutionStageErrortjobb
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobb 0'
          }// EndStepstjobb
        }// EndStagetjobb
        stage('tjobc IdResource: home-timeline post social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobc 0 http://tjobc-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobc 0 http://tjobc-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineExcludesOwnPosts,TestApiTimeline#testAPIHomeTimelineShowsFollowedUserPost"'
            }// EndExecutionStageErrortjobc
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobc 0'
          }// EndStepstjobc
        }// EndStagetjobc
        stage('tjobd IdResource: user-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobd 0 http://tjobd-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobd 0 http://tjobd-nginx-thrift:8080 "TestApiPosts#testAPIReadUserTimelineBadRequest"'
            }// EndExecutionStageErrortjobd
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobd 0'
          }// EndStepstjobd
        }// EndStagetjobd
        stage('tjobe IdResource: frontend post user user-timeline web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobe 0 http://tjobe-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobe 0 http://tjobe-nginx-thrift:8080 "TestPosts#testComposePostAppearsInTimeline"'
            }// EndExecutionStageErrortjobe
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobe 0'
          }// EndStepstjobe
        }// EndStagetjobe
        stage('tjobf IdResource: frontend social-graph user web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobf 0 http://tjobf-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobf 0 http://tjobf-nginx-thrift:8080 "TestSocialGraph#testFollowAndUnfollow"'
            }// EndExecutionStageErrortjobf
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobf 0'
          }// EndStepstjobf
        }// EndStagetjobf
        stage('tjobg IdResource: frontend user web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobg 0 http://tjobg-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobg 0 http://tjobg-nginx-thrift:8080 "TestLogin#testLogin,TestNavigation#testNavigation,TestLogin#testRegister,TestPosts#testVisibilityComposeForm"'
            }// EndExecutionStageErrortjobg
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobg 0'
          }// EndStepstjobg
        }// EndStagetjobg
} // End Parallel
} // End Stage
    stage('Stage 1') {
      failFast false
      parallel {
        stage('tjobh IdResource: post user user-timeline ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobh 1 http://tjobh-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobh 1 http://tjobh-nginx-thrift:8080 "TestApiPosts#testAPIComposePostWithMentionPopulatesUserMentions,TestApiPosts#testAPIReadUserTimeline"'
            }// EndExecutionStageErrortjobh
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobh 1'
          }// EndStepstjobh
        }// EndStagetjobh
        stage('tjobi IdResource: post social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobi 1 http://tjobi-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobi 1 http://tjobi-nginx-thrift:8080 "TestApiPosts#testAPIComposePost"'
            }// EndExecutionStageErrortjobi
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobi 1'
          }// EndStepstjobi
        }// EndStagetjobi
        stage('tjobj IdResource: social-graph user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobj 1 http://tjobj-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobj 1 http://tjobj-nginx-thrift:8080 "TestApiSocialGraph#testAPIFollowUser,TestApiSocialGraph#testAPIGetFolloweesWithToken,TestApiSocialGraph#testAPIGetFollowers,TestApiSocialGraph#testAPIUnfollowRemovesFollowee"'
            }// EndExecutionStageErrortjobj
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobj 1'
          }// EndStepstjobj
        }// EndStagetjobj
        stage('tjobk IdResource: home-timeline user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobk 1 http://tjobk-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobk 1 http://tjobk-nginx-thrift:8080 "TestApiTimeline#testAPIHomeTimelineReturnsArray"'
            }// EndExecutionStageErrortjobk
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobk 1'
          }// EndStepstjobk
        }// EndStagetjobk
} // End Parallel
} // End Stage
    stage('Stage 2') {
      failFast false
      parallel {
        stage('tjobl IdResource: user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobl 2 http://tjobl-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobl 2 http://tjobl-nginx-thrift:8080 "TestApiUsers#testAPILoginUser,TestApiUsers#testAPILoginWrongPasswordStatus,TestApiUsers#testAPIRegisterDuplicateUsername,TestApiUsers#testAPIRegisterUser,TestApiUsers#testLoginWrongPassword"'
            }// EndExecutionStageErrortjobl
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobl 2'
          }// EndStepstjobl
        }// EndStagetjobl
        stage('tjobm IdResource: user ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobm 2 http://tjobm-nginx-thrift:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobm 2 http://tjobm-nginx-thrift:8080 "TestApiUsers#testAPIRegisterMissingField"'
            }// EndExecutionStageErrortjobm
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobm 2'
          }// EndStepstjobm
        }// EndStagetjobm
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
