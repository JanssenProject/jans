# Makefile for Cedarling C tests

CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2
LDFLAGS = -L../../../target/release -Wl,-rpath=../../../target/release -lcedarling_c
INCLUDES = -I../target/include

# Detect OS for library linking
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -ldl -lpthread -lm
endif
ifeq ($(UNAME_S),Darwin)
    LDFLAGS += -lpthread -lm
endif

# Source files
SOURCES = test_cedarling.c
TARGETS = $(SOURCES:.c=)

.PHONY: all clean

all: $(TARGETS)

test_cedarling: test_cedarling.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

clean:
	rm -f $(TARGETS)

# Run the tests
run: test_cedarling
	LD_LIBRARY_PATH=../../../target/release ./test_cedarling

# Static linking version
static: test_cedarling.c
	$(CC) $(CFLAGS) $(INCLUDES) -o test_cedarling_static $< ../../../target/release/libcedarling_c.a -ldl -lpthread -lm

.PHONY: run static