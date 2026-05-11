# Makefile for Cedarling C benchmarks
#
# Link against ../../../target/release/libcedarling_c.so — run
#   cargo build --release -p cedarling_c
# from the jans-cedarling workspace root first so the binary matches current sources.

CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2
LDFLAGS = -L../../../target/release -Wl,-rpath=../../../target/release -lcedarling_c -lm
INCLUDES = -I../target/include

# Detect OS for library linking
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -ldl -lpthread
endif
ifeq ($(UNAME_S),Darwin)
    LDFLAGS += -lpthread
endif

.PHONY: all clean run

all: benchmark_cedarling

benchmark_cedarling: benchmark_cedarling.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

run: benchmark_cedarling
	LD_LIBRARY_PATH=../../../target/release DYLD_LIBRARY_PATH=../../../target/release ./benchmark_cedarling

clean:
	rm -f benchmark_cedarling
