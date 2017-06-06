//test script with defined methods of git,,,,]
//phewww.................


node('master') {

stage('checkout')
  checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false,
  extensions: [], submoduleCfg: [],
  userRemoteConfigs: [[credentialsId: 'b27f7cb2-efa8-496a-90d8-825b9332bf44', url: 'git@github.com:RocketScienceProjects/objectsRepo.git']]])


  gitCheckout("$CurrentReleaseBranch")
  gitCheckout("master")
  gitMerge("$CurrentReleaseBranch")
  gitPush("master")
  gitCheckout("$NextReleaseBranch")
  gitMerge("$CurrentReleaseBranch")
  gitPush("$NextReleaseBranch")

}


def gitCheckout(branch) {
  println "Checking out $branch"
  sh(script: 'git checkout $branch', returnStdout: true)
  sh(script: 'git branch', returnStdout: true)
}

def gitMerge(frombranch) {
  sh(script: "git merge $frombranch", returnStdout: true)
}

def gitPush(remotebranch){
  sh(script: "git push origin $remotebranch", returnStdout: true)
}
