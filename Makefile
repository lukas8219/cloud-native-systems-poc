mvn-install:
	@if [ ! -z $(which mvn) ] ; then \
		echo "Maven not found. Installing..."; \
		brew install maven; \
	fi

install-deps: s3stream mvn-install
	git clone --depth=1 git@github.com:AutoMQ/automq.git
	cd automq
	git fetch --depth=1 origin fc0cc7d6808b64ef77c910b2a0f763afad16bdac
	git checkout fc0cc7d6808b64ef77c910b2a0f763afad16bdac
	mv ./s3stream ../s3stream
	mvn install
	cd ../
	rm -rf automq

build:
	./gradlew build

poc:
	docker-compose up -d
	@sleep 3;
	./gradlew run

.PHONY: mvn-install install-deps