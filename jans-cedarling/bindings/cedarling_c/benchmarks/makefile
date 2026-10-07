# Makefile for Cedarling C benchmarks
#
# Link against ../../../target/release/libcedarling_c.so — run
#   cargo build --release -p cedarling_c
# from the jans-cedarling workspace root first so the binary matches current sources.

CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2
LDFLAGS = -L../../../target/release -Wl,-rpath=../../../target/release -lcedarling_c -lm
INCLUDES = -I../target/include

PYTHON3 := $(shell command -v python3 2>/dev/null)
ifeq ($(PYTHON3),)
$(error python3 is required to generate scenarios_generated.h. Install python3 (e.g. `apt install python3` or `brew install python3`) and re-run make.)
endif

# Detect OS for library linking
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -ldl -lpthread
endif
ifeq ($(UNAME_S),Darwin)
    LDFLAGS += -lpthread
endif

.PHONY: all clean run gen-scenarios

all: benchmark_cedarling

# scenarios_generated.h is produced from bindings/benchmarks/fixtures/scenarios.json
# by gen_scenarios.py. C has no built-in JSON parser, so the bench consumes
# pre-formatted string literals instead of parsing the manifest at runtime.
gen-scenarios: scenarios_generated.h

scenarios_generated.h: ../../benchmarks/fixtures/scenarios.json gen_scenarios.py
	$(PYTHON3) gen_scenarios.py

benchmark_cedarling: benchmark_cedarling.c scenarios_generated.h
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ benchmark_cedarling.c $(LDFLAGS)

run: benchmark_cedarling
	LD_LIBRARY_PATH=../../../target/release DYLD_LIBRARY_PATH=../../../target/release ./benchmark_cedarling

clean:
	rm -f benchmark_cedarling scenarios_generated.h
