package io.jans.service.custom.script.jit;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;


public class JavaCodeGenerator {
    /**
     * Current scope
     */
    private Scope scope;

    /**
     * Parent of this code generator
     */
    private final JavaCodeGenerator parent;

    /**
     * Creates a new JavaCodeGenerator that writes to the specified writer
     */
    public JavaCodeGenerator() {
        scope = new Scope();
        parent = null;
    }

    /**
     * Creates a new JavaCodeGenerator that generates in a sub-scope of the specified generator
     */
    private JavaCodeGenerator(JavaCodeGenerator parent) {
        this.scope = new Scope(parent.scope);
        this.parent = parent;
    }

    /**
     * @return the name with a unique integer appended. The number is unique only for the current scope.
     */
    public String unique(String name) {
        return scope.unique(name);
    }

    /**
     * @return this code generator's parent or null if top-level
     */
    public JavaCodeGenerator parent() {
        return parent;
    }

    /**
     * Generates an import
     */
    public void generateImport(Class<?> aClass) {
        printf("import %s;%n", aClass.getName());
    }

    /**
     * Declares an uninitialized local variable
     */
    public JavaCodeGenerator declare(String type, String name) {
        return declare(type, name, null);
    }

    /**
     * Declare a pre-existing variable in the current scope. No code is emitted.
     */
    public JavaCodeGenerator declareExisting(String name) {
        scope.declare(name);
        return this;
    }

    /**
     * Declares an initialized local variable
     */
    public JavaCodeGenerator declare(String type, String name, String initialValue, Object... args) {
        scope.delayed.add(declareInternal(scope, type, name, initialValue == null ? null : String.format(initialValue, args)));
        return this;
    }

    /**
     * Declares an initialized local variable at the beginning of the current scope.
     * Useful to declare temporary variables on complex expressions.
     */
    public JavaCodeGenerator declareOnScope(final String type, final String name, final String initialValue) {
        scope.declare(type, name, initialValue);
        return this;
    }

    /**
     * Declares an initialized local variable at the beginning of the current scope.
     * Useful to declare temporary variables on complex expressions.
     *
     * @return the resolved name of the variable
     */
    public static String declareOnScope(final Scope scope, final String type, final String name, final String initialValue) {
        scope.delayed.add(scope.lastDeclaration++, declareInternal(scope, type, name, initialValue));
        return scope.variable(name);
    }

    /**
     * Prints the specified code at the beginning of the current scope.
     * Useful to initialize temporary variables on complex expressions.
     */
    public JavaCodeGenerator printfOnScope(final String format, final Object... args) {
        scope.delayed.add(scope.lastDeclaration++, printfInternal(format, args));
        return this;
    }

    private Consumer<IndentedPrintWriter> printfInternal(final String format, final Object[] args) {
        return pw -> pw.printf(format, args);
    }

    private static Consumer<IndentedPrintWriter> declareInternal(final Scope scope, final String type, String name, final String initialValue) {
        scope.declare(name);
        final String resolvedVariable = scope.variable(name);
        return pw -> {
            if (initialValue == null) {
                pw.printf("%s %s;%n", type, resolvedVariable);
            } else {
                pw.printf("%s %s = %s;%n", type, resolvedVariable, initialValue);
            }
        };
    }

    /**
     * Simple bounded loop over an int index variable
     */
    public JavaCodeGenerator boundedLoop(String indexVar, String lowerBound, String upperBound, Consumer<JavaCodeGenerator> body) {
        enterScope();
        scope.declare(indexVar);
        printf("for (int %1$s = %2$s; %1$s < %3$s; %1$s++) {%n", variable(indexVar), lowerBound, upperBound);
        indent();
        body.accept(this);
        dedent();
        println("}");
        leaveScope();
        return this;
    }

    /**
     * Unbounded loop
     */
    public JavaCodeGenerator loop(Consumer<JavaCodeGenerator> body) {
        println("for (;;) {");
        enterScope();
        indent();
        body.accept(this);
        dedent();
        println("}");
        leaveScope();
        return this;
    }

    /**
     * Fore-each loop over an iterable
     */
    public JavaCodeGenerator forEach(String type, String elementVar, String collection, Consumer<JavaCodeGenerator> body) {
        enterScope();
        scope.declare(elementVar);
        printf("for (%1$s %2$s : %3$s) {%n", type, variable(elementVar), collection);
        indent();
        body.accept(this);
        dedent();
        println("}");
        leaveScope();
        return this;
    }

