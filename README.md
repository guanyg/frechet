# ppDFDR

This repo is the implementation of our research paper, named "Achieving Privacy-Preserving Discrete Fréchet Distance Range Queries."

## Setup

This project is built upon JDK 11. The test dataset can be downloaded from [https://www.martinwerner.de/files/dataset-sample.tgz](https://www.martinwerner.de/files/dataset-sample.tgz). The detailed setup procedure can be found in `scripts/setup.sh`.

## Run

```bash
./gradlew run
```

Experimental results are printed with prefix `DATA>> `, which can be converted into multiple CSV files by `scripts/data-processing.awk` through the following script.
```bash
./gradlew run | grep "DATA>> " | ./scripts/data-processing.awk
```


Specifically, `src/data-processing.awk` will output five CSV files, namely, `enc-idx.csv`, `token-gen.csv`, `query.csv`, `token-gen-baseline.csv`, and `query-baseline.csv`, which report time values in ns. 
