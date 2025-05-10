.PHONY: mvn-install telepresence-install ensure-kubernetes-context helm-dependencies build poc clean

MINIO_BUCKET_NAME=s3stream-test

mvn-install:
	@if [ -z $$(which mvn) ] ; then \
		echo "Maven not found. Installing..."; \
		brew install maven; \
	fi

telepresence-install:
	@if [ ! -z $(which telepresence) ] ; then \
		echo "Telepresence not found. Installing..."; \
		brew install telepresence; \
	fi

ensure-kubernetes-context:
	@if [ ! -z $(kubectl config current-context) ] ; then \
		echo "No Kubernetes context found. Please set up a context first."; \
		exit 1; \
	fi

# Create a marker file to track s3stream installation
s3stream/pom.xml: mvn-install
	@if [ ! -d "s3stream" ]; then \
		echo "Installing s3stream..."; \
		git clone --depth=1 git@github.com:AutoMQ/automq.git && \
		cd automq && \
		git fetch --depth=1 origin fc0cc7d6808b64ef77c910b2a0f763afad16bdac && \
		git checkout fc0cc7d6808b64ef77c910b2a0f763afad16bdac && \
		mv ./s3stream ../s3stream && \
		cd ../ && \
		rm -rf automq && \
		cd s3stream && mvn install -DskipTests=true; \
		touch pom.xml; \
	else \
		echo "s3stream directory already exists. Skipping."; \
	fi

# Define s3stream as a phony target that depends on the marker
.PHONY: s3stream
s3stream: s3stream/pom.xml

build:
	./gradlew build

poc: telepresence-install s3stream ensure-kubernetes-context
	helm install poc deployment/charts -n poc --create-namespace
	telepresence connect
	@sleep 3;
	$(MAKE) run
run:
	@echo "Setting up MinIO credentials from Kubernetes secrets..."
	@MINIO_ACCESS_KEY=$$(kubectl get secret -n poc poc-minio -o jsonpath="{.data.root-user}" | base64 --decode) && \
	MINIO_SECRET_KEY=$$(kubectl get secret -n poc poc-minio -o jsonpath="{.data.root-password}" | base64 --decode) && \
	echo "MinIO credentials retrieved successfully" && \
	echo "Running application..." && \
	echo "MINIO_ENDPOINT_URL=http://poc-minio.poc.svc.cluster.local:9000" && \
	export MINIO_ENDPOINT_URL=http://poc-minio.poc.svc.cluster.local:9000 && \
	export MINIO_SECRET_KEY=$$MINIO_SECRET_KEY && \
	export MINIO_ACCESS_KEY=$$MINIO_ACCESS_KEY && \
	export MINIO_BUCKET_NAME=$$MINIO_BUCKET_NAME && \
	./gradlew run

clean:
	telepresence quit
	helm uninstall poc -n poc
	rm -rf s3stream
	rm -rf $$HOME/.m2/repository/com/automq
	./gradlew --stop

.PHONY: env-shell

#TODO reuse this configs in run or vice-versa``
env-shell:
	@echo "Launching a shell with environment variables set..."
		@MINIO_ACCESS_KEY=$$(kubectl get secret -n poc poc-minio -o jsonpath="{.data.root-user}" | base64 --decode) && \
	MINIO_SECRET_KEY=$$(kubectl get secret -n poc poc-minio -o jsonpath="{.data.root-password}" | base64 --decode) && \
	echo "MINIO_ENDPOINT_URL=http://poc-minio.poc.svc.cluster.local:9000" && \
	echo "MINIO_BUCKET_NAME=$(MINIO_BUCKET_NAME)" && \
	echo "MINIO_ACCESS_KEY=$(MINIO_ACCESS_KEY)" && \
	echo "MINIO_SECRET_KEY=$(MINIO_SECRET_KEY)" && \
	export MINIO_ENDPOINT_URL=http://poc-minio.poc.svc.cluster.local:9000 && \
	export MINIO_SECRET_KEY=$$MINIO_SECRET_KEY && \
	export MINIO_ACCESS_KEY=$$MINIO_ACCESS_KEY && \
	export MINIO_BUCKET_NAME=$$MINIO_BUCKET_NAME && \
	zsh;