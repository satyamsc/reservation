provider "aws" {
  region = "us-west-2"
}

resource "aws_lambda_function" "example_lambda" {
  filename         = "target/your_lambda_function.zip" # Path to your zip file
  function_name    = "example_lambda_function"
  role             = "arn:aws:iam::YOUR_ACCOUNT_ID:role/abc" # Replace YOUR_ACCOUNT_ID with your AWS account ID
  handler          = "com.example.Handler::handleRequest" # Replace with your handler
  runtime          = "java17"
  source_code_hash = filebase64sha256("target/your_lambda_function.zip")

  environment {
    variables = {
      EXAMPLE_ENV_VAR = "example_value"
    }
  }
}

resource "aws_iam_role_policy_attachment" "attach_lambda_policy" {
  role       = "abc"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}