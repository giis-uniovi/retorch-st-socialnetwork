pipeline {
  agent {label 'xretorch-agent'}
  environment {
    TJOB_NAME        = "tjob0"
    SELENOID_PRESENT = "TRUE"
    SUT_URL          = "http://tjob0-nginx-thrift:8080"
    frontend_port    = "8080"
  }
  options {
    disableConcurrentBuilds()
  }
  stages {
    stage('Clean Workspace') {
      steps {
        cleanWs()
      }
    }
    stage('Clone Project') {
      steps {
        checkout scm
      }
    }
    stage('Deploy SUT') {
      steps {
        sh 'docker network create jenkins_network 2>/dev/null || true'
        sh 'docker compose -p $TJOB_NAME up -d --build'
        sh '''
          echo "Waiting for SUT at $SUT_URL..."
          TIMEOUT=300; ELAPSED=0
          until curl -sf "$SUT_URL" | grep -q "Death Star"; do
            sleep 5; ELAPSED=$((ELAPSED + 5))
            echo "  ...${ELAPSED}s / ${TIMEOUT}s"
            if [ $ELAPSED -ge $TIMEOUT ]; then
              echo "SUT did not become ready"
              docker compose -p $TJOB_NAME logs --tail 50
              exit 1
            fi
          done
          echo "SUT ready"
        '''
      }
    }
    stage('Run Tests') {
      steps {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
          sh 'echo "HERE TEST EXEC"'
        }
      }
    }
    stage('Teardown SUT') {
      steps {
        sh 'docker compose -p $TJOB_NAME down --volumes'
      }
    }
  }
  }
}