    /**
     * Emmit a single-line comment. Supports new lines.
     */
    public void comment(String s, Object... args) {
        println("//" + String.format(s, args).replaceAll("\n", "\n//"));
    }

    /**
     * @return the mangled name of the variable if it's shadowing another on an outer scope
     */
    public String variable(String name) {
        return scope.variable(name);
    }

    /**
     * Print the specified string so it is a valid java string literal.
     */
    public JavaCodeGenerator printQuoted(String s) {
        if (s == null) {
            print("null");
        } else {
            print('"');
            print(escape(s));
            print('"');
        }
        return this;
    }

    /**
     * Generates a simple if-then statement
     */
    public JavaCodeGenerator ifThen(final String condition, Consumer<JavaCodeGenerator> accept) {
        return ifThen(literal(condition), accept);
    }

    /**
     * Generates a simple if-then statement
     */
    public JavaCodeGenerator ifThen(Consumer<JavaCodeGenerator> condition, Consumer<JavaCodeGenerator> accept) {
        return ifElse(condition, accept, null);
    }

    /**
     * Generate an if-else statement
     */
    public JavaCodeGenerator ifElse(String condition, Consumer<JavaCodeGenerator> accept, Consumer<JavaCodeGenerator> reject) {
        return ifElse(literal(condition), accept, reject);
    }

    /**
     * @return the current scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Generate an if-else statement
     */
    public JavaCodeGenerator ifElse(Consumer<JavaCodeGenerator> condition, Consumer<JavaCodeGenerator> accept, Consumer<JavaCodeGenerator> reject) {
        print("if (");
        condition.accept(this);
        println(") {");
        enterScope();
        indent();
        accept.accept(this);
        dedent();
        leaveScope();
        if (reject != null) {
            println("} else {");
            enterScope();
            indent();
            reject.accept(this);
            dedent();
            leaveScope();
        }
        println("}");
        return this;
    }

    private Consumer<JavaCodeGenerator> literal(final String condition) {
        return cg -> cg.print(condition);
    }

    /**
     * Close all open scopes and generate the code
     */
    public void generate(Writer out) {
        final IndentedPrintWriter pw = new IndentedPrintWriter(out);
        leaveUntil(parent == null ? null : parent.scope);
        for (Consumer<IndentedPrintWriter> cmd : scope.delayed) {
            cmd.accept(pw);
        }
    }

    private void leaveUntil(Scope root) {
        while (scope.parent != root)
            leaveScope();
    }

    /**
     * Begins a new block
     */
    public JavaCodeGenerator beginBlock() {
        println("{");
        indent();
        enterScope();
        return this;
    }

    /**
     * Ends the current block
     */
    public JavaCodeGenerator endBlock() {
        leaveScope();
        dedent();
        println("}");
        return this;
    }

    private void enterScope() {
        scope = new Scope(scope);
    }

    private void leaveScope() {
        if (scope.parent == null) throw new IllegalStateException("you must be in a scope to leave it");
        scope.parent.delayed.addAll(scope.delayed);
        scope = scope.parent;
    }

