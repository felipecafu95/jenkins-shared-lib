def call() {
 node {
     environment {
        REGION = "sa-east-1"
        OWNER = "Prevent"
        TAG_GROUP = 'App'
     }
  stage('Checkout') {
   checkout scm
  }
  def values = terraformAwsEc2()
  if (values.terraformVersion <= 0.10 ) {
   stage('Version') {
    dir(values.ec2Module) {
       echo "Version unsupported!"
       sh values.versionUnsupported
    }
   }
  }
  if (params.ENVIRONMENT == 'PROD' ) {
   stage('Init') {
    dir(values.ec2Module) {
       sh "terraform init -backend-config='${values.s3Bucket}' -backend-config='key=application/${params.NOME}-${params.TAG}/terraform.tfstate' -backend-config='${values.awsRegion}'"
   }
  } 
  } else {
    dir(values.ec2Module) {
       sh "terraform init -backend-config='${values.s3BucketDevHom}' -backend-config='key=application/${params.NOME}-${params.TAG}/terraform.tfstate' -backend-config='${values.awsRegionDevHom}'"
    }      
   }  
  stage('Plan') {
      dir(values.ec2Module) {
    env.TF_VAR_environment = "${params.ENVIRONMENT}"
     env.TF_VAR_vpc_id = "${params.VPC}"
     env.TF_VAR_tag_description = "${params.TIPO}"
     env.TF_VAR_region = "${REGION}"
     env.TF_VAR_owner = "${OWNER}"
     env.TF_VAR_count_number = "${params.NUMBER}"
     env.TF_VAR_instance_environment = "${params.TAG}"
     env.TF_VAR_instance_name = "${params.NAME}"
     env.TF_VAR_tag_group = "${params.NAME}-${params.TAG}"
     if ("${params.MEM}" == "2GB" && "${params.ENVIRONMENT}" == "PRD") {
      echo "Nao utilizar familia t2 em prod!" 
      sh "sleep 15 && exit 1"
     } else if ("${params.MEM}" == "2GB" && "${params.ENVIRONMENT}" == "DEVOHOM") {
      env.TF_VAR_backup_option = "nao"
      env.TF_VAR_instance_type = "t3.small"
      env.TF_VAR_tag_group = "${TAG_GROUP}-${params.TAG}"
      sh "terraform plan -target='module.ec2.aws_instance.generic_ec2' -out=${params.NAME}-${params.TAG}.tfplan"
     } else if ("${params.MEM}" == "4GB" && "${params.ENVIRONMENT}" == "DEVOHOM") {
      env.TF_VAR_backup_option = "nao"
      env.TF_VAR_instance_type = "c5.large"
      env.TF_VAR_tag_group = "${TAG_GROUP}-${params.TAG}"
      sh "terraform plan -target='module.ec2.aws_instance.generic_ec2' -out=${params.NAME}-${params.TAG}.tfplan"
     } else if ("${params.MEM}" == "4GB" && "${params.ENVIRONMENT}" == "PRD") {
      env.TF_VAR_backup_option = "sim"
      env.TF_VAR_instance_type = "c5.large"
      env.TF_VAR_tag_group = "${TAG_GROUP}"
      env.TF_VAR_alarm_name = "${params.NAME}-${params.TAG}-down-recovering"
      sh "terraform plan -target='module.ec2.aws_instance.generic_ec2' -target='module.ec2.aws_cloudwatch_metric_alarm.ec2_autorecover' -out=${params.NAME}-${params.TAG}.tfplan"
     } else if ("${params.MEM}" == "8GB" && "${params.ENVIRONMENT}" == "DEVOHOM") {
      env.TF_VAR_backup_option = "nao"
      env.TF_VAR_instance_type = "m5.large"
      env.TF_VAR_tag_group = "${TAG_GROUP}-${params.TAG}"
      sh "terraform plan -out=${params.NAME}-${params.TAG}.tfplan"
     } else if ("${params.MEM}" == "8GB"  && "${params.AMBIENTE}" == "PRD") {
      env.TF_VAR_backup_option = "sim" 
      env.TF_VAR_instance_type = "m5.large"
      env.TF_VAR_tag_group = "${TAG_GROUP}"
      env.TF_VAR_alarm_name = "${params.NAME}-${params.TAG}-down-recovering"  
      sh "terraform plan -target='module.ec2.aws_instance.generic_ec2' -target='module.ec2.aws_cloudwatch_metric_alarm.ec2_autorecover' -out=${params.NAME}-${params.TAG}.tfplan"
      }
   }
  }
 }
}