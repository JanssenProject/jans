from click.testing import CliRunner


def test_encoding_decode_file_salt_literal():
    from jans.pycloudlib.cli.encoding import decode_file

    salt = "2CG9qwCzH5haWXXuUIUe4wFT"

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY=")

        result = runner.invoke(
            decode_file,
            ["password.txt", "--salt-literal", salt]
        )
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_file_salt_file():
    from jans.pycloudlib.cli.encoding import decode_file

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY=")

        with open("salt.txt", "w") as f:
            f.write("2CG9qwCzH5haWXXuUIUe4wFT")

        result = runner.invoke(
            decode_file,
            ["password.txt", "--salt-file", "salt.txt"]
        )
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_file_salt(monkeypatch):
    from jans.pycloudlib.cli.encoding import decode_file

    monkeypatch.setattr(
        "jans.pycloudlib.manager.SecretManager.get",
        lambda key, default: "2CG9qwCzH5haWXXuUIUe4wFT",
    )

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY=")

        result = runner.invoke(decode_file, ["password.txt"])
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_file_no_salt():
    from jans.pycloudlib.cli.encoding import decode_file

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY=")

        result = runner.invoke(decode_file, ["password.txt"])
        assert result.exit_code == 1
        assert "Aborted" in result.output.strip()


def test_encoding_decode_file_value_error(monkeypatch):
    from jans.pycloudlib.cli.encoding import decode_file

    monkeypatch.setattr(
        "jans.pycloudlib.manager.SecretManager.get",
        lambda key, default: "2CG9qwCzH5haWXXuUIUe4wFT",
    )

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY")

        result = runner.invoke(decode_file, ["password.txt"])
        assert result.exit_code == 1
        assert "Error:" in result.output.strip()


def test_encoding_decode_string_salt_literal():
    from jans.pycloudlib.cli.encoding import decode_string

    salt = "2CG9qwCzH5haWXXuUIUe4wFT"

    runner = CliRunner()
    result = runner.invoke(
        decode_string,
        ["4rl2tJEQFkY=", "--salt-literal", salt],
    )
    assert result.exit_code == 0
    assert result.output.strip() == "secret"


def test_encoding_decode_string_salt_file():
    from jans.pycloudlib.cli.encoding import decode_string

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("salt.txt", "w") as f:
            f.write("2CG9qwCzH5haWXXuUIUe4wFT")

        result = runner.invoke(
            decode_string,
            ["4rl2tJEQFkY=", "--salt-file", "salt.txt"],
        )
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_string_salt(monkeypatch):
    from jans.pycloudlib.cli.encoding import decode_string

    monkeypatch.setattr(
        "jans.pycloudlib.manager.SecretManager.get",
        lambda key, default: "2CG9qwCzH5haWXXuUIUe4wFT",
    )

    runner = CliRunner()
    result = runner.invoke(decode_string, ["4rl2tJEQFkY="])
    assert result.exit_code == 0
    assert result.output.strip() == "secret"


def test_encoding_decode_string_no_salt():
    from jans.pycloudlib.cli.encoding import decode_string

    runner = CliRunner()
    result = runner.invoke(decode_string, ["4rl2tJEQFkY="])
    assert result.exit_code == 1
    assert "Aborted" in result.output.strip()


def test_encoding_decode_string_value_error(monkeypatch):
    from jans.pycloudlib.cli.encoding import decode_string

    monkeypatch.setattr(
        "jans.pycloudlib.manager.SecretManager.get",
        lambda key, default: "2CG9qwCzH5haWXXuUIUe4wFT",
    )

    runner = CliRunner()
    result = runner.invoke(decode_string, ["4rl2tJEQFkY"])
    assert result.exit_code == 1
    assert "Error:" in result.output.strip()
