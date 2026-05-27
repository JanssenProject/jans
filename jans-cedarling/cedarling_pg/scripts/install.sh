#!/usr/bin/env bash
# Install cedarling_pg on a PostgreSQL server.
#
# Two supported paths:
#
#   1. binary  — copy a prebuilt release tarball (or an extracted directory)
#                into the directories reported by pg_config.
#   2. source  — compile with cargo-pgrx and install into a local Postgres.
#
# Examples:
#   # Install a release tarball (PG major is read from pg_config).
#   ./scripts/install.sh binary cedarling_pg-1.0.0-pg16-linux-x86_64.tar.gz
#
#   # Install from an already-extracted directory.
#   ./scripts/install.sh binary ./cedarling_pg-1.0.0-pg16-linux-x86_64
#
#   # Build from source for the default pgrx PG major (pg16).
#   ./scripts/install.sh source --release
#
#   # Build from source for PostgreSQL 17.
#   PG_VERSION=pg17 ./scripts/install.sh source --release
#
# Environment variables (binary mode):
#   PG_CONFIG   path to pg_config for the target server (default: pg_config)
#
# Environment variables (source mode):
#   PG_VERSION  pgrx feature to build against (default: pg16)
#   PG_CONFIG   pg_config for the target server; when unset, uses PATH or
#               the pgrx-managed Postgres for PG_VERSION
#   PG_DB       database used for the post-install health check
#               (default: cedarling_pg_install_check)
#   PSQL        psql binary matching PG_VERSION (default: next to pg_config)

set -euo pipefail

readonly EXT_NAME="cedarling_pg"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

log()  { printf '==> %s\n' "$*" >&2; }
warn() { printf 'WARNING: %s\n' "$*" >&2; }
die()  { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

usage() {
    cat <<'EOF'
Usage:
  install.sh binary <tarball-or-directory>
  install.sh source [--release] [--skip-health]

Commands:
  binary   Install a prebuilt .tar.gz or an extracted release directory.
           PostgreSQL major is taken from pg_config and must match the
           pgNN segment in the artifact name (for example pg16).

  source   Build with cargo-pgrx and install into the Postgres instance
           selected by PG_VERSION / PG_CONFIG. Runs a psql health check
           unless --skip-health is passed.

Options:
  --release      Release build (source mode only).
  --skip-health  Skip the psql health check (source mode only).
  -h, --help     Show this help text.

Environment:
  PG_CONFIG      pg_config for the target server (binary and source modes).
                 Source mode default: first on PATH, then pgrx-managed Postgres
                 for PG_VERSION. Example: /usr/lib/postgresql/16/bin/pg_config
  PG_VERSION     pgrx feature for source builds (default: pg16).
  PG_DB          Health-check database (source mode).
  PSQL           psql binary for the health check (source mode).
EOF
}

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "missing required command: $1"
}

detect_pg_major() {
    local pg_config="${1}"
    local raw
    raw="$("${pg_config}" --version)" || die "could not run ${pg_config} --version"
    echo "${raw}" | awk '{print $2}' | cut -d. -f1
}

# Resolve pg_config for source builds: PG_CONFIG, then PATH, then pgrx-managed Postgres.
resolve_pg_config_for_source() {
    local pg_version="${1}"

    if [[ -n "${PG_CONFIG:-}" ]]; then
        echo "${PG_CONFIG}"
        return 0
    fi

    if command -v pg_config >/dev/null 2>&1; then
        command -v pg_config
        return 0
    fi

    local pgrx_config
    if ! pgrx_config="$(cargo pgrx info pg-config "${pg_version}" 2>/dev/null)"; then
        die "pg_config not found on PATH and Postgres ${pg_version} is not managed by pgrx. Run: cargo pgrx init --${pg_version} download   Or set PG_CONFIG to your server's pg_config."
    fi
    [[ -x "${pgrx_config}" ]] || die "pgrx pg_config is not executable: ${pgrx_config}"
    echo "${pgrx_config}"
}

