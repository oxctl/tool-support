# Templates README - App Templates - Tool Support 

- [Templates README - App Templates - Tool Support](#templates-readme---app-templates---tool-support)
  - [Prerequisites for using the templates in this directory](#prerequisites-for-using-the-templates-in-this-directory)
  - [Running the templates](#running-the-templates)
  - [0.global.yaml (AWS Region: eu-west-1)](#0globalyaml-aws-region-eu-west-1)
    - [Function](#function)
    - [Resources Created](#resources-created)
    - [Cloudformation Inputs / Parameters](#cloudformation-inputs--parameters)
    - [Cloudformation Outputs / Exports](#cloudformation-outputs--exports)
    - [Notes](#notes)
  - [1.region.yaml (AWS Regions: eu-west-1 \& eu-central-1)](#1regionyaml-aws-regions-eu-west-1--eu-central-1)
    - [Function](#function-1)
    - [Resources Created](#resources-created-1)
    - [CloudFormation outputs / exports](#cloudformation-outputs--exports-1)
  - [3.s3-and-cloudfront.yaml (AWS Region: eu-west-1)](#3s3-and-cloudfrontyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created)
    - [CloudFormation Parameters](#cloudformation-parameters)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-2)
    - [Notes](#notes-1)
  - [3.rds.yaml (AWS Region: eu-west-1)](#3rdsyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created-1)
    - [CloudFormation Parameters](#cloudformation-parameters-1)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-3)
    - [Notes](#notes-2)
  - [4.eb-env.yaml (AWS Region: eu-west-1)](#4eb-envyaml-aws-region-eu-west-1)
    - [Function / Resources Created](#function--resources-created-2)
    - [CloudFormation Parameters](#cloudformation-parameters-2)
    - [CloudFormation Outputs / Exports](#cloudformation-outputs--exports-4)
    - [SSH into the ec2 instance of the EB env](#ssh-into-the-ec2-instance-of-the-eb-env)


## Prerequisites for using the templates in this directory

Before running any templates in this app
 
* Ensure the templates in the following folders have been run to provision resources the templates are dependent on:
  * `..\..\0.cad-account-global`
  * `..\..\0.cad-account-region` - for the region in which we wish to run the app templates
* The prefixed numbers in the template filenames indicate the sequential order of running them.  It is important to maintain this other because of dependencies between templates. 
* Where 2 templates have the same sequence prefix, there are no dependencies and as such they can be run in any order

## Running the templates

For a nested stack to work the templates need to be in S3 this is because the template URI must be a S3 URL. The package step uploads templates to S3 and then re-writes the template to point to the S3 URL.

```bash
aws cloudformation package --template-file stack.yaml --s3-bucket oxcanvas-nonprod-shared --s3-prefix cloudformation-nonprod/tool-support --output-template-file build/stack.yaml 
```

The stack can then be deployed using the built template:

```bash
aws cloudformation deploy --template-file ./build/stack.yaml --stack-name cad-apps-tool-support --capabilities CAPABILITY_NAMED_IAM --parameter-overrides file://aws/730335587339.json
```

If things didn't go well then you can remove check the logs with:

```bash
aws cloudformation describe-stack-events --stack-name cad-apps-tool-support --output table
```

Then delete things with:

```bash
aws cloudformation delete-stack --stack-name cad-apps-tool-support
aws cloudformation wait stack-delete-complete --stack-name cad-apps-tool-support
```

## 0.global.yaml (AWS Region: eu-west-1)

`ToDo: Matthew needs to update this template to reflect the changes he made in renaming some templats and moving some resources around.`

### Function

  * Create AWS resources required once for tool-support in all regions.

### Resources Created

* SES & Secrets manager policies
* EB instance profiles
* route53 subdomain for app: 
  * `${appName}.apps.canvas.ox.ac.uk` OR `${appName}.apps-nonprod.canvas.ox.ac.uk`
  * Along wth automatic Route53 NS records for the app domain

### Cloudformation Inputs / Parameters

* Accept the defaults for tool-support

### Cloudformation Outputs / Exports

* Route53 domainname / zoneid / nameservers .. for the app domain
* EB instance profile details

### Notes

* As an extra check, consider using nslookup or dig to ensure the ns records for the subdomain are resolvable, before running the next templates
* In future, add other resources that is required globally for this app in all regions


## 1.region.yaml (AWS Regions: eu-west-1 & eu-central-1)

### Function

* Create other AWS resources required once for each region in which we intend to deploy tool-support
* Run in DR region only if we want to test or deploy tool-support in that region

### Resources Created

* Will setup SES bounce handling for apps that require it
* EB application for the app.

### CloudFormation outputs / exports

* EB app name

## 3.s3-and-cloudfront.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* a beta or prod buckett
* Creates a CloudFront distribution for the prod or dev bucket
  * along with the other resources required by CloudFront
  * Enables CloudFront logging to a prefix of the shared bucket
* Creates a route53 alias record pointing to the CloudFront distribution
  * It's able to do this, by importing the route53 ZoneId for the app subdomain exported by an earlier template

### CloudFormation Parameters

* `cfSSLCertARN` - the ARN of the SSL certificate for cloudfront
  * issued in one of the account gloal templates
  * copy that from the exports of the stack built with that template (in Region `us-east-1`)  - `Physical ID` column
  
### CloudFormation Outputs / Exports

  * `cfOID` .. the Origin Access Id for the cloudfront distribution .. may be useful later
  * `cfDistributionID` .. the ID of the cloudfront distribution 

### Notes

* After the stack builds, one can visit the static bucket at, eg:
  * https://static.tool-support.apps-nonprod.canvas.ox.ac.uk
  * https://static.tool-support.apps.canvas.ox.ac.uk
  * https://static.manage-courses.apps-nonprod.canvas.ox.ac.uk
* if there is an index.html file there it will be displayed as a webpage

## 3.rds.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* Creates an RDS instance for app, along with:
  * a DB parameter group
  * A secrets manager record for the auto-generated DB password, and a link between the RDS DB and the secrets manager entry
* Cloudwatch alarms for onitoring the RDS instance
* SNS resources for reporting on the monitoring the RDS instance 
* creates lambda functions:
  * to create a readonly user in the RDS instance for the app
  * to rotate the RDS superuser (`admin`) password monthly
  * conditionally (if run on prod envType) creates a dbdump function to dump prod RDS instance and load the dump in beta RDS instance
  * conditionally (if run on prod envType) creates a weekly schedule for dumping prod RDS instance and loading the dump in beta RDS instance
  * conditionally (if run on beta envType) creates a dbload function invoked from dump in prod AWS account

### CloudFormation Parameters

* set `snapshotToUse` if restoring this instance from a snapshot .. not necessarily a good idea since snapshots bring elements of the RDS instance from where they were taken
* set `roUserName` if you want a non-default username for the readonly RDS user
  * Note: you can also create additional readonly users, by changing this parameter and scheduling a new function run.
* set `rouserSchedule` to 5-10 minutes in future from when stack is deployed for the first time - **IMPORTANT**
* set `dbInstanceSize` to use a larger instance size
* Accept defaults

### CloudFormation Outputs / Exports

  * `dbSecretProdRegion` .. the secrets manager secret created for the RDS instance
  * The RDS DB's endpoint address and instance ARN

### Notes

* Because of the way dbdump/dbload are setup, if we want to add additional envs to be loaded from prod, we will need to duplicate a few resources and rename them as apt:
  * `dbdumpFunctionBeta`
  * `scheduleDBDumpToBeta`
  * `dbloadFunctionBeta`
* See also the general [Lambda Functions & Sources README](https://github.com/oxctl/aws-shared/blob/main/gitsync-deploy/2.aws-account-region/src/README.md)

## 4.eb-env.yaml (AWS Region: eu-west-1)

### Function / Resources Created

* Creates an REB env, along with:
  * an application version resource
  * various EB env configuration templates
* A cloudfront distribution for fronting the EB env
  * Traffic to the EB env is restricted to only traffic originating from cloudfront
* A Route53 alias record for the cloudfront distribution, eg:
  * `appname.apps.canvas.ox.ac.uk`
  * `appname.apps-nonprod.canvas.ox.ac.uk`
* SNS resources for monitoring the EB env
* Event rule resources for monitoring the EB env

### CloudFormation Parameters

* Accept defaults
* *NOTE:*
  * To avoid putting back the platform's solution stack for the EB, env, we have set it up as a mapping
  * This means that we have to look uo the latest solution if this is a new environment, just to be sure we are using the latest.
  * After that, in further updates, we would not need to bother with this parameter
  * To look up the solution stack to use, run the following command (once per region of interest .. note, the region is mentioned twice in the command (once as an ARN)
  * In the output of the command, an AMI ID is also listed, feed that into the apt field in the mapping too)
  * ```bash
      aws elasticbeanstalk describe-platform-version --region eu-west-1 \
        --platform-arn "arn:aws:elasticbeanstalk:eu-west-1::platform/Docker running on 64bit Amazon Linux 2023/4.3.0" \
        --query "{AMI: PlatformDescription.CustomAmiList[1].ImageId,solStack: PlatformDescription.SolutionStackName}" \
        --out=table
              ----------------------------------------------------------------------------
              |                          DescribePlatformVersion                         |
              +------------------------+-------------------------------------------------+
              |           AMI          |                    solStack                     |
              +------------------------+-------------------------------------------------+
              |  ami-087e2ccdd3b0de771 |  64bit Amazon Linux 2023 v4.3.0 running Docker  |
              +------------------------+-------------------------------------------------+
      ```

### CloudFormation Outputs / Exports

  * various exports, not imported by any other template

###  SSH into the ec2 instance of the EB env

An example of connecting to the ec2 instance of the EB env

```bash
appName="tool-support"
envType=beta
region="eu-west-1"

#lookup instance-id
instanceId=$(aws --region ${regIon} ec2 describe-instances --filters "Name=tag:Name,Values=${appName}-${envType}" --query "Reservations[*].Instances[*].[InstanceId]")

# ssh only session
aws ssm start-session \
    --region $region \
    --target $instanceId \
```
