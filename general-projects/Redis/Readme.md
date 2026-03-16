Below are the steps to connect to the Redis Cache hosted in the ZT environment:


1. Authenticate with Google Cloud Run the following command to authenticate your application:

gcloud auth application-default login


2. Create a secure tunnel to the Redis Cache instance Use the command below, replacing the IP address with your actual Redis hostname and port (Refer the app value yaml file)
gcloud compute ssh --zone "us-central1-a" "lmcc-gce-vm-fcsrezt-demo-qa-aa6acd" --project "lord-8102d88937e90a6e521a8971" --tunnel-through-iap -- -N -L 6000:100.65.1.92:6378

 

3. Confirm tunnel connection is active

 

4. Download Redis Desktop Manager


Visit the following link to download and install version 1.6.1 exe of Another Redis Desktop Manager:- Release v1.6.1 · qishibo/AnotherRedisDesktopManager 
https://github.com/qishibo/AnotherRedisDesktopManager/releases/tag/v1.6.1

“Another-Redis-Desktop-Manager.1.6.1.exe”  - https://github.com/qishibo/AnotherRedisDesktopManager/releases/download/v1.6.1/Another-Redis-Desktop-Manager.1.6.1.exe

 

5. Configure Redis Desktop Manager Open the application and create a new connection with the following settings:

Host: localhost
Port: 6000
Password: Refer to the Secret Manager of your ZT project
Enable SSL
Authority: Refer to the Secret Manager for the certificate (.crt) of your ZT project