install_file() {
    local src="${1}" dst="${2}"
    if [[ -w "$(dirname "${dst}")" ]]; then
        install -m 0644 "${src}" "${dst}"
    elif command -v sudo >/dev/null 2>&1; then
        sudo install -m 0644 "${src}" "${dst}"
    else
        die "no write access to ${dst} and sudo is not available"
    fi
}

# Return "lib_dir|share_dir" for the matched PG major inside a staged tree.
locate_staged_files() {
    local root="${1}" pg_major="${2}"

    local lib_dir="${root}/usr/lib/postgresql/${pg_major}/lib"
    local share_dir="${root}/usr/share/postgresql/${pg_major}/extension"
    if [[ -d "${lib_dir}" && -d "${share_dir}" \
          && -f "${lib_dir}/${EXT_NAME}.so" \
          && -f "${share_dir}/${EXT_NAME}.control" ]]; then
        echo "${lib_dir}|${share_dir}"
        return 0
    fi

    lib_dir="$(find "${root}" -type f -name "${EXT_NAME}.so" -print -quit 2>/dev/null)"
    share_dir="$(find "${root}" -type f -name "${EXT_NAME}.control" -print -quit 2>/dev/null)"
    if [[ -n "${lib_dir}" && -n "${share_dir}" ]]; then
        echo "$(dirname "${lib_dir}")|$(dirname "${share_dir}")"
        return 0
    fi

    return 1
}

install_binary_from_stage() {
    local stage="${1}" pg_major="${2}" pg_config="${3}"

    local ext_share pkg_lib pair lib_dir share_dir
    ext_share="$("${pg_config}" --sharedir)/extension"
    pkg_lib="$("${pg_config}" --pkglibdir)"

    pair="$(locate_staged_files "${stage}" "${pg_major}")" || \
        die "could not find ${EXT_NAME}.so / ${EXT_NAME}.control for PG ${pg_major} under ${stage}"
    lib_dir="${pair%|*}"
    share_dir="${pair#*|}"

    log "Target PG ${pg_major}"
    log "  shared extension dir: ${ext_share}"
    log "  library dir:          ${pkg_lib}"

    local sql_count
    sql_count="$(find "${share_dir}" -maxdepth 1 -name "${EXT_NAME}--*.sql" | wc -l)"
    [[ "${sql_count}" -gt 0 ]] || die "no ${EXT_NAME}--*.sql files found in ${share_dir}"

    shopt -s nullglob
    local f
    for f in "${share_dir}/${EXT_NAME}.control" "${share_dir}/${EXT_NAME}--"*.sql; do
        log "Installing $(basename "${f}")"
        install_file "${f}" "${ext_share}/$(basename "${f}")"
    done
    shopt -u nullglob

    log "Installing ${EXT_NAME}.so"
    install_file "${lib_dir}/${EXT_NAME}.so" "${pkg_lib}/${EXT_NAME}.so"
}

prepare_binary_stage() {
    local input="${1}"
    local pg_major="${2}"

    case "${input}" in
        *.tar.gz|*.tgz)
            require_cmd tar
            local tmp extracted
            tmp="$(mktemp -d)"
            tar -xzf "${input}" -C "${tmp}"
            extracted="$(find "${tmp}" -mindepth 1 -maxdepth 1 -type d | head -1)"
            [[ -n "${extracted}" ]] || die "tarball ${input} did not contain a top-level directory"
            echo "${extracted}|${tmp}"
            ;;
        *)
            if [[ -d "${input}" ]]; then
                echo "${input}|"
            else
                die "binary input must be a .tar.gz file or an extracted directory: ${input}"
            fi
            ;;
    esac
}

cmd_binary() {
    local input="${1:-}"
    [[ -n "${input}" ]] || die "binary mode requires a tarball or directory argument (see --help)"

    local pg_config="${PG_CONFIG:-pg_config}"
    require_cmd "${pg_config}"
    local pg_major
    pg_major="$(detect_pg_major "${pg_config}")"
    [[ "${pg_major}" =~ ^[0-9]+$ ]] || die "could not parse PG major from ${pg_config}"

    if [[ "${input}" == *"-pg${pg_major}-"* || "${input}" == *"-pg${pg_major}/"* || "${input}" == *"-pg${pg_major}" ]]; then
        : # artifact name matches detected server major
    else
        warn "artifact name does not contain -pg${pg_major}-; continuing because paths inside the tree are authoritative"
    fi

    local stage_info stage cleanup
    stage_info="$(prepare_binary_stage "${input}")"
    stage="${stage_info%%|*}"
    cleanup="${stage_info#*|}"

    install_binary_from_stage "${stage}" "${pg_major}" "${pg_config}"

    if [[ -n "${cleanup}" ]]; then
        rm -rf "${cleanup}"
    fi

    log "Installed ${EXT_NAME} for PG ${pg_major}."
    log "Next: in psql (as superuser) run    CREATE EXTENSION ${EXT_NAME};"
}

cmd_source() {
    local pg_version="${PG_VERSION:-pg16}"
    local pg_db="${PG_DB:-cedarling_pg_install_check}"
    local release_flag=""
    local skip_health="false"

    for arg in "$@"; do
        case "${arg}" in
            --release) release_flag="--release" ;;
            --skip-health) skip_health="true" ;;
            *) die "unknown source option: ${arg} (see --help)" ;;
        esac
    done

    require_cmd cargo
    local pg_config
    pg_config="$(resolve_pg_config_for_source "${pg_version}")"
    local pg_config_major
    pg_config_major="$(detect_pg_major "${pg_config}")"
    local pg_version_major="${pg_version#pg}"
    if [[ "${pg_config_major}" != "${pg_version_major}" ]]; then
        warn "PG_VERSION=${pg_version} but ${pg_config} reports PostgreSQL ${pg_config_major}; continuing because PG_CONFIG/PATH was explicit"
    fi

    local psql_bin="${PSQL:-$(dirname "${pg_config}")/psql}"
    log "Building and installing ${EXT_NAME} from source (${pg_version}${release_flag:+, release})"
    log "Using pg_config: ${pg_config}"
    cd "${EXT_DIR}"
    # shellcheck disable=SC2086
    cargo pgrx install --pg-config "${pg_config}" --features "${pg_version}" ${release_flag}

    if [[ "${skip_health}" == "true" ]]; then
        log "Skipping health check (--skip-health)"
        return 0
    fi

    require_cmd "${psql_bin}"
    log "Running post-install health check in database '${pg_db}'"
    if ! "${psql_bin}" -lqt | awk '{print $1}' | grep -qx "${pg_db}"; then
        "${psql_bin}" -v ON_ERROR_STOP=1 -d postgres -c "CREATE DATABASE \"${pg_db//\"/\"\"}\""
    fi

    "${psql_bin}" -v ON_ERROR_STOP=1 -d "${pg_db}" <<'SQL'
DROP EXTENSION IF EXISTS cedarling_pg CASCADE;
CREATE EXTENSION cedarling_pg;

SELECT count(*) = 1 AS cedarling_schema_present
  FROM pg_namespace
 WHERE nspname = 'cedarling';

SELECT relname
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'cedarling'
   AND c.relkind = 'r'
 ORDER BY relname;

SELECT count(*) > 0 AS cedarling_functions_present
  FROM pg_proc p
  JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'public'
   AND proname LIKE 'cedarling_%';
SQL

    log "OK — ${EXT_NAME} built, installed, and health check passed"
    log "Next: point cedarling.bootstrap_config at your bootstrap YAML and reload."
}

main() {
    if [[ $# -eq 0 ]]; then
        usage
        exit 2
    fi

    case "${1}" in
        -h|--help|help)
            usage
            ;;
        binary)
            shift
            cmd_binary "${1:-}"
            ;;
        source)
            shift
            cmd_source "$@"
            ;;
        --release|--skip-health)
            # Back-compat: bare flags imply source mode.
            cmd_source "$@"
            ;;
        *)
            die "unknown command: ${1} (expected 'binary' or 'source'; try --help)"
            ;;
    esac
}

main "$@"
