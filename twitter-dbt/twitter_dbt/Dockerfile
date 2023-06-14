FROM ghcr.io/dbt-labs/dbt-postgres:1.3.0

WORKDIR /usr/my-project
COPY . .
RUN dbt deps

# TODO obviously this kind of thing needs to be parameterized in future.
ENTRYPOINT ["dbt"]