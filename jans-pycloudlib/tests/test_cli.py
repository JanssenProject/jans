from click.testing import CliRunner


def test_encoding_decode_file_salt_literal():
    from jans.pycloudlib.cli.encoding import decode_file

    salt = "7MEDWVFAG3DmakHRyjMqp5EE"

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("fHL54sT5qHk=")

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
            f.write("fHL54sT5qHk=")

        with open("salt.txt", "w") as f:
            f.write("7MEDWVFAG3DmakHRyjMqp5EE")

        result = runner.invoke(
            decode_file,
            ["password.txt", "--salt-file", "salt.txt"]
        )
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_file_salt(monkeypatch, gmanager):
    from jans.pycloudlib.cli.encoding import decode_file

    monkeypatch.setattr(
        "jans.pycloudlib.manager.Manager",
        lambda: gmanager,
    )

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("fHL54sT5qHk=")

        result = runner.invoke(decode_file, ["password.txt"])
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_file_no_salt(monkeypatch, gmanager):
    from jans.pycloudlib.cli.encoding import decode_file

    monkeypatch.setattr(
        "jans.pycloudlib.manager.get_manager",
        lambda: gmanager,
    )

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("fHL54sT5qHk=")

        result = runner.invoke(decode_file, ["password.txt"])
        assert result.exit_code == 1
        assert "Aborted" in result.output.strip()


def test_encoding_decode_file_value_error(monkeypatch):
    from jans.pycloudlib.cli.encoding import decode_file

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY")

        with open("salt.txt", "w") as f:
            f.write("7MEDWVFAG3DmakHRyjMqp5EE")

        result = runner.invoke(
            decode_file,
            ["password.txt", "--salt-file", "salt.txt"]
        )
        assert result.exit_code == 1
        assert "Error:" in result.output.strip()


def test_encoding_decode_string_salt_literal():
    from jans.pycloudlib.cli.encoding import decode_string

    salt = "7MEDWVFAG3DmakHRyjMqp5EE"

    runner = CliRunner()
    result = runner.invoke(
        decode_string,
        ["fHL54sT5qHk=", "--salt-literal", salt],
    )
    assert result.exit_code == 0
    assert result.output.strip() == "secret"


def test_encoding_decode_string_salt_file():
    from jans.pycloudlib.cli.encoding import decode_string

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("salt.txt", "w") as f:
            f.write("7MEDWVFAG3DmakHRyjMqp5EE")

        result = runner.invoke(
            decode_string,
            ["fHL54sT5qHk=", "--salt-file", "salt.txt"],
        )
        assert result.exit_code == 0
        assert result.output.strip() == "secret"


def test_encoding_decode_string_salt(monkeypatch, gmanager):
    from jans.pycloudlib.cli.encoding import decode_string

    monkeypatch.setattr(
        "jans.pycloudlib.manager.Manager",
        lambda: gmanager,
    )

    runner = CliRunner()
    result = runner.invoke(decode_string, ["fHL54sT5qHk="])
    print(result.output)
    assert result.exit_code == 0
    assert result.output.strip() == "secret"


def test_encoding_decode_string_no_salt(monkeypatch, gmanager):
    from jans.pycloudlib.cli.encoding import decode_string

    monkeypatch.setattr(
        "jans.pycloudlib.manager.get_manager",
        lambda: gmanager,
    )

    runner = CliRunner()
    result = runner.invoke(decode_string, ["fHL54sT5qHk="])
    assert result.exit_code == 1
    assert "Aborted" in result.output.strip()


def test_encoding_decode_string_value_error():
    from jans.pycloudlib.cli.encoding import decode_string

    runner = CliRunner()
    with runner.isolated_filesystem():
        with open("password.txt", "w") as f:
            f.write("4rl2tJEQFkY")

        with open("salt.txt", "w") as f:
            f.write("7MEDWVFAG3DmakHRyjMqp5EE")

        result = runner.invoke(
            decode_string,
            ["password.txt", "--salt-file", "salt.txt"]
        )
        assert result.exit_code == 1
        assert "Error:" in result.output.strip()
