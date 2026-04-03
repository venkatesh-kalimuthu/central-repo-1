KBS= 
gcloud auth application-default login

gcloud auth login --impersonate-service-account sa-developer@ford-7530d00819fb0f1db9a43327.iam.gserviceaccount.com

gcloud auth application-default login --impersonate-service-account sa-developer@ford-7530d00819fb0f1db9a43327.iam.gserviceaccount.com

gcloud config set project ford-7530d00819fb0f1db9a43327



Token generation - cloud run

gcloud auth print-identity-token --impersonate-service-account=sa-developer@ford-7530d00819fb0f1db9a43327.iam.gserviceaccount.com --audiences="https://knowledgebaseservice-api-dev-958034508202.us-central1.run.app"


