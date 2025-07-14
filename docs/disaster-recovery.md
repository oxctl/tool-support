# Disaster Recovery

If something has gone wrong, and we need to recover the service from scratch then this document outlines the steps. 

This process will only ever be followed on the **production AWS account** in the chosen DR region. 

As indicated below, the DNS change (and subsequent smoke test of the tool) should (obviously) only be carried out when following the process 'for real'.

## Overview

The whole application is deployed through CloudFormation and this is launched from GitHub Actions. To test
or recover the service for real, create a specially named branch, enable builds for that branch and push it to GitHub. This should cause GitHub Actions to build the application and deploy it to AWS in the DR region using CloudFormation.

## Requirements

You must be able to login to, and / or have the relevant access to
  - the production Canvas AWS account
  - the DR region (in EMEA the DR region should be `eu-central-1`)
  - the `tool-support` Github repository
  - the secrets required by the application
  - the institutional DNS update interface :warning: only for real disasters!

Check that:
  - the low-level infrastructure, for example, VPC,  has been deployed to the AWS region where recovery is to happen
  - GitHub Actions is able to connect to the AWS account (should work if low-level infrastructure is present)

## Steps

### Deploy the regional stacks

Create the regional stacks, see: https://github.com/oxctl/aws-shared/blob/main/docs/disaster_recovery.md


### Deploy the application


The following can be undertaken via the Github web UI or on your local desktop assuming you have cloned the repository and ensured it is up to date:

1. Create a new branch named with `dr-` at the start, `dr-tool-support`. Switch to that branch. :warning: **if the branch does not start with `dr-` then the DR process will not work**
2. Situate yourself at the point you wish to deploy the repository; this will probably be the latest commit (the 'master' branch) but can be earlier in the repository history.
  
3. Login to AWS production account and switch to the DR region 
4. Find the RDS backup you want to restore in [AWS DR Backup Vault](https://eu-central-1.console.aws.amazon.com/backup/home?region=eu-central-1#/backupvaults/details/dr), this is usually the most recent. Click on the `Recovery point ID` (hyperlink), copy the *ARN* then, **after checking you are on the `dr-tool-support` branch in Github**, edit `tool-support/aws/dr.json` and set the value of 'snapshotToUse' to be the *ARN*, commit changes and push the new branch:
    ```json
    {
      .....
      "snapshotToUse=<*ARN*>"
    }
    ```
   You shouldn't need to make any other changes to the file, but in the unlikely event that you do, note:
     the `appName` should be unique - if it uses the same name as an existing app it will overwrite it,
     it should only use lowercase alphanumeric characters and hyphens,
     it should have a maximum of 20 characters, and,
     the first character must be a letter, and it cannot end with a hyphen or contain two consecutive hyphens.
5. Once any changes have been comitted and pushed to the remote branch, GitHub Actions will start the build automatically (provided the branch name starts with `dr-`). You can check progress https://github.com/oxctl/tool-support/actions
6. :warning: Take care with this step :warning:. The Cloudformation deployment process will wait for an hour before tearing itself down but the application will not start up until the secrets are in place. :warning: If you are conducting a DR test then it is important that any URLs, secrets, or other credentials are modified (so data isnt written to a production system). A good approach is to monitor the creation of the Elastic Beanstalk 'configuration' secrets in AWS Secrets Manager `${appName}/${envType}/eb-env/config` (e.g., `tool-support-dr/prod/eb-env/config`), and, as soon as it appears (try after 30 mins),  populate with the secrets JSON which is located within 1-Password. So long as the secrets are in place, the application will attempt to start up when it is ready.
8. Check Spring Boot Actuator (https://tool-support-dr.apps.canvas.ox.ac.uk/actuator/health) - everything should be "UP". If there is a problem then it is worth checking that:
    Elastic Beanstalk has started correctly
    the RDS database RDS looks OK
    the application logs do not indicate a problem
9. :warning: :warning: The FOLLOWING STEPS ARE NOT TO BE DONE WHILST TESTING: 
10. Update the CNAME(s) in DNS (remove `lti`, `proxy` and `tools`)
11. Remove (or set to "false")  `"drTest=true"` from `aws/dr.json` 
12. Check the tool is working in Canvas (Tool Support will need to be in place)


   
### Tear things down

If something didn't work correctly, and you wish to start again there is a GitHub action called `Delete Stack` that will attempt to clean-up everything associated with a deployment. 


#### Tear down application

1. in AWS, remove delete protection on the RDS DB (the prodution DB will always have this set)

   
In Github, run the `Delete Stack` Action. In the dialogue box that appears:

1. the workflow resides on the `master` branch
1. the names of the stacks to delete are derived from 'appName' (which in this case is `tool-support-dr` (defined in [tool-support dr.json](../aws/dr.json))). The Stack 'prefix' can be found in 'Cloudformation > Stacks' in the DR region - it is everything up to, but not including, the final hyphen: `tool-support-dr-prod-prod`. NB, it is sometimes necessary to use a contracted version of `appName` due to limitation on the number of characters allowed.
1. the account is the ID of the production account and 
1. in EMEA, the DR region is `eu-central-1`

Before removing all the CloudFormation stacks it will empty the created S3 buckets (if there are any) so that the CloudFormation stacks can be successfully deleted (CF will refuse to delete a non-empty S3 bucket).

If the `Delete Stack` Action fails then the stack can be 'force deleted' via the AWS UI.

#### Tear down regional stacks

Remove the regional stacks, see: https://github.com/oxctl/aws-shared/blob/main/docs/disaster_recovery.md

#### Tidy up Github

Once the DR process has been completed, delete the branch, e.g., `dr-tool-support` in Github.
