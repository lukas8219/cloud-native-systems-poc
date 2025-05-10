# AutoMQ S3Stream POC - S3 Backed Storage
This just pet-project to understand the low level concepts of implementing an ObjectStorage-backed engine.

Based on the calculations and default configurations for AutoMQ, S3Stream writes the WAL log file to S3 at each 256ms.
On a month, this is 10,368,000 PUT requests - total of 50 USD bucket (API call pricing).

This gives this implementation of S3Stream - a capability of Infinity Storage on a fair price.

## How to run the POC:
```bash
make
make poc
```