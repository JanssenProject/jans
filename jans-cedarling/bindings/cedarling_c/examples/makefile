# Makefile for Cedarling C examples

CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2
LIBDIR = ../../../target/release
STATIC_LIB = $(LIBDIR)/libcedarling_c.a
LDFLAGS = -L$(LIBDIR) -Wl,-rpath=$(LIBDIR) -lcedarling_c
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
SOURCES = basic_example.c authorize_unsigned.c authorize_multi_issuer.c
TARGETS = $(SOURCES:.c=)

.PHONY: all clean

all: $(TARGETS)

basic_example: basic_example.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

authorize_unsigned: authorize_unsigned.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

authorize_multi_issuer: authorize_multi_issuer.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

clean:
	rm -f $(TARGETS)

# Run examples
run: basic_example
	LD_LIBRARY_PATH=../../../target/release ./basic_example

run-unsigned: authorize_unsigned
	LD_LIBRARY_PATH=../../../target/release ./authorize_unsigned

run-multi-issuer: authorize_multi_issuer
	LD_LIBRARY_PATH=../../../target/release ./authorize_multi_issuer

# Static linking version
static: basic_example.c
	$(CC) $(CFLAGS) $(INCLUDES) -o basic_example_static $< $(STATIC_LIB) -ldl -lpthread -lm

.PHONY: run run-unsigned run-multi-issuer static