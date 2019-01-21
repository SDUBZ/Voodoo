pipeline {
    agent any
    // environment {
    //     GRADLE_OPTS = '-Dkotlin.compiler.execution.strategy="in-process"'
    // }
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
	        }
	    }
		stage("test") {
			steps {
                sh './gradlew clean'
				sh './gradlew test -S'
			}
		}
		stage("voodoo") {
			steps {
				sh './gradlew :voodoo:clean'
				sh './gradlew :voodoo:shadowJar -S'
				archiveArtifacts artifacts:  'build/libs/*jar'
			}
		}
	    stage("multimc-installer") {
	        steps {
	            sh './gradlew :multimc:multimc-installer:clean'
	            sh './gradlew :multimc:multimc-installer:shadowJar -S'
	            archiveArtifacts artifacts:  'multimc/installer/build/libs/*jar'
	        }
	    }
	    stage("server-installer") {
	        steps {
	            sh './gradlew :server-installer:clean'
	            sh './gradlew :server-installer:shadowJar -S'
	            archiveArtifacts artifacts:  'server-installer/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            sh './gradlew :bootstrap:shadowJar -Ptarget=voodoo -S'
	            sh './gradlew :bootstrap:shadowJar -Ptarget=multimc-installer -S'
	            archiveArtifacts artifacts:  'bootstrap/build/libs/*voodoo*'
	            archiveArtifacts artifacts:  'bootstrap/build/libs/*multimc-installer*'
	        }
	    }
	    stage('publish') {
            steps {
                sh './gradlew publish -S'
            }
        }
		stage('counter') {
			steps {
				sh './gradlew buildnumberIncrease'
			}
		}
	}
}