#!/bin/bash
# Improved version of generate-swagger-specs.sh with error handling and dependency checks
set -euo pipefail

# The below variable represents the top level directory of the repository
MAIN_DIRECTORY_LOCATION=$1
echo "Generate Swagger yaml SPEC with improved error handling"

# Function to check if Maven is available
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo "ERROR: Maven (mvn) is not installed or not in PATH"
        echo "Please install Maven before running this script"
        return 1
    fi
    echo "Maven found: $(mvn --version | head -1)"
    return 0
}

# Function to check if Java is available
check_java() {
    if ! command -v java &> /dev/null; then
        echo "ERROR: Java is not installed or not in PATH"
        echo "Please install Java 11+ before running this script"
        return 1
    fi
    echo "Java found: $(java -version 2>&1 | head -1)"
    return 0
}

# Function to verify jans-config-api directory structure
verify_config_api_structure() {
    local config_api_dir="$MAIN_DIRECTORY_LOCATION/jans-config-api"

    if [[ ! -d "$config_api_dir" ]]; then
        echo "ERROR: jans-config-api directory not found: $config_api_dir"
        return 1
    fi

    if [[ ! -f "$config_api_dir/pom.xml" ]]; then
        echo "ERROR: jans-config-api pom.xml not found: $config_api_dir/pom.xml"
        return 1
    fi

    # Create docs directory if it doesn't exist
    local docs_dir="$config_api_dir/docs"
    if [[ ! -d "$docs_dir" ]]; then
        echo "Creating jans-config-api docs directory: $docs_dir"
        mkdir -p "$docs_dir"
    fi

    echo "jans-config-api structure verified"
    return 0
}

# Function to compile jans-config-api with error handling
compile_config_api() {
    local config_api_pom="$MAIN_DIRECTORY_LOCATION/jans-config-api/pom.xml"

    echo "Compiling jans-config-api to generate Swagger specifications..."

    if ! mvn -q -f "$config_api_pom" -DskipTests clean compile; then
        echo "ERROR: Failed to compile jans-config-api"
        echo "This will prevent Swagger specification generation"
        return 1
    fi

    echo "Successfully compiled jans-config-api"
    return 0
}

# Function to check for generated Swagger files
check_generated_swagger_files() {
    local config_api_dir="$MAIN_DIRECTORY_LOCATION/jans-config-api"
    local target_dir="$config_api_dir/target"

    echo "Checking for generated Swagger files..."

    # Common locations where Swagger files might be generated
    local potential_locations=(
        "$target_dir/classes"
        "$target_dir/generated-sources"
        "$target_dir/swagger"
        "$config_api_dir/src/main/resources"
    )

    local found_files=0
    for loc in "${potential_locations[@]}"; do
        if [[ -d "$loc" ]]; then
            local swagger_files=$(find "$loc" -name "*.yaml" -o -name "*.yml" -o -name "*.json" | grep -i swagger || true)
            if [[ -n "$swagger_files" ]]; then
                echo "Found Swagger files in $loc:"
                echo "$swagger_files"
                found_files=1

                # Copy found files to docs directory
                while IFS= read -r file; do
                    if [[ -n "$file" ]]; then
                        local filename=$(basename "$file")
                        cp "$file" "$config_api_dir/docs/$filename"
                        echo "Copied $filename to jans-config-api/docs/"
                    fi
                done <<< "$swagger_files"
            fi
        fi
    done

    if [[ $found_files -eq 0 ]]; then
        echo "WARNING: No Swagger files found in expected locations"
        echo "The Maven compilation may not have generated Swagger specifications"
        echo "This could be due to:"
        echo "  - Missing Swagger annotations in the code"
        echo "  - Missing Swagger generation plugins in pom.xml"
        echo "  - Compilation errors that prevented generation"
        return 1
    fi

    return 0
}

# Function to create a summary report
create_summary_report() {
    local config_api_dir="$MAIN_DIRECTORY_LOCATION/jans-config-api"
    local docs_dir="$config_api_dir/docs"
    local report_file="$docs_dir/swagger-generation-report.md"

    echo "Creating Swagger generation summary report..."

    cat > "$report_file" << EOF
# Swagger Specification Generation Report

Generated on: $(date)
Script: generate-swagger-specs.sh (improved version)

## Configuration API Compilation Status
- Maven compilation: $(if [[ -d "$config_api_dir/target" ]]; then echo "SUCCESS"; else echo "FAILED"; fi)
- Target directory: $config_api_dir/target

## Generated Swagger Files
$(if [[ -d "$docs_dir" ]]; then
    find "$docs_dir" -name "*.yaml" -o -name "*.yml" -o -name "*.json" | while read -r file; do
        echo "- $(basename "$file") ($(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "unknown") bytes)"
    done
else
    echo "- No files generated"
fi)

## Notes
- Swagger files are automatically generated during Maven compilation
- Files are placed in the jans-config-api/docs directory
- Generation depends on proper Swagger annotations in the source code

EOF

    echo "Summary report created: $report_file"
}

# Main execution
main() {
    echo "Starting Swagger specification generation process..."

    # Verify prerequisites
    if ! check_java; then
        exit 1
    fi

    if ! check_maven; then
        exit 1
    fi

    if ! verify_config_api_structure; then
        exit 1
    fi

    # Compile jans-config-api to generate new Swagger SPECs from API annotations
    if compile_config_api; then
        check_generated_swagger_files || echo "WARNING: Swagger file generation may have issues"
    else
        echo "FATAL: Failed to compile jans-config-api, cannot generate Swagger specifications"
        exit 1
    fi

    # Create summary report
    create_summary_report

    echo "Swagger specification generation completed!"
    echo "Check jans-config-api/docs/ for generated files and the summary report."
}

# Run main function
main "$@"