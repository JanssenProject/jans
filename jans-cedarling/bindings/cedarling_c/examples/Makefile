# Makefile for Cedarling C examples

CC = gcc
CFLAGS = -Wall -Wextra -std=c99 -O2
LDFLAGS = -L../../../target/release -Wl,-rpath=../../../target/release -lcedarling_c
INCLUDES = -I../include

# Detect OS for library linking
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
    LDFLAGS += -ldl -lpthread -lm
endif
ifeq ($(UNAME_S),Darwin)
    LDFLAGS += -ldl -lpthread -lm
endif

# Source files
SOURCES = basic_example.c
TARGETS = $(SOURCES:.c=)

.PHONY: all clean

all: $(TARGETS)

basic_example: basic_example.c
	$(CC) $(CFLAGS) $(INCLUDES) -o $@ $< $(LDFLAGS)

clean:
	rm -f $(TARGETS)

# Run the basic example
run: basic_example
	LD_LIBRARY_PATH=.. ./basic_example

# Static linking version
static: basic_example.c
	$(CC) $(CFLAGS) $(INCLUDES) -o basic_example_static $< ../../libcedarling_c.a -ldl -lpthread -lm

.PHONY: run static