//this block deletes our jobs
def folder = Jenkins.instance.getItemByFullName("rootfolder") //the root folder where all of our dynamic jobs sits under
folder.getAllItems(AbstractItem.class).each { jenkinsItem-> //iterating on every jenkins job/folder under the root folder
    if (jenkinsItem instanceof org.jenkinsci.plugins.workflow.job.WorkflowJob){ //inspecting job only
        
        def jobLatestRunStatus = jenkinsItem.getLastBuild().getResult() //getting latest build job status SUCCESS/FAILED
        
        def jobStartTimeInMillis = new Date(jenkinsItem.getLastBuild().getStartTimeInMillis()) //getting latest build start time in milliseconds
        
        //getting the number of days from today since the latest build execution
        def daysFromLastExecution = millisecondsFromEndOfJobRun / (1000*60*60*24) 
 
        //if job latest status is SUCCESS and days from latest build is more than 7 delete the jobs 
        deleteJenkinsItem(jobLatestRunStatus == Result.SUCCESS && daysFromLastExecution > 7, jenkinsItem, "job Result SUCCESS and daysFromLastExecution > 7") ||
        
        //if job latest status not SUCCESS which mean it FAILED or got ABORTED we will check if 14 days has been passed
        deleteJenkinsItem(jobLatestRunStatus != Result.SUCCESS && daysFromLastExecution > 14, jenkinsItem, "job Result FAILED/ABORTED and daysFromLastExecution > 14")
    }
 
};
 
//this block deletes our created and emptied folders
folder.getAllItems(AbstractItem.class).each { jenkinsItem-> //iterating on every jenkins job/folder under the root folder
    if (jenkinsItem instanceof com.cloudbees.hudson.plugins.folder.Folder){
        
        def numberOfJobs = jenkinsItem.getAllItems(AbstractItem.class).size() //calculating the number of jobs under a specific folder
        deleteJenkinsItem(!numberOfJobs, jenkinsItem, "folder is empty") //deleting the folder if folder is empty
    }
 
};
 
//this method check for the condition of item(folder/job) deletion and delete the item
def deleteJenkinsItem(deleteCondition, item, reason){
    if (deleteCondition){
        println("deleting item: ${item.fullName}, reason: ${reason}")
        item.delete()
        return true
    }
    else {
        println("skipping deletion: ${reason} - not satisfied!")
        return false
    }
}
