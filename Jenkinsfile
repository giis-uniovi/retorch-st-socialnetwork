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
        stage('tjoba IdResource: frontend gateway owner pet web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjoba 0 http://tjoba-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjoba 0 http://tjoba-api-gateway:8080 "TestPets#addPetToOwner"'
            }// EndExecutionStageErrortjoba
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjoba 0'
          }// EndStepstjoba
        }// EndStagetjoba
        stage('tjobb IdResource: frontend gateway owner pet visit web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobb 0 http://tjobb-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobb 0 http://tjobb-api-gateway:8080 "TestVisits#addVisitToPet"'
            }// EndExecutionStageErrortjobb
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobb 0'
          }// EndStepstjobb
        }// EndStagetjobb
        stage('tjobc IdResource: frontend gateway owner web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobc 0 http://tjobc-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobc 0 http://tjobc-api-gateway:8080 "TestOwners#editPetOwner,TestOwners#searchPetOwner"'
            }// EndExecutionStageErrortjobc
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobc 0'
          }// EndStepstjobc
        }// EndStagetjobc
        stage('tjobd IdResource: frontend gateway vet web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobd 0 http://tjobd-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobd 0 http://tjobd-api-gateway:8080 "TestVets#listVeterinaries"'
            }// EndExecutionStageErrortjobd
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobd 0'
          }// EndStepstjobd
        }// EndStagetjobd
        stage('tjobe IdResource: gateway owner ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobe 0 http://tjobe-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobe 0 http://tjobe-api-gateway:8080 "TestApiOwners#testAPICreateOwner,TestApiGateway#testAPIGatewayOwnerFields,TestApiOwners#testAPIUpdateOwner"'
            }// EndExecutionStageErrortjobe
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobe 0'
          }// EndStepstjobe
        }// EndStagetjobe
      } // End Parallel
    } // End Stage
    stage('Stage 1') {
      failFast false
      parallel {
        stage('tjobf IdResource: frontend owner web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobf 1 http://tjobf-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobf 1 http://tjobf-api-gateway:8080 "TestOwners#createNewPetOwner"'
            }// EndExecutionStageErrortjobf
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobf 1'
          }// EndStepstjobf
        }// EndStagetjobf
        stage('tjobg IdResource: frontend gateway web-browser ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobg 1 http://tjobg-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobg 1 http://tjobg-api-gateway:8080 "TestNavigation#navigationFrontend"'
            }// EndExecutionStageErrortjobg
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobg 1'
          }// EndStepstjobg
        }// EndStagetjobg
        stage('tjobh IdResource: gateway owner pet ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobh 1 http://tjobh-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobh 1 http://tjobh-api-gateway:8080 "TestApiPets#testAPICreatePet,TestApiPets#testAPIUpdatePet"'
            }// EndExecutionStageErrortjobh
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobh 1'
          }// EndStepstjobh
        }// EndStagetjobh
        stage('tjobi IdResource: gateway owner pet visit ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobi 1 http://tjobi-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobi 1 http://tjobi-api-gateway:8080 "TestApiVisits#testAPICreateVisit,TestApiGateway#testAPIGatewayOwnerHasPetsWithVisits,TestApiVisits#testAPIGetVisitsForMultiplePets,TestApiVisits#testAPIVisitListStructure"'
            }// EndExecutionStageErrortjobi
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobi 1'
          }// EndStepstjobi
        }// EndStagetjobi
      } // End Parallel
    } // End Stage
    stage('Stage 2') {
      failFast false
      parallel {
        stage('tjobj IdResource: gateway pet ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobj 2 http://tjobj-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobj 2 http://tjobj-api-gateway:8080 "TestApiPets#testAPIGetPetTypes"'
            }// EndExecutionStageErrortjobj
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobj 2'
          }// EndStepstjobj
        }// EndStagetjobj
        stage('tjobk IdResource: gateway vet ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobk 2 http://tjobk-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobk 2 http://tjobk-api-gateway:8080 "TestApiVets#testAPIGetVetList"'
            }// EndExecutionStageErrortjobk
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobk 2'
          }// EndStepstjobk
        }// EndStagetjobk
        stage('tjobl IdResource: gateway owner ') {
          steps {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-setup.sh tjobl 2 http://tjobl-api-gateway:8080'
              sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-testexecution.sh tjobl 2 http://tjobl-api-gateway:8080 "TestApiOwners#testAPIListOwners"'
            }// EndExecutionStageErrortjobl
            sh '$SCRIPTS_FOLDER/tjoblifecycles/tjob-teardown.sh tjobl 2'
          }// EndStepstjobl
        }// EndStagetjobl
      } // End Parallel
    } // End Stage
stage('TEARDOWN-Infrastructure') {
      failFast false
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
