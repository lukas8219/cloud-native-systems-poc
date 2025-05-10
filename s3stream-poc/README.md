# AutoMQ S3Stream POC - S3 Backed Storage
This just pet-project to understand the low level concepts of implementing an ObjectStorage-backed engine.

Based on the calculations and default configurations for AutoMQ, S3Stream writes the WAL log file to S3 at each 256ms.
On a month, this is 10,368,000 PUT requests - total of 50 USD bucket (API call pricing).

This gives this implementation of S3Stream - a capability of Infinity Storage on a fair price.

## Requirements:
- kubectl
- helm
- telepresence
- make
- java
- maven
- gradle
- Running kubenetes cluster (`poc` namespace is going to be used) (Kind, K3d, Minikube, etc.)

## How to run the POC:
```bash
make poc
```

## How to clean up the POC:
```bash
make clean
```
