terraform {
  backend "s3" {
    bucket         = "parkhere-terraform-state-bucket"
    key            = "terraform/state.tfstate"
    region         = "eu-central-1"
    dynamodb_table = "terraform-lock-table"
  }
}