// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! `cedarling-pg-codegen`: generate Cedar schema files (`.cedarschema`) from
//! live `PostgreSQL` tables. Sister tool to the `cedarling_pg` extension —
//! intended to run at build time (or in CI) so the Cedar schema stays in sync
//! with the database schema.

use std::fs;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};
use clap::Parser;
use postgres::{Client, NoTls};

use cedarling_pg_codegen::{Column, EntityRender, render_entity, wrap_namespace};

#[derive(Debug, Parser)]
#[command(
    name = "cedarling-pg-codegen",
    about = "Generate Cedar schema (.cedarschema) from live PostgreSQL tables.",
    version
)]
struct Args {
    /// `PostgreSQL` connection string. Defaults to the `DATABASE_URL` env var.
    /// Example: `postgres://user:pass@localhost:5432/dbname`
    #[arg(long, env = "DATABASE_URL")]
    database_url: String,

    /// Cedar namespace to wrap the generated entities in.
    #[arg(long, default_value = "Jans")]
    namespace: String,

    /// Postgres schema to read tables from.
    #[arg(long, default_value = "public")]
    schema: String,

    /// Generate a single table.
    #[arg(
        long,
        conflicts_with = "all_tables",
        required_unless_present = "all_tables"
    )]
    table: Option<String>,

    /// Generate every table in the schema (one file each).
    #[arg(long, conflicts_with = "table")]
    all_tables: bool,

    /// Output file (for `--table`) or directory (for `--all-tables`).
    #[arg(long, short)]
    output: PathBuf,
}

fn main() -> Result<()> {
    let args = Args::parse();
    let mut client =
        Client::connect(&args.database_url, NoTls).context("connecting to PostgreSQL")?;

    if let Some(ref table) = args.table {
        let cols = fetch_columns(&mut client, &args.schema, table)?;
        if cols.is_empty() {
            bail!(
                "table {}.{} not found or has no columns",
                args.schema,
                table
            );
        }
        let render = render_entity(table, &cols);
        let file = wrap_namespace(&args.namespace, std::slice::from_ref(&render.cedar_text));
        write_output(&args.output, &file)?;
        report(&[(table.clone(), render)], &args.output);
    } else {
        if !args.all_tables {
            bail!("either --table or --all-tables is required");
        }
        let tables = fetch_tables(&mut client, &args.schema)?;
        if tables.is_empty() {
            bail!("no tables found in schema {}", args.schema);
        }
        fs::create_dir_all(&args.output)
            .with_context(|| format!("creating output dir {}", args.output.display()))?;
        let mut rendered: Vec<(String, EntityRender)> = Vec::new();
        for table in &tables {
            let cols = fetch_columns(&mut client, &args.schema, table)?;
            let render = render_entity(table, &cols);
            let file = wrap_namespace(&args.namespace, std::slice::from_ref(&render.cedar_text));
            let out = args.output.join(format!("{table}.cedarschema"));
            write_output(&out, &file)?;
            rendered.push((table.clone(), render));
        }
        report(&rendered, &args.output);
    }

    Ok(())
}

fn fetch_tables(client: &mut Client, schema: &str) -> Result<Vec<String>> {
    let rows = client
        .query(
            "SELECT c.relname
               FROM pg_class c
               JOIN pg_namespace n ON n.oid = c.relnamespace
              WHERE c.relkind IN ('r', 'p')
                AND n.nspname = $1
              ORDER BY c.relname",
            &[&schema],
        )
        .with_context(|| format!("listing tables in schema {schema}"))?;
    Ok(rows.into_iter().map(|r| r.get::<_, String>(0)).collect())
}

fn fetch_columns(client: &mut Client, schema: &str, table: &str) -> Result<Vec<Column>> {
    let rows = client
        .query(
            "SELECT a.attname,
                    pg_catalog.format_type(a.atttypid, a.atttypmod) AS pg_type
               FROM pg_attribute a
               JOIN pg_class c ON c.oid = a.attrelid
               JOIN pg_namespace n ON n.oid = c.relnamespace
              WHERE n.nspname = $1
                AND c.relname = $2
                AND a.attnum > 0
                AND NOT a.attisdropped
              ORDER BY a.attnum",
            &[&schema, &table],
        )
        .with_context(|| format!("describing columns of {schema}.{table}"))?;
    Ok(rows
        .into_iter()
        .map(|r| Column {
            name: r.get::<_, String>(0),
            pg_type: r.get::<_, String>(1),
        })
        .collect())
}

fn write_output(path: &Path, contents: &str) -> Result<()> {
    if let Some(parent) = path.parent()
        && !parent.as_os_str().is_empty()
    {
        fs::create_dir_all(parent)
            .with_context(|| format!("creating parent dir {}", parent.display()))?;
    }

    fs::write(path, contents).with_context(|| format!("writing {}", path.display()))?;
    Ok(())
}

fn report(rendered: &[(String, EntityRender)], output: &Path) {
    eprintln!(
        "Wrote {} entity definition(s) to {}",
        rendered.len(),
        output.display()
    );
    for (table, r) in rendered {
        if !r.unmapped_columns.is_empty() {
            eprintln!(
                "  - {} → {}: skipped unmapped columns ({})",
                table,
                r.entity_name,
                r.unmapped_columns.join(", ")
            );
        }
    }
}