    private String escape(String s) {
        final int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Print a new line
     */
    public JavaCodeGenerator println() {
        scope.delayed.add(PrintWriter::println);
        return this;
    }

    /**
     * Prints an Object and then terminates the line.
     */
    public JavaCodeGenerator println(final Object s) {
        scope.delayed.add(pw -> pw.println(s));
        return this;
    }

    /**
     * Prints an object.
     */
    public JavaCodeGenerator print(final Object s) {
        scope.delayed.add(pw -> pw.print(s));
        return this;
    }

    /**
     * Prints a formatted string using the specified format string and arguments.
     */
    public JavaCodeGenerator printf(final String format, final Object... args) {
        scope.delayed.add(printfInternal(format, args));
        return this;
    }

    private void indent() {
        scope.delayed.add(IndentedPrintWriter::indent);
    }

    private void dedent() {
        scope.delayed.add(IndentedPrintWriter::dedent);
    }

    /**
     * Build a new public class
     */
    public ClassBuilder publicClass(String name) {
        return new ClassBuilder("public", name);
    }

    /**
     * Helper class to generate classes
     */
    public class ClassBuilder {
        private ClassBuilder(String visibility, String name) {
            declareExisting(name);
            if (visibility != null) {
                printf("%s class %s", visibility, name);
            } else {
                printf("class %s", name);
            }
        }

        /**
         * Make the generated class extend another class.
         */
        public ClassBuilder extend(String aClass) {
            printf(" extends %s", aClass);
            return this;
        }

        /**
         * Make the generated class extend another class.
         */
        public ClassBuilder implement(String... anInterface) {
            StringJoiner joiner = new StringJoiner(",");
            for (String s : anInterface) {
                joiner.add(s);
            }
            printf(" implements %s", joiner);
            return this;
        }

        /**
         * Build the class' body
         */
        public JavaCodeGenerator build(Consumer<JavaCodeGenerator> body) {
            println(" {");
            enterScope();
            indent();
            body.accept(JavaCodeGenerator.this);
            dedent();
            println("}");
            leaveScope();
            return JavaCodeGenerator.this;
        }
    }

    /**
     * Create a private method
     */
    public MethodBuilder packageMethod(String returnType, String name) {
        return new MethodBuilder("", returnType, name);
    }

    /**
     * Create a private method
     */
    public MethodBuilder privateMethod(String returnType, String name) {
        return new MethodBuilder("private ", returnType, name);
    }

    /**
     * Create a public method
     */
    public MethodBuilder publicMethod(String returnType, String name) {
        return new MethodBuilder("public ", returnType, name);
    }

    /**
     * Helper class to build methods
     */
    public class MethodBuilder {
        private JavaCodeGenerator cg = new JavaCodeGenerator(JavaCodeGenerator.this);
        private boolean hasArgs;

        private MethodBuilder(String visibility, String returnType, String name) {
            cg.printf("%s%s %s(", visibility, returnType, name);
        }

        /**
         * Adds an argument of the specified type
         */
        public MethodBuilder arg(String type, String name) {
            cg.declareExisting(name);
            if (hasArgs) {
                cg.print(", ");
            }
            cg.printf("%s %s", type, name);
            hasArgs = true;
            return this;
        }

        /**
         * Builds this method's body
         */
        public JavaCodeGenerator build(Consumer<JavaCodeGenerator> body) {
            cg.println(") {");
            cg.indent();
            // Generate the body in it's own scope.
            final JavaCodeGenerator bodyGen = new JavaCodeGenerator(cg);
            body.accept(bodyGen);
            bodyGen.leaveScope();
            cg.dedent();
            cg.println("}");
            cg.leaveScope();
            return JavaCodeGenerator.this;
        }
    }

    /**
     * Simple scope for generation of shadowed local variables
     */
    public static class Scope {
        private final Scope parent;
        private final Set<String> variables = new HashSet<>();
        private int lastDeclaration;
        private final List<Consumer<IndentedPrintWriter>> delayed = new ArrayList<>();
        private int varId;

        private Scope() {
            this.parent = null;
        }

        private Scope(Scope parent) {
            this.parent = parent;
        }

        private void declare(String name) {
            if (variables.contains(name))
                throw new IllegalArgumentException(String.format("variable '%s' already declared in this scope", name));
            variables.add(name);
        }

        private String mangle(String name) {
            int level = -1;
            for (Scope scope = this; scope != null; scope = scope.parent) {
                if (scope.variables.contains(name)) {
                    ++level;
                }
            }
            return level > 0 ? name + "$" + level : name;
        }

        String variable(String name) {
            if (variables.contains(name)) {
                return mangle(name);
            } else if (parent != null) {
                return parent.variable(name);
            } else {
                throw new IllegalArgumentException("Unknown variable: " + name);
            }
        }

        private String unique(String name) {
            return name + "_" + (++varId);
        }

        /**
         * Declares a variable at the beginning of the this scope.
         */
        public String declare(String type, String name, String initialValue) {
            delayed.add(lastDeclaration++, declareInternal(type, name, initialValue));
            return variable(name);
        }

        private Consumer<IndentedPrintWriter> declareInternal(final String type, String name, final String initialValue) {
            declare(name);
            final String resolvedVariable = variable(name);
            return pw -> {
                if (initialValue == null) {
                    pw.printf("%s %s;%n", type, resolvedVariable);
                } else {
                    pw.printf("%s %s = %s;%n", type, resolvedVariable, initialValue);
                }
            };
        }

        /**
         * @return true if the specified variable is defined in this scope
         */
        public boolean isDefined(String variable) {
            return variables.contains(variable);
        }
    }
}
