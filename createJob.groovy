//creating build parameters definition for the dynamic jenkins job
@NonCPS
def getBuildParametersJobProperty(buildParameters) {
    JenkinsJobBuildParameters = []
    buildParameters.each { 
        JenkinsJobBuildParameters.add(new StringParameterDefinition(it.key, it.value))
    }
    return JenkinsJobBuildParameters
}
 
def create(folderPath, jobName, gitRepo, gitBranch, buildParameters) {
    Jenkins jenkins = Jenkins.instance // get the Jenkins instance for API call
 
    def folder = Jenkins.instance.getItemByFullName(folderPath) //the jenkins folder where the job would be created
                                                                //full job path name should be given for example - prod/monitoring
                                                                // where prod is the root folder in jenkins and monitoring is the sub folder of prod. so the jobs
                                                                // will get created under the monitoring folder
    if (folder == null) {
        throw new Exception("The folder ${folderPath} to hold the job requested does not exists")
    }
    String credentialsId = "xxxxxxxx" //cred for git repo ssh key defined in jenkins credentials plugin
    
    branches = new ArrayList<>()
    branches.add(new BranchSpec(gitBranch)) //jenkins scm plugin git repo branch
 
    UserRemoteConfig userRemoteConfig = new UserRemoteConfig(gitRepo, null, null, credentialsId) //jenkins scm configuration git repo is given 
    
    GitSCM scm = new GitSCM([userRemoteConfig], branches, false, null, null, null, [])
    
    FlowDefinition flowDefinition = (FlowDefinition) new CpsScmFlowDefinition(scm, "Jenkinsfile") //create scm job definition
 
    // Check if the job already exists
    def job = null
    job = folder.getItem(jobName)
 
    if (job == null) {
        println "job ${jobName} does not exists creating new one"
        job = folder.createProject(WorkflowJob, jobName)
    }
    else {
        println "job ${jobName} exists overriding it"
    }
 
    // Add/override the job definition
    job.setDefinition(flowDefinition)
 
    //create job build parameters based on the parameters passed from the orchestrator job
    JenkinsJobBuildParameters = getBuildParametersJobProperty(buildParameters) // collecte all build parameters
    def BuildParametersJobProperty = new ParametersDefinitionProperty(JenkinsJobBuildParameters)
    job.addProperty(BuildParametersJobProperty)
    
    // actually creating the jenkins job 
    job.save() 
 
    //the following steps is for triggering the job
    def currentWorkflowJob = Jenkins.instance.getItemByFullName(JOB_NAME) //get the jenkins job object
 
    //each job in jenkins has a cause which mean the one that trigger it, so in our case the orchestrator job which actually 
    //running the following code is the cause for the the job that is about to be triggered so we collecting the job number of it
    //and creating a cause action which is injected to the new job execution
    def currentWorkflowRun = currentWorkflowJob.getBuildByNumber(Integer.parseInt(BUILD_NUMBER)) 
    def upstreamCauseJobAction = new CauseAction(new Cause.UpstreamCause(currentWorkflowRun))
    
    //Running the job
    job.scheduleBuild2(0, upstreamCauseJobAction)
 
    //printing the job url which got created
    print "Job URL: " + jenkins.getRootUrl()+job.getUrl()
}
 
