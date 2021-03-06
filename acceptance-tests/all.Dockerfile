FROM nsjail:latest

RUN apt-get -y update && apt-get install -y \
    maven \
    && rm -rf /var/lib/apt/lists/*

COPY acceptance-tests/runall.sh /
RUN chmod +x /runall.sh

ENTRYPOINT ["/bin/bash", "/runall.sh"]