expect <<EOF
spawn ssh-keygen  -t rsa -P '' -f ~/.ssh/id_rsa
expect { "Overwrite (y/n)?" {send "y\r"} }
spawn ssh-copy-id -f -i "$HOME/.ssh/id_rsa.pub" ${USERNAME}@${HOST}
expect {
  "yes/no" { send "yes\n";exp_continue }
  "password:" { send "${PASSWD}\r" }

}
expect eof
EOF
